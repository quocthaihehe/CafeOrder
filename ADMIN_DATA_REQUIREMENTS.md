# TÀI LIỆU YÊU CẦU DỮ LIỆU ADMIN (ADMIN DATA REQUIREMENTS)
*Dự án: CafeOrder — Hệ Thống Quản Trị & Vận Hành Quán Café*  
*Tài liệu dành cho bộ phận thiết kế Cơ sở dữ liệu và Backend API*

---

## 1. TỔNG QUAN
Tài liệu này chuẩn hóa toàn bộ các thực thể dữ liệu (Entities), các trường thông tin (Fields), trạng thái vòng đời (Statuses) và các thao tác nghiệp vụ (Operations) được sử dụng trong giao diện Quản trị viên (Admin Panel). Các định nghĩa này đóng vai trò là hợp đồng dữ liệu (Data Contract) chính thức để chuyển giao sang bước thiết kế Database và phát triển API thật.

---

## 2. CHI TIẾT CÁC THỰC THỂ DỮ LIỆU (ENTITIES & FIELDS)

### 2.1. Người dùng & Phân quyền (Users & Roles)
- **Mô tả:** Quản lý tài khoản nhân sự đăng nhập vào hệ thống (Quản trị, Thu ngân, Pha chế, Phục vụ). Không bao gồm thiết bị bàn Kiosk.
- **Các trường dữ liệu (Fields):**
  - `id`: Định danh duy nhất (Integer, PK, Auto-increment).
  - `username`: Tên đăng nhập (String, Unique, Not Null, Min 4 ký tự).
  - `fullName`: Họ và tên nhân viên (String, Not Null).
  - `password`: Mật khẩu băm (Password Hash, Not Null).
  - `role`: Vai trò (Enum: `ADMIN`, `CASHIER`, `BARISTA`, `STAFF`).
  - `status`: Trạng thái tài khoản (Enum/String: `ACTIVE`, `DISABLED`).
  - `createdAt`: Thời điểm khởi tạo tài khoản (Timestamp/Datetime).
- **Trạng thái (Statuses):**
  - `ACTIVE`: Tài khoản hoạt động bình thường, được phép đăng nhập.
  - `DISABLED`: Tài khoản bị vô hiệu hóa, bị chặn đăng nhập ngay lập tức.
- **Thao tác (Operations):**
  - `Create`: Thêm nhân viên mới (Kiểm tra trùng username, mật khẩu xác nhận).
  - `Read`: Lấy danh sách, tìm kiếm theo từ khóa (username, fullName), lọc theo role và status.
  - `Update`: Cập nhật họ tên, vai trò, trạng thái.
  - `Disable / Enable`: Bật/tắt trạng thái hoạt động (Quy tắc nghiệp vụ: Không cho phép Admin tự vô hiệu hóa tài khoản của chính mình).
  - `Reset Password`: Đặt lại mật khẩu nhân viên.

---

### 2.2. Danh mục thực đơn (Categories)
- **Mô tả:** Phân nhóm các món ăn và đồ uống trong thực đơn.
- **Các trường dữ liệu (Fields):**
  - `id`: Định danh duy nhất (Integer, PK, Auto-increment).
  - `name`: Tên danh mục (String, Unique, Not Null, ví dụ: "Cà phê", "Trà & Trái cây", "Bánh ngọt").
  - `status`: Trạng thái danh mục (Enum/String: `ACTIVE`, `INACTIVE`).
  - `createdAt`: Thời điểm tạo (Timestamp/Datetime).
- **Trạng thái (Statuses):**
  - `ACTIVE`: Danh mục đang hiển thị trên Menu Kiosk.
  - `INACTIVE`: Danh mục tạm ẩn, các món thuộc danh mục này cũng sẽ bị ẩn.
- **Thao tác & Ràng buộc nghiệp vụ (Operations & Constraints):**
  - `Create`: Thêm danh mục mới (Validate tên không rỗng, không trùng lặp).
  - `Read`: Danh sách toàn bộ danh mục, tìm kiếm theo tên.
  - `Update`: Đổi tên danh mục, đổi trạng thái.
  - `Activate / Deactivate`: Bật/tắt trạng thái.
  - **Quy tắc bảo vệ toàn vẹn (Critical Business Rule):** Tuyệt đối **không cho phép chuyển danh mục từ `ACTIVE` sang `INACTIVE` nếu vẫn còn món ăn đang `ACTIVE` (mở bán) thuộc danh mục đó**. Hệ thống phải từ chối thao tác ở cả tầng Service lẫn Database constraint (`RESTRICT`) và yêu cầu vô hiệu hóa hoặc chuyển nhóm món trước.

---

### 2.3. Món ăn & Đồ uống (Menu Items)
- **Mô tả:** Các sản phẩm thức uống, bánh và đồ ăn phục vụ khách hàng.
- **Các trường dữ liệu (Fields):**
  - `id`: Định danh duy nhất (Integer, PK, Auto-increment).
  - `name`: Tên món ăn (String, Unique, Not Null).
  - `price`: Đơn giá bán hiện tại (Decimal/Double, Check price > 0).
  - `category`: Tên hoặc ID danh mục liên kết (FK tới Category, Not Null).
  - `description`: Mô tả thành phần, hương vị đặc trưng (Text/String, Optional).
  - `iconEmoji`: Biểu tượng cảm xúc hoặc URL hình ảnh đại diện (String, ví dụ: "☕", "🥐").
  - `status`: Trạng thái kinh doanh (Enum/String: `ACTIVE`, `INACTIVE`).
  - `updatedAt`: Thời điểm cập nhật giá hoặc thông tin gần nhất (Timestamp).
- **Trạng thái (Statuses):**
  - `ACTIVE`: Đang mở bán, hiển thị và cho phép đặt trên Kiosk bàn.
  - `INACTIVE`: Tạm ngừng bán/Hết nguyên liệu, ẩn khỏi Kiosk.
- **Thao tác (Operations):**
  - `Create`: Thêm món mới (Validate giá > 0, tên không trùng).
  - `Read`: Tìm kiếm theo tên/mô tả, lọc theo danh mục, lọc theo trạng thái active.
  - `Update`: Cập nhật tên, giá bán, danh mục, mô tả, icon.
  - `Activate / Deactivate`: Đổi trạng thái kinh doanh (Bảo lưu lịch sử, không xóa vật lý).

---

### 2.4. Sơ đồ bàn & Thiết bị Kiosk (Tables & Devices)
- **Mô tả & Mô hình CSDL thống nhất:** Hệ thống quản lý thực thể bàn và thiết bị Kiosk máy tính bảng tại bàn.
  > 📌 **CHỐT MÔ HÌNH LƯU TRỮ DATABASE:** Database sử dụng **một bảng duy nhất `tables`** chứa cả thông tin bàn, thiết bị gắn kèm và mã băm xác thực. Không tạo bảng `devices` độc lập trong Database để tránh dư thừa và phân mảnh dữ liệu.
- **Các trường dữ liệu (Fields của bảng `tables`):**
  - `id`: Định danh duy nhất (Integer, PK, Auto-increment).
  - `tableNumber`: Số bàn (Integer, Unique, Not Null, Check > 0).
  - `deviceId`: Mã định danh phần cứng Kiosk gắn tại bàn (String, Unique, Nullable, ví dụ: "DEV-TB01-A9F").
  - `deviceSecretHash`: Mã băm bảo mật của thiết bị (String, BCrypt/Argon2/SHA-256 hash, Nullable). **Tuyệt đối không lưu hoặc hiển thị plaintext secret!**
  - `status`: Trạng thái phục vụ của bàn (Enum/String: `AVAILABLE`, `OCCUPIED`, `OFFLINE`).
  - `createdAt`: Thời điểm tạo bàn (Timestamp).
- **Giao diện quản lý Thiết bị (Devices View):**
  - Trên Admin UI vẫn duy trì màn hình "Quản lý thiết bị (Devices)" riêng biệt để quản trị viên dễ dàng theo dõi tình trạng kết nối mạng Socket (`ONLINE`/`OFFLINE`), lần cuối gửi Heartbeat (`lastSeen`) và trạng thái mật mã xác thực (`credentialStatus`: `ACTIVE`, `RESET_REQUIRED`).
  - Dữ liệu giữa màn hình Tables và Devices được đồng bộ hai chiều (2-way synchronization) để đảm bảo không mâu thuẫn trạng thái.
- **Quy tắc gán / gỡ thiết bị:**
  - Một thiết bị tại một thời điểm chỉ được gán vào tối đa một bàn (`deviceId UNIQUE`).
  - Khi gán thiết bị vào bàn mới, liên kết cũ (nếu có) tự động được gỡ.
  - Thao tác Reset Credential chỉ chuyển trạng thái `credentialStatus` sang `RESET_REQUIRED`, buộc thiết bị cấu hình lại passcode mới khi khởi động, không làm lộ thông tin bí mật.

---

### 2.5. Đơn hàng (Orders)
- **Mô tả:** Phiếu order món của khách tại bàn hoặc do nhân viên lập.
- **Các trường dữ liệu (Fields):**
  - `orderId`: Mã đơn hàng (Integer, PK, ví dụ: 1001, 1025).
  - `requestId`: **Mã định danh yêu cầu chống trùng lặp (Idempotency Key)** (String/UUID, Unique, Not Null).
    * *Cơ chế hoạt động:* Phía gửi yêu cầu (Client Kiosk/Staff terminal) sinh mã UUID duy nhất khi khách bấm gửi đơn. Server dùng `requestId` để kiểm tra chống tạo đơn trùng lặp khi mạng bị chập chờn, timeout hoặc gửi lại yêu cầu. Ràng buộc `UNIQUE` được bảo vệ tuyệt đối ở cấp độ Database.
  - `table`: Số bàn đặt món (Integer, FK tới Tables).
  - `items`: Danh sách các món thuộc đơn (List<OrderItem>).
  - `status`: Trạng thái đơn hàng (Tuân thủ nghiêm ngặt State Machine bên dưới).
  - `timestamp`: Thời điểm tạo đơn (Long/Timestamp).
  - `createdBy`: Người tạo đơn (String, ví dụ: "Bàn 02", "staff01", "cashier01").
  - `timeline`: Dòng thời gian lịch sử chuyển trạng thái của đơn.
  - `totalAmount`: Tổng tiền đơn hàng (Computed = Tổng thành tiền các món có trạng thái hợp lệ).
- **Vòng đời trạng thái đơn hàng (Strict Order State Machine):**
  Quy trình chuyển trạng thái được bảo vệ nghiêm ngặt ở tầng Service và Database, không phụ thuộc vào giao diện:
  * `CREATED` → `CONFIRMED` hoặc `CANCELLED`
  * `CONFIRMED` → `PREPARING` hoặc `CANCELLED`
  * `PREPARING` → `READY` hoặc `CANCELLED`
  * `READY` → `SERVED` hoặc `CANCELLED`
  * `SERVED` → `WAITING_PAYMENT`
  * `WAITING_PAYMENT` → `PAID`
  * `PAID` → `COMPLETED`
  * `COMPLETED` và `CANCELLED` là trạng thái kết thúc (Terminal states), không được phép chuyển tiếp.
  > ⚠️ **Quy tắc quyền hạn:** Admin **không được phép** coi quyền quản trị là quyền phá vỡ quy trình nghiệp vụ (ví dụ: không được nhảy cóc từ `CREATED` sang `PAID`, không được bỏ qua thanh toán, không được chuyển đơn `CANCELLED` quay lại hoạt động).

---

### 2.6. Chi tiết món trong đơn (Order Items)
- **Mô tả:** Các dòng sản phẩm chi tiết thuộc về một Order cụ thể.
- **Các trường dữ liệu (Fields):**
  - `name`: Tên món (String).
  - `qty`: Số lượng đặt (Integer, Check > 0).
  - `unitPrice`: Đơn giá tại thời điểm đặt (Decimal/Double, Chốt giá lịch sử).
  - `note`: Ghi chú biến tấu (String, ví dụ: "Ít đường", "Không đá", "Hâm nóng").
  - `status`: Trạng thái dòng món (Enum/String: `CONFIRMED`, `OUT_OF_STOCK`, `CANCELLED`). Mặc định khi tạo đơn là `CONFIRMED`.
  - `subtotal`: Thành tiền = `qty * unitPrice`.
- **Quy tắc tính tiền (Billing Rule):**
  - Các món có trạng thái `OUT_OF_STOCK` (hết nguyên liệu tại quầy bar) hoặc `CANCELLED` (khách hủy món lẻ) **TUYỆT ĐỐI KHÔNG ĐƯỢC TÍNH VÀO TỔNG TIỀN PHẢI THANH TOÁN** của hóa đơn.
  - *Ghi nhận phạm vi:* Ở phase hiện tại, data model đã được bổ sung đầy đủ trường `status` và hiển thị trên giao diện chi tiết hóa đơn. Luồng tự động tính toán lại tổng tiền khi nhân viên hủy món lẻ sẽ được hoàn thiện toàn diện trong phase KDS/POS tiếp theo.

---

### 2.7. Giao dịch thanh toán (Payments)
- **Mô tả:** Bản ghi thu tiền của hóa đơn.
- **Các trường dữ liệu (Fields):**
  - `id`: Mã giao dịch thanh toán (Integer, PK).
  - `orderId`: Mã đơn hàng thanh toán (Integer, FK tới Orders).
  - `amount`: Số tiền thanh toán (Decimal/Double, Check > 0).
  - `method`: Phương thức thanh toán (Enum/String: `CASH`, `CARD`, `BANK_TRANSFER`).
  - `status`: Trạng thái giao dịch (Enum/String: `SUCCESS`, `PENDING`, `FAILED`).
  - `transactionCode`: Mã đối soát giao dịch ngân hàng/POS (String).
  - `processedBy`: Tài khoản thu ngân xử lý (String, FK tới Users).
  - `createdAt`: Thời gian tạo giao dịch.
  - `paidAt`: Thời gian ghi nhận thanh toán thành công.

---

### 2.8. Báo cáo & Thống kê (Reports)
- **Tổng hợp (Summary KPI):**
  - `totalRevenue`: Tổng doanh thu theo kỳ (chỉ tính các đơn `PAID` / `COMPLETED`).
  - `totalOrders`: Tổng số đơn hàng phát sinh.
  - `completedOrders`: Số đơn hoàn tất thành công.
  - `cancelledOrders`: Số đơn bị hủy bỏ.
  - `averageOrderValue`: Giá trị trung bình trên mỗi đơn = `totalRevenue / completedOrders`.
- **Báo cáo món bán chạy (Product Sales):**
  - `productName`, `category`, `quantitySold`, `revenue`.
- **Báo cáo tỷ trọng danh mục (Category Sales):**
  - `categoryName`, `orderCount`, `revenue`, `percentage` (% đóng góp trên tổng doanh thu).

---

### 2.9. Nhật ký kiểm toán (Audit Logs)
- **Mô tả:** Bản ghi lịch sử các hành động quan trọng trong hệ thống nhằm mục đích đối soát an ninh.
- **Các trường dữ liệu (Fields):**
  - `id`: Mã log (Integer, PK).
  - `timestamp`: Thời điểm diễn ra hành động (Timestamp).
  - `user`: Tài khoản thực hiện hành động (String, ví dụ: "admin01", "cashier02").
  - `action`: Mã hành động (String: `LOGIN`, `CREATE_USER`, `UPDATE_USER`, `DISABLE_USER`, `RESET_PASSWORD`, `CREATE_CATEGORY`, `UPDATE_CATEGORY`, `DISABLE_CATEGORY`, `ENABLE_CATEGORY`, `CREATE_MENU`, `UPDATE_MENU`, `DISABLE_MENU`, `CREATE_TABLE`, `UPDATE_TABLE`, `ASSIGN_DEVICE`, `UNASSIGN_DEVICE`, `RESET_DEVICE_SECRET`, `UPDATE_ORDER_STATUS`, `PAYMENT`, v.v.).
  - `target`: Đối tượng bị tác động (String).
  - `result`: Kết quả thực thi (Enum/String: `SUCCESS`, `FAILED (...)`).
- **Thao tác (Operations):**
  - `Read`: Tra cứu, lọc theo hành động, kết quả và tác nhân. **TUYỆT ĐỐI CHỈ ĐỌC (Read-only)**, không có chức năng sửa/xóa nhật ký.
