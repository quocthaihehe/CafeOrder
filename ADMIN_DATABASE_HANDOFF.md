# TÀI LIỆU CHUYỂN GIAO THIẾT KẾ DATABASE (ADMIN DATABASE HANDOFF)
*Dành cho: Thành viên phụ trách thiết kế Cơ sở dữ liệu và Tầng lưu trữ (Persistence Layer)*  
*Dự án: CafeOrder*

---

## 1. MỤC TIÊU VÀ Ý NGHĨA TÀI LIỆU
Tài liệu này được xuất và chuẩn hóa từ quá trình triển khai thực tế của Admin Panel và Phase Admin Fix. Nó mô tả chính xác:
1. **Giao diện người dùng (UI) cần những dữ liệu gì.**
2. **Các ràng buộc nghiệp vụ (Constraints) cần được bảo vệ ở cấp độ Database.**
3. **Mối quan hệ giữa các thực thể (Relationships) và quyết định schema đã chốt.**
4. **Phân định rõ phần Mock đã hoàn thành và phần nghiệp vụ chuyển giao cho các Phase tiếp theo.**

---

## 2. QUAN HỆ GIỮA CÁC THỰC THỂ (ENTITY RELATIONSHIPS)

```text
ROLES (1) ───────────< USERS (N)
USERS (1) ───────────< AUDIT_LOGS (N)

CATEGORIES (1) ──────< MENU_ITEMS (N)

TABLES (1) ──────────< ORDERS (N)
[Bảng tables chứa cả: device_id & device_secret_hash]

ORDERS (1) ──────────< ORDER_ITEMS (N)
MENU_ITEMS (1) ──────< ORDER_ITEMS (N)

ORDERS (1) ──────────< PAYMENTS (N)
USERS (1) ───────────< PAYMENTS (N)  [Người xử lý thu ngân]

ORDERS (1) ──────────< ORDER_STATUS_HISTORY (N)
```

### Chi tiết các mối quan hệ và quyết định kiến trúc đã chốt:
- **`USERS -> ROLES` (N - 1):** Nhiều tài khoản thuộc về một vai trò nhất định (`ADMIN`, `CASHIER`, `BARISTA`, `STAFF`).
- **`MENU_ITEMS -> CATEGORIES` (N - 1):** Nhiều món ăn thuộc về một danh mục.
  * *Ràng buộc:* Thiết lập `ON DELETE RESTRICT`. Ở tầng Service, ngăn chặn chuyển `status='INACTIVE'` nếu vẫn còn món ăn đang `ACTIVE`.
- **`TABLES` & `DEVICES` (Quyết định một bảng):**
  * Không tạo bảng `devices` riêng trong CSDL.
  * Toàn bộ thông tin Kiosk bàn được lưu trực tiếp trên bảng `tables`: gồm `device_id` (chuỗi định danh phần cứng) và `device_secret_hash` (mã băm bảo mật Kiosk).
  * Màn hình "Devices" trên Admin UI chỉ là một góc nhìn (View) chuyên biệt giúp người quản trị quản lý trạng thái mạng và cấp lại mã bảo mật, truy vấn trực tiếp từ bảng `tables`.
- **`ORDERS -> TABLES` (N - 1):** Một bàn phát sinh nhiều đơn hàng.
- **`ORDERS.request_id` (Idempotency Key):**
  * Bắt buộc là trường `VARCHAR(64)` hoặc `UUID`, có ràng buộc `UNIQUE` và `NOT NULL`.
  * Được Client tạo ra một lần khi gửi đơn. Server kiểm tra `request_id` để chặn triệt để tình trạng tạo trùng đơn khi mạng bị timeout hoặc rớt kết nối.
- **`ORDER_ITEMS -> ORDERS` (N - 1):** Một đơn hàng chứa nhiều dòng món.
  * Cột `unit_price`: **BẮT BUỘC** lưu lại đơn giá tại thời điểm đặt hàng (Price Snapshot). Tuyệt đối không tính thành tiền bằng JOIN sang `menu_items` lấy giá hiện tại.
  * Cột `status`: Bổ sung trạng thái món (`CONFIRMED`, `OUT_OF_STOCK`, `CANCELLED`). Mặc định là `CONFIRMED`.
  * *Quy tắc tính tiền:* Các món có trạng thái `OUT_OF_STOCK` hoặc `CANCELLED` **không được tính vào tổng tiền phải thanh toán**.
- **`PAYMENTS -> ORDERS` (N - 1):** Một đơn hàng có thể có một hoặc nhiều bản ghi thanh toán. Cột `processed_by` là FK trỏ tới `users`.
- **`AUDIT_LOGS -> USERS` (N - 1):** Bản ghi nhật ký kiểm toán, chỉ đọc (Read-only), lưu lại vết thao tác của người dùng.

---

## 3. RÀNG BUỘC TOÀN VẸN DỮ LIỆU BẮT BUỘC (INTEGRITY CONSTRAINTS)

### 3.1. Ràng buộc duy nhất (UNIQUE Constraints)
- `users.username`: Duy nhất, không phân biệt hoa thường.
- `categories.name`: Tên danh mục không được trùng lặp.
- `menu_items.name`: Tên món không được trùng lặp trong hệ thống thực đơn.
- `tables.table_number`: Số bàn phải duy nhất.
- `tables.device_id`: Mã thiết bị Kiosk phải duy nhất (Unique khi khác null).
- `orders.request_id`: Bắt buộc UNIQUE toàn hệ thống.

### 3.2. Ràng buộc kiểm tra (CHECK Constraints)
- `menu_items.price > 0`: Giá bán của món phải là số dương.
- `order_items.quantity > 0`: Số lượng đặt phải lớn hơn 0.
- `order_items.unit_price >= 0`: Đơn giá không âm.
- `order_items.status IN ('CONFIRMED', 'OUT_OF_STOCK', 'CANCELLED')`.
- `payments.amount > 0`: Số tiền thanh toán phải lớn hơn 0.
- `tables.table_number > 0`: Số bàn phải là số nguyên dương.
- `orders.status IN ('CREATED', 'CONFIRMED', 'PREPARING', 'READY', 'SERVED', 'WAITING_PAYMENT', 'PAID', 'COMPLETED', 'CANCELLED')`.

### 3.3. Ràng buộc NOT NULL
- `users`: `username`, `password_hash`, `full_name`, `role_id`, `status`.
- `categories`: `name`, `status`.
- `menu_items`: `name`, `category_id`, `price`, `status`.
- `tables`: `table_number`, `status`.
- `orders`: `request_id`, `table_id`, `status`, `total_amount`, `created_at`.
- `order_items`: `order_id`, `menu_item_id`, `quantity`, `unit_price`, `status`.
- `payments`: `order_id`, `amount`, `method`, `status`.

---

## 4. QUY TẮC STATE MACHINE ĐƠN HÀNG (ORDER STATE TRANSITIONS)

```text
    ┌──────────┐
    │ CREATED  ├────────────────────────┐
    └────┬─────┘                        │
         │ (Xác nhận đơn)               │
         ▼                              │
    ┌───────────┐                       │
    │ CONFIRMED ├───────────────────────┤
    └────┬──────┘                       │
         │ (Bắt đầu làm)                │
         ▼                              │
    ┌───────────┐                       │
    │ PREPARING ├───────────────────────┤
    └────┬──────┘                       │
         │ (Pha chế xong)               │
         ▼                              │
    ┌──────────┐                        │
    │  READY   ├────────────────────────┤
    └────┬─────┘                        │
         │ (Bưng ra bàn)                │
         ▼                              │
    ┌──────────┐                        │
    │  SERVED  │                        │
    └────┬─────┘                        │
         │ (Yêu cầu thanh toán)         │
         ▼                              │
    ┌─────────────────┐                 │
    │ WAITING_PAYMENT │                 │
    └────┬────────────┘                 │
         │ (Thu tiền xong)              │
         ▼                              │
    ┌──────────┐                        │
    │   PAID   │                        │
    └────┬─────┘                        │
         │ (Đóng đơn)                   │
         ▼                              ▼
    ┌───────────┐                 ┌───────────┐
    │ COMPLETED │ (Terminal)      │ CANCELLED │ (Terminal)
    └───────────┘                 └───────────┘
```

- Các trạng thái hợp lệ tiếp theo:
  * `CREATED` → `CONFIRMED`, `CANCELLED`
  * `CONFIRMED` → `PREPARING`, `CANCELLED`
  * `PREPARING` → `READY`, `CANCELLED`
  * `READY` → `SERVED`, `CANCELLED`
  * `SERVED` → `WAITING_PAYMENT`
  * `WAITING_PAYMENT` → `PAID`
  * `PAID` → `COMPLETED`
  * `COMPLETED`, `CANCELLED` là trạng thái kết thúc (Terminal states), cấm chuyển tiếp.

---

## 5. DANH MỤC TRẠNG THÁI CHUẨN HÓA (ENUM / STATUS SPECIFICATIONS)

| Thực thể | Tên trường | Danh sách giá trị hợp lệ |
| :--- | :--- | :--- |
| **User** | `status` | `'ACTIVE'`, `'DISABLED'` |
| **Category** | `status` | `'ACTIVE'`, `'INACTIVE'` |
| **MenuItem** | `status` | `'ACTIVE'`, `'INACTIVE'` |
| **Table** | `status` | `'AVAILABLE'`, `'OCCUPIED'`, `'OFFLINE'` |
| **Table** | `device_secret_hash` | Chuỗi băm mật mã Kiosk (BCrypt hash) hoặc NULL |
| **OrderItem** | `status` | `'CONFIRMED'`, `'OUT_OF_STOCK'`, `'CANCELLED'` |
| **Order** | `status` | `'CREATED'`, `'CONFIRMED'`, `'PREPARING'`, `'READY'`, `'SERVED'`, `'WAITING_PAYMENT'`, `'PAID'`, `'COMPLETED'`, `'CANCELLED'` |
| **Payment** | `method` | `'CASH'`, `'CARD'`, `'BANK_TRANSFER'` |
| **Payment** | `status` | `'SUCCESS'`, `'PENDING'`, `'FAILED'` |
| **AuditLog** | `result` | `'SUCCESS'`, `'FAILED'` |

---

## 6. PHÂN ĐỊNH PHẠM VI (MOCK HIỆN TẠI VS. PHASE DATABASE/BACKEND TIẾP THEO)

| Hạng mục | Đã hoàn thành ở Phase Admin Fix (Mock UI) | Nhiệm vụ của Phase Database & Backend tiếp theo |
| :--- | :--- | :--- |
| **Quản lý danh mục** | Service chặn vô hiệu hóa nếu có món active; UI thông báo lỗi chi tiết. | Database bảo vệ bằng FK `ON DELETE RESTRICT`. |
| **Order State Machine** | Service & UI ép đúng 100% luồng chuyển trạng thái; chặn Admin nhảy cóc. | Viết hàm / Trigger / Service Database bảo vệ ràng buộc state transition. |
| **Chống trùng đơn (Idempotency)** | Model `Order` có `requestId` (UUID); MockRepo sinh và lưu UUID duy nhất. | Client thật sinh UUID gửi qua TCP; Database bắt `UNIQUE(request_id)`. |
| **Trạng thái từng món (OrderItem)** | Model có `status`; UI hiển thị badge món `CONFIRMED`/`OUT_OF_STOCK`/`CANCELLED`. | KDS cập nhật out-of-stock; Trigger/Service DB tự trừ tiền món hủy khỏi bill. |
| **Bàn & Kiosk bàn** | Model `CafeTable` lưu `deviceSecretHash` băm; đồng bộ 2 chiều Mock Table & Device. | Database tạo 1 bảng `tables` duy nhất có `device_id` và `device_secret_hash`. |

---

## 7. KẾT LUẬN
Mã nguồn Admin UI, Service và Data Model hiện tại đã được **chuẩn hóa 100% khớp với đặc tả kỹ thuật Phase 1.5 và các yêu cầu nghiệp vụ khắt khe**. Bộ phận Database có thể sử dụng tài liệu này làm căn cứ chính thức để thiết kế DDL `schema.sql` và khởi tạo CSDL PostgreSQL thật mà không lo gặp bất kỳ mâu thuẫn hay thiếu sót nào.
