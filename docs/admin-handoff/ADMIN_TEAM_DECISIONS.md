# DANH MỤC CÁC QUYẾT ĐỊNH KỸ THUẬT CẦN NHÓM THỐNG NHẤT (ADMIN TEAM DECISIONS)

> **Dự án:** CafeOrder  
> **Mục đích:** Tài liệu tổng hợp 10 quyết định kiến trúc và dữ liệu quan trọng giữa nhánh `ui_admin` và các nhánh `Client / Server / KDS / Database` để cả nhóm họp chốt trước khi ghép nhánh (merge) và viết DAO/JDBC thật.  
> **Trạng thái mặc định:** `Chưa quyết định` (chờ nhóm biểu quyết).

---

## QUYẾT ĐỊNH 01: Thống nhất Vòng đời Trạng thái Đơn hàng (Order State Machine)

- **Vấn đề hiện tại:**
  - Module Admin (`OrderService`) đang thực thi đầy đủ **9 trạng thái** chuẩn theo đặc tả Phase 1.5: `CREATED -> CONFIRMED -> PREPARING -> READY -> SERVED -> WAITING_PAYMENT -> PAID -> COMPLETED` (và `CANCELLED`).
  - Trong khi đó, `CafeServerService` hiện tại khi nhận `CREATE_ORDER` thì gán thẳng trạng thái `"CONFIRMED"` và kết thúc ở `"PAID"` (bỏ qua 3 trạng thái `CREATED`, `WAITING_PAYMENT`, `COMPLETED`).
- **Cách Admin đang làm:** Kiểm tra chặt chẽ chuyển trạng thái từng bước một qua `OrderService.getAllowedNextStatuses()`.
- **Cách nhóm (Client/Server/KDS) đang làm:** Chỉ dùng 6 trạng thái (`CONFIRMED`, `PREPARING`, `READY`, `SERVED`, `PAID`, `CANCELLED`).
- **Ưu / Nhược điểm & Rủi ro:**
  - *Giữ 9 trạng thái:* Phản ánh đúng quy trình thực tế tại quán (khách đặt tại bàn -> nhân viên xác nhận -> pha chế -> phục vụ -> khách gọi tính tiền -> thu ngân thu tiền -> hoàn tất & dọn bàn). Rủi ro: Cần thêm nút/luồng chuyển `WAITING_PAYMENT` và `COMPLETED` trên Server/KDS.
  - *Rút gọn về 6 trạng thái:* Ít thao tác bấm hơn trên KDS nhưng không phân biệt được đơn "đang chờ thu ngân thanh toán" (`WAITING_PAYMENT`) và thời điểm "khách đã rời bàn để giải phóng bàn" (`COMPLETED`).
- **Khuyến nghị kỹ thuật:** **Giữ 9 trạng thái chuẩn.** Nếu quán muốn bỏ qua bước xác nhận thủ công khi khách đặt tại bàn, có thể dùng cờ cấu hình `Auto-confirm Order` (đã có trên `SettingsView` của Admin) để Server tự động chuyển `CREATED -> CONFIRMED` ngay khi tạo.
- **Thành viên cần tham gia:** Phụ trách Admin, Phụ trách Server/KDS, Phụ trách Client, Phụ trách Database.
- **Trạng thái:** `Chưa quyết định`

---

## QUYẾT ĐỊNH 02: Chuẩn hóa Trạng thái Dòng món (`OrderItem.status`: `"CONFIRMED"` vs `"DONE"`)

- **Vấn đề hiện tại:**
  - Trong `OrderItem.java`, các trạng thái dòng món được định nghĩa là `CONFIRMED`, `OUT_OF_STOCK`, `CANCELLED`. Hàm `OrderItem.isBillable()` chỉ tính tiền các món có `status == null || "CONFIRMED"`.
  - Tuy nhiên, trong [`CafeServerService.java`](file:///d:/LTM/DoAnMonHoc/CafeOrder/src/cafe/services/CafeServerService.java#L483), khi Barista bấm xác nhận làm xong từng món trên màn hình Server, code cũ lại gán `it.setStatus("DONE")`. Nếu gán `"DONE"`, hàm `isBillable()` sẽ coi món đó không phải `"CONFIRMED"` và dẫn đến tổng tiền đơn bị tính bằng `0`!
- **Cách Admin đang làm:** Dùng `"CONFIRMED"` cho món hợp lệ được phục vụ và tính tiền; `"OUT_OF_STOCK"` và `"CANCELLED"` cho món không tính tiền.
- **Cách nhóm đang làm:** Dùng `"DONE"` khi xác nhận món trên Server.
- **Ưu / Nhược điểm & Rủi ro:** Nếu không thống nhất ngay, khi ghép code Server và Admin, mọi món được Barista bấm xác nhận (`"DONE"`) sẽ biến mất khỏi tổng tiền hóa đơn.
- **Khuyến nghị kỹ thuật:**
  1. Sửa [`CafeServerService.java` dòng 483](file:///d:/LTM/DoAnMonHoc/CafeOrder/src/cafe/services/CafeServerService.java#L483) từ `it.setStatus("DONE")` thành `it.setStatus("CONFIRMED")`.
  2. Hoặc coi `"DONE"` và `"CONFIRMED"` đều là trạng thái hợp lệ tính tiền (`isBillable()`).
- **Thành viên cần tham gia:** Phụ trách Server/KDS, Phụ trách Admin.
- **Trạng thái:** `Chưa quyết định`

---

## QUYẾT ĐỊNH 03: Cơ chế Snapshot Giá Bán (`unitPrice`) vào `OrderItem` khi Tạo Đơn

- **Vấn đề hiện tại:**
  - Khi Client gửi gói tin `CREATE_ORDER`, [`MessageProtocol.parseItems()`](file:///d:/LTM/DoAnMonHoc/CafeOrder/src/cafe/protocol/MessageProtocol.java#L118-L143) chỉ truyền `name`, `qty`, `note` (không có `unitPrice`), nên `OrderItem.unitPrice` mặc định bằng `0.0`.
  - `CafeServerService` hiện tính tiền bằng cách tra tên món trong `MenuRepository` ngay lúc bấm thanh toán. Nếu Admin thay đổi giá món trên Menu trong lúc khách đang dùng bữa, hoặc sau này xem lại báo cáo lịch sử, giá đơn cũ sẽ bị nhảy theo giá mới.
- **Cách Admin đang làm:** Tính tiền đơn hàng và báo cáo doanh thu dựa hoàn toàn trên `OrderItem.getUnitPrice()` (snapshot giá tại thời điểm đặt món).
- **Cách nhóm đang làm:** Tra giá động theo tên món từ `MenuRepository` mỗi lần cần cộng tiền.
- **Khuyến nghị kỹ thuật:** Khi `CafeServerService.createOrder()` nhận danh sách món từ Client, Server phải tra cứu ngay đơn giá hiện hành từ `MenuItemRepository` và gọi `item.setUnitPrice(menuItem.getPrice())` trước khi lưu vào Database.
- **Thành viên cần tham gia:** Phụ trách Server, Phụ trách Database, Phụ trách Admin.
- **Trạng thái:** `Chưa quyết định`

---

## QUYẾT ĐỊNH 04: Liên kết Danh mục (`Category`) và Món ăn (`MenuItem`) trong Database

- **Vấn đề hiện tại:**
  - Trong Java model `MenuItem`, trường `category` đang là kiểu `String` (lưu tên danh mục như `"Cà phê"`) để tương thích với `ClientController`.
  - Trong Database chuẩn (ERD Phase 1.5), bảng `menu_items` nên liên kết với bảng `categories` qua khóa ngoại `category_id INT REFERENCES categories(id)`.
- **Cách Admin đang làm:** Giữ `String category` trên model `MenuItem` và thực hiện cascade cập nhật tên danh mục trong `CategoryService.updateCategory()` khi Admin đổi tên danh mục.
- **Cách nhóm đang làm:** `ClientController` lọc món theo chuỗi `MenuItem.getCategory()`.
- **Khuyến nghị kỹ thuật:**
  - Trong Database: Dùng khóa ngoại `category_id INT NOT NULL REFERENCES categories(id)`.
  - Trong câu lệnh SQL `SELECT` của `MenuItemRepository`: `JOIN categories c ON m.category_id = c.id` để điền tên danh mục vào trường `MenuItem.category` (hoặc thêm trường `int categoryId` vào `MenuItem` làm поля phụ). Cách này vừa chuẩn 3NF trong DB vừa không phải sửa code `ClientController` hay `MenuManagementView`.
- **Thành viên cần tham gia:** Phụ trách Database, Phụ trách Admin, Phụ trách Client.
- **Trạng thái:** `Chưa quyết định`

---

## QUYẾT ĐỊNH 05: Mô hình Vai trò (`Role`) và Cơ chế Đăng nhập hợp nhất

- **Vấn đề hiện tại:**
  - Admin định nghĩa enum `Role` gồm 5 vai trò: `ADMIN`, `CASHIER`, `BARISTA`, `STAFF`, `TABLE` và lưu mật khẩu ở dạng băm `SHA-256`.
  - `CafeServerService.login()` chỉ nhận `"staff"/"123"` và `"client"/"123"` (trả về chuỗi `"CLIENT"`).
- **Khuyến nghị kỹ thuật:**
  - Bảng `users` trong DB lưu các tài khoản nhân sự (`ADMIN`, `CASHIER`, `BARISTA`, `STAFF`) với `password_hash` kiểu `CHAR(64)` (SHA-256).
  - Bảng `devices` và `cafe_tables` lưu thông tin định danh của máy tính bảng tại bàn (`role = TABLE`).
  - Nâng cấp hàm `login()` trên Server để kiểm tra bảng `users` (bằng `User.hashPassword(rawPassword)`) và trả về đúng `Role`.
- **Thành viên cần tham gia:** Phụ trách Server, Phụ trách Database, Phụ trách Admin, Phụ trách Client.
- **Trạng thái:** `Chưa quyết định`

---

## QUYẾT ĐỊNH 06: Định danh & Xác thực Thiết bị Bàn (`tableNumber + deviceId + secretHash`)

- **Vấn đề hiện tại:**
  - Hiện tại trên `ClientController`, khách hàng có thể tự bấm ComboBox đổi từ Bàn 1 sang Bàn 5 tùy ý.
  - Trong Admin (`TableService` & `DeviceService`), mỗi bàn gắn cố định 1–1 với 1 mã thiết bị `deviceId` và có `secretHash` (SHA-256).
- **Khuyến nghị kỹ thuật:**
  - Khi khởi động `ClientMain` tại bàn, cấu hình sẵn `tableNumber`, `deviceId` và `deviceSecret` (trong file cấu hình cục bộ hoặc màn hình thiết lập kỹ thuật viên).
  - Server xác thực `Device.hashSecret(rawSecret).equals(device.getSecretHash())` và khóa ComboBox chọn bàn trên màn hình khách hàng (chỉ cho phép nhân viên `STAFF`/`ADMIN` đổi bàn).
- **Thành viên cần tham gia:** Phụ trách Client, Phụ trách Server, Phụ trách Admin.
- **Trạng thái:** `Chưa quyết định`

---

## QUYẾT ĐỊNH 07: Quy trình Thanh toán (`Payment` Entity vs Gói tin `PAY`)

- **Vấn đề hiện tại:**
  - Gói tin `PAY` hiện tại chỉ gửi `{ "table": 1 }` từ Client/Server và đánh dấu tất cả đơn của bàn đó sang `"PAID"`, không lưu phương thức thanh toán (`CASH`, `CARD`, `BANK_TRANSFER`), không có mã giao dịch (`transactionCode`), và không hỗ trợ trạng thái chờ thanh toán chuyển khoản (`PENDING`).
  - Trong Admin (`OrderService` & `PaymentService`), đơn hàng muốn chuyển từ `WAITING_PAYMENT` sang `PAID` bắt buộc phải có bản ghi `Payment` đạt trạng thái `SUCCESS` với `amount == order.getTotalAmount()`.
- **Khuyến nghị kỹ thuật:**
  - Khi khách tại bàn bấm "Thanh toán", gói tin chuyển đơn sang trạng thái `WAITING_PAYMENT` (và có thể tạo 1 bản ghi `Payment` ở trạng thái `PENDING` nếu khách chọn chuyển khoản QR).
  - Khi Thu ngân (`CASHIER`) hoặc `ADMIN` xác nhận đã nhận tiền (`confirmPendingPayment` hoặc thu tiền mặt trực tiếp), hệ thống lưu `Payment (status = SUCCESS)` rồi chuyển `Order` sang `PAID`.
- **Thành viên cần tham gia:** Phụ trách Server, Phụ trách Client, Phụ trách Database, Phụ trách Admin.
- **Trạng thái:** `Chưa quyết định`

---

## QUYẾT ĐỊNH 08: Kiến trúc Kết nối Dữ liệu của Ứng dụng Admin (`AdminMain`)

- **Vấn đề hiện tại:**
  - `AdminMain` là một JavaFX Application độc lập đang dùng `Mock*Repository`.
  - Nhóm cần chốt xem khi bàn giao cuối kỳ, `AdminMain` sẽ:
    - **Phương án A:** Kết nối trực tiếp tới PostgreSQL/MySQL thông qua `Jdbc*Repository` (chạy trên máy quản lý trong cùng mạng LAN quán cafe) và gửi thông điệp thông báo qua TCP khi cập nhật Menu/Table.
    - **Phương án B:** Mọi thao tác CRUD của Admin đều gửi qua TCP Socket (`MessageProtocol`) tới `ServerMain`, và chỉ `ServerMain` mới kết nối Database.
- **Ưu / Nhược điểm & Rủi ro:**
  - *Phương án A (JDBC trực tiếp cho Admin + TCP cho Client/KDS):* Triển khai cực kỳ nhanh, tận dụng ngay 8 interface `Repository` đã viết sẵn trong nhánh `ui_admin` (chỉ cần viết `JdbcUserRepository`, `JdbcOrderRepository`,... implements các interface đó), không phải định nghĩa thêm 30 loại `MessageType` mới cho TCP.
  - *Phương án B (100% qua TCP Socket):* Đồng nhất tuyệt đối qua cổng TCP 5000 nhưng khối lượng viết parser JSON trên `CafeServerService` sẽ tăng gấp 3 lần.
- **Khuyến nghị kỹ thuật:** **Chọn Phương án A** (viết `Jdbc*Repository` implements 8 interface `cafe.repositories.*` cho cả `ServerMain` và `AdminMain` cùng đọc/ghi chung 1 Database PostgreSQL/MySQL, kết hợp gói tin TCP `RELOAD_MENU` để báo cho Server cập nhật khi Admin sửa thực đơn).
- **Thành viên cần tham gia:** Toàn bộ thành viên nhóm.
- **Trạng thái:** `Chưa quyết định`

---

## QUYẾT ĐỊNH 09: Quy ước Thời gian (`long epoch ms` vs `TIMESTAMP`) trong Database & Model

- **Vấn đề hiện tại:**
  - `Order.timestamp` đang dùng `long` (epoch milliseconds), trong khi `Payment.createdAt`, `Payment.paidAt`, `User.createdAt`, `AuditLog.timestamp` đang dùng chuỗi định dạng `"yyyy-MM-dd HH:mm:ss"`.
- **Khuyến nghị kỹ thuật:**
  - Trong Database PostgreSQL/MySQL: Tất cả các cột thời gian (`created_at`, `paid_at`, `last_seen`) đều dùng kiểu `TIMESTAMP` chuẩn.
  - Trong tầng `Jdbc*Repository`: Khi đọc `ResultSet.getTimestamp(...)`, chuyển thành `.getTime()` (`long`) cho `Order.timestamp` và format `"yyyy-MM-dd HH:mm:ss"` cho các trường hiển thị trên UI để không phải sửa bất kỳ dòng code nào của View hay Service.
- **Thành viên cần tham gia:** Phụ trách Database, Phụ trách Admin.
- **Trạng thái:** `Chưa quyết định`

---

## QUYẾT ĐỊNH 10: Lưu trữ Cấu hình Quán (`SettingsView`) trong Database hay File

- **Vấn đề hiện tại:**
  - Màn hình `SettingsView` của Admin cho phép cấu hình: Tên quán, Địa chỉ, SĐT, Wifi, VAT (%), Phí phục vụ (%), TCP Host/Port, Timeout, và các cờ tự động xác nhận đơn / bắt buộc thanh toán trước khi hoàn tất. Hiện đang lưu trong biến `static` theo phiên chạy.
- **Khuyến nghị kỹ thuật:**
  - Có thể lưu vào file `config/cafe-settings.properties` hoặc bảng `system_settings (setting_key VARCHAR PK, setting_value VARCHAR)` trong Database để cấu hình được giữ nguyên sau khi tắt mở lại ứng dụng.
- **Thành viên cần tham gia:** Phụ trách Database, Phụ trách Admin.
- **Trạng thái:** `Chưa quyết định`
