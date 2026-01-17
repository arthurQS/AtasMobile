const crypto = require("crypto");
const admin = require("firebase-admin");
const functions = require("firebase-functions");

admin.initializeApp();

function hashPassword(password, salt) {
  return crypto
    .pbkdf2Sync(password, salt, 120000, 64, "sha512")
    .toString("hex");
}

function assertSignedIn(context) {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Auth required");
  }
}

function assertAdmin(context) {
  assertSignedIn(context);
  if (!context.auth.token || context.auth.token.admin !== true) {
    throw new functions.https.HttpsError("permission-denied", "Admin required");
  }
}

function normalizeWardCode(value) {
  return String(value || "").trim().toLowerCase();
}

function assertPayloadString(value, label) {
  if (typeof value !== "string" || value.trim().length === 0) {
    throw new functions.https.HttpsError(
      "invalid-argument",
      `${label} is required`
    );
  }
}

function sanitizeAgendaPayload(payload) {
  if (!payload || typeof payload !== "object" || Array.isArray(payload)) {
    throw new functions.https.HttpsError("invalid-argument", "Invalid payload");
  }
  assertPayloadString(payload.title, "title");
  assertPayloadString(payload.date, "date");
  if (!payload.data || typeof payload.data !== "object" || Array.isArray(payload.data)) {
    throw new functions.https.HttpsError("invalid-argument", "data is required");
  }
  const sanitized = {
    title: payload.title.trim(),
    date: payload.date.trim(),
    data: payload.data
  };
  if (payload.status) {
    const status = String(payload.status).trim();
    if (!["draft", "final"].includes(status)) {
      throw new functions.https.HttpsError("invalid-argument", "Invalid status");
    }
    sanitized.status = status;
  }
  return sanitized;
}

exports.createWard = functions.https.onCall(async (data, context) => {
  assertAdmin(context);
  const name = String(data.name || "").trim();
  const wardCode = normalizeWardCode(data.wardCode);
  const password = String(data.password || "").trim();
  if (!name || !wardCode || !password) {
    throw new functions.https.HttpsError("invalid-argument", "Missing fields");
  }
  const salt = crypto.randomBytes(16).toString("hex");
  const hash = hashPassword(password, salt);
  const wardRef = admin.firestore().collection("wards").doc(wardCode);
  const existing = await wardRef.get();
  if (existing.exists) {
    throw new functions.https.HttpsError("already-exists", "Ward already exists");
  }
  await wardRef.set({
    name,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    masterPasswordHash: hash,
    masterPasswordSalt: salt
  });
  return { wardId: wardCode };
});

exports.joinWard = functions.https.onCall(async (data, context) => {
  assertSignedIn(context);
  const wardCode = normalizeWardCode(data.wardCode);
  const password = String(data.password || "").trim();
  if (!wardCode || !password) {
    throw new functions.https.HttpsError("invalid-argument", "Missing fields");
  }

  const wardRef = admin.firestore().collection("wards").doc(wardCode);
  const wardSnap = await wardRef.get();
  if (!wardSnap.exists) {
    throw new functions.https.HttpsError("not-found", "Ward not found");
  }
  const wardData = wardSnap.data();
  const hash = hashPassword(password, wardData.masterPasswordSalt);
  const isValid = crypto.timingSafeEqual(
    Buffer.from(hash, "hex"),
    Buffer.from(wardData.masterPasswordHash, "hex")
  );
  if (!isValid) {
    throw new functions.https.HttpsError("permission-denied", "Invalid password");
  }

  const uid = context.auth.uid;
  const memberRef = wardRef.collection("members").doc(uid);
  await memberRef.set(
    {
      role: "editor",
      joinedAt: admin.firestore.FieldValue.serverTimestamp(),
      lastActiveAt: admin.firestore.FieldValue.serverTimestamp()
    },
    { merge: true }
  );
  return { wardId: wardCode, role: "editor" };
});

exports.updateAgenda = functions.https.onCall(async (data, context) => {
  assertSignedIn(context);
  const wardId = normalizeWardCode(data.wardId);
  const agendaId = String(data.agendaId || "").trim();
  const payload = sanitizeAgendaPayload(data.payload || {});
  const expectedVersion = Number.isFinite(data.expectedVersion)
    ? Number(data.expectedVersion)
    : null;
  if (!wardId || !agendaId) {
    throw new functions.https.HttpsError("invalid-argument", "Missing fields");
  }

  const uid = context.auth.uid;
  const memberRef = admin
    .firestore()
    .collection("wards")
    .doc(wardId)
    .collection("members")
    .doc(uid);
  const agendaRef = admin
    .firestore()
    .collection("wards")
    .doc(wardId)
    .collection("agendas")
    .doc(agendaId);

  const result = await admin.firestore().runTransaction(async (tx) => {
    const memberSnap = await tx.get(memberRef);
    if (!memberSnap.exists) {
      throw new functions.https.HttpsError(
        "permission-denied",
        "User is not a member"
      );
    }
    const role = memberSnap.data().role;
    if (!["admin", "editor"].includes(role)) {
      throw new functions.https.HttpsError("permission-denied", "Role not allowed");
    }
    const snap = await tx.get(agendaRef);
    if (!snap.exists) {
      if (expectedVersion && expectedVersion > 0) {
        throw new functions.https.HttpsError(
          "failed-precondition",
          "Version mismatch"
        );
      }
      const version = 1;
      tx.set(agendaRef, {
        ...payload,
        version,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedBy: uid
      });
      tx.update(memberRef, {
        lastActiveAt: admin.firestore.FieldValue.serverTimestamp()
      });
      return { version };
    }

    const current = snap.data().version || 0;
    if (expectedVersion !== null && expectedVersion !== current) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "Version mismatch"
      );
    }
    const version = current + 1;
    tx.set(
      agendaRef,
      {
        ...payload,
        version,
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedBy: uid
      },
      { merge: true }
    );
    tx.update(memberRef, {
      lastActiveAt: admin.firestore.FieldValue.serverTimestamp()
    });
    return { version };
  });

  return result;
});
