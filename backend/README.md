# Hajri Card — WhatsApp Authentication Backend Gateway

This secure Node.js backend handles official Meta WhatsApp Business Cloud API communication and Firebase Custom Token minting for the Hajri Card Android application.

## Security Architecture

1. **Zero Client Secrets**: Meta App Secrets, WhatsApp System User Access Tokens, and Firebase Admin Private Keys stay exclusively on this server and are NEVER exposed to the Android client.
2. **Cryptographic Session Tokens**: When an OTP is initiated, the server generates an HMAC-signed session token with a 5-minute expiry. The raw OTP is not stored in plaintext and cannot be spoofed.
3. **Official Meta Cloud API**: OTPs are delivered via official Meta WhatsApp Business Authentication templates directly to the user's WhatsApp number.
4. **Firebase Admin Custom Tokens**: Upon successful verification, the backend generates an official Firebase Custom Token using the Firebase Admin SDK (`admin.auth().createCustomToken()`), which the Android app uses with `signInWithCustomToken()`.

## Setup & Deployment Instructions

### 1. Requirements from Meta for Developers
1. Register a Meta Developer account at [developers.facebook.com](https://developers.facebook.com).
2. Create an App of type **Business** and add the **WhatsApp** product.
3. Obtain your **Phone Number ID** (`META_WA_PHONE_NUMBER_ID`).
4. Generate a permanent System User **Access Token** (`META_WA_ACCESS_TOKEN`) with `whatsapp_business_messaging` and `whatsapp_business_management` permissions.
5. In WhatsApp Manager, create an **Authentication Template** (e.g., named `hajri_auth_code`) with a one-time password button/body.

### 2. Requirements from Firebase Console
1. Go to Firebase Console -> Project Settings -> **Service Accounts**.
2. Click **Generate new private key**.
3. Copy `project_id`, `client_email`, and `private_key` into your environment variables.

### 3. Deploy to Cloud Run / Render / Railway / Heroku
Set the environment variables defined in `.env.example`:
- `PORT`
- `SESSION_SECRET`
- `META_WA_PHONE_NUMBER_ID`
- `META_WA_ACCESS_TOKEN`
- `META_WA_TEMPLATE_NAME`
- `META_WA_TEMPLATE_LANG`
- `FIREBASE_PROJECT_ID`
- `FIREBASE_CLIENT_EMAIL`
- `FIREBASE_PRIVATE_KEY`

### 4. Connect to Android App
Once deployed (e.g., `https://hajri-auth-backend.your-domain.run.app`), enter the URL in the Hajri Card Login screen or configure it in the app settings.
