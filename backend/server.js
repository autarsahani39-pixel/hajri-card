const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const crypto = require('crypto');
const axios = require('axios');
const admin = require('firebase-admin');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 8080;

app.use(helmet());
app.use(cors({ origin: true }));
app.use(express.json());

// -------------------------------------------------------------
// 1. Firebase Admin SDK Initialization
// -------------------------------------------------------------
let firebaseInitialized = false;
try {
  if (process.env.FIREBASE_PROJECT_ID && process.env.FIREBASE_CLIENT_EMAIL && process.env.FIREBASE_PRIVATE_KEY) {
    const privateKey = process.env.FIREBASE_PRIVATE_KEY.replace(/\\n/g, '\n');
    admin.initializeApp({
      credential: admin.credential.cert({
        projectId: process.env.FIREBASE_PROJECT_ID,
        clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
        privateKey: privateKey,
      }),
    });
    firebaseInitialized = true;
    console.log('[Firebase] Admin SDK initialized successfully with environment service account credentials.');
  } else if (process.env.GOOGLE_APPLICATION_CREDENTIALS) {
    admin.initializeApp();
    firebaseInitialized = true;
    console.log('[Firebase] Admin SDK initialized via GOOGLE_APPLICATION_CREDENTIALS.');
  } else {
    console.warn('[Firebase] Warning: Firebase Admin credentials not provided in environment variables. Custom tokens will fail until configured.');
  }
} catch (err) {
  console.error('[Firebase] Initialization error:', err.message);
}

// -------------------------------------------------------------
// 2. Constants & Helpers
// -------------------------------------------------------------
const SESSION_SECRET = process.env.SESSION_SECRET || 'hajri-card-secure-session-secret-key-32b';
const META_WA_PHONE_NUMBER_ID = process.env.META_WA_PHONE_NUMBER_ID;
const META_WA_ACCESS_TOKEN = process.env.META_WA_ACCESS_TOKEN;
const META_WA_TEMPLATE_NAME = process.env.META_WA_TEMPLATE_NAME || 'hajri_auth_code';
const META_WA_TEMPLATE_LANG = process.env.META_WA_TEMPLATE_LANG || 'en_US';
const OTP_EXPIRY_MINUTES = 5;

/**
 * Generate cryptographically secure 6-digit OTP code.
 */
function generateSecureOtp() {
  const num = crypto.randomInt(100000, 999999);
  return num.toString();
}

/**
 * Mint an HMAC-signed session token encapsulating phone, OTP hash, and expiration timestamp.
 */
function createSessionToken(phoneNumber, otpCode) {
  const expiresAt = Date.now() + OTP_EXPIRY_MINUTES * 60 * 1000;
  const otpHash = crypto.createHmac('sha256', SESSION_SECRET).update(`${phoneNumber}:${otpCode}`).digest('hex');
  const payload = `${phoneNumber}|${expiresAt}|${otpHash}`;
  const signature = crypto.createHmac('sha256', SESSION_SECRET).update(payload).digest('hex');
  const sessionToken = Buffer.from(`${payload}|${signature}`).toString('base64url');
  return { sessionToken, expiresAt };
}

const verifiedSessionTokens = new Set();

/**
 * Verify session token and check code validity using constant-time comparison.
 */
function verifySessionToken(sessionToken, phoneNumber, enteredCode) {
  try {
    if (verifiedSessionTokens.has(sessionToken)) {
      return { valid: false, reason: 'This verification code has already been used. Please request a new code.' };
    }
    const raw = Buffer.from(sessionToken, 'base64url').toString('utf8');
    const parts = raw.split('|');
    if (parts.length !== 4) return { valid: false, reason: 'Malformed session token' };

    const [tokenPhone, expiresAtStr, expectedOtpHash, signature] = parts;
    const payload = `${tokenPhone}|${expiresAtStr}|${expectedOtpHash}`;
    const expectedSignature = crypto.createHmac('sha256', SESSION_SECRET).update(payload).digest('hex');

    // Constant-time signature verification
    const sigBufferA = Buffer.from(signature, 'hex');
    const sigBufferB = Buffer.from(expectedSignature, 'hex');
    if (sigBufferA.length !== sigBufferB.length || !crypto.timingSafeEqual(sigBufferA, sigBufferB)) {
      return { valid: false, reason: 'Invalid token signature' };
    }

    if (tokenPhone !== phoneNumber) {
      return { valid: false, reason: 'Phone number mismatch' };
    }

    const expiresAt = parseInt(expiresAtStr, 10);
    if (Date.now() > expiresAt) {
      return { valid: false, reason: 'Verification code has expired. Please request a new code.' };
    }

    // Verify OTP code hash
    const computedHash = crypto.createHmac('sha256', SESSION_SECRET).update(`${phoneNumber}:${enteredCode.trim()}`).digest('hex');
    const hashBufferA = Buffer.from(expectedOtpHash, 'hex');
    const hashBufferB = Buffer.from(computedHash, 'hex');
    if (hashBufferA.length !== hashBufferB.length || !crypto.timingSafeEqual(hashBufferA, hashBufferB)) {
      return { valid: false, reason: 'Incorrect verification code' };
    }

    return { valid: true };
  } catch (err) {
    return { valid: false, reason: 'Failed to parse session token' };
  }
}

/**
 * Normalize phone number to standard E.164 without leading '+' for Meta API.
 */
function normalizePhoneNumber(phone) {
  const cleaned = phone.replace(/[^0-9+]/g, '');
  if (cleaned.startsWith('+')) {
    return cleaned.substring(1);
  }
  return cleaned;
}

// -------------------------------------------------------------
// 3. API Routes
// -------------------------------------------------------------

/**
 * Health check endpoint.
 */
app.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    service: 'Hajri Card WhatsApp Authentication Gateway',
    firebaseConfigured: firebaseInitialized,
    metaWhatsAppConfigured: Boolean(META_WA_PHONE_NUMBER_ID && META_WA_ACCESS_TOKEN),
    timestamp: new Date().toISOString(),
  });
});

/**
 * POST /auth/whatsapp/initiate
 * Body: { phoneNumber: string }
 */
app.post('/auth/whatsapp/initiate', async (req, res) => {
  try {
    const { phoneNumber } = req.body;
    if (!phoneNumber || typeof phoneNumber !== 'string' || phoneNumber.trim().length < 8) {
      return res.status(400).json({ error: 'Valid phone number with country code is required.' });
    }

    const trimmedPhone = phoneNumber.trim();
    const recipientPhone = normalizePhoneNumber(trimmedPhone);

    // Verify Meta Cloud API configuration
    if (!META_WA_PHONE_NUMBER_ID || !META_WA_ACCESS_TOKEN) {
      return res.status(503).json({
        error: 'Meta WhatsApp Business Cloud API is not configured on the backend server. Please set META_WA_PHONE_NUMBER_ID and META_WA_ACCESS_TOKEN in environment variables.',
      });
    }

    const otpCode = generateSecureOtp();
    const { sessionToken, expiresAt } = createSessionToken(trimmedPhone, otpCode);

    // Dispatch Official Meta WhatsApp Business Authentication Message
    // Reference: https://developers.facebook.com/docs/whatsapp/cloud-api/guides/send-message-templates/authentication-templates
    const metaGraphUrl = `https://graph.facebook.com/v20.0/${META_WA_PHONE_NUMBER_ID}/messages`;

    const metaPayload = {
      messaging_product: 'whatsapp',
      recipient_type: 'individual',
      to: recipientPhone,
      type: 'template',
      template: {
        name: META_WA_TEMPLATE_NAME,
        language: {
          code: META_WA_TEMPLATE_LANG,
        },
        components: [
          {
            type: 'body',
            parameters: [
              {
                type: 'text',
                text: otpCode,
              },
            ],
          },
          {
            type: 'button',
            sub_type: 'url',
            index: '0',
            parameters: [
              {
                type: 'text',
                text: otpCode,
              },
            ],
          },
        ],
      },
    };

    try {
      const metaResponse = await axios.post(metaGraphUrl, metaPayload, {
        headers: {
          Authorization: `Bearer ${META_WA_ACCESS_TOKEN}`,
          'Content-Type': 'application/json',
        },
        timeout: 10000,
      });

      console.log(`[Meta WA API] OTP template dispatched to ${recipientPhone}. Message ID:`, metaResponse.data?.messages?.[0]?.id);

      return res.json({
        success: true,
        sessionToken: sessionToken,
        expiresInSeconds: OTP_EXPIRY_MINUTES * 60,
        message: 'Official WhatsApp verification code sent successfully.',
      });
    } catch (metaErr) {
      const errData = metaErr.response?.data?.error;
      console.error('[Meta WA API Error]:', errData || metaErr.message);

      const errorMessage = errData?.message || 'Failed to dispatch WhatsApp authentication template message via Meta API.';
      return res.status(502).json({
        error: `Meta WhatsApp API error: ${errorMessage}`,
        details: errData?.error_user_msg || errData?.error_data?.details,
      });
    }
  } catch (err) {
    console.error('Error in /auth/whatsapp/initiate:', err);
    return res.status(500).json({ error: 'Internal server error while initiating WhatsApp authentication.' });
  }
});

/**
 * POST /auth/whatsapp/verify
 * Body: { phoneNumber: string, sessionToken: string, code: string }
 */
app.post('/auth/whatsapp/verify', async (req, res) => {
  try {
    const { phoneNumber, sessionToken, code } = req.body;

    if (!phoneNumber || !sessionToken || !code) {
      return res.status(400).json({ error: 'phoneNumber, sessionToken, and code are required.' });
    }

    if (!firebaseInitialized) {
      return res.status(503).json({
        error: 'Firebase Admin SDK is not initialized on the server. Please configure FIREBASE_PROJECT_ID, FIREBASE_CLIENT_EMAIL, and FIREBASE_PRIVATE_KEY.',
      });
    }

    const verification = verifySessionToken(sessionToken, phoneNumber.trim(), code.trim());
    if (!verification.valid) {
      return res.status(400).json({ error: verification.reason });
    }

    // Invalidate session token to prevent reuse
    verifiedSessionTokens.add(sessionToken);

    // Normalized phone identifier for Firebase Auth UID
    const cleanPhone = normalizePhoneNumber(phoneNumber.trim());
    const uid = `wa_${cleanPhone}`;

    // Ensure Firebase user exists or sync phone number
    try {
      await admin.auth().getUser(uid);
    } catch (userNotFoundErr) {
      if (userNotFoundErr.code === 'auth/user-not-found') {
        console.log(`[Firebase Auth] Creating new user for WhatsApp UID: ${uid}`);
        await admin.auth().createUser({
          uid: uid,
          phoneNumber: `+${cleanPhone}`,
          displayName: `Hajri User (${cleanPhone.slice(-4)})`,
        });
      } else {
        console.warn('[Firebase Auth] User fetch warning:', userNotFoundErr.message);
      }
    }

    // Mint Firebase Custom Token
    const customClaims = {
      auth_provider: 'whatsapp',
      verified_phone: `+${cleanPhone}`,
      auth_timestamp: Date.now(),
    };

    const customToken = await admin.auth().createCustomToken(uid, customClaims);

    console.log(`[Firebase Auth] Minted custom token for WhatsApp user: ${uid}`);

    return res.json({
      success: true,
      customToken: customToken,
      uid: uid,
      phoneNumber: `+${cleanPhone}`,
      message: 'WhatsApp authentication successful.',
    });
  } catch (err) {
    console.error('Error in /auth/whatsapp/verify:', err);
    return res.status(500).json({ error: `Internal server error during verification: ${err.message}` });
  }
});

// -------------------------------------------------------------
// 4. Start Server
// -------------------------------------------------------------
app.listen(PORT, '0.0.0.0', () => {
  console.log(`[Hajri Card Backend] Server running on port ${PORT}`);
});
