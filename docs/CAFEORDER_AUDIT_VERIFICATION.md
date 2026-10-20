# BÁO CÁO XÁC MINH KIỂM TOÁN CAFEORDER (AUDIT VERIFICATION REPORT)
## ĐỐI CHIẾU TUYÊN BỐ VỚI BẰNG CHỨNG MÃ NGUỒN VÀ HÀNH VI THỰC TẾ

- **Hệ thống thẩm tra:** CafeOrder (Client – Server – KDS – Admin – Database)
- **Nhánh kiểm toán:** `ui_admin` (sau khi gộp `GiaiDoan4`)
- **Tài liệu gốc được đối chiếu:** `docs/CAFEORDER_REAL_WORLD_AUDIT.md`
- **Bộ kiểm chứng hành vi thực tế:** `test/AuditVerificationSuite.java` & `test/RealWorldComprehensiveAuditTest.java`
- **Nguyên tắc thẩm định:** Không suy đoán, không dùng Reflection ngụy tạo kết quả, chỉ kết luận dựa trên hành vi thực tế khi chạy socket và mã nguồn.

---

## 1. MA TRẬN XÁC MINH CÁC TUYÊN BỐ TRONG BÁO CÁO GỐC

| STT | Tuyên bố trong báo cáo gốc | Bằng chứng mã nguồn thực tế | Phương pháp kiểm chứng | Kết quả thực tế | Trạng thái thẩm định |
|---|---|---|---|---|---|
| **1** | *"Chống tạo đơn trùng lặp với IdempotencyKey"* | `CafeServerService.processedIdempotencyKeys` (ConcurrentHashMap) | Gửi 2 request tuần tự và 2 request đồng thời trên 2 socket thread khác nhau cùng `idempotencyKey` | **Ban đầu:** Gửi tuần tự chặn được; nhưng gửi đồng thời bị Race Condition (Thread 1 sinh đơn 1022, Thread 2 sinh đơn 1023).<br>**Sau khi sửa:** Đồng bộ hóa nguyên tử `synchronized (processedIdempotencyKeys)` ➔ Cả 2 luồng chỉ sinh đúng 1 đơn 1022. | **VERIFIED** *(Sau khi sửa Race Condition)* |
| **2** | *"Dữ liệu đơn được lưu bền vững vào SQL Server qua worker riêng không chặn socket"* | `CafeServerService` dòng 360: `session.send(ack)` chạy TRƯỚC khi `dbExecutor.submit(() -> DatabaseManager.saveOrder(...))` | Rà soát luồng I/O và khối xử lý lỗi `try-catch` | **Phát hiện rủi ro:** Phản hồi ACK gửi về Client trước khi DB bắt đầu lưu. Nếu SQL Server lỗi hoặc sập mạng đúng lúc này, đơn hàng chỉ ghi log lỗi ra console, không có hàng đợi thử lại (Retry Queue), không thông báo lỗi lại cho Client/KDS, đơn bị mất vĩnh viễn nếu Server restart. | **FAILED / VULNERABLE** |
| **3** | *"Server tự xác thực giá, chống gian lận giá 0 đ và hủy voucher lậu"* | `CafeServerService.validateAndSanitizeOrder()` tra cứu `MenuRepository` | Socket Client gửi món giá 0 đ, voucher giả `GIAM100K`, và món hết hàng `inStock=false` | Server tự động áp lại giá gốc niêm yết 64,000 đ, hủy voucher giả về 0 đ, và loại bỏ món hết hàng khỏi đơn. | **VERIFIED** |
| **4** | *"Dữ liệu Menu đã đồng bộ giữa Admin, Client và KDS"* | `AdminMain` dùng `MockMenuItemRepository`. Client & Server dùng `MenuRepository` tĩnh | Thao tác sửa giá món trong `AdminMain` và kiểm tra bên Client | **Không đồng bộ:** Thay đổi trong `AdminMain` chỉ lưu vào MockRepository trong RAM của tiến trình đó. Client và KDS đọc từ `MenuRepository.java` tĩnh nên không nhận được thay đổi nào. | **FAILED** *(Tách biệt 2 kiến trúc)* |
| **5** | *"Tự động trừ kho định lượng BOM khi Barista bấm DONE"* | `CafeServerService.updateOrderStatus()` gọi `DatabaseManager.deductStockForOrder()` | Gọi `updateOrderStatus(orderId, "DONE")` 2 lần liên tiếp cho cùng 1 đơn | **Ban đầu:** Bị trừ kho 2 lần (5000g ➔ 4975g ➔ 4950g) do thiếu cờ kiểm tra.<br>**Sau khi sửa:** Thêm cờ `order.isStockDeducted()` ➔ Lần 2 không trừ tiếp (5000g ➔ 4975g ➔ 4975g). | **VERIFIED** *(Sau khi sửa Double Deduction)* |
| **6** | *"Báo cáo tài chính, giá vốn COGS và lãi ròng động theo ca/ngày"* | `AdminSubViews.java` dòng 1111–1143 (Tab 9 Báo cáo tài chính POS) | Đọc mã nguồn phương thức `renderFinancials` | **Dữ liệu giả lập cứng:** Báo cáo Tab 9 sử dụng mảng dữ liệu chuỗi gán cứng (`data = new String[][]{...}`) gồm các ca sáng/trưa/tối giả, hoàn toàn không tính toán từ `activeOrders` hay cơ sở dữ liệu. | **FAILED** *(Chỉ là giao diện mẫu)* |
| **7** | *"Thanh toán VietQR động và xác nhận giao dịch"* | `PaymentDialog.java` dòng 164–168 và `ClientController.submitOrder` | Kiểm tra luồng gọi hàm khi bấm "Xác nhận thanh toán" | **Mô phỏng 100%:** Bấm thanh toán chỉ nối chuỗi `"Thanh toán: VIETQR"` vào ghi chú của đơn hàng. Không có Webhook, không có API ngân hàng, không lưu bản ghi `Payment` vào database. | **VERIFIED AS SIMULATION** |
| **8** | *"Bộ kiểm thử RealWorldComprehensiveAuditTest chứng minh toàn bộ hệ thống E2E"* | `test/RealWorldComprehensiveAuditTest.java` | Kiểm tra từng ca kiểm thử xem dùng Socket/DB thật hay Reflection | **Phát hiện:** TC-01, 02, 03, 04, 05, 12 dùng Socket thật. Tuy nhiên TC-06, TC-07, TC-13 ở báo cáo trước chỉ dùng Reflection kiểm tra tên hàm/trường để ép kết quả PASS thay vì gửi dữ liệu kiểm tra hành vi. | **VERIFIED WITH CAVEATS** |
| **9** | *"Cơ sở dữ liệu Microsoft SQL Server lưu trữ bền vững"* | `DatabaseManager.isAvailable()` | Kiểm tra kết nối JDBC tới `localhost:1433;databaseName=CafeOrderDB` | Kết quả: `isAvailable() = false`. SQL Server không chạy trong môi trường cục bộ; hệ thống đang vận hành 100% trên cơ chế Fallback In-Memory Seed. | **NOT RUNNING IN ENVIRONMENT** |

---

## 2. NHẬT KÝ KIỂM CHỨNG HÀNH VI THỰC TẾ (REAL EXECUTION LOG)

Đoạn log dưới đây được trích xuất trực tiếp từ bài kiểm thử hành vi thực tế [`test.AuditVerificationSuite`](file:///d:/LTM/DoAnMonHoc/CafeOrder/test/AuditVerificationSuite.java) sau khi khắc phục các lỗi nghiệp vụ:

```text
===============================================================================
  BẮT ĐẦU CHẠY BỘ KIỂM CHỨNG THỰC TẾ (CODE & BEHAVIORAL VERIFICATION)
===============================================================================

--- 1. KIỂM CHỨNG XỬ LÝ TRÙNG LẶP (IDEMPOTENCY KEY) ---
   [Socket Sequential] Lần 1 OrderId: 1021 | Lần 2 OrderId: 1021
   -> Kết quả: PASSED (Gửi tuần tự cùng key trả về đúng OrderId cũ, không sinh đơn mới)

--- 2. KIỂM CHỨNG ĐUA LỆNH ĐỒNG THỜI (CONCURRENT IDEMPOTENCY RACE) ---
   [Socket Concurrent] Thread 1 OrderId: 1022 | Thread 2 OrderId: 1022
   -> Kết quả: PASSED (An toàn tuyệt đối dưới tải đồng thời, 2 luồng chỉ tạo 1 đơn)

--- 3. KIỂM CHỨNG XÁC THỰC GIÁ & VOUCHER PHÍA SERVER ---
   [Server Tamper Check] Subtotal thực: 64000.0 đ | Discount thực: 0.0 đ | Total: 64000.0 đ
   -> Kết quả: PASSED (Server đã tự động sửa giá 0 đ thành giá niêm yết 64,000 đ và hủy voucher lậu)

--- 4. KIỂM CHỨNG CHẶN MÓN HẾT HÀNG TẠI SERVER ---
   -> Kết quả: PASSED (Server đã lọc bỏ món hết hàng khỏi đơn)

--- 5. KIỂM CHỨNG TRỪ KHO BOM KHI CHUYỂN 'DONE' LẶP LẠI ---
   [BOM Deduct] Ban đầu: 5000.0g | Sau lần 1: 4975.0g | Sau lần 2: 4975.0g
   -> Kết quả: PASSED (Có cờ chặn stockDeducted, không trừ kho lặp lại)

--- 6. KIỂM CHỨNG KẾT NỐI CƠ SỞ DỮ LIỆU SQL SERVER THẬT ---
   [DB Check] DatabaseManager.isAvailable(): false
   -> Kết quả: NOT RUNNING / FALLBACK MODE (SQL Server localhost:1433 không bật. Hệ thống đang chạy 100% trên In-Memory Seed!)

===============================================================================
  HOÀN THÀNH BỘ KIỂM CHỨNG THỰC TẾ
===============================================================================
```

---

## 3. PHÂN TÍCH RỦI RO & BẢN CHẤT KỸ THUẬT CỦA CÁC VẤN ĐỀ CÒN TỒN TẠI

### 3.1. Rủi ro mất đồng bộ giữa ORDER_ACK và Ghi Cơ sở dữ liệu (At-least-once vs At-most-once)
- **Vấn đề:** Trong `CafeServerService`, `session.send(ack)` gửi thông báo đơn đã được tiếp nhận cho khách hàng ngay lập tức. Sau đó mới giao nhiệm vụ ghi DB cho `dbExecutor`.
- **Hậu quả:** Nếu SQL Server gặp sự cố (mất mạng, quá tải, lỗi khóa ngoại), khách hàng vẫn nhìn thấy màn hình "Đã tạo đơn thành công", Barista vẫn thấy đơn trên KDS (vì nằm trong RAM). Nhưng đơn hàng này **không hề tồn tại trong database**. Nếu máy chủ bị cúp điện hoặc tắt đi bật lại ngay sau đó, đơn hàng biến mất hoàn toàn.
- **Biện pháp kiến trúc:** Cần đánh dấu cờ `isPersistedInDb` trên `Order`. Nếu DB ghi thất bại, hiển thị cảnh báo màu vàng trên KDS và có luồng Worker chạy định kỳ để thử ghi lại (Retry Queue).

### 3.2. Sự độc lập giữa 2 hệ thống Admin
- Dự án hiện có **2 phân hệ Admin hoàn toàn tách biệt**:
  1. `cafe.views.AdminShellView` (Tab POS trong `ServerMain`): Đọc và ghi trực tiếp vào `activeOrders`, `MenuRepository`, `DatabaseManager.ingredientsList`. Đây là phân hệ sống.
  2. `cafe.views.admin.AdminShellView` (trong `AdminMain`): Chạy độc lập, chỉ tương tác với các `MockRepository` (`MockMenuItemRepository`, `MockUserRepository`...). Không có bất kỳ kết nối Socket hay JDBC nào tới phần còn lại. Mọi thao tác thêm/sửa/xóa ở đây không ảnh hưởng gì tới Client hay KDS.

### 3.3. Báo cáo Tài chính Tab 9 là Dữ liệu giả định
- Trong `AdminSubViews.java`, giao diện Báo cáo tài chính hiển thị rất đẹp mắt với biểu đồ và xuất file CSV, nhưng số liệu được gán cứng từ các mảng chuỗi (`Ca Sáng: 54 đơn, 3,620,000 đ...`).
- Nếu muốn báo cáo phản ánh thực tế, cần viết hàm tổng hợp động duyệt qua danh sách các đơn có trạng thái `DONE`/`PAID` trong `activeOrders` hoặc câu truy vấn `SELECT SUM(total_amount) FROM Orders`.

---

## 4. KẾT LUẬN CHI TIẾT TỪNG PHÂN HỆ

### 4.1. Logic Đặt món và KDS: **ĐẠT (VERIFIED)**
- Luồng gửi đơn từ Client Kiosk qua TCP Socket tới KDS hoạt động trơn tru.
- Đã khắc phục thành công lỗi Race condition của `idempotencyKey` bằng khối khóa nguyên tử.
- KDS hiển thị đúng các thuộc tính Size, Đá, Đường, Topping và chuyển trạng thái từ `QUEUED` ➔ `PREPARING` ➔ `DONE` chuẩn xác.

### 4.2. Giá, Voucher và Thanh toán: **ĐẠT VỀ LOGIC KIỂM SOÁT — MÔ PHỎNG VỀ CỔNG THANH TOÁN**
- **Giá & Voucher:** Server đã thực hiện kiểm soát tính toàn vẹn (Server-side validation), chặn đứng việc Client sửa giá 0 đ hoặc tự bịa mã giảm giá.
- **Thanh toán:** Quy trình thanh toán hiện tại là **mô phỏng nghiệp vụ tại bàn**, chưa tích hợp cổng thanh toán trực tuyến tự động (Webhook/IPN). Cần xác nhận thủ công tại quầy thu ngân.

### 4.3. Kho nguyên liệu (BOM): **ĐẠT (VERIFIED)**
- Đã khắc phục triệt để lỗi trừ kho lặp lại (Double deduction) khi Barista bấm `DONE` nhiều lần nhờ cờ `order.isStockDeducted()`.
- Định lượng 25 nguyên vật liệu được cập nhật chuẩn xác.

### 4.4. Tính bền vững dữ liệu: **CHƯA ĐẠT KHI CHẠY SQL SERVER THẬT (FALLBACK CHẠY TỐT)**
- Cơ chế In-Memory Seed Fallback hoạt động ổn định, đảm bảo ứng dụng không bị sập khi không có SQL Server.
- Khi có SQL Server: Luồng ghi DB bất đồng bộ sau khi đã gửi ACK tiềm ẩn rủi ro mất đơn nếu DB sụp đổ giữa chừng (thiếu Retry/Recovery mechanism).

### 4.5. Đồng bộ Admin với các thành phần còn lại: **CHƯA ĐỒNG BỘ GIỮA ADMINMAIN VÀ SERVER**
- `AdminMain` (Backoffice) vẫn là một đảo dữ liệu riêng biệt chạy trên Mock Repository.
- Quản trị thực tế đang dựa vào `AdminShellView` tích hợp sẵn trong `ServerMain`.

---

## 5. ĐÁNH GIÁ CHUNG VỀ MỨC ĐỘ SẴN SÀNG

```
========================================================================================
  ĐÁNH GIÁ CUỐI CÙNG: SẴN SÀNG CÓ GIỚI HẠN (READY WITH DOCUMENTED LIMITATIONS)
========================================================================================
```

- **Được phép:** Sử dụng hệ thống để demo, báo cáo đồ án, kiểm thử vận hành nghiệp vụ gọi món tại bàn, hiển thị KDS pha chế, trừ kho định lượng và sơ đồ bàn trong môi trường cục bộ (hoạt động rất mượt mà trên nền tảng In-Memory Fallback).
- **Chưa được phép:** Triển khai vào môi trường sản xuất thực tế (Production) của một quán cà phê thật nếu chưa triển khai:
  1. Cổng thanh toán trực tuyến tự động (Webhook xác thực tiền đã vào tài khoản ngân hàng).
  2. Cơ chế Dead-letter / Retry Queue bảo đảm 100% dữ liệu được ghi vào cơ sở dữ liệu bền vững trước khi xác nhận đơn.
  3. Cầu nối JDBC thật thay thế Mock Repository cho cổng quản trị độc lập `AdminMain`.
