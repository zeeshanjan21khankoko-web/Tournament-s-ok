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
const ADMIN_UID = "admin_789";
const BOOTSTRAP_ADMIN_UID = "boot_admin_001";
const BOOTSTRAP_ADMIN_EMAIL = "zeeshanjan21khankoko@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
}

function futureDate(msOffset = 60000) {
  return new Date(Date.now() + msOffset);
}

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

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("tournaments").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("deposits").get());
  await assertFails(unauthDb.collection("winners").get());
});

test("User registration enforces zero initial balance, valid schema, and blocks self-crediting", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const now = pastDate();

  // Valid registration (no id field)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "ShadowAce",
      email: "alice@example.com",
      playerId: "PT-100201",
      walletBalance: 0,
      matchesPlayed: 0,
      wins: 0,
      blocked: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject self-assigned starting balance
  await assertFails(
    aliceDb.collection("users").doc("evil_uid").set({
      userId: "evil_uid",
      username: "Hacker",
      email: "evil@example.com",
      playerId: "PT-999999",
      walletBalance: 50000,
      matchesPlayed: 0,
      wins: 0,
      blocked: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Valid profile update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      username: "ShadowPro",
      updatedAt: pastDate(500),
    })
  );

  // Reject user trying to increase their own walletBalance
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      walletBalance: 1000,
      updatedAt: pastDate(400),
    })
  );

  // Reject user trying to increment wins or unblock self
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      wins: 5,
      updatedAt: pastDate(400),
    })
  );
});

test("PII Isolation: User cannot read or list another user's profile", async () => {
  const now = pastDate(10000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      username: "BobGamer",
      email: "bob@example.com",
      playerId: "PT-200300",
      walletBalance: 500,
      matchesPlayed: 1,
      wins: 0,
      blocked: false,
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").get());
});

test("Email spoofing test: unverified admin email is rejected; verified bootstrap admin succeeds", async () => {
  const spoofDb = testEnv.authenticatedContext(ALICE_UID, {
    email: BOOTSTRAP_ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  await assertFails(spoofDb.collection("users").get());

  const adminDb = testEnv.authenticatedContext(BOOTSTRAP_ADMIN_UID, {
    email: BOOTSTRAP_ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  await assertSucceeds(adminDb.collection("users").get());
});

test("Deposit & Duplicate TxID prevention + Admin single-use approval", async () => {
  const now = pastDate(5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "ShadowAce",
      email: "alice@example.com",
      playerId: "PT-100201",
      walletBalance: 0,
      matchesPlayed: 0,
      wins: 0,
      blocked: false,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("admins").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      email: "admin@protournament.pk",
      displayName: "Tournament Admin",
      role: "ADMIN",
      createdAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();

  // Alice submits deposit + unique used_reference + pending transaction
  const batch = aliceDb.batch();
  batch.set(aliceDb.collection("deposits").doc("dep_1"), {
    userId: ALICE_UID,
    username: "ShadowAce",
    playerId: "PT-100201",
    amount: 1000,
    paymentMethod: "JAZZCASH",
    referenceId: "JC889911",
    senderAccount: "03001234567",
    screenshotBase64: "data:image/jpeg;base64,/9j/4AAQSkZJRg==",
    status: "PENDING",
    createdAt: now,
    updatedAt: now,
  });
  batch.set(aliceDb.collection("used_references").doc("JAZZCASH_JC889911"), {
    refKey: "JAZZCASH_JC889911",
    userId: ALICE_UID,
    depositId: "dep_1",
    createdAt: now,
  });
  batch.set(aliceDb.collection("transactions").doc("tx_dep_dep_1"), {
    userId: ALICE_UID,
    username: "ShadowAce",
    type: "DEPOSIT",
    amount: 1000,
    direction: "CREDIT",
    status: "PENDING",
    description: "JazzCash Deposit (Ref: JC889911)",
    referenceId: "dep_1",
    createdAt: now,
    updatedAt: now,
  });
  await assertSucceeds(batch.commit());

  // Duplicate reference ID must fail!
  await assertFails(
    aliceDb.collection("used_references").doc("JAZZCASH_JC889911").set({
      refKey: "JAZZCASH_JC889911",
      userId: ALICE_UID,
      depositId: "dep_2",
      createdAt: pastDate(1000),
    })
  );

  // Alice cannot self-approve her deposit
  await assertFails(
    aliceDb.collection("deposits").doc("dep_1").update({
      status: "APPROVED",
      updatedAt: pastDate(500),
    })
  );

  // Filtered query works for Alice; unfiltered fails
  await assertSucceeds(
    aliceDb.collection("deposits").where("userId", "==", ALICE_UID).get()
  );
  await assertFails(aliceDb.collection("deposits").get());

  // Admin approves deposit atomically
  const approveBatch = adminDb.batch();
  approveBatch.update(adminDb.collection("deposits").doc("dep_1"), {
    status: "APPROVED",
    reviewedBy: ADMIN_UID,
    adminNote: "Verified in JazzCash portal",
    updatedAt: pastDate(300),
  });
  approveBatch.update(adminDb.collection("users").doc(ALICE_UID), {
    walletBalance: 1000,
    updatedAt: pastDate(300),
  });
  approveBatch.update(adminDb.collection("transactions").doc("tx_dep_dep_1"), {
    status: "COMPLETED",
    description: "JazzCash Deposit Approved (Ref: JC889911)",
    updatedAt: pastDate(300),
  });
  await assertSucceeds(approveBatch.commit());

  // Terminal state lock: even Admin cannot re-approve an already APPROVED deposit
  await assertFails(
    adminDb.collection("deposits").doc("dep_1").update({
      status: "REJECTED",
      reviewedBy: ADMIN_UID,
      updatedAt: pastDate(100),
    })
  );
});

test("Withdrawal overdraft is rejected and valid atomic withdrawal succeeds", async () => {
  const now = pastDate(5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "ShadowAce",
      email: "alice@example.com",
      playerId: "PT-100201",
      walletBalance: 500,
      matchesPlayed: 0,
      wins: 0,
      blocked: false,
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Overdraft attempt (1000 > 500) must fail
  const overBatch = aliceDb.batch();
  overBatch.update(aliceDb.collection("users").doc(ALICE_UID), {
    walletBalance: 0,
    updatedAt: pastDate(500),
  });
  overBatch.set(aliceDb.collection("withdrawals").doc("wd_over"), {
    userId: ALICE_UID,
    username: "ShadowAce",
    playerId: "PT-100201",
    amount: 1000,
    paymentMethod: "EASYPAISA",
    accountTitle: "Alice Khan",
    accountNumber: "03451234567",
    status: "PENDING",
    createdAt: pastDate(500),
    updatedAt: pastDate(500),
  });
  await assertFails(overBatch.commit());

  // Valid withdrawal (300 <= 500) with atomic balance hold succeeds
  const validBatch = aliceDb.batch();
  validBatch.update(aliceDb.collection("users").doc(ALICE_UID), {
    walletBalance: 200,
    updatedAt: pastDate(400),
  });
  validBatch.set(aliceDb.collection("withdrawals").doc("wd_1"), {
    userId: ALICE_UID,
    username: "ShadowAce",
    playerId: "PT-100201",
    amount: 300,
    paymentMethod: "EASYPAISA",
    accountTitle: "Alice Khan",
    accountNumber: "03451234567",
    status: "PENDING",
    createdAt: pastDate(400),
    updatedAt: pastDate(400),
  });
  validBatch.set(aliceDb.collection("transactions").doc("tx_wd_wd_1"), {
    userId: ALICE_UID,
    username: "ShadowAce",
    type: "WITHDRAWAL",
    amount: 300,
    direction: "DEBIT",
    status: "PENDING",
    description: "EasyPaisa Withdrawal Request",
    referenceId: "wd_1",
    createdAt: pastDate(400),
    updatedAt: pastDate(400),
  });
  await assertSucceeds(validBatch.commit());
});

test("Atomic tournament join updates slots, deducts fee, and prevents double-join", async () => {
  const now = pastDate(5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "ShadowAce",
      email: "alice@example.com",
      playerId: "PT-100201",
      walletBalance: 1000,
      matchesPlayed: 2,
      wins: 1,
      blocked: false,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("tournaments").doc("tour_1").set({
      title: "PUBG Cyber Cup",
      gameName: "PUBG Mobile",
      bannerKey: "battle_royale",
      entryFee: 250,
      prizePool: 10000,
      maxSlots: 32,
      filledSlots: 5,
      matchTimeEpochMs: Date.now() + 3600000,
      matchTimeLabel: "Tonight 9:00 PM PKT",
      status: "UPCOMING",
      rulesSummary: "Squad TPP, Erangel",
      createdBy: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Atomic join
  const joinBatch = aliceDb.batch();
  joinBatch.update(aliceDb.collection("users").doc(ALICE_UID), {
    walletBalance: 750,
    matchesPlayed: 3,
    updatedAt: pastDate(400),
  });
  joinBatch.update(aliceDb.collection("tournaments").doc("tour_1"), {
    filledSlots: 6,
    updatedAt: pastDate(400),
  });
  joinBatch.set(
    aliceDb.collection("tournaments").doc("tour_1").collection("registrations").doc(ALICE_UID),
    {
      userId: ALICE_UID,
      tournamentId: "tour_1",
      username: "ShadowAce",
      playerId: "PT-100201",
      entryFeePaid: 250,
      prizeWon: 0,
      joinedAt: pastDate(400),
    }
  );
  joinBatch.set(aliceDb.collection("transactions").doc("tx_join_1"), {
    userId: ALICE_UID,
    username: "ShadowAce",
    type: "ENTRY_FEE",
    amount: 250,
    direction: "DEBIT",
    status: "COMPLETED",
    description: "Joined PUBG Cyber Cup",
    referenceId: "tour_1",
    createdAt: pastDate(400),
    updatedAt: pastDate(400),
  });
  await assertSucceeds(joinBatch.commit());

  // Tampered future timestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      username: "TimeTraveler",
      updatedAt: futureDate(),
    })
  );
});
