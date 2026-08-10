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
