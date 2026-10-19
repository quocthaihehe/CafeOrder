# MA TRẬN HÀNH ĐỘNG CỦA QUẢN TRỊ VIÊN (ADMIN ACTION MATRIX)
*Dự án: CafeOrder — Hệ Thống Quản Trị Trung Tâm*

---

## 1. CÂY PHÂN CẤP THAO TÁC CỦA ADMIN (ACTION TREE)

```text
ADMIN
├── 1. Dashboard (Tổng quan)
│   ├── Xem KPI Cards (Doanh thu hôm nay, Đơn hôm nay, Bàn đang dùng, Kiosk online)
│   ├── Xem Biểu đồ Doanh thu 7 ngày gần nhất
│   ├── Xem Tỷ trọng trạng thái đơn hàng (Order status distribution)
│   ├── Xem Top món bán chạy nhất
│   └── Xem Đơn hàng phát sinh gần nhất
│
├── 2. User Management (Quản lý người dùng)
│   ├── Create: Thêm tài khoản nhân viên (Admin, Cashier, Barista, Staff)
│   ├── Read: Tra cứu danh sách, tìm kiếm theo tên/username, lọc theo Role và Status
│   ├── Update: Chỉnh sửa họ tên, đổi vai trò, cập nhật trạng thái
│   ├── Toggle Status: Kích hoạt / Vô hiệu hóa tài khoản (Chặn tự vô hiệu hóa tài khoản Admin đang login)
│   └── Reset Password: Đặt lại mật khẩu nhân viên
│
├── 3. Category Management (Quản lý danh mục)
│   ├── Create: Thêm nhóm món mới (Validate tên duy nhất)
│   ├── Read: Tra cứu danh mục, tìm kiếm theo tên
│   ├── Update: Đổi tên danh mục
│   └── Toggle Status: Ẩn/Hiện danh mục (RÀNG BUỘC: Chặn vô hiệu hóa danh mục nếu có món ăn đang mở bán ACTIVE)
│
├── 4. Menu Management (Quản lý thực đơn)
│   ├── Create: Thêm món ăn / thức uống mới (Validate giá > 0, tên duy nhất)
│   ├── Read: Danh sách món, lọc theo danh mục, lọc theo trạng thái mở bán
│   ├── Update: Thay đổi tên món, danh mục, đơn giá, mô tả, biểu tượng
│   └── Toggle Status: Ngừng bán / Mở bán lại món ăn (Bảo lưu lịch sử đơn hàng, không xóa vật lý)
│
├── 5. Table Management (Quản lý sơ đồ bàn)
│   ├── Create: Thêm bàn mới vào sơ đồ quán (Validate số bàn > 0, không trùng lặp)
│   ├── Read: Tra cứu sơ đồ bàn, lọc theo trạng thái phục vụ
│   └── Update: Điều chỉnh số bàn, cập nhật Device ID gán kèm, đổi trạng thái
│
├── 6. Device Management (Quản lý thiết bị Kiosk)
│   ├── Read: Theo dõi danh sách máy tính bảng Kiosk, tình trạng kết nối Online/Offline
│   ├── Assign: Gán thiết bị Kiosk vào một bàn cụ thể (Tự động cập nhật bảng tables)
│   ├── Unassign: Tháo gỡ thiết bị Kiosk khỏi bàn (Tự động xóa liên kết thiết bị trên bàn)
│   └── Reset Credential: Yêu cầu cấp lại mã bảo mật xác thực (Đổi credentialStatus sang RESET_REQUIRED, không lưu plaintext)
│
├── 7. Order Management (Quản lý đơn hàng)
│   ├── Read: Xem toàn bộ đơn hàng trong ngày và lịch sử (Kèm Request ID chống trùng lặp)
│   ├── Search & Filter: Tìm theo mã đơn (#1025), số bàn, lọc theo trạng thái đơn
│   ├── View Detail: Xem hóa đơn chi tiết (Từng món, số lượng, đơn giá, trạng thái món CONFIRMED/OUT_OF_STOCK/CANCELLED, tổng tiền)
│   ├── View Timeline: Xem lịch sử diễn tiến thời gian của đơn hàng
│   └── Change Status: Can thiệp đổi trạng thái đơn (TUÂN THỦ STATE MACHINE: CREATED -> CONFIRMED -> PREPARING -> READY -> SERVED -> WAITING_PAYMENT -> PAID -> COMPLETED)
│
├── 8. Payment Management (Quản lý giao dịch thanh toán)
│   ├── Read: Xem danh sách các khoản thu tiền và giao dịch POS/Chuyển khoản
│   ├── Search & Filter: Tìm theo mã thanh toán, mã đơn, mã giao dịch, lọc theo hình thức và kết quả
│   └── View Detail: Xem chi tiết chứng từ giao dịch thanh toán
│
├── 9. Reports & Statistics (Báo cáo & Thống kê)
│   ├── Read Summary: Báo cáo tổng doanh thu, số đơn hoàn tất, tỷ lệ hủy, AOV theo kỳ
│   ├── Product Sales Report: Thống kê sản lượng và doanh thu từng món (Sắp xếp theo số lượng / doanh thu)
│   └── Category Sales Report: Thống kê tỷ trọng đóng góp của từng nhóm thực đơn
│
├── 10. Audit Logs (Nhật ký kiểm toán)
│   ├── Read: Tra cứu toàn bộ lịch sử các tác vụ quản trị, đổi giá, thêm tài khoản, đổi trạng thái đơn
│   ├── Search & Filter: Tìm theo người thao tác, hành động, kết quả và đối tượng
│   └── Read-Only Enforcement: Nghiêm cấm mọi hành vi sửa đổi hoặc xóa dữ liệu nhật ký kiểm toán
│
└── 11. System Settings (Cài đặt)
    ├── Cấu hình thông tin quán café, đơn vị tiền tệ, ngôn ngữ
    └── Tùy chọn thông báo chuông và quy trình tự động duyệt đơn
```

---

## 2. MA TRẬN ĐỐI CHIẾU QUYỀN HẠN (COMPREHENSIVE PERMISSION MATRIX)

| Nghiệp vụ \ Vai trò | ADMIN | CASHIER (Thu ngân) | BARISTA (Pha chế) | STAFF (Phục vụ) | TABLE (Kiosk) |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **Xem Dashboard Quản trị** | ✓ | - | - | - | - |
| **Quản lý Tài khoản (Users)** | ✓ | - | - | - | - |
| **Quản lý Thực đơn (Menu)** | ✓ | - | - | - | - |
| **Quản lý Danh mục (Categories)** | ✓ | - | - | - | - |
| **Quản lý Sơ đồ bàn (Tables)** | ✓ | - | - | - | - |
| **Quản lý Thiết bị Kiosk (Devices)**| ✓ | - | - | - | - |
| **Xem Báo cáo Doanh thu (Reports)** | ✓ | - | - | - | - |
| **Xem Nhật ký Kiểm toán (Audit)** | ✓ | - | - | - | - |
| **Xem Danh sách Đơn hàng** | ✓ | ✓ | ✓ | ✓ | Chỉ đơn bàn mình |
| **Tạo Đơn hàng mới** | ✓ | ✓ | - | ✓ | ✓ (Kiosk bàn) |
| **Nhận Đơn / Báo Xong Món (KDS)** | ✓ | - | ✓ | - | - |
| **Xử lý Thu tiền / In Bill** | ✓ | ✓ | - | - | - |
| **Yêu cầu tính tiền (Checkout)** | ✓ | ✓ | - | ✓ | ✓ (Kiosk bàn) |

---

## 3. NGUYÊN TẮC BẢO MẬT & KIỂM SOÁT THAO TÁC CỦA ADMIN

1. **Bảo toàn danh tính:** Mọi thao tác ghi/sửa dữ liệu đều tự động gắn nhãn người thực hiện (`actor = currentLoggedInAdmin`) vào `AuditLog`.
2. **Tuân thủ State Machine:** Admin phải tuân thủ đúng quy trình nghiệp vụ của đơn hàng, không được phép coi quyền quản trị là quyền nhảy cóc trạng thái hay bỏ qua thu tiền.
3. **Bảo vệ toàn vẹn thực đơn:** Không cho phép vô hiệu hóa danh mục nếu vẫn còn món ăn đang mở bán trong danh mục đó.
4. **Không lưu/hiển thị Plaintext Secret:** Mật khẩu nhân viên và mật mã Kiosk bàn phải được băm an toàn (`deviceSecretHash`), giao diện chỉ hiển thị trạng thái đã cấu hình hoặc yêu cầu reset.
5. **Không phá vỡ dữ liệu lịch sử:** Tuyệt đối không xóa vật lý (Hard delete) món ăn, danh mục hoặc người dùng. Chỉ chuyển đổi trạng thái `ACTIVE` / `INACTIVE` / `DISABLED`.
6. **An toàn tài khoản Admin:** Ngăn chặn tuyệt đối việc quản trị viên tự khóa tài khoản của chính mình trong phiên làm việc.
7. **Hộp thoại xác nhận (Confirmation Dialog):** Bắt buộc hiển thị hộp thoại cảnh báo trước các thao tác có tính ảnh hưởng cao (Khóa tài khoản, Ngừng bán món, Reset mật khẩu, Đổi trạng thái đơn hàng).
