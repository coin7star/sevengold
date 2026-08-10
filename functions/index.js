/**
 * Cloud Function ini yang beneran "mendengar" perubahan di Firestore lalu ngirim
 * push notification ke semua HP user PREMIUM (via topic "premium_signals").
 *
 * Kenapa harus lewat sini (server), bukan langsung dari app Android?
 * Karena notifikasi harus tetap nyampe walau HP user lain lagi nutup/gak buka app-nya.
 * Client Android cuma bisa NERIMA notif, gak bisa ngirim notif ke HP orang lain.
 */

const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getMessaging } = require("firebase-admin/messaging");
const { getFirestore } = require("firebase-admin/firestore");

initializeApp();

const TOPIC = "premium_signals";

function typeLabel(type) {
  return type === "SELL" ? "SELL" : "BUY";
}

/** Sinyal baru dipublish admin -> notif "Sinyal Baru XAUUSD". */
exports.onSignalCreated = onDocumentCreated("signals/{signalId}", async (event) => {
  const data = event.data?.data();
  if (!data) return;

  await getMessaging().send({
    topic: TOPIC,
    notification: {
      title: "📢 Sinyal Baru XAUUSD",
      body: `${typeLabel(data.type)} @ ${data.entry} | TP: ${data.tp} | SL: ${data.sl}`,
    },
  });
});

/** Status sinyal berubah (TP_HIT / SL_HIT / BE / CANCELLED) -> notif sesuai hasilnya. */
exports.onSignalUpdated = onDocumentUpdated("signals/{signalId}", async (event) => {
  const before = event.data?.before?.data();
  const after = event.data?.after?.data();
  if (!before || !after) return;
  if (before.status === after.status) return; // cuma kirim kalau status BERUBAH

  const titles = {
    TP_HIT: "🎯 TP HIT — Profit!",
    SL_HIT: "🛑 Kena SL",
    BE: "⚖️ Sinyal di-set Break Even",
    CANCELLED: "❌ Sinyal Dibatalkan",
  };
  const title = titles[after.status];
  if (!title) return; // status ACTIVE atau status lain gak perlu notif

  await getMessaging().send({
    topic: TOPIC,
    notification: {
      title,
      body: `${typeLabel(after.type)} XAUUSD @ ${after.entry}`,
    },
  });
});



/**
 * Saat teman yang punya referredByUid benar-benar mengaktifkan subscription,
 * referrer mendapat bonus 2 hari Premium. Reward hanya diberikan sekali per teman.
 * lastSubscriptionActivatedAt ditulis oleh transaction redeem subscription di Android.
 */
exports.onReferralSubscriptionActivated = onDocumentUpdated("users/{userId}", async (event) => {
  const before = event.data?.before?.data();
  const after = event.data?.after?.data();
  if (!before || !after) return;

  const activationChanged = after.lastSubscriptionActivatedAt &&
    after.lastSubscriptionActivatedAt !== before.lastSubscriptionActivatedAt;
  if (!activationChanged || after.referralRewardGranted === true) return;

  const referrerUid = after.referredByUid;
  if (!referrerUid || referrerUid === event.params.userId) return;

  const db = getFirestore();
  const referredRef = db.collection("users").doc(event.params.userId);
  const referrerRef = db.collection("users").doc(referrerUid);
  const settingsSnap = await db.collection("appSettings").doc("referral").get();
  const settings = settingsSnap.exists ? settingsSnap.data() : {};
  const referralEnabled = settings.enabled !== false;
  const rewardDays = Math.max(0, Math.min(365, Number(settings.rewardPremiumDays ?? 2)));
  if (!referralEnabled || rewardDays <= 0) return;

  const now = Date.now();
  const bonusMillis = rewardDays * 24 * 60 * 60 * 1000;

  await db.runTransaction(async (tx) => {
    const [referredSnap, referrerSnap] = await tx.getAll(referredRef, referrerRef);

    if (!referredSnap.exists || !referrerSnap.exists) return;
    const referred = referredSnap.data();
    const referrer = referrerSnap.data();

    // Idempotency: kalau retry/function terpicu ulang, jangan kasih bonus kedua.
    if (referred.referralRewardGranted === true) return;

    const currentExpiry = Number(referrer.premiumExpiryMillis || 0);
    const base = currentExpiry > now ? currentExpiry : now;
    const newExpiry = base + bonusMillis;

    tx.update(referrerRef, {
      role: referrer.role === "ADMIN" ? "ADMIN" : "PREMIUM",
      premiumExpiryMillis: referrer.role === "ADMIN" ? (referrer.premiumExpiryMillis || null) : newExpiry,
      referralSuccessfulCount: Number(referrer.referralSuccessfulCount || 0) + 1,
      referralRewardDaysEarned: Number(referrer.referralRewardDaysEarned || 0) + rewardDays,
      lastReferralRewardAt: now,
    });

    tx.update(referredRef, {
      referralRewardGranted: true,
      referralRewardGrantedAt: now,
    });
  });
});
