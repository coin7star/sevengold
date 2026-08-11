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
    scope: "https://www.googleapis.com/auth/firebase.messaging",
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

export default {
  async fetch(request, env) {
    if (request.method !== "POST") return json({ ok: false, error: "POST only" }, 405);

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
      console.log(JSON.stringify({ ok: true, event, uid: claims.sub, result }));
      return json({ ok: true, event, messageId: result.name || null });
    } catch (error) {
      console.error("Premium push error", error);
      return json({ ok: false, error: error?.message || "Push failed" }, 500);
    }
  },
};
