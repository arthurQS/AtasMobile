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

exports.createWard = functions.https.onCall(async (data, context) => {
  assertAdmin(context);
  const name = String(data.name || "").trim();
  const wardCode = String(data.wardCode || "").trim();
  const password = String(data.password || "").trim();
  if (!name || !wardCode || !password) {
    throw new functions.https.HttpsError("invalid-argument", "Missing fields");
  }
  const salt = crypto.randomBytes(16).toString("hex");
  const hash = hashPassword(password, salt);
  const wardRef = admin.firestore().collection("wards").doc(wardCode);
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
  const wardCode = String(data.wardCode || "").trim();
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
  const wardId = String(data.wardId || "").trim();
  const agendaId = String(data.agendaId || "").trim();
  const payload = data.payload || {};
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
    return { version };
  });

  return result;
});
