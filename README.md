# Key Management Android App

Android application for managing Keys (Licenses) for the Panel system, written in **Kotlin** and using modern Android libraries.

## 🛠 Tech Stack

- **Language**: Kotlin
- **UI**: XML Layouts, Material Design 3 (Rosé Pine Theme)
- **Networking**: Retrofit 2, OkHttp 3
- **Data Processing**: Gson (JSON Parsing)
- **Asynchronous**: Kotlin Coroutines
- **Storage**: SharedPreferences (Session Management)
- **Architecture**: MVVM-like (Activity + Repository pattern simplification), BaseActivity architecture.

---

## 📂 Source Code Organization (`app/src/main/java/com/panel/keymanager`)

The source code is organized by functional packages for easy maintenance and scalability:

### 1. `api` (Networking)

Contains classes related to making API calls to the server.

- **`ApiService.kt`**: Interface defining API endpoints such as `/login`, `/keys`, `/keys/generate`, etc.
- **`RetrofitClient.kt`**: Singleton for initializing Retrofit. Configures `OkHttpClient` with Interceptors (to add Authorization Header) and `TokenAuthenticator` (to automatically refresh tokens).
- **`TokenAuthenticator.kt`**: Mechanism to automatically handle Token expiration (401 Error). It calls the refresh token API, saves the new token, and retries the original request without interrupting the user experience.

### 2. `ui` (User Interface)

Contains Activities and Adapters for displaying screens.

- **`BaseActivity.kt`**: Parent class for all Activities. Handles common events like **Session Expired** (automatically kicks to login screen when the session fully expires).
- **`auth/LoginActivity.kt`**: Login screen.
- **`keys/`**:
  - `MainActivity.kt`: Main screen, displaying the list of Keys. Supports "Swipe to Refresh" and search.
  - `CreateKeyActivity.kt`: Screen for creating new Keys.
  - `KeyDetailActivity.kt`: Key detail screen (Edit, Delete, Reset HWID).
  - `KeyAdapter.kt`: Adapter for RecyclerView to display the list of Keys.
- **`profile/ProfileActivity.kt`**: User profile screen (Balance, Rank).

### 3. `models` (Data Models)

Contains POJO/Data Classes representing data from the API.

- **`User.kt`**: User information.
- **`Key.kt`**: License key information.
- **`ApiResponse.kt`**: Standard response structure from the server.
- _Note:_ Fields are marked with `@SerializedName` to ensure no errors during Release build (due to R8/ProGuard renaming variables).

### 4. `utils` (Utilities)

- **`SessionManager.kt`**: Manages local storage (Token, Refresh Token, Username, Balance) using `SharedPreferences`.

---

## ⚙️ Core Mechanisms

### 1. Authentication & Token

- Upon successful login, the Server returns an `AccessToken` and a `RefreshToken`.
- `RetrofitClient` automatically attaches the `AccessToken` to the `Authorization: Bearer ...` Header of every request.
- **Auto Refresh**: If the API returns **401 Unauthorized**, `TokenAuthenticator` intercepts the request, uses the `RefreshToken` to request a new `AccessToken`, and then retries the original request. The user does not need to log in again.

### 2. UI & Theme

- The application uses the **Rosé Pine** theme (Dark tone, soothing purple/pink).
- Fully supports **Dark Mode**.
- Color configuration files are located in `res/values/colors.xml` and `res/values-night/colors.xml`.

### 3. Error Handling

- All network and server errors are caught (try-catch) and displayed as clear Toast messages to the user.
- If the Refresh Token also expires (or is revoked), the application automatically redirects to the Login screen (via a simple EventBus mechanism in `SessionManager` and `BaseActivity`).

---

## 🚀 Build & Installation Guide

### System Requirements

- **JDK 17** or higher
- **Android SDK** (API Level 28-35)
- **Android Studio** (recommended) or Gradle CLI

### Step 1: Configure `local.properties`

1. Copy the example file:

   ```bash
   cp local.properties.example local.properties
   ```

2. Open `local.properties` and fill in the information:

   ```properties
   # Keystore password (for signing release APK)
   KEYSTORE_PASSWORD=your_actual_password
   KEY_PASSWORD=your_actual_password

   # Panel API Server URL (REQUIRED)
   PANEL_URL=https://panel.example.com/
   ```

   **Note:**

   - `PANEL_URL` is required - the app will not build without it.

### Step 2: Prepare Keystore (Release Build only)

If you want to build a signed Release APK:

- Create a keystore (if you don't have one):
  ```bash
  keytool -genkey -v -keystore keystore.jks -keyalg RSA \
    -keysize 2048 -validity 10000 -alias keymanager
  ```

### Step 3: Build

#### Build Debug

```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

#### Build Release

```bash
./gradlew clean assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`
