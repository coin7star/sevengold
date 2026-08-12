/**
 * Cloud Functions untuk push notification sinyal Premium.
 *
 * Alur:
 * Admin -> Firestore /signals -> Cloud Function -> FCM topic premium_signals
 *
 * Topic hanya berisi device yang sedang Premium aktif.
 */
const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getMessaging } = require("firebase-admin/messaging");
const { getFirestore } = require("firebase-admin/firestore");

initializeApp();

const TOPIC = "premium_signals";

function typeLabel(type) {
  return String(type || "BUY").toUpperCase() === "SELL" ? "SELL" : "BUY";
}

function signalBody(data) {
  const type = typeLabel(data?.type);
  const pair = data?.pair || "XAUUSD";
  const entry = data?.entry ?? "-";
  const tp = data?.tp ?? "-";
  const sl = data?.sl ?? "-";
  return `${type} ${pair} @ ${entry} | TP: ${tp} | SL: ${sl}`;
}

function telegramEventEnabled(user, eventName) {
  const events = Array.isArray(user?.telegramNotificationEvents)
    ? user.telegramNotificationEvents.map((v) => String(v))
    : ["SIGNAL_CREATED", "TP_HIT", "SL_HIT", "BE", "CANCELLED"];
  return events.includes(eventName);
}

function telegramSignalMessage(data, eventName) {
  const type = typeLabel(data?.type);
  const pair = data?.pair || "XAUUSD";
  const entry = data?.entry ?? "-";
  const tp = data?.tp ?? "-";
  const sl = data?.sl ?? "-";
  const note = data?.note ? `\n📝 ${data.note}` : "";

  const headers = {
    SIGNAL_CREATED: "📢 SIGNAL BARU",
    SIGNAL_ACTIVE: "📢 SIGNAL AKTIF",
    TP_HIT: "🎯 TP HIT — PROFIT",
    SL_HIT: "🛑 SL HIT",
    BE: "⚖️ BREAK EVEN",
    CANCELLED: "❌ SIGNAL DIBATALKAN",
  };

  return `${headers[eventName] || "📡 SIGNAL UPDATE"}

${type === "BUY" ? "🟢" : "🔴"} ${pair}
📈 ${type}

Entry: ${entry}
TP: ${tp}
SL: ${sl}${note}

⚡ SevenGold Premium`;
}

async function sendTelegramSignalNotifications(data, eventName) {
  const botToken = String(process.env.TELEGRAM_BOT_TOKEN || "").trim();
  if (!botToken) {
    console.warn("[TelegramPush] TELEGRAM_BOT_TOKEN is not configured; skipping Telegram notification.");
    return { sent: 0, failed: 0, skipped: 0 };
  }

  const db = getFirestore();
  const now = Date.now();
  const snapshot = await db.collection("users")
    .where("role", "==", "PREMIUM")
    .get();

  let sent = 0;
  let failed = 0;
  let skipped = 0;
  const message = telegramSignalMessage(data, eventName);

  const jobs = [];
  snapshot.forEach((doc) => {
    const user = doc.data() || {};
    const expiry = Number(user.premiumExpiryMillis || 0);
    const chatId = String(user.telegramChatId || "").trim();

    if (!chatId || (expiry > 0 && expiry <= now) || !telegramEventEnabled(user, eventName)) {
      skipped++;
      return;
    }

    jobs.push((async () => {
      try {
        const response = await fetch(`https://api.telegram.org/bot${botToken}/sendMessage`, {
          method: "POST",
          headers: { "content-type": "application/json" },
          body: JSON.stringify({
            chat_id: chatId,
            text: message,
            disable_web_page_preview: true,
          }),
        });
        if (!response.ok) {
          failed++;
          console.error(`[TelegramPush] ${eventName} failed for user ${doc.id}: HTTP ${response.status}`);
          return;
        }
        const result = await response.json().catch(() => null);
        if (!result?.ok) {
          failed++;
          console.error(`[TelegramPush] ${eventName} failed for user ${doc.id}: Telegram API error`);
          return;
        }
        sent++;
      } catch (error) {
        failed++;
        console.error(`[TelegramPush] ${eventName} exception for user ${doc.id}:`, error?.message);
      }
    })());
  });

  // Send concurrently so a large premium list does not delay the Firestore trigger unnecessarily.
  await Promise.all(jobs);
  console.log(`[TelegramPush] ${eventName}: sent=${sent}, skipped=${skipped}, failed=${failed}`);
  return { sent, failed, skipped };
}


async function sendPremiumNotification(title, body, eventName) {
  try {
    const response = await getMessaging().send({
      topic: TOPIC,
      notification: { title, body },
      android: {
        priority: "high",
        notification: {
          channelId: "premium_signals",
          priority: "high",
          sound: "default",
        },
      },
      data: {
        event: eventName,
        title,
        body,
      },
    });
    console.log(`[PremiumPush] ${eventName} sent: ${response}`);
    return response;
  } catch (error) {
    console.error(`[PremiumPush] ${eventName} failed:`, error);
    throw error;
  }
}

/**
 * Publish membuat dokumen baru dengan status ACTIVE.
 */
exports.onSignalCreated = onDocumentCreated("signals/{signalId}", async (event) => {
  const data = event.data?.data();
  if (!data) return;

  await Promise.allSettled([
    sendPremiumNotification(
      "📢 Sinyal Baru XAUUSD",
      signalBody(data),
      "SIGNAL_CREATED"
    ),
    sendTelegramSignalNotifications(data, "SIGNAL_CREATED"),
  ]);
});

/**
 * Perubahan status:
 * ACTIVE -> BE / TP_HIT / SL_HIT / CANCELLED
 * dan juga perubahan apa pun -> ACTIVE dianggap sebagai sinyal yang baru diaktifkan.
 */
exports.onSignalUpdated = onDocumentUpdated("signals/{signalId}", async (event) => {
  const before = event.data?.before?.data();
  const after = event.data?.after?.data();
  if (!before || !after) return;

  const beforeStatus = String(before.status || "");
  const afterStatus = String(after.status || "");
  if (beforeStatus === afterStatus) return;

  if (afterStatus === "ACTIVE") {
    await Promise.allSettled([
      sendPremiumNotification(
        "📢 Sinyal Aktif XAUUSD",
        signalBody(after),
        "SIGNAL_ACTIVE"
      ),
      sendTelegramSignalNotifications(after, "SIGNAL_ACTIVE"),
    ]);
    return;
  }

  const titles = {
    TP_HIT: "🎯 TP HIT — Profit!",
    SL_HIT: "🛑 Kena SL",
    BE: "⚖️ Sinyal di-set Break Even",
    CANCELLED: "❌ Sinyal Dibatalkan",
  };
  const title = titles[afterStatus];
  if (!title) return;

  await Promise.allSettled([
    sendPremiumNotification(title, signalBody(after), afterStatus),
    sendTelegramSignalNotifications(after, afterStatus),
  ]);
});

/** Referral reward. */
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
    tx.update(referredRef, { referralRewardGranted: true, referralRewardGrantedAt: now });
  });
});

/** Approval langganan manual. */
exports.onSubscriptionOrderUpdated = onDocumentUpdated("subscriptionOrders/{orderId}", async (event) => {
  const before = event.data?.before?.data();
  const after = event.data?.after?.data();
  if (!before || !after) return;
  if (before.status === after.status) return;
  if (after.status !== "APPROVED") return;
  if (after.approvalProcessedAt) return;

  const db = getFirestore();
  const orderRef = db.collection("subscriptionOrders").doc(event.params.orderId);
  const userRef = db.collection("users").doc(after.uid);
  const now = Date.now();

  const packageSnap = await db.collection("appSettings").doc("subscriptionPackages").get();
  const packageList = packageSnap.exists && Array.isArray(packageSnap.data()?.packages)
    ? packageSnap.data().packages
    : [
      { id: "starter", name: "Starter", price: 10000, durationDays: 7, enabled: true },
      { id: "basic", name: "Basic", price: 15000, durationDays: 10, enabled: true },
      { id: "pro", name: "Pro", price: 30000, durationDays: 20, enabled: true },
      { id: "vip", name: "VIP", price: 50000, durationDays: 30, enabled: true },
    ];
  const configuredPackage = packageList.find((pkg) =>
    pkg && pkg.id === after.packageId && pkg.enabled !== false &&
    Number(pkg.price) === Number(after.price) &&
    Number(pkg.durationDays) === Number(after.durationDays)
  );
  if (!configuredPackage) return;

  const durationDays = Math.max(1, Math.min(3650, Number(after.durationDays || 0)));
  const durationMillis = durationDays * 24 * 60 * 60 * 1000;

  await db.runTransaction(async (tx) => {
    const [orderSnap, userSnap] = await tx.getAll(orderRef, userRef);
    if (!orderSnap.exists || !userSnap.exists) return;
    const order = orderSnap.data();
    const user = userSnap.data();
    if (order.approvalProcessedAt) return;

    const currentExpiry = Number(user.premiumExpiryMillis || 0);
    const base = currentExpiry > now ? currentExpiry : now;
    const newExpiry = base + durationMillis;

    tx.update(userRef, {
      role: user.role === "ADMIN" ? "ADMIN" : "PREMIUM",
      premiumExpiryMillis: user.role === "ADMIN" ? (user.premiumExpiryMillis || null) : newExpiry,
      lastSubscriptionActivatedAt: now,
      lastSubscriptionOrderId: event.params.orderId,
    });
    tx.update(orderRef, {
      approvalProcessedAt: now,
      processedExpiryMillis: newExpiry,
    });
  });
});
