const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read assignments", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("assignments").get());
});

test("Authenticated user: cannot read another user's assignments", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("assignments").doc("task_1").set({
      userId: ALICE_UID,
      title: "Math Homework",
      courseName: "Calculus",
      courseColorHex: "#3B82F6",
      dueTimestamp: new Date(),
      priority: "HIGH",
      status: "NOT_STARTED",
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("assignments").doc("task_1").get());
});

test("Authenticated user: can read and write own assignments", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("assignments").doc("task_1").set({
      userId: ALICE_UID,
      title: "Physics Lab Report",
      courseName: "Physics 101",
      courseColorHex: "#10B981",
      dueTimestamp: now,
      priority: "HIGH",
      status: "NOT_STARTED",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("assignments").doc("task_1").get()
  );
});

test("Schema validation: rejects invalid priority", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("assignments").doc("task_2").set({
      userId: ALICE_UID,
      title: "History Essay",
      courseName: "History",
      courseColorHex: "#EF4444",
      dueTimestamp: now,
      priority: "URGENT_CRITICAL", // invalid priority
      status: "NOT_STARTED",
      createdAt: now,
      updatedAt: now,
    })
  );
});
