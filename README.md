# Key Management Android App

Ứng dụng Android quản lý Key (License) cho hệ thống Panel, được viết bằng ngôn ngữ **Kotlin** và sử dụng các thư viện hiện đại của Android.

## 🛠 Công nghệ sử dụng

*   **Ngôn ngữ**: Kotlin
*   **Giao diện (UI)**: XML Layouts, Material Design 3 (Theme Rosé Pine)
*   **Mạng (Networking)**: Retrofit 2, OkHttp 3
*   **Xử lý dữ liệu**: Gson (JSON Parsing)
*   **Bất đồng bộ**: Kotlin Coroutines
*   **Lưu trữ**: SharedPreferences (Session Management)
*   **Kiến trúc**: MVVM-like (Activity + Repository pattern simplication), BaseActivity architecture.

---

## 📂 Tổ chức mã nguồn (`app/src/main/java/com/panel/keymanager`)

Mã nguồn được tổ chức theo các package chức năng để dễ dàng bảo trì và mở rộng:

### 1. `api` (Kết nối mạng)
Chứa các lớp liên quan đến việc gọi API lên server.
*   **`ApiService.kt`**: Interface định nghĩa các endpoint (đường dẫn) API như `/login`, `/keys`, `/keys/generate`, v.v.
*   **`RetrofitClient.kt`**: Singleton khởi tạo Retrofit. Cấu hình `OkHttpClient` với các Interceptor (để thêm Header Authorization) và `TokenAuthenticator` (để tự động refresh token).
*   **`TokenAuthenticator.kt`**: Cơ chế tự động xử lý khi Token hết hạn (Lỗi 401). Nó sẽ gọi API refresh token, lưu token mới và thử lại request cũ mà không làm gián đoạn trải nghiệm người dùng.

### 2. `ui` (Giao diện người dùng)
Chứa các Activity và Adapter hiển thị màn hình.
*   **`BaseActivity.kt`**: Lớp cha của tất cả Activity. Xử lý các sự kiện chung như **Session Expired** (khi phiên đăng nhập hết hạn hẳn thì tự động đá về màn hình đăng nhập).
*   **`auth/LoginActivity.kt`**: Màn hình đăng nhập.
*   **`keys/`**:
    *   `MainActivity.kt`: Màn hình chính, hiển thị danh sách Key. Hỗ trợ "kéo để tải lại" (SwipeRefresh) và tìm kiếm.
    *   `CreateKeyActivity.kt`: Màn hình tạo Key mới.
    *   `KeyDetailActivity.kt`: Màn hình chi tiết Key (Sửa, Xóa, Reset HWID).
    *   `KeyAdapter.kt`: Adapter cho RecyclerView để hiển thị danh sách Key.
*   **`profile/ProfileActivity.kt`**: Màn hình thông tin người dùng (Số dư, cấp bậc).

### 3. `models` (Mô hình dữ liệu)
Chứa các class POJO/Data Class đại diện cho dữ liệu từ API.
*   **`User.kt`**: Thông tin người dùng.
*   **`Key.kt`**: Thông tin license key.
*   **`ApiResponse.kt`**: Cấu trúc phản hồi chuẩn từ server.
*   *Lưu ý:* Các field được đánh dấu `@SerializedName` để đảm bảo không bị lỗi khi build Release (do R8/ProGuard đổi tên biến).

### 4. `utils` (Tiện ích)
*   **`SessionManager.kt`**: Quản lý lưu trữ cục bộ (Token, Refresh Token, Username, Balance) sử dụng `SharedPreferences`.

---

## ⚙️ Cơ chế hoạt động chính

### 1. Xác thực & Token (Authentication)
*   Khi đăng nhập thành công, Server trả về `AccessToken` và `RefreshToken`.
*   `RetrofitClient` sẽ tự động đính kèm `AccessToken` vào Header `Authorization: Bearer ...` của mọi request.
*   **Tự động làm mới (Auto Refresh)**: Nếu API trả về **401 Unauthorized**, `TokenAuthenticator` sẽ chặn request đó lại, dùng `RefreshToken` để xin cấp lại `AccessToken` mới, sau đó thực hiện lại request ban đầu. Người dùng không cần đăng nhập lại.

### 2. Giao diện & Theme
*   Ứng dụng sử dụng theme **Rosé Pine** (Tông màu tối, tím/hồng dịu mắt).
*   Hỗ trợ **Dark Mode** hoàn toàn.
*   Các file cấu hình màu sắc nằm trong `res/values/colors.xml` và `res/values-night/colors.xml`.

### 3. Xử lý lỗi (Error Handling)
*   Tất cả các lỗi mạng, lỗi server đều được bắt (try-catch) và hiển thị thông báo Toast rõ ràng cho người dùng.
*   Nếu Refresh Token cũng hết hạn (hoặc bị thu hồi), ứng dụng sẽ tự động chuyển hướng về màn hình Đăng nhập (thông qua cơ chế EventBus đơn giản trong `SessionManager` và `BaseActivity`).

---

## 🚀 Hướng dẫn Build (Cài đặt)

### Yêu cầu
*   JDK 17
*   Android SDK (API Level 34/35)

### Lệnh Build
Để tạo file APK cài đặt (Release):

```bash
# Tại thư mục android-app
./gradlew clean assembleRelease
```

File APK sau khi build sẽ nằm tại: `app/build/outputs/apk/release/app-release-unsigned.apk` (hoặc signed nếu đã cấu hình key).

### Chạy máy ảo/Debug
```bash
./gradlew assembleDebug
```
