# KẾ HOẠCH NÂNG CẤP GIAO DIỆN & CHUẨN HÓA LOGIC ĐẶT BÀN THỜI GIAN THỰC (PLAN UI VER 8.0)

**Dự án:** Hệ thống Quản lý & Đặt món Quán Cafe (`Brew & Bean — CafeOrder`)  
**Tài liệu:** `Plan UI ver8.md`  
**Đối tượng thực thi:** `agy cli` / Developer  
**Ràng buộc nghiêm ngặt:**
1. **Không chỉnh sửa hay chạy bất kỳ dòng code nào ở bước lập kế hoạch này.**
2. **Không dùng icon mặc định của Windows / NetBeans / Emoji thô** — chỉ dùng bộ icon có sẵn trong `src/cafe/resources/images/icons/` qua `IconHelper` và chấm tròn `Circle` chuẩn màu thương hiệu.

---

## 1. TỔNG QUAN 3 VẤN ĐỀ CỐT LÕI CẦN XỬ LÝ TRONG VER 8.0

Dựa trên 2 ảnh chụp màn hình thực tế và phản hồi của bạn, dưới đây là 3 điểm nghẽn về **Giao diện (UI)** và **Nghiệp vụ (Logic)** cần giải quyết triệt để:

| STT | Hạng mục | Hiện trạng lỗi (Từ ảnh chụp & thực nghiệm) | Giải pháp trọng tâm (Ver 8.0) |
| :-: | :--- | :--- | :--- |
| **1** | **Thanh công cụ Sơ đồ bàn bị rối & cắt chữ `...` hàng loạt** *(Ảnh 1)* | Nhồi nhét 11 nút (`Tầng 1..3`, 5 nút lọc trạng thái kèm icon + số đếm, 3 nút công cụ) vào 1 dòng ngang duy nhất $\rightarrow$ tất cả bị bóp nghẹt thành `...`, `T...`, `Tất cả ...`, `Bàn trống...`, `Đang phục v...`, `Đặt trướ...`, `Chờ dọ...`. | **Chọn lọc thông tin & Tái bố cục:** Rút gọn nhãn bộ lọc, bỏ các nút trùng lặp trên thanh ngang, thêm `minWidth(USE_PREF_SIZE)` cho 100% nút để không bao giờ bị cắt `...`. |
| **2** | **Nhập `Bàn 5` (hoặc số `5`, `1`) ở Login bị báo *"Mật khẩu không chính xác!"*** | Trong `DatabaseManager.java` dòng 328-329 đang gán cứng alias `"5" -> "cashier1"` và `"1" -> "admin"`. Khi nhập `5` hoặc `Bàn 5` (mật khẩu trống), hệ thống tưởng nhầm là nhân viên `cashier1` đăng nhập thiếu mật khẩu nên chặn lại! | **Xóa ngay alias số `"1"` và `"5"`** trong `DatabaseManager.java`. Đảm bảo mọi số bàn (`1..12`, `201..208`, `301..306`) luôn vào thẳng **Role Khách hàng (Kiosk)** mà không hỏi mật khẩu nhân viên. |
| **3** | **Logic Đặt bàn trước chưa tính theo Giờ thực tế** *(Ảnh 2: Hẹn 19:30 nhưng 15:50 đã khóa bàn)* | Bàn 05 hẹn `19:30` và Bàn 10 hẹn `20:00`, nhưng lúc `15:50` (cách giờ hẹn gần 4 tiếng) bàn đã bị đóng khung cam `Đặt trước` cố định, không phản ánh việc bàn vẫn trống và vẫn phục vụ khách bình thường trước giờ hẹn. | **Logic Đặt bàn theo Khung giờ (`Time-Aware Reservation`):** Trước giờ hẹn $> 60\text{ phút}$, bàn vẫn là **Bàn trống (Sẵn sàng)** và cho phép khách ngồi / đặt món bình thường, chỉ hiển thị dòng nhắc nhẹ `Sẵn sàng • Hẹn 19:30`. Chỉ chuyển sang trạng thái giữ bàn màu cam khi còn $\le 60\text{ phút}$ trước giờ hẹn. |

---

## 2. CHI TIẾT HẠNG MỤC 1: CHỌN LỌC THÔNG TIN & THIẾT KẾ LẠI THANH ĐIỀU KHIỂN SƠ ĐỒ BÀN (`FloorPlanView.java`)

### 2.1. Phân tích Nguyên nhân Gây Rối & Mất Chữ (Ảnh 1)
Hiện tại, khu vực phía trên Sơ đồ bàn có tới **3 tầng thông tin trùng lặp** và quá tải chiều ngang:
1. **Dòng 1 (`topBar`):** `Bàn 05  [● Đặt trước]` ... khoảng trống lớn ... `[Đặt trước bàn]` `[+ Gọi món cho bàn 05]` `[Đăng xuất]`.
   - *(Lưu ý: Nút `[Đăng xuất]` ở góc trên bên phải này cũng đang trùng lặp với nút `[Đăng xuất ca trực]` ở chân Sidebar bên trái!)*
2. **Dòng 2 (`floorToolbar`):** Nhồi 11 nút trên cùng 1 hàng có bề rộng chỉ khoảng `760px`:
   - `tabF1 ("Tầng 1")`, `tabF2 ("Tầng 2")`, `tabF3 ("Tầng 3")` $\rightarrow$ Không có `minWidth`, bị JavaFX bóp nát thành `[ ... ] [ T... ] [ T... ]`.
   - 5 nút lọc: `[Tất cả (26)]`, `[○ Bàn trống (14)]`, `[✔ Đang phục vụ (8)]`, `[🪑 Đặt trước (3)]`, `[Chờ dọn (1)]` $\rightarrow$ Vừa có icon PNG, vừa có chữ dài, vừa có số đếm trong ngoặc $\rightarrow$ Bị cắt thành `Tất cả ...`, `Bàn trống...`, `Đang phục v...`.
   - 3 nút công cụ: `[Làm mới]`, `[Chuyển bàn]`, `[Gộp bàn]` $\rightarrow$ Đẩy tràn ra mép phải.
3. **Dòng dưới đáy (`legendBar`):** Lại liệt kê lại 5 chấm màu chú thích trạng thái bàn (trùng lặp 100% với các nút lọc ở trên).

### 2.2. Giải pháp Thiết kế Lại Tinh Gọn, Sạch Đẹp (Zero Truncation)

```
+-------------------------------------------------------------------------------------------------------+
| DÒNG 1 (TOP BAR):                                                                                     |
| Bàn 05  [ ● Trống • Có hẹn 19:30 ]           [ Làm mới ] [ Chuyển bàn ] [ Gộp bàn ] [ + Gọi món ]     |
+-------------------------------------------------------------------------------------------------------+
| DÒNG 2 (FLOOR & FILTER BAR — Rộng rãi, 100% hiển thị đủ chữ, không bao giờ bị "..."):                 |
| [ Tầng 1 ] [ Tầng 2 ] [ Tầng 3 ]             [ Tất cả (26) ] [ ● Trống (16) ] [ ● Có khách (8) ] [ ● Đặt hẹn (2) ] |
+-------------------------------------------------------------------------------------------------------+
```

#### Các thay đổi cụ thể trong `FloorPlanView.java`:
1. **Trên Dòng 1 (`topBar`):**
   - **Xóa nút `btnTopLogout` (`Đăng xuất`)** ở góc trên phải vì ở chân Sidebar trái đã có nút `Đăng xuất ca trực` cố định.
   - Đưa 3 nút thao tác bàn **`[ Làm mới ]`**, **`[ Chuyển bàn ]`**, **`[ Gộp bàn ]`** (sử dụng icon chuẩn từ `src/cafe/resources/images/icons/staff/`: `lam-moi.png`, `chuyen-ban.png`, `tach-gop-ban.png`) và nút chính **`[ + Gọi món ]`** lên Dòng 1 (`topBar`), tận dụng hoàn hảo khoảng trống lớn trên thanh tiêu đề.
   - *(Nút `Đặt trước bàn` đã có sẵn trong Ngăn Chi Tiết Bàn bên phải `rightInspector`, không cần để lặp trên `topBar`).*
2. **Trên Dòng 2 (`floorToolbar`):**
   - **Cụm Trái — Chọn Tầng:** `[ Tầng 1 ]` `[ Tầng 2 ]` `[ Tầng 3 ]`. Bắt buộc gọi `setMinWidth(Region.USE_PREF_SIZE)` cho cả 3 nút để luôn hiển thị rõ chữ `"Tầng 1"`, `"Tầng 2"`, `"Tầng 3"`.
   - **Cụm Phải — Bộ lọc Trạng thái Tinh gọn (4 nút):**
     - Thay vì dùng icon PNG rối mắt bên trong từng viên thuốc lọc và cụm từ dài dòng, sử dụng chấm tròn màu `Circle(4.5)` chuẩn xác + tên ngắn gọn + `setMinWidth(Region.USE_PREF_SIZE)`:
       1. **`Tất cả (26)`**
       2. **`● Trống (16)`** (Chấm xanh lá `#16A34A` — thay cho *"Bàn trống"*)
       3. **`● Có khách (8)`** (Chấm vàng cam `#E2943B` — thay cho *"Đang phục vụ"*)
       4. **`● Đặt hẹn (2)`** (Chấm cam `#F59E0B` — thay cho *"Đặt trước"*)
     - Lược bỏ nút lọc `"Chờ dọn"` trên thanh ngang để tiết kiệm diện tích (trạng thái dọn bàn vẫn thao tác bình thường ở cột Inspector bên phải).
3. **Trên Mặt Bàn (`TableWithChairsNode` - Ảnh 2):**
   - Chọn lọc lại 3 dòng thông tin trên mỗi mặt bàn để thoáng và dễ nhìn trong 1 giây:
     - **Dòng 1 (Số bàn):** `01`, `02`... hoặc `Bàn 11 • Sofa` (Chữ to, rõ).
     - **Dòng 2 (Trạng thái ngắn gọn):**
       - Nếu bàn trống (hoặc có hẹn xa): `● Sẵn sàng`
       - Nếu đang có khách: `Chờ pha` / `Đang pha` / `Đang dùng`
       - Nếu sắp tới giờ hẹn ($\le 60\text{ phút}$): `Giữ bàn hẹn`
     - **Dòng 3 (Thông tin phụ):**
       - Nếu đang có khách: `112k • 3 món`
       - Nếu có lịch hẹn: `Hẹn 19:30 (2 khách)`
       - Nếu bàn trống hoàn toàn: **Ẩn dòng 3** (bỏ chữ `"Sẵn sàng"` bị lặp lại dưới chữ `"● Bàn trống"` như ở Ảnh 2 hiện tại, giúp các bàn trống số `02, 04, 07, 08, 11` nhìn thoáng và sạch hơn hẳn!).

---

## 3. CHI TIẾT HẠNG MỤC 2: SỬA LỖI NHẬP "BÀN 5" BÁO SAI MẬT KHẨU (`DatabaseManager.java` & `LoginController.java`)

### 3.1. Nguyên nhân Gốc rễ trong Code
Khi bạn nhập `5` hoặc `bàn 5` vào ô Tài khoản lúc `15:50`, hệ thống báo **`"Mật khẩu không chính xác! Vui lòng kiểm tra lại."`** KHÔNG PHẢI vì giờ hẹn 19:30, mà do **một lỗi logic ánh xạ tài khoản** nằm ở `src/cafe/services/DatabaseManager.java` (dòng 328-329 và 466-467):

```java
// ĐOẠN CODE GÂY LỖI TRONG DatabaseManager.java:
if (userLower.equals("1") || userLower.equals("quanly")) userLower = "admin";
else if (userLower.equals("5") || userLower.equals("nhanvien") || userLower.equals("staff")) userLower = "cashier1";
```
- Khi bạn nhập `"5"` (hoặc bấm nút chọn nhanh `Kiosk Khách` vốn tự điền số `"5"` và để trống mật khẩu), hàm `DatabaseManager.findStaffByUsername("5")` lại dịch số `"5"` thành tài khoản thu ngân `"cashier1"`!
- Hệ quả: `LoginController.java` thấy `staffLookup != null` nên tưởng bạn là Nhân viên Thu ngân đang đăng nhập mà quên nhập mật khẩu $\rightarrow$ Báo lỗi **`"Mật khẩu không chính xác!"`**! (Lỗi tương tự cũng xảy ra nếu khách nhập bàn số `"1"` vì bị dịch thành `"admin"`).

### 3.2. Hướng Sửa Đổi Chuẩn Xác
1. **Trong `src/cafe/services/DatabaseManager.java` (dòng 328-329 & 466-467):**
   - **Xóa bỏ hoàn toàn điều kiện `userLower.equals("1")` và `userLower.equals("5")`** khỏi hàm `findStaffByUsername()` và `authenticateStaff()`.
   - Chỉ giữ lại các alias bằng chữ: `"quanly"`, `"nhanvien"`, `"staff"`, `"phucvu"`, `"phache"`.
2. **Trong `src/cafe/controllers/LoginController.java` (dòng 163-179):**
   - Kiểm tra `int potentialTable = parseTableNumber(account);` **TRƯỚC** khi kiểm tra nhân viên:
   - Nếu `potentialTable > 0` (người dùng nhập số bàn `1..12`, `201..208`, `301..306` hoặc `"Bàn 5"`) và ô mật khẩu để trống (hoặc trùng số bàn) $\rightarrow$ **Luôn luôn cho đăng nhập thẳng vào màn hình Kiosk Khách hàng của bàn đó!**

---

## 4. CHI TIẾT HẠNG MỤC 3: LOGIC ĐẶT BÀN TRƯỚC THEO KHUNG GIỜ THỰC TẾ (`Time-Aware Reservation Logic`)

### 4.1. Quy tắc Nghiệp vụ Thực tế
Một bàn có lịch đặt trước (ví dụ: **Bàn 05 hẹn `19:30`**, **Bàn 10 hẹn `20:00`**) không thể bị khóa chết từ lúc `15:50` chiều! Hệ thống cần tự động tính khoảng cách thời gian $\Delta t = \text{Giờ hẹn} - \text{Giờ hiện tại}$ (`java.time.LocalTime`):

```
                 [15:50 Hiện tại]                      [18:30 Bắt đầu giữ bàn]        [19:30 Giờ hẹn]
─────────────────────────┼────────────────────────────────────────┼──────────────────────────────►
  TRẠNG THÁI 1: CÒN XA GIỜ HẸN (> 60 phút)                        │  TRẠNG THÁI 2: SẮP TỚI GIỜ HẸN (<= 60 phút)
  - Bàn VẪN TRỐNG & HOẠT ĐỘNG BÌNH THƯỜNG!                        │  - Bàn chuyển sang màu Cam "Đặt trước"
  - Khách vào Bàn 05 lúc 15:50 vẫn đăng nhập & gọi món tự do.     │    để giữ chỗ đón khách đã hẹn.
  - Trên sơ đồ bàn hiện màu Trắng (Trống) kèm dòng nhắc nhỏ:      │  - Khi khách đến, mở bàn gọi món bình thường.
    "Hẹn 19:30 (2 khách)" để nhân viên biết.                      │
```

### 4.2. Thiết kế Hàm Kiểm Tra Khung Giờ Đặt Bàn (`CafeServerService.java` & `FloorPlanView.java`)

1. **Thêm hàm `isReservationLockActive(ReservationInfo res)` trong `CafeServerService.java`:**
   - Parse chuỗi `res.reservationTime` (định dạng `"HH:mm"`, ví dụ `"19:30"`) thành `LocalTime targetTime`.
   - Lấy giờ hệ thống hiện tại `LocalTime now = LocalTime.now()`.
   - Tính số phút chênh lệch: `long minutesUntil = Duration.between(now, targetTime).toMinutes();`
   - **Quy tắc kích hoạt giữ bàn (`RESERVED_ACTIVE`):**
     - Chỉ trả về `true` (khóa giữ bàn màu cam) khi **`minutesUntil >= -30 && minutesUntil <= 60`** (tức là trong vòng **60 phút trước giờ hẹn** đến **30 phút sau giờ hẹn**).
     - Nếu `minutesUntil > 60` (ví dụ hiện tại `15:50`, hẹn `19:30` $\rightarrow$ còn `220 phút` $> 60$): Trả về `false`! Bàn vẫn mang trạng thái thực tế là **`EMPTY` (Bàn trống)** (hoặc **`SERVING`** nếu đang có khách ngồi ăn uống).

2. **Hiển thị Thông Minh trên Sơ đồ bàn (`FloorPlanView.java`) & Màn hình Khách (`LoginController.java`):**
   - **Trên Sơ đồ bàn (`FloorPlanView.java`):**
     - Khi `minutesUntil > 60` (chưa tới giờ giữ bàn) và bàn chưa có đơn:
       - Bàn 05 và Bàn 10 hiển thị giao diện **Bàn trống (Nền trắng, chấm xanh lá)** để nhân viên biết bàn đang dùng được ngay.
       - Ở dòng phụ thứ 3 trên mặt bàn vẫn hiển thị nhắc nhở: **`Hẹn 19:30 • 2 khách`** để nhân viên nắm lịch.
     - Trong **Ngăn Chi Tiết Bàn bên phải (`refreshInspectorDrawer`)**:
       - Nếu bàn có lịch hẹn lúc `19:30` nhưng hiện tại mới `15:50`, hiển thị cả **Nút `[ + Mở bàn & Đặt món ngay ]`** (cho khách hiện tại) LẪN thẻ thông tin **`Lịch hẹn sắp tới: 19:30 (Nguyễn Hoàng Nam • 2 khách)`** (kèm nút sửa/hủy lịch hẹn).
   - **Khi Khách đăng nhập vào `Bàn 5` từ `LoginView`:**
     - Cho phép đăng nhập và gọi món bình thường 100%! Không bao giờ chặn khách chỉ vì bàn có lịch hẹn vào buổi tối.

---

## 5. LỘ TRÌNH THỰC THI THEO TỪNG FILE CHO `AGY CLI`

1. **`src/cafe/services/DatabaseManager.java`:**
   - Tại `findStaffByUsername()` (dòng 328-329) và `authenticateStaff()` (dòng 466-467): Xóa bỏ `userLower.equals("1") ||` và `userLower.equals("5") ||` để số bàn `1` và `5` không bao giờ bị nhận nhầm thành tài khoản `admin` / `cashier1`.
2. **`src/cafe/controllers/LoginController.java`:**
   - Ưu tiên nhánh đăng nhập Khách hàng (`potentialTable > 0` khi mật khẩu trống) trước nhánh kiểm tra nhân sự, đảm bảo nhập `5` hoặc `Bàn 5` vào thẳng giao diện gọi món của Bàn 05.
3. **`src/cafe/services/CafeServerService.java`:**
   - Bổ sung phương thức `isReservationWithinLockWindow(int tableNumber)` (kiểm tra khoảng cách giờ hiện tại so với `reservationTime` trong ngưỡng `[-30 phút, +60 phút]`).
   - Cập nhật `getTableStatus(int tableNumber)`: Nếu bàn không có đơn hàng (`activeOrders`) và có `ReservationInfo`, chỉ trả về `"RESERVED"` khi `isReservationWithinLockWindow` là `true`; ngược lại trả về `"EMPTY"` (nhưng vẫn giữ `ReservationInfo` trong `tableReservations` để hiển thị dòng nhắc lịch hẹn).
4. **`src/cafe/views/FloorPlanView.java` & `src/cafe/views/TableWithChairsNode.java`:**
   - **Thiết kế lại `topBar` & `floorToolbar`:**
     - Xóa nút `btnTopLogout` bị lặp trên `topBar`.
     - Chuyển `btnRefresh`, `btnTransfer`, `btnMerge` lên `topBar`.
     - Rút gọn bộ lọc trên `floorToolbar` thành 4 nút có `setMinWidth(Region.USE_PREF_SIZE)`: `Tất cả (X)`, `● Trống (X)`, `● Có khách (X)`, `● Đặt hẹn (X)` (dùng `Circle` dot, không dùng icon PNG lặp lại).
     - Gắn `setMinWidth(Region.USE_PREF_SIZE)` cho `tabF1`, `tabF2`, `tabF3`.
   - **Tinh gọn thông tin trên mặt bàn (`TableWithChairsNode`):**
     - Bỏ chữ `"Sẵn sàng"` thừa ở dòng 3 đối với bàn trống không có lịch hẹn.
     - Nếu bàn đang trống nhưng có lịch hẹn xa (`> 60 phút`), giữ màu bàn trống (Trắng) nhưng hiển thị dòng nhắc `"Hẹn 19:30 • 2 khách"` ở dòng 3.
     - Trong cột `rightInspector`: Cho phép mở bàn gọi món bình thường đối với bàn có lịch hẹn xa, đồng thời hiển thị thẻ thông tin lịch hẹn bên dưới.
