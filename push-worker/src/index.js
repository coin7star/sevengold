import { importX509, importPKCS8, jwtVerify, SignJWT } from "jose";

const TOPIC = "premium_signals";
const TOKEN_URL = "https://oauth2.googleapis.com/token";
const CERTS_URL = "https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com";
const ALLOWED_EVENTS = new Set([
  "SIGNAL_CREATED",
  "SIGNAL_ACTIVE",
  "TP_HIT",
  "SL_HIT",
  "BE",
  "CANCELLED",
]);

let cachedGoogleCerts = null;
let cachedGoogleCertsExpiresAt = 0;
let cachedAccessToken = null;
let cachedAccessTokenExpiresAt = 0;
const adminTestSignalLastAt = new Map();

function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" },
  });
}

function cleanPem(value) {
  return String(value || "").replace(/\\n/g, "\n");
}

async function getFirebaseCerts() {
  const now = Date.now();
  if (cachedGoogleCerts && now < cachedGoogleCertsExpiresAt) return cachedGoogleCerts;

  const response = await fetch(CERTS_URL);
  if (!response.ok) throw new Error(`Firebase public cert fetch failed: HTTP ${response.status}`);
  const certs = await response.json();
  const maxAge = Number((response.headers.get("cache-control") || "").match(/max-age=(\d+)/)?.[1] || 3600);
  cachedGoogleCerts = certs;
  cachedGoogleCertsExpiresAt = now + Math.max(60, maxAge - 30) * 1000;
  return certs;
}

async function verifyFirebaseIdToken(token, projectId) {
  const headerPart = token.split(".")[0];
  if (!headerPart) throw new Error("Invalid Firebase ID token");
  const header = JSON.parse(atob(headerPart.replace(/-/g, "+").replace(/_/g, "/")));
  if (header.alg !== "RS256" || !header.kid) throw new Error("Unsupported Firebase token");

  const certs = await getFirebaseCerts();
  const certPem = certs[header.kid];
  if (!certPem) throw new Error("Firebase token key not found");
  const publicKey = await importX509(certPem, "RS256");

  const { payload } = await jwtVerify(token, publicKey, {
    algorithms: ["RS256"],
    issuer: `https://securetoken.google.com/${projectId}`,
    audience: projectId,
  });

  if (!payload.sub || typeof payload.sub !== "string") throw new Error("Firebase token has no UID");
  return payload;
}

function getServiceAccount(env) {
  const raw = env.FIREBASE_SERVICE_ACCOUNT_JSON;
  if (!raw) throw new Error("FIREBASE_SERVICE_ACCOUNT_JSON is not configured");
  const account = JSON.parse(raw);
  if (!account.client_email || !account.private_key || !account.project_id) {
    throw new Error("Invalid Firebase service account JSON");
  }
  return account;
}

async function getGoogleAccessToken(env, account) {
  const now = Date.now();
  if (cachedAccessToken && now < cachedAccessTokenExpiresAt) return cachedAccessToken;

  const privateKey = await importPKCS8(cleanPem(account.private_key), "RS256");
  const assertion = await new SignJWT({
    scope: "https://www.googleapis.com/auth/cloud-platform",
  })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" })
    .setIssuer(account.client_email)
    .setSubject(account.client_email)
    .setAudience(TOKEN_URL)
    .setIssuedAt()
    .setExpirationTime("1h")
    .sign(privateKey);

  const tokenResponse = await fetch(TOKEN_URL, {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });
  const tokenJson = await tokenResponse.json();
  if (!tokenResponse.ok || !tokenJson.access_token) {
    throw new Error(`Google OAuth failed: HTTP ${tokenResponse.status}`);
  }

  cachedAccessToken = tokenJson.access_token;
  cachedAccessTokenExpiresAt = now + Math.max(60, Number(tokenJson.expires_in || 3600) - 120) * 1000;
  return cachedAccessToken;
}

function titleFor(event) {
  return {
    SIGNAL_CREATED: "📢 Sinyal Baru XAUUSD",
    SIGNAL_ACTIVE: "📢 Sinyal Aktif XAUUSD",
    TP_HIT: "🎯 TP HIT — Profit!",
    SL_HIT: "🛑 Kena SL",
    BE: "⚖️ Sinyal di-set Break Even",
    CANCELLED: "❌ Sinyal Dibatalkan",
  }[event] || "Update Sinyal";
}

function bodyFor(signal) {
  const type = String(signal.type || "BUY").toUpperCase() === "SELL" ? "SELL" : "BUY";
  const pair = String(signal.pair || "XAUUSD");
  return `${type} ${pair} @ ${signal.entry ?? "-"} | TP: ${signal.tp ?? "-"} | SL: ${signal.sl ?? "-"}`;
}


function firestoreBase(projectId) {
  return `https://firestore.googleapis.com/v1/projects/${encodeURIComponent(projectId)}/databases/(default)/documents`;
}

function firestoreValue(value) {
  if (value === null || value === undefined) return { nullValue: null };
  if (typeof value === "boolean") return { booleanValue: value };
  if (typeof value === "number" && Number.isInteger(value)) return { integerValue: String(value) };
  return { stringValue: String(value) };
}

async function firestoreRunQuery(env, projectId, accessToken, whereField, whereValue) {
  const response = await fetch(`${firestoreBase(projectId)}:runQuery`, {
    method: "POST",
    headers: { authorization: `Bearer ${accessToken}`, "content-type": "application/json" },
    body: JSON.stringify({
      structuredQuery: {
        from: [{ collectionId: "users" }],
        where: {
          fieldFilter: {
            field: { fieldPath: whereField },
            op: "EQUAL",
            value: firestoreValue(whereValue),
          },
        },
        limit: 200,
      },
    }),
  });
  if (!response.ok) throw new Error(`Firestore query failed: HTTP ${response.status}`);
  return await response.json();
}

/**
 * Sama seperti firestoreRunQuery tapi buat kombinasi role == PREMIUM DAN
 * premiumExpiryMillis dalam rentang [minMillis, maxMillis) — dipakai reminder H-1.
 * Butuh composite index (role ASC, premiumExpiryMillis ASC) di Firestore.
 */
async function firestoreRunRangeQuery(env, projectId, accessToken, minMillis, maxMillis) {
  const response = await fetch(`${firestoreBase(projectId)}:runQuery`, {
    method: "POST",
    headers: { authorization: `Bearer ${accessToken}`, "content-type": "application/json" },
    body: JSON.stringify({
      structuredQuery: {
        from: [{ collectionId: "users" }],
        where: {
          compositeFilter: {
            op: "AND",
            filters: [
              {
                fieldFilter: {
                  field: { fieldPath: "role" },
                  op: "EQUAL",
                  value: firestoreValue("PREMIUM"),
                },
              },
              {
                fieldFilter: {
                  field: { fieldPath: "premiumExpiryMillis" },
                  op: "GREATER_THAN_OR_EQUAL",
                  value: firestoreValue(minMillis),
                },
              },
              {
                fieldFilter: {
                  field: { fieldPath: "premiumExpiryMillis" },
                  op: "LESS_THAN",
                  value: firestoreValue(maxMillis),
                },
              },
            ],
          },
        },
        limit: 300,
      },
    }),
  });
  if (!response.ok) throw new Error(`Firestore range query failed: HTTP ${response.status}`);
  return await response.json();
}

async function updateUserTelegram(env, projectId, accessToken, uid, fields) {
  const updateMask = Object.keys(fields).map((field) => `updateMask.fieldPaths=${encodeURIComponent(field)}`).join("&");
  const response = await fetch(
    `${firestoreBase(projectId)}/users/${encodeURIComponent(uid)}?${updateMask}`,
    {
      method: "PATCH",
      headers: { authorization: `Bearer ${accessToken}`, "content-type": "application/json" },
      body: JSON.stringify({
        name: `${firestoreBase(projectId)}/users/${uid}`,
        fields: Object.fromEntries(Object.entries(fields).map(([k, v]) => [k, firestoreValue(v)])),
      }),
    }
  );
  if (!response.ok) {
    const body = await response.text();
    throw new Error(`Firestore update failed: HTTP ${response.status} ${body.slice(0, 180)}`);
  }
}

function fromFirestoreValue(value) {
  if (!value) return null;
  if ("stringValue" in value) return value.stringValue;
  if ("integerValue" in value) return Number(value.integerValue);
  if ("booleanValue" in value) return value.booleanValue;
  if ("arrayValue" in value) return (value.arrayValue.values || []).map(fromFirestoreValue);
  return null;
}

function fromFirestoreDoc(doc) {
  const fields = doc?.fields || {};
  return Object.fromEntries(Object.entries(fields).map(([k, v]) => [k, fromFirestoreValue(v)]));
}

async function findTelegramConnection(env, projectId, accessToken, code) {
  const rows = await firestoreRunQuery(env, projectId, accessToken, "telegramConnectionCode", code);
  const doc = rows.find((row) => row.document)?.document;
  if (!doc) return null;
  return { uid: doc.name.split("/").pop(), data: fromFirestoreDoc(doc) };
}

async function sendTelegram(env, chatId, title, body) {
  const botToken = String(env.TELEGRAM_BOT_TOKEN || "").trim();
  if (!botToken) throw new Error("TELEGRAM_BOT_TOKEN is not configured");

  const response = await fetch(`https://api.telegram.org/bot${botToken}/sendMessage`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({
      chat_id: chatId,
      text: `${title}\n\n${body}\n\n— SevenGold`,
      disable_web_page_preview: true,
    }),
  });
  const result = await response.json();
  if (!response.ok || !result.ok) {
    throw new Error(`Telegram send failed: HTTP ${response.status}`);
  }
  return result;
}

function telegramBodyFor(signal, event) {
  const type = String(signal.type || "BUY").toUpperCase() === "SELL" ? "SELL" : "BUY";
  const pair = String(signal.pair || "XAUUSD");
  const lines = [
    `${type} ${pair}`,
    `Entry: ${signal.entry ?? "-"}`,
    `TP: ${signal.tp ?? "-"}`,
    `SL: ${signal.sl ?? "-"}`,
  ];
  if (event === "TP_HIT") lines.unshift("🎯 TAKE PROFIT TERCAPAI");
  else if (event === "SL_HIT") lines.unshift("🛑 STOP LOSS TERCAPAI");
  else if (event === "BE") lines.unshift("⚖️ BREAK EVEN");
  else if (event === "CANCELLED") lines.unshift("❌ SINYAL DIBATALKAN");
  else lines.unshift("📢 SINYAL BARU");
  return lines.join("\n");
}

async function sendTelegramToPremium(env, projectId, accessToken, signal, event) {
  const rows = await firestoreRunQuery(env, projectId, accessToken, "role", "PREMIUM");
  const now = Date.now();
  const title = titleFor(event);
  const body = telegramBodyFor(signal, event);
  let sent = 0;
  let skipped = 0;
  let failed = 0;

  for (const row of rows) {
    const data = fromFirestoreDoc(row.document);
    const chatId = String(data.telegramChatId || "").trim();
    const expiry = Number(data.premiumExpiryMillis || 0);
    if (!chatId || expiry <= now) {
      skipped++;
      continue;
    }
    const preferences = Array.isArray(data.telegramNotificationEvents) ? data.telegramNotificationEvents : [];
    if (preferences.length && !preferences.includes(event)) {
      skipped++;
      continue;
    }
    try {
      await sendTelegram(env, chatId, title, body);
      sent++;
    } catch (error) {
      failed++;
      console.error("Telegram send failed", JSON.stringify({ uid: row.document?.name, error: error?.message }));
    }
  }
  return { sent, skipped, failed };
}


function adminTelegramChatIds(env) {
  return String(env.ADMIN_TELEGRAM_CHAT_IDS || "")
    .split(",")
    .map((v) => v.trim())
    .filter(Boolean);
}

function isTelegramAdmin(env, chatId) {
  return adminTelegramChatIds(env).includes(String(chatId));
}

async function findPremiumTelegramUsers(env, projectId, accessToken) {
  const rows = await firestoreRunQuery(env, projectId, accessToken, "role", "PREMIUM");
  const now = Date.now();
  const users = [];
  for (const row of rows) {
    if (!row.document) continue;
    const data = fromFirestoreDoc(row.document);
    const chatId = String(data.telegramChatId || "").trim();
    const expiry = Number(data.premiumExpiryMillis || 0);
    if (!chatId || expiry <= now) continue;
    users.push({
      uid: row.document.name.split("/").pop(),
      chatId,
      data,
    });
  }
  return users;
}

async function telegramStats(env, projectId, accessToken) {
  const rows = await firestoreRunQuery(env, projectId, accessToken, "role", "PREMIUM");
  const now = Date.now();
  let premium = 0;
  let connected = 0;
  for (const row of rows) {
    if (!row.document) continue;
    const data = fromFirestoreDoc(row.document);
    if (Number(data.premiumExpiryMillis || 0) <= now) continue;
    premium++;
    if (String(data.telegramChatId || "").trim()) connected++;
  }
  return { premium, connected, disconnected: Math.max(0, premium - connected) };
}

async function broadcastTelegram(env, users, title, body) {
  let sent = 0;
  let failed = 0;
  for (const user of users) {
    try {
      await sendTelegram(env, user.chatId, title, body);
      sent++;
    } catch (error) {
      failed++;
      console.error("Telegram broadcast send failed", JSON.stringify({ error: error?.message }));
    }
  }
  return { sent, failed };
}

async function handleAdminTelegramAction(request, env, claims, payload) {
  const adminUids = String(env.ADMIN_UIDS || "").split(",").map((v) => v.trim()).filter(Boolean);
  if (!adminUids.includes(String(claims.sub || ""))) return json({ ok: false, error: "Admin access required" }, 403);

  const projectId = String(env.FIREBASE_PROJECT_ID || "").trim();
  const action = String(payload.action || "").trim();
  if (!projectId || !action) return json({ ok: false, error: "Invalid request" }, 400);

  const accessToken = await getGoogleAccessToken(env, getServiceAccount(env));
  if (action === "stats") {
    return json({ ok: true, ...(await telegramStats(env, projectId, accessToken)) });
  }

  if (action === "testme") {
    const chatId = adminTelegramChatIds(env)[0];
    if (!chatId) return json({ ok: false, error: "ADMIN_TELEGRAM_CHAT_IDS is not configured" }, 500);
    await sendTelegram(
      env,
      chatId,
      "🧪 TEST NOTIFICATION",
      "🚨 TEST SIGNAL\n\n🟢 BTCUSDT\n📈 LONG\n\nEntry: 116250\nSL: 115500\nTP1: 117000\nTP2: 118200\n\n⚡ TEST NOTIFICATION — NOT A REAL TRADE"
    );
    return json({ ok: true, sent: 1 });
  }

  if (action === "testsignal") {
    const key = String(claims.sub);
    const last = Number(adminTestSignalLastAt.get(key) || 0);
    if (last && Date.now() - last < 30000) {
      return json({ ok: false, error: "⏳ Please wait before sending another test broadcast." }, 429);
    }
    adminTestSignalLastAt.set(key, Date.now());

    const stats = await telegramStats(env, projectId, accessToken);
    const users = await findPremiumTelegramUsers(env, projectId, accessToken);
    const result = await broadcastTelegram(
      env,
      users,
      "🚨 TEST PREMIUM SIGNAL",
      "🟢 BTCUSDT\n📈 LONG\n\nEntry: 116250\nSL: 115500\nTP1: 117000\nTP2: 118200\n\n⚡ TEST SIGNAL — NOT A REAL TRADE"
    );
    return json({ ok: true, premium: stats.premium, connected: stats.connected, ...result });
  }

  if (action === "test_expiry_reminder") {
    const result = await sendPremiumExpiryReminders(env);
    return json({ ok: true, ...result });
  }

  return json({ ok: false, error: "Unsupported admin Telegram action" }, 400);
}

async function handleTelegramWebhook(request, env) {
  const projectId = String(env.FIREBASE_PROJECT_ID || "").trim();
  if (!projectId) return json({ ok: false, error: "FIREBASE_PROJECT_ID is not configured" }, 500);
  const botToken = String(env.TELEGRAM_BOT_TOKEN || "").trim();
  if (!botToken) return json({ ok: false, error: "TELEGRAM_BOT_TOKEN is not configured" }, 500);

  const webhookSecret = String(env.TELEGRAM_WEBHOOK_SECRET || "").trim();
  if (webhookSecret) {
    const receivedSecret = request.headers.get("X-Telegram-Bot-Api-Secret-Token") || "";
    if (receivedSecret !== webhookSecret) {
      return json({ ok: false, error: "Invalid Telegram webhook secret" }, 401);
    }
  }

  const update = await request.json();
  const message = update?.message;
  const chatId = message?.chat?.id;
  const username = message?.from?.username || "";
  const text = String(message?.text || "").trim();
  if (!chatId || !text.startsWith("/")) return json({ ok: true, ignored: true });

  const command = text.split(/\s+/)[0].split("@")[0].toLowerCase();
  const rawCode = text.split(/\s+/)[1]?.trim().toUpperCase() || "";

  const accessToken = await getGoogleAccessToken(env, getServiceAccount(env));

  if (command === "/admin") {
    if (!isTelegramAdmin(env, chatId)) return json({ ok: true, ignored: true });
    await sendTelegram(env, chatId, "🛠 ADMIN PANEL", "📡 /testme\n🚨 /testsignal\n👥 /premium_count\n📊 /telegram_stats");
    return json({ ok: true });
  }

  if (command === "/status") {
    const connection = await firestoreRunQuery(env, projectId, accessToken, "telegramChatId", String(chatId));
    const connected = connection.some((row) => row.document);
    await sendTelegram(env, chatId, "📡 Status Telegram", connected ? "🟢 Connected\n🔔 Notifications: ON" : "⚪ Belum terhubung ke akun SevenGold.");
    return json({ ok: true });
  }

  if (command === "/premium_count") {
    if (!isTelegramAdmin(env, chatId)) return json({ ok: true, ignored: true });
    const stats = await telegramStats(env, projectId, accessToken);
    await sendTelegram(env, chatId, "👥 PREMIUM USERS", `Premium accounts: ${stats.premium}\nTelegram connected: ${stats.connected}\nTelegram disconnected: ${stats.disconnected}`);
    return json({ ok: true, ...stats });
  }

  if (command === "/telegram_stats") {
    if (!isTelegramAdmin(env, chatId)) return json({ ok: true, ignored: true });
    const stats = await telegramStats(env, projectId, accessToken);
    await sendTelegram(env, chatId, "📊 TELEGRAM STATUS", `🟢 Connected: ${stats.connected}\n⚪ Not connected: ${stats.disconnected}`);
    return json({ ok: true, ...stats });
  }

  if (command === "/testme") {
    if (!isTelegramAdmin(env, chatId)) return json({ ok: true, ignored: true });
    await sendTelegram(env, chatId, "🧪 TEST NOTIFICATION", "🚨 TEST SIGNAL\n\n🟢 BTCUSDT\n📈 LONG\n\nEntry: 116250\nSL: 115500\nTP1: 117000\nTP2: 118200\n\n⚡ TEST NOTIFICATION — NOT A REAL TRADE");
    return json({ ok: true });
  }

  if (command === "/testsignal") {
    if (!isTelegramAdmin(env, chatId)) return json({ ok: true, ignored: true });
    const key = `telegram:${String(chatId)}`;
    const last = Number(adminTestSignalLastAt.get(key) || 0);
    if (last && Date.now() - last < 30000) {
      await sendTelegram(env, chatId, "⏳ Tunggu sebentar", "Test broadcast dibatasi 30 detik untuk mencegah pengiriman berulang.");
      return json({ ok: true, rateLimited: true });
    }
    adminTestSignalLastAt.set(key, Date.now());
    const stats = await telegramStats(env, projectId, accessToken);
    const users = await findPremiumTelegramUsers(env, projectId, accessToken);
    const result = await broadcastTelegram(env, users, "🚨 TEST PREMIUM SIGNAL", "🟢 BTCUSDT\n📈 LONG\n\nEntry: 116250\nSL: 115500\nTP1: 117000\nTP2: 118200\n\n⚡ TEST SIGNAL — NOT A REAL TRADE");
    await sendTelegram(env, chatId, "✅ Test signal completed", `👥 Premium users: ${stats.premium}\n🔗 Telegram connected: ${stats.connected}\n📨 Sent: ${result.sent}\n❌ Failed: ${result.failed}`);
    return json({ ok: true, ...result });
  }

  if (command === "/disconnect") {
    const rows = await firestoreRunQuery(env, projectId, accessToken, "telegramChatId", String(chatId));
    if (!rows.some((row) => row.document)) {
      await sendTelegram(env, chatId, "ℹ️ Telegram belum terhubung", "Tidak ada koneksi Telegram aktif pada akun ini.");
      return json({ ok: true });
    }
    await sendTelegram(env, chatId, "⚠️ Putuskan koneksi Telegram?", "Kirim /disconnect_confirm untuk melanjutkan.");
    return json({ ok: true });
  }

  if (command === "/disconnect_confirm") {
    const rows = await firestoreRunQuery(env, projectId, accessToken, "telegramChatId", String(chatId));
    for (const row of rows) {
      if (row.document) {
        const uid = row.document.name.split("/").pop();
        await updateUserTelegram(env, projectId, accessToken, uid, {
          telegramChatId: "",
          telegramUsername: "",
          telegramConnectedAt: null,
          telegramNotificationEvents: [],
        });
      }
    }
    await sendTelegram(env, chatId, "✅ Telegram terputus", "Koneksi Telegram dari akun SevenGold sudah dihapus.");
    return json({ ok: true });
  }

  if (!command.startsWith("/start")) return json({ ok: true, ignored: true });
  const code = rawCode.startsWith("SG-") ? rawCode : (rawCode ? `SG-${rawCode}` : "");
  if (!code) {
    await sendTelegram(env, chatId, "🔗 Hubungkan SevenGold", "Kirim /start KODE yang tampil di aplikasi SevenGold.");
    return json({ ok: true });
  }

  const connection = await findTelegramConnection(env, projectId, accessToken, code);
  if (!connection) {
    await sendTelegram(env, chatId, "❌ Kode tidak valid", "Kode koneksi tidak ditemukan atau sudah kedaluwarsa. Buat kode baru dari Profil Premium.");
    return json({ ok: true });
  }

  const expiresAt = Number(connection.data.telegramConnectionExpiresAt || 0);
  if (!expiresAt || expiresAt < Date.now()) {
    await sendTelegram(env, chatId, "⌛ Kode kedaluwarsa", "Buat kode koneksi baru dari Profil Premium.");
    return json({ ok: true });
  }

  await updateUserTelegram(env, projectId, accessToken, connection.uid, {
    telegramChatId: String(chatId),
    telegramUsername: String(username),
    telegramConnectedAt: Date.now(),
    telegramConnectionCode: "",
    telegramConnectionExpiresAt: null,
    telegramNotificationEvents: ["SIGNAL_CREATED", "TP_HIT", "SL_HIT", "BE", "CANCELLED"],
  });

  await sendTelegram(env, chatId, "✅ Telegram berhasil terhubung", "Notifikasi sinyal Premium SevenGold sekarang aktif. Kamu bisa mengatur jenis notifikasi dari Profil di aplikasi.");
  return json({ ok: true, connected: true });
}

async function sendFcm(env, signal, event) {
  const account = getServiceAccount(env);
  const accessToken = await getGoogleAccessToken(env, account);
  const projectId = account.project_id;
  const title = titleFor(event);
  const body = bodyFor(signal);

  const data = {
    event,
    signalId: String(signal.signalId || ""),
    pair: String(signal.pair || "XAUUSD"),
    type: String(signal.type || "BUY"),
    entry: String(signal.entry ?? ""),
    tp: String(signal.tp ?? ""),
    sl: String(signal.sl ?? ""),
    status: String(signal.status || ""),
    createdAt: String(signal.createdAt || ""),
  };

  const response = await fetch(`https://fcm.googleapis.com/v1/projects/${encodeURIComponent(projectId)}/messages:send`, {
    method: "POST",
    headers: {
      "authorization": `Bearer ${accessToken}`,
      "content-type": "application/json",
    },
    body: JSON.stringify({
      message: {
        topic: TOPIC,
        notification: { title, body },
        android: {
          priority: "HIGH",
          notification: {
            channel_id: "premium_signals",
            sound: "default",
          },
        },
        data,
      },
    }),
  });

  const result = await response.json();
  if (!response.ok) throw new Error(`FCM send failed: HTTP ${response.status}`);
  return result;
}

/**
 * Kirim FCM ke SATU device (token spesifik), beda dari sendFcm yang broadcast ke
 * topic premium_signals. Dipakai buat notif personal kayak reminder H-1 expiry.
 */
async function sendFcmToToken(env, projectId, accessToken, token, title, body, data = {}) {
  const response = await fetch(`https://fcm.googleapis.com/v1/projects/${encodeURIComponent(projectId)}/messages:send`, {
    method: "POST",
    headers: {
      "authorization": `Bearer ${accessToken}`,
      "content-type": "application/json",
    },
    body: JSON.stringify({
      message: {
        token,
        notification: { title, body },
        android: {
          priority: "HIGH",
          notification: {
            channel_id: "premium_signals",
            sound: "default",
          },
        },
        data,
      },
    }),
  });
  const result = await response.json();
  if (!response.ok) throw new Error(`FCM token send failed: HTTP ${response.status}`);
  return result;
}

/**
 * Reminder H-1: cek user PREMIUM yang premiumExpiryMillis-nya jatuh 24-48 jam ke
 * depan dari sekarang, lalu kirim push notif (kalau ada fcmToken) + Telegram
 * (kalau sudah connect bot). Window 24-48 jam (bukan cuma <24 jam) supaya cron
 * yang jalan sekali sehari tetap nangkep semua user walau expiry-nya jatuh pas
 * di antara dua jadwal run. `premiumExpiryReminderSentForMillis` disimpan biar
 * satu masa aktif cuma dapat 1 reminder, gak dobel tiap cron jalan.
 */
async function sendPremiumExpiryReminders(env) {
  const account = getServiceAccount(env);
  const accessToken = await getGoogleAccessToken(env, account);
  const projectId = account.project_id;

  const now = Date.now();
  const ONE_DAY = 24 * 60 * 60 * 1000;
  const windowStart = now + ONE_DAY;
  const windowEnd = now + 2 * ONE_DAY;

  const rows = await firestoreRunRangeQuery(env, projectId, accessToken, windowStart, windowEnd);
  const docs = (rows || []).filter((row) => row.document);

  let sentPush = 0;
  let sentTelegram = 0;
  let skipped = 0;

  for (const row of docs) {
    const uid = row.document.name.split("/").pop();
    const data = fromFirestoreDoc(row.document);
    const expiry = Number(data.premiumExpiryMillis || 0);

    if (Number(data.premiumExpiryReminderSentForMillis || 0) === expiry) {
      skipped++;
      continue;
    }

    const expiryDate = new Date(expiry).toLocaleDateString("id-ID", {
      day: "numeric", month: "long", year: "numeric", timeZone: "Asia/Jakarta",
    });

    const fcmToken = String(data.fcmToken || "").trim();
    if (fcmToken) {
      try {
        await sendFcmToToken(
          env, projectId, accessToken, fcmToken,
          "⏰ Langganan Premium Segera Berakhir",
          `Langganan kamu berakhir besok, ${expiryDate}. Perpanjang sekarang biar sinyal gak putus.`,
          { event: "EXPIRY_REMINDER" }
        );
        sentPush++;
      } catch (error) {
        console.error("ExpiryReminder FCM failed", JSON.stringify({ uid, error: error?.message }));
      }
    }

    const chatId = String(data.telegramChatId || "").trim();
    if (chatId) {
      try {
        await sendTelegram(
          env, chatId, "⏰ LANGGANAN SEGERA BERAKHIR",
          `Langganan Premium kamu berakhir besok, ${expiryDate}.\nPerpanjang sekarang biar akses sinyal gak kepotong.`
        );
        sentTelegram++;
      } catch (error) {
        console.error("ExpiryReminder Telegram failed", JSON.stringify({ uid, error: error?.message }));
      }
    }

    try {
      await updateUserTelegram(env, projectId, accessToken, uid, {
        premiumExpiryReminderSentForMillis: expiry,
      });
    } catch (error) {
      console.error("ExpiryReminder mark-sent failed", JSON.stringify({ uid, error: error?.message }));
    }
  }

  console.log(JSON.stringify({ ok: true, task: "sendPremiumExpiryReminders", sentPush, sentTelegram, skipped, total: docs.length }));
  return { sentPush, sentTelegram, skipped, total: docs.length };
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (request.method !== "POST") return json({ ok: false, error: "POST only" }, 405);

    if (url.pathname === "/telegram/webhook") {
      try {
        return await handleTelegramWebhook(request, env);
      } catch (error) {
        console.error("Telegram webhook error", error);
        return json({ ok: false, error: error?.message || "Telegram webhook failed" }, 500);
      }
    }

    if (url.pathname === "/admin/telegram") {
      const auth = request.headers.get("authorization") || "";
      const match = auth.match(/^Bearer\s+(.+)$/i);
      if (!match) return json({ ok: false, error: "Missing Firebase ID token" }, 401);
      try {
        const projectId = String(env.FIREBASE_PROJECT_ID || "").trim();
        const claims = await verifyFirebaseIdToken(match[1], projectId);
        const payload = await request.json();
        return await handleAdminTelegramAction(request, env, claims, payload);
      } catch (error) {
        console.error("Admin Telegram action error", error);
        return json({ ok: false, error: error?.message || "Admin Telegram action failed" }, 500);
      }
    }

    const projectId = String(env.FIREBASE_PROJECT_ID || "").trim();
    if (!projectId || projectId === "CHANGE_ME") return json({ ok: false, error: "FIREBASE_PROJECT_ID is not configured" }, 500);

    const auth = request.headers.get("authorization") || "";
    const match = auth.match(/^Bearer\s+(.+)$/i);
    if (!match) return json({ ok: false, error: "Missing Firebase ID token" }, 401);

    try {
      const claims = await verifyFirebaseIdToken(match[1], projectId);
      const adminUids = String(env.ADMIN_UIDS || "").split(",").map((v) => v.trim()).filter(Boolean);
      if (!adminUids.includes(claims.sub)) return json({ ok: false, error: "Admin access required" }, 403);

      const payload = await request.json();
      const event = String(payload.event || "");
      if (!ALLOWED_EVENTS.has(event)) return json({ ok: false, error: "Unsupported event" }, 400);

      const signal = payload;
      const result = await sendFcm(env, signal, event);

      let telegram = { sent: 0, skipped: 0, failed: 0 };
      try {
        const account = getServiceAccount(env);
        const accessToken = await getGoogleAccessToken(env, account);
        telegram = await sendTelegramToPremium(env, projectId, accessToken, signal, event);
      } catch (telegramError) {
        // Telegram is an optional channel. Never fail FCM just because Telegram is unavailable.
        console.error("Telegram channel error", telegramError);
      }

      console.log(JSON.stringify({ ok: true, event, uid: claims.sub, fcm: result, telegram }));
      return json({ ok: true, event, messageId: result.name || null, telegram });
    } catch (error) {
      console.error("Premium push error", error);
      return json({ ok: false, error: error?.message || "Push failed" }, 500);
    }
  },

  /**
   * Dipanggil otomatis oleh Cloudflare Cron Trigger (lihat wrangler.toml).
   * Ini yang jalanin reminder H-1 tiap hari — gratis, gak butuh Firebase Blaze.
   */
  async scheduled(event, env, ctx) {
    ctx.waitUntil(
      sendPremiumExpiryReminders(env).catch((error) => {
        console.error("Scheduled sendPremiumExpiryReminders failed", error);
      })
    );
  },
};
