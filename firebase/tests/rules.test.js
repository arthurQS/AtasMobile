const fs = require("fs");
const path = require("path");
const {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment
} = require("@firebase/rules-unit-testing");
const { Timestamp } = require("firebase/firestore");

async function run() {
  const rulesPath = path.join(__dirname, "..", "firestore.rules");
  const rules = fs.readFileSync(rulesPath, "utf8");
  const testEnv = await initializeTestEnvironment({
    projectId: "demo-atasmobile",
    firestore: { rules }
  });

  const wardId = "ward-1";
  const agendaId = "1";
  const memberUid = "user-1";

  const anonDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(anonDb.doc(`wards/${wardId}`).get());
  await assertFails(anonDb.doc(`wards/${wardId}/agendas/${agendaId}`).get());

  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();
    await adminDb.doc(`wards/${wardId}`).set({
      name: "Ala Central",
      createdAt: Timestamp.now(),
      masterPasswordHash: "hash",
      masterPasswordSalt: "salt"
    });
    await adminDb.doc(`wards/${wardId}/members/${memberUid}`).set({
      role: "editor",
      joinedAt: Timestamp.now(),
      displayName: "Usuario"
    });
    await adminDb.doc(`wards/${wardId}/agendas/${agendaId}`).set({
      title: "Agenda",
      date: "2026-01-16",
      data: { teste: true },
      version: 1,
      updatedAt: Timestamp.now(),
      updatedBy: memberUid,
      status: "draft"
    });
  });

  const memberDb = testEnv.authenticatedContext(memberUid).firestore();
  await assertSucceeds(memberDb.doc(`wards/${wardId}/members/${memberUid}`).get());

  await assertFails(
    memberDb.doc(`wards/${wardId}/members/${memberUid}`).set({
      role: "admin",
      joinedAt: Timestamp.now()
    })
  );

  await assertSucceeds(memberDb.doc(`wards/${wardId}/agendas/${agendaId}`).get());

  await assertFails(
    memberDb.doc(`wards/${wardId}/agendas/${agendaId}`).set({
      title: "Agenda",
      date: "2026-01-16",
      data: { teste: true },
      version: 3,
      updatedAt: Timestamp.now(),
      updatedBy: memberUid,
      status: "draft"
    })
  );

  await assertSucceeds(
    memberDb.doc(`wards/${wardId}/agendas/${agendaId}`).set({
      title: "Agenda Atualizada",
      date: "2026-01-16",
      data: { teste: true },
      version: 2,
      updatedAt: Timestamp.now(),
      updatedBy: memberUid,
      status: "draft"
    })
  );

  await testEnv.cleanup();
}

run()
  .then(() => {
    console.log("Firestore rules OK");
    process.exit(0);
  })
  .catch((error) => {
    console.error(error);
    process.exit(1);
  });
