# ☕ BẢN THIẾT KẾ CÔNG THÁI HỌC & QUY CHUẨN KÍCH THƯỚC TOÀN DIỆN (RESPONSIVE DUAL-SCALE)
> **Dự án:** `CafeOrder` (Brew & Bean CAFE / CoffeePOS)  
> **Chuyên đề:** Điều chỉnh Kích thước Khung Form, Textbox, Cỡ chữ (Typography) & Icon tương thích hoàn hảo giữa **Cửa Sổ Thường (Normal Window)** và **Toàn Màn Hình (Max Form / 1080p Touch POS)**.  
> **Tiêu chuẩn công thái học:** Tuân thủ chuẩn F&B Touch-Target (Apple HIG & Material Design 3 — kích thước chạm tối thiểu 44×44 px khi Maximize).

---

## MỤC LỤC
1. [Bản So Sánh Quy Chuẩn Kích Thước: Cửa Sổ Thường vs Toàn Màn Hình](#1-bản-so-sánh-quy-chuẩn-kích-thước-cửa-sổ-thường-vs-toàn-màn-hình)
2. [Chi Tiết Điều Chỉnh Khung Form, Textbox, Nút Bấm & Cỡ Chữ Từng Màn Hình](#2-chi-tiết-điều-chỉnh-khung-form-textbox-nút-bấm--cỡ-chữ-từng-màn-hình)
   - [2.1. Màn hình Đăng nhập (LoginView)](#21-màn-hình-đăng-nhập-loginview)
   - [2.2. Màn hình Thực đơn Kiosk Khách hàng (ClientView & ItemCard)](#22-màn-hình-thực-đơn-kiosk-khách-hàng-clientview--itemcard)
   - [2.3. Màn hình Sơ đồ Bàn 2.5D Nhân viên (FloorPlanView)](#23-màn-hình-sơ-đồ-bàn-25d-nhân-viên-floorplanview)
   - [2.4. Màn hình Quản trị Quầy Trung tâm (AdminShellView - CoffeePOS)](#24-màn-hình-quản-trị-quầy-trung-tâm-adminshellview---coffeepos)
   - [2.5. Màn hình Pha chế Quầy Bar (ServerKDSView & OrderTicketCard)](#25-màn-hình-pha-chế-quầy-bar-serverkdsview--orderticketcard)
   - [2.6. Các Hộp thoại Nổi (Dialogs: Giỏ hàng, Tùy biến món, Thanh toán, Đơn hàng)](#26-các-hộp-thoại-nổi-dialogs-giỏ-hàng-tùy-biến-món-thanh-toán-đơn-hàng)
3. [Bảng Tra Cứu Toàn Bộ 72 Icon — Cập Nhật Kích Thước Normal vs Max Form vs File Tải Về](#3-bảng-tra-cứu-toàn-bộ-72-icon--cập-nhật-kích-thước-normal-vs-max-form-vs-file-tải-về)
4. [Hướng Dẫn Tích Hợp Cơ Chế Responsive Tự Động Trong JavaFX](#4-hướng-dẫn-tích-hợp-cơ-chế-responsive-tự-động-trong-javafx)

---

## 1. BẢN SO SÁNH QUY CHUẨN KÍCH THƯỚC: CỬA SỔ THƯỜNG VS TOÀN MÀN HÌNH

Trong môi trường thực tế của chuỗi F&B, phần mềm chạy trên 2 trạng thái màn hình:
* **Chế độ Cửa sổ thường (Windowed Mode - ~1100×720 hoặc 740×700):** Dùng khi dev kiểm thử, chạy song song nhiều cửa sổ Client trên 1 máy tính để mô phỏng.
* **Chế độ Toàn màn hình (Max Form / Maximized - 1920×1080 Full HD hoặc 2K):** Dùng thực tế tại quán trên màn hình cảm ứng POS thu ngân, màn hình Kiosk chân đứng tại sảnh và máy tính bảng iPad tại bàn.

| Thành phần UI | Cửa sổ thường (Normal ~1100×720) | Toàn màn hình (Max Form ~1920×1080) | Lý do điều chỉnh công thái học F&B |
| :--- | :---: | :---: | :--- |
| **Chiều rộng Sidebar điều hướng** | `110px – 160px` | **`180px – 240px`** | Màn hình rộng cần cột sidebar đủ dày để không bị lọt thỏm, chữ rõ ràng từ xa 1 mét. |
| **Chiều cao Ô nhập liệu (Textbox / Search)** | `31px – 34px` | **`46px – 48px`** | Đạt chuẩn Touch-Target tối thiểu 44px của Apple/Google; ngón tay chạm không bị trượt. |
| **Cỡ chữ trong Textbox** | `11.5px – 12px` | **`14.5px – 15px`** | Khách đứng gọi món hoặc thu ngân nhìn rõ nội dung nhập mà không phải dí sát mắt. |
| **Chiều cao Nút chính (CTA Button)** | `34px – 40px` | **`48px – 52px`** | Nút bấm to, bo góc mềm mại (8px), bấm êm tay trên màn cảm ứng. |
| **Cỡ chữ Tiêu đề chính (Page Title)** | `18px – 22px` | **`26px – 32px bold`** | Phân cấp thị giác mạnh mẽ (Visual Hierarchy), đọc lướt nhanh trong 0.5 giây. |
| **Cỡ chữ Nội dung / Mô tả món** | `11px – 12px` | **`13.5px – 14.5px`** | Dễ đọc mô tả thành phần món ăn dưới ánh sáng quán cafe. |
| **Icon nhỏ (Trong ô tìm kiếm, stepper, nút phụ)** | `14×14 – 16×16 px` | **`18×18 – 20×20 px`** | Tương quan cân xứng hoàn hảo với ô nhập 46px và nút 48px. |
| **Icon vừa (Sidebar danh mục, thanh toán)** | `18×18 – 22×22 px` | **`24×24 – 28×28 px`** | Đặt trong container 32×32 hoặc 36×36 px tạo khoảng đệm (padding) thoáng đãng. |
| **Icon lớn (Avatar, Empty state, Success badge)** | `28px – 48px` | **`40px – 64px – 80px`** | Làm điểm nhấn thị giác chính (Focal Point) giữa không gian màn hình rộng. |

---

## 2. CHI TIẾT ĐIỀU CHỈNH KHUNG FORM, TEXTBOX, NÚT BẤM & CỠ CHỮ TỪNG MÀN HÌNH

### 2.1. Màn hình Đăng nhập (`LoginView.java`)
* **Bố cục Hero Banner (Cột trái):**
  - *Normal (740×700):* Rộng ~300px, hiển thị trọn vẹn `login_hero.png` (821×1916).
  - *Max Form (1920×1080):* Rộng **`780px – 820px`**, tự động chuyển sang `login_hero2.png` (1058×1487) hiển thị trọn vẹn poster nghệ thuật.
* **Khung Card Đăng nhập (Cột phải):**
  - *Độ rộng khung card (`formCard`):* Tăng từ `max 440px / min 360px` lên **`max 540px / min 460px`**; padding tăng từ `32px 40px` lên **`48px 56px`**; spacing giữa các khối tăng từ `14px` lên **`20px`**.
  - *Icon Hạt cà phê đỉnh card:* Tăng từ `24px` lên **`36px`** (icon vector **`36×36 px`**).
  - *Tiêu đề "Chào mừng trở lại!":* Font tăng từ `18px bold` lên **`26px bold`**; Dòng phụ tăng từ `11.5px` lên **`14px`**.
  - *Ô nhập "Tài khoản" & "Mật khẩu":*
    - Chiều cao (Height): Tăng từ `34px` lên **`48px`**.
    - Font chữ nhập: Tăng từ `12px` lên **`15px`**; Padding: tăng từ `6px 10px` lên **`10px 14px`**.
    - Icon nhãn (`👤 Tài khoản`, `🔒 Mật khẩu`): Tăng từ `11px` lên **`16×16 px`**.
    - Nút mắt xem mật khẩu (`btnToggleEye`): Tăng từ `12px` lên **`18×18 px`**.
  - *Nút chính "Đăng nhập":* Chiều cao tăng từ `34px` lên **`50px`**; Cỡ chữ tăng từ `12px bold` lên **`16px bold`**; bo góc `8px`.
  - *Nút vai trò `[👤 Nhân viên]` & `[💼 Quản lý]`:* Chiều cao tăng từ `32px` lên **`44px`**; Font tăng từ `11px` lên **`14px bold`**; Icon tăng từ `12px` lên **`16×16 px`**.

---

### 2.2. Màn hình Thực đơn Kiosk Khách hàng (`ClientView.java` & `ItemCard.java`)
* **Thanh Sidebar Danh mục dọc (Cột trái):**
  - Độ rộng sidebar: Tăng từ `110px` lên **`150px – 160px`**; padding tăng từ `16px 8px` lên **`24px 12px`**.
  - Nút "Sơ đồ bàn" (dành cho nhân viên): Chiều cao tăng từ `32px` lên **`44px`**; Font tăng từ `11px` lên **`13.5px bold`**.
  - Nút từng danh mục (`btnCat`):
    - Chiều cao tăng từ `34px` lên **`46px`**; Bo góc `8px`.
    - Container icon cố định (`iconWrapper`): Tăng từ `22×22 px` lên **`32×32 px`**.
    - Icon danh mục: Tăng từ `16×16 px` lên **`22×22 px`**.
    - Tên chữ danh mục: Tăng từ `11px` lên **`14px bold`** (cột chữ thẳng hàng tuyệt đối).
* **Top Bar (Thanh công cụ trên đỉnh):**
  - Nhãn "Bàn 05": Font tăng từ `18px bold` lên **`26px bold`** (Playfair Display).
  - Badge "● Đang phục vụ": Font tăng từ `9.5px` lên **`12px`**; Padding tăng lên `4px 10px`.
  - Ô tìm kiếm món (`txtSearch`):
    - Chiều cao tăng từ `31px` lên **`46px`**; Chiều dài tăng từ `220px` lên **`380px – 420px`**.
    - Cỡ chữ tăng từ `11.5px` lên **`14.5px`**; Icon kính lúp tìm kiếm tăng từ `14px` lên **`18×18 px`**.
  - Nút Giỏ hàng nổi (`btnFloatingCart`):
    - Chiều cao tăng từ `32px` lên **`46px`**; Padding tăng lên `6px 16px 6px 12px`.
    - Icon túi mua sắm: Tăng từ `16×16 px` lên **`22×22 px`**.
    - Chữ "Giỏ hàng": Tăng từ `11.5px bold` lên **`14.5px bold`**.
    - Huy hiệu đếm số lượng (`lblCartBadge`): Tăng từ `9.5px` lên **`12px bold`**; Padding `2px 8px`.
* **Lưới Sản Phẩm (`ItemCard` Grid):**
  - Số lượng cột tự động thích ứng: Cửa sổ thường 3 cột ➔ Toàn màn hình: **4 hoặc 5 cột** (giúp ảnh món giữ tỉ lệ vuông 1:1 đẹp mắt, không bị kéo dẹt).
  - Thẻ món (`ItemCard`):
    - Khung ảnh món ăn: Chiều cao tăng từ `120px` lên **`180px – 200px`**; Bo góc ảnh `8px`.
    - Nút Quick-Add `(+)`: Vòng tròn tăng từ `22×22 px` lên **`34×34 px`**; Icon dấu cộng tăng từ `12px` lên **`18×18 px`** (chạm bằng ngón tay cực kỳ chính xác).
    - Tên món ăn: Font tăng từ `12.5px bold` lên **`15.5px bold`**.
    - Giá tiền món: Font tăng từ `12.5px bold` lên **`16px bold`** (`#C97A44`).
    - Huy hiệu Bestseller / New: Font tăng từ `9px` lên **`11.5px bold`**; Icon tăng lên **`14×14 px`**.

---

### 2.3. Màn hình Sơ đồ Bàn 2.5D Nhân viên (`FloorPlanView.java`)
* **Thanh Navigation Rail trái:**
  - Chiều rộng tăng từ `110px` lên **`150px`**; Spacing giữa các mục tăng từ `5px` lên **`8px`**.
  - Nút điều hướng (Bàn, Thực đơn, Đơn hàng, Thanh toán, Cài đặt):
    - Chiều cao tăng từ `34px` lên **`46px`**; Bo góc `8px`.
    - Icon vector: Tăng từ `16×16 px` lên **`22×22 px`**.
    - Tên mục: Tăng từ `11.5px` lên **`14px bold`**.
* **Top Bar Nhân viên:**
  - Tiêu đề bàn ("Bàn 05"): Font tăng từ `22px bold` lên **`30px bold`**.
  - Avatar nhân viên (`Trần Minh Anh · Nhân viên`):
    - Đường kính avatar tăng từ `28px` lên **`38px`**; Cỡ chữ tên tăng từ `11.5px` lên **`14.5px bold`**; Dòng phụ tăng lên **`11px`**; Mũi tên `▾` tăng lên **`12px`**.
  - Nút CTA `"Vào thực đơn đặt món →"`: Chiều cao tăng từ `34px` lên **`48px`**; Font tăng từ `11.5px` lên **`15px bold`**; Padding `10px 24px`.
* **Bộ lọc tầng (`Tầng 1`, `Tầng 2`, `Tầng 3`):**
  - Chiều cao nút tăng từ `28px` lên **`40px`**; Font tăng từ `11.5px` lên **`14px bold`**; Padding `6px 18px`.
* **Mặt bằng 2.5D (`floorCanvas`):**
  - Chiều cao canvas tăng từ `490px` lên **`700px – 750px`**; Các khối bàn `TableWithChairsNode` tăng kích thước 1.25x; Icon chậu cây Zen tăng từ `32px` lên **`48px`**.

---

### 2.4. Màn hình Quản trị Quầy Trung tâm (`AdminShellView.java` - CoffeePOS)
* **Thanh Sidebar 8 Phân Hệ (Trái):**
  - Chiều rộng tăng từ `160px` lên **`230px – 240px`**; Padding tăng lên `20px 12px`.
  - 8 Nút Sidebar (Tổng quan, Gọi món, Đơn hàng, Màn pha chế, Món & giá, Nhân viên, Báo cáo, Cài đặt):
    - Chiều cao tăng từ `34px` lên **`46px`**; Bo góc `8px`.
    - Icon emoji/SVG: Tăng từ `13px` lên **`20×20 px`** (trong khung 28px).
    - Tên mục: Tăng từ `11.5px` lên **`14.5px bold`**.
  - Profile Quản lý đáy Sidebar (`Minh Anh · Quản lý · Q.3`):
    - Đường kính avatar tăng từ `28px` lên **`38px`**; Tên tăng từ `11.5px` lên **`14.5px bold`**; Chức vụ tăng lên **`11.5px`**.
* **Header Quản trị:**
  - Tiêu đề "Tổng quan hôm nay": Font tăng từ `18px bold` lên **`28px bold`**; Dòng phụ tăng lên **`13.5px`**.
  - Ô tìm kiếm toàn cục `Tìm kiếm... ⌘K`: Chiều cao tăng từ `32px` lên **`46px`**; Chiều dài tăng từ `240px` lên **`380px – 420px`**; Font tăng lên **`14.5px`**; Icon kính lúp tăng lên **`18×18 px`**.
  - Nút tím/xanh `+ Tạo mới`: Chiều cao tăng từ `32px` lên **`46px`**; Font tăng từ `12px` lên **`14.5px bold`**; Icon `+` tăng lên **`18×18 px`**.
* **4 Thẻ Chỉ Số Cốt Lõi (KPI Cards):**
  - Chiều cao thẻ tăng từ `95px` lên **`130px – 140px`**; Padding tăng từ `12px` lên **`18px 20px`**.
  - Con số chính (Doanh thu `8.420.000đ`, Số đơn `126`, AOV `66.800đ`, Thành viên `41%`): Font tăng từ `20px bold` lên **`30px – 32px bold`**.
  - Nhãn tiêu đề KPI: Font tăng từ `11.5px` lên **`14px`**; Dòng tăng trưởng `+12.4%` font tăng lên **`13px bold`**; Icon mũi tên tăng lên **`16×16 px`**.
* **Biểu đồ Xu hướng & Cột Món bán chạy:**
  - Chiều cao biểu đồ tăng từ `220px` lên **`320px – 360px`**; Cỡ chữ các thứ trong tuần (T2..CN) tăng lên **`13px`**.
  - Dòng món bán chạy: Chiều cao tăng từ `34px` lên **`48px`**; Tên món tăng lên **`14.5px bold`**; Số ly bán tăng lên **`14.5px bold`**.
* **Bảng Đơn Gần Đây (Recent Orders Table):**
  - Chiều cao mỗi dòng đơn: Tăng từ `40px` lên **`54px – 58px`**; Cỡ chữ mã đơn, tên thu ngân, số món, tổng tiền tăng từ `11.5px` lên **`14px – 14.5px`**.
  - Badge trạng thái (`Đang pha`, `Hoàn tất`, `Đã huỷ`): Chiều cao tăng lên `26px`; Font tăng lên **`12px bold`**; Padding `4px 12px`.
  - Nút `Xem tất cả →`: Font tăng lên **`13.5px bold`**; Icon mũi tên tăng lên **`16×16 px`**.

---

### 2.5. Màn hình Pha chế Quầy Bar (`ServerKDSView.java` & `OrderTicketCard.java`)
> *Ghi chú:* Nhân viên pha chế thường đứng cách xa màn hình từ 0.8m đến 1.5m, do đó khi Max Form các thông số phải cực kỳ to rõ:
* **Header KDS Barista:**
  - Tiêu đề "KDS • QUẦY ĐIỀU PHỐI PHA CHẾ": Font tăng từ `18px bold` lên **`26px bold`**.
  - 3 Thẻ đếm (`Đang chờ: X`, `Đang pha: Y`, `Đã xong: Z`): Chiều cao tăng từ `28px` lên **`42px`**; Font tăng từ `12px bold` lên **`15.5px bold`**; Padding `6px 16px`.
  - Đồng hồ thời gian thực: Font tăng từ `12.5px` lên **`16px bold`**.
  - Ô tìm kiếm mã đơn / bàn: Chiều cao tăng từ `36px` lên **`48px`**; Chiều dài tăng lên **`360px`**; Font tăng lên **`14.5px`**.
  - Nút "Xóa đơn đã xong": Chiều cao tăng từ `36px` lên **`48px`**; Font tăng lên **`14px bold`**.
* **Thẻ Vé Đơn Hàng (`OrderTicketCard.java`):**
  - Chiều rộng thẻ vé: Tăng từ `260px` lên **`330px – 350px`**; Padding tăng lên `16px`.
  - Tiêu đề số bàn (`BÀN 05`): Font tăng từ `15px bold` lên **`20px bold`**.
  - Mã đơn (`#1001`) & Giờ đặt: Font tăng lên **`14px`**.
  - Dòng từng món ăn: Tên món font tăng từ `12.5px bold` lên **`16px bold`**; Dòng cấu hình (Size L, Đá 50%, Đường 70%) font tăng lên **`13.5px`**; Topping & Ghi chú khách tăng lên **`13px`**.
  - Nút hành động chính (`▶ PHA CHẾ` / `✓ HOÀN TẤT & GỬI BÀN`):
    - Chiều cao tăng từ `36px` lên **`50px`**; Font tăng từ `12px bold` lên **`15.5px bold`**; Icon nút tăng lên **`18×18 px`** (chạm 1 chạm tức thì, không bị trượt).

---

### 2.6. Các Hộp thoại Nổi (Dialogs: Giỏ hàng, Tùy biến món, Thanh toán, Đơn hàng)
* **Kích thước khung hộp thoại:** Chiều rộng tăng từ `380px – 400px` lên **`480px – 520px`**; Padding tăng lên `24px 28px`.
* **Tiêu đề Dialog:** Font tăng từ `17px bold` lên **`24px bold`**; Nút đóng `(×)` tăng từ `16px` lên **`20×20 px`** (khung bấm 32×32 px).
* **Bộ đếm Stepper (`-` / `+`):** Kích thước nút bấm tăng từ `24×24 px` lên **`36×36 px`**; Icon tăng lên **`16×16 px`**; Con số hiển thị tăng lên **`16px bold`**.
* **Thẻ chọn phương thức thanh toán (Tiền mặt, Thẻ, Ví):**
  - Chiều cao thẻ tăng từ `44px` lên **`62px`**; Padding tăng lên `12px 16px`.
  - Icon phương thức: Tăng từ `22×22 px` lên **`30×30 px`**.
  - Tên phương thức: Font tăng từ `11.5px` lên **`15.5px bold`**.
  - Dấu tick chọn `✔`: Tăng từ `11px` lên **`16×16 px bold`**; Mũi tên `›` tăng lên **`18px`**.
* **Bảng tổng kết tiền (Tổng tiền, Giảm giá, Thành tiền):**
  - Dòng Thành tiền: Font tăng từ `15px bold` lên **`22px – 24px bold`**.
* **Nút Xác nhận thanh toán (CTA):** Chiều cao tăng từ `40px` lên **`52px`**; Font tăng từ `12px bold` lên **`16px bold`**.
* **Phiếu xác nhận thành công (`OrderSuccessDialog`):** Icon tích xanh thành công tăng từ `48×48 px` lên **`72×72 px`**; Mã đơn to rõ **`26px bold`**.

---

## 3. BẢNG TRA CỨU TOÀN BỘ 72 ICON — CẬP NHẬT KÍCH THƯỚC NORMAL VS MAX FORM VS FILE TẢI VỀ

> **Quy ước trong bảng:**
> - **Cột `Size Normal`:** Kích thước hiển thị khi cửa sổ ở trạng thái thường (~1100×720).
> - **Cột `Size Max Form`:** Kích thước hiển thị khi phóng to toàn màn hình (Maximized / Full HD 1080p).
> - **Cột `Size Tải Về`:** Kích thước file khuyên dùng khi tải về từ mạng (khuyên dùng file **`.svg`** để co giãn tự động theo code; nếu tải file **`.png`** thì tải kích thước lớn x2 hoặc x3 để hiển thị sắc nét 100%).

### 3.1. Nhóm Quản Trị Quầy Trung Tâm (CoffeePOS Admin / `ServerMain`)

| STT | Tên Icon / Ý nghĩa | Tên Icon tìm trên mạng (Lucide / Flaticon) | Size Normal (px) | Size Max Form (px) | Size Tải Về Đề Xuất (px) | Vị trí xuất hiện cụ thể |
| :---: | :--- | :--- | :---: | :---: | :---: | :--- |
| **01** | **Tổng quan (Dashboard)** | `layout-dashboard`, `bar-chart-3` | 20×20 | **24×24** | **SVG** hoặc **48×48 PNG** | Sidebar trái — Tab 1 "Tổng quan" |
| **02** | **Gọi món tại quầy** | `shopping-cart`, `bag` | 20×20 | **24×24** | **SVG** hoặc **48×48 PNG** | Sidebar trái — Tab 2 "Gọi món" |
| **03** | **Quản lý Đơn hàng** | `receipt`, `clipboard-list` | 20×20 | **24×24** | **SVG** hoặc **48×48 PNG** | Sidebar trái — Tab 3 "Đơn hàng" |
| **04** | **Màn pha chế (KDS)** | `coffee`, `cup-hot` | 20×20 | **24×24** | **SVG** hoặc **48×48 PNG** | Sidebar trái — Tab 4 "Màn pha chế" |
| **05** | **Món & Giá bán** | `tag`, `utensils` | 20×20 | **24×24** | **SVG** hoặc **48×48 PNG** | Sidebar trái — Tab 5 "Món & giá" |
| **06** | **Quản lý Nhân viên** | `users`, `user-check` | 20×20 | **24×24** | **SVG** hoặc **48×48 PNG** | Sidebar trái — Tab 6 "Nhân viên" |
| **07** | **Báo cáo & Tồn kho** | `package`, `boxes` | 20×20 | **24×24** | **SVG** hoặc **48×48 PNG** | Sidebar trái — Tab 7 "Báo cáo & Kho" |
| **08** | **Cài đặt hệ thống** | `settings`, `sliders-horizontal` | 20×20 | **24×24** | **SVG** hoặc **48×48 PNG** | Sidebar trái — Tab 8 "Cài đặt" |
| **09** | **Tìm kiếm toàn cục** | `search`, `magnifying-glass` | 16×16 | **20×20** | **SVG** hoặc **40×40 PNG** | Ô tìm kiếm nhanh `⌘K` ở Header |
| **10** | **Nút tạo mới đơn** | `plus`, `plus-circle` | 16×16 | **20×20** | **SVG** hoặc **40×40 PNG** | Nút `+ Tạo mới` góc trên bên phải |
| **11** | **Mũi tên tăng trưởng (+)** | `trending-up`, `arrow-up-right` | 14×14 | **18×18** | **SVG** hoặc **36×36 PNG** | Cạnh con số `+12.4%`, `+3.1%` thẻ KPI |
| **12** | **Mũi tên giảm (-)** | `trending-down`, `arrow-down-right` | 14×14 | **18×18** | **SVG** hoặc **36×36 PNG** | Thẻ KPI khi chỉ số giảm |
| **13** | **Avatar Quản lý** | `user`, `circle-user-round` | 28×28 | **38×38** | **SVG** hoặc **64×64 PNG** | Hồ sơ `Minh Anh · Quản lý` đáy Sidebar |
| **14** | **Xem tất cả đơn** | `arrow-right`, `chevron-right` | 14×14 | **18×18** | **SVG** hoặc **36×36 PNG** | Nút `Xem tất cả →` bảng Đơn gần đây |

---

### 3.2. Nhóm Đăng Nhập & Phân Luồng (`LoginView.java`)

| STT | Tên Icon / Ý nghĩa | Tên Icon tìm trên mạng (Lucide / Flaticon) | Size Normal (px) | Size Max Form (px) | Size Tải Về Đề Xuất (px) | Vị trí xuất hiện cụ thể |
| :---: | :--- | :--- | :---: | :---: | :---: | :--- |
| **15** | **Logo hạt cà phê** | `coffee-bean`, `coffee-beans-outline` | 24×24 | **36×36** | **SVG** hoặc **72×72 PNG** | Đỉnh khung form đăng nhập |
| **16** | **Tài khoản / Người dùng** | `user`, `user-circle` | 14×14 | **18×18** | **SVG** hoặc **36×36 PNG** | Đứng trước nhãn "Tài khoản (số bàn)" |
| **17** | **Mật khẩu / Ổ khóa** | `lock`, `lock-keyhole` | 14×14 | **18×18** | **SVG** hoặc **36×36 PNG** | Đứng trước nhãn "Mật khẩu" |
| **18** | **Xem mật khẩu (Hiện)** | `eye` | 16×16 | **20×20** | **SVG** hoặc **40×40 PNG** | Nút con mắt ô mật khẩu khi ẩn |
| **19** | **Ẩn mật khẩu (Che)** | `eye-off` | 16×16 | **20×20** | **SVG** hoặc **40×40 PNG** | Nút con mắt ô mật khẩu khi hiện |
| **20** | **Nút vai trò Nhân viên** | `user`, `badge-check` | 14×14 | **18×18** | **SVG** hoặc **36×36 PNG** | Nút chọn nhanh `[👤 Nhân viên]` |
| **21** | **Nút vai trò Quản lý** | `briefcase`, `shield-check` | 14×14 | **18×18** | **SVG** hoặc **36×36 PNG** | Nút chọn nhanh `[💼 Quản lý]` |

---

### 3.3. Nhóm Sơ Đồ Bàn 2.5D & Quản Lý Đơn (`FloorPlanView.java` & `OrdersDialog.java`)

| STT | Tên Icon / Ý nghĩa | Tên Icon tìm trên mạng (Lucide / Flaticon) | Size Normal (px) | Size Max Form (px) | Size Tải Về Đề Xuất (px) | Vị trí xuất hiện cụ thể |
| :---: | :--- | :--- | :---: | :---: | :---: | :--- |
| **22** | **Mục Bàn (Sơ đồ mặt bằng)**| `layout-grid`, `table` | 18×18 | **22×22** | **SVG** hoặc **48×48 PNG** | Mục 1 Navigation Rail trái |
| **23** | **Mục Thực đơn gọi món** | `menu`, `book-open` | 18×18 | **22×22** | **SVG** hoặc **48×48 PNG** | Mục 2 Navigation Rail trái |
| **24** | **Mục Đơn hàng toàn quán** | `orders`, `clipboard-list` | 18×18 | **22×22** | **SVG** hoặc **48×48 PNG** | Mục 3 Navigation Rail trái (OrdersDialog) |
| **25** | **Mục Thanh toán hóa đơn** | `wallet`, `credit-card` | 18×18 | **22×22** | **SVG** hoặc **48×48 PNG** | Mục 4 Navigation Rail trái |
| **26** | **Mục Cài đặt sảnh** | `settings`, `sliders` | 18×18 | **22×22** | **SVG** hoặc **48×48 PNG** | Mục 5 Navigation Rail trái |
| **27** | **Mục Đăng xuất ca trực** | `log-out`, `power` | 18×18 | **22×22** | **SVG** hoặc **48×48 PNG** | Dưới đáy Navigation Rail trái |
| **28** | **Mũi tên menu nhân viên** | `chevron-down`, `triangle-down` | 10×10 | **14×14** | **SVG** hoặc **24×24 PNG** | Cạnh tên `Trần Minh Anh ▾` ở Top bar |
| **29** | **Chậu cây trang trí Zen** | `potted-plant`, `plant` | 32×32 | **48×48** | **SVG** hoặc **96×96 PNG** | Các góc phòng trên Canvas mặt bằng 2.5D |
| **30** | **Bàn ăn trong thẻ đơn** | `table`, `armchair` | 18×18 | **22×22** | **SVG** hoặc **48×48 PNG** | Bên trái mỗi thẻ đơn trong `OrdersDialog` |
| **31** | **Xóa / Dọn dẹp đơn** | `trash-2`, `trash` | 16×16 | **20×20** | **SVG** hoặc **40×40 PNG** | Nút thùng rác `🗑️` ở Header `OrdersDialog` |

---

### 3.4. Nhóm Thực Đơn Kiosk & 8 Danh Mục Món (`ClientView.java` & `ItemCard.java`)

| STT | Tên Icon / Ý nghĩa | Tên Icon tìm trên mạng (Lucide / Flaticon) | Size Normal (px) | Size Max Form (px) | Size Tải Về Đề Xuất (px) | Vị trí xuất hiện cụ thể |
| :---: | :--- | :--- | :---: | :---: | :---: | :--- |
| **32** | **Quay lại Sơ đồ bàn** | `arrow-left`, `table` | 16×16 | **20×20** | **SVG** hoặc **40×40 PNG** | Nút `Sơ đồ bàn` góc trên bên trái |
| **33** | **Giỏ hàng nổi (Floating Cart)**| `shopping-bag`, `shopping-cart` | 18×18 | **24×24** | **SVG** hoặc **48×48 PNG** | Nút Giỏ hàng nổi ở Top bar |
| **34** | **Tìm kiếm món ăn** | `search`, `magnifying-glass` | 16×16 | **20×20** | **SVG** hoặc **40×40 PNG** | Bên trong ô nhập `Tìm kiếm món ...` |
| **35** | **Danh mục "Tất cả"** | `layout-grid`, `apps` | 16×16 | **22×22** | **SVG** hoặc **48×48 PNG** | Mục 1 trên Sidebar danh mục |
| **36** | **Danh mục "Cà phê"** | `coffee`, `cup-hot` | 16×16 | **22×22** | **SVG** hoặc **48×48 PNG** | Mục 2 trên Sidebar danh mục |
| **37** | **Danh mục "Trà & Trái cây"** | `leaf`, `tea` | 16×16 | **22×22** | **SVG** hoặc **48×48 PNG** | Mục 3 trên Sidebar danh mục |
| **38** | **Danh mục "Đá xay & Sinh tố"**| `cup-soda`, `ice-cream-2` | 16×16 | **22×22** | **SVG** hoặc **48×48 PNG** | Mục 4 trên Sidebar danh mục |
| **39** | **Danh mục "Trà sữa"** | `milk-tea`, `glass-straw` | 16×16 | **22×22** | **SVG** hoặc **48×48 PNG** | Mục 5 trên Sidebar danh mục |
| **40** | **Danh mục "Soda & Nước ép"** | `citrus-fruit`, `sparkles` | 16×16 | **22×22** | **SVG** hoặc **48×48 PNG** | Mục 6 trên Sidebar danh mục |
| **41** | **Danh mục "Matcha & Chocolate"**| `sparkles`, `candy` | 16×16 | **22×22** | **SVG** hoặc **48×48 PNG** | Mục 7 trên Sidebar danh mục |
| **42** | **Danh mục "Bánh ngọt"** | `cake-slice`, `croissant` | 16×16 | **22×22** | **SVG** hoặc **48×48 PNG** | Mục 8 trên Sidebar danh mục |
| **43** | **Danh mục "Món ăn nhẹ"** | `utensils`, `sandwich` | 16×16 | **22×22** | **SVG** hoặc **48×48 PNG** | Mục 9 trên Sidebar danh mục |
| **44** | **Thêm nhanh món `(+)`** | `plus`, `add` | 12×12 | **18×18** | **SVG** hoặc **36×36 PNG** | Góc dưới ảnh món (nút tròn 34×34) |
| **45** | **Nhãn Bán chạy (Bestseller)** | `flame`, `star` | 12×12 | **14×14** | **SVG** hoặc **28×28 PNG** | Huy hiệu cam trên ảnh món ăn |
| **46** | **Nhãn Món mới (New)** | `sparkles`, `badge-alert` | 12×12 | **14×14** | **SVG** hoặc **28×28 PNG** | Huy hiệu xanh trên ảnh món ăn |

---

### 3.5. Nhóm Tùy Biến Món, Giỏ Hàng & Thanh Toán (Dialogs)

| STT | Tên Icon / Ý nghĩa | Tên Icon tìm trên mạng (Lucide / Flaticon) | Size Normal (px) | Size Max Form (px) | Size Tải Về Đề Xuất (px) | Vị trí xuất hiện cụ thể |
| :---: | :--- | :--- | :---: | :---: | :---: | :--- |
| **47** | **Đóng hộp thoại `(×)`** | `x`, `close` | 16×16 | **20×20** | **SVG** hoặc **40×40 PNG** | Góc trên bên phải mọi Popup Dialog |
| **48** | **Nút Giảm số lượng `(-)`** | `minus`, `hyphen` | 14×14 | **18×18** | **SVG** hoặc **36×36 PNG** | Bộ đếm Stepper (nút tròn 36×36) |
| **49** | **Nút Tăng số lượng `(+)`** | `plus` | 14×14 | **18×18** | **SVG** hoặc **36×36 PNG** | Bộ đếm Stepper (nút tròn 36×36) |
| **50** | **Xóa món khỏi giỏ** | `trash-2`, `trash` | 16×16 | **20×20** | **SVG** hoặc **40×40 PNG** | Cạnh mỗi dòng món trong `CartModalDialog` |
| **51** | **Mã giảm giá / Voucher** | `ticket`, `tag` | 16×16 | **20×20** | **SVG** hoặc **40×40 PNG** | Cạnh ô nhập mã voucher khuyến mãi |
| **52** | **Mũi tên sang Thanh toán**| `arrow-right`, `chevron-right` | 16×16 | **20×20** | **SVG** hoặc **40×40 PNG** | Nút `Thanh toán →` dưới đáy giỏ hàng |
| **53** | **Quay lại giỏ hàng `(←)`** | `arrow-left`, `chevron-left` | 18×18 | **22×22** | **SVG** hoặc **44×44 PNG** | Header của `PaymentDialog` |
| **54** | **Phương thức Tiền mặt** | `banknote`, `cash` | 22×22 | **30×30** | **SVG** hoặc **60×60 PNG** | Thẻ chọn "Tiền mặt" trong `PaymentDialog` |
| **55** | **Phương thức Thẻ ATM/Visa** | `credit-card`, `bank-card` | 22×22 | **30×30** | **SVG** hoặc **60×60 PNG** | Thẻ chọn "Thẻ ngân hàng" |
| **56** | **Phương thức Ví điện tử** | `wallet`, `qr-code` | 22×22 | **30×30** | **SVG** hoặc **60×60 PNG** | Thẻ chọn "Ví điện tử / VietQR" |
| **57** | **Dấu tích chọn active** | `check`, `check-circle-2` | 14×14 | **18×18** | **SVG** hoặc **36×36 PNG** | Dấu tick `✔` trên thẻ phương thức chọn |
| **58** | **Mũi tên chuyển tiếp `(›)`** | `chevron-right` | 14×14 | **18×18** | **SVG** hoặc **36×36 PNG** | Bên phải các thẻ phương thức chưa chọn |
| **59** | **Xác nhận đặt hàng thành công**| `check-circle-2`, `circle-check` | 48×48 | **72×72** | **SVG** hoặc **144×144 PNG** | Giữa hộp thoại `OrderSuccessDialog` |

---

### 3.6. Nhóm Quầy Pha Chế KDS (`ServerKDSView.java` & `OrderTicketCard.java`)

| STT | Tên Icon / Ý nghĩa | Tên Icon tìm trên mạng (Lucide / Flaticon) | Size Normal (px) | Size Max Form (px) | Size Tải Về Đề Xuất (px) | Vị trí xuất hiện cụ thể |
| :---: | :--- | :--- | :---: | :---: | :---: | :--- |
| **60** | **Đồng hồ thời gian thực** | `clock`, `timer` | 14×14 | **18×18** | **SVG** hoặc **36×36 PNG** | Góc trên bên phải Header KDS |
| **61** | **Tìm bàn / đơn trên KDS** | `search` | 16×16 | **20×20** | **SVG** hoặc **40×40 PNG** | Bên trong ô tìm kiếm vé pha chế |
| **62** | **Xóa đơn đã xong** | `trash-2`, `check-check` | 16×16 | **20×20** | **SVG** hoặc **40×40 PNG** | Nút `Xóa đơn đã xong` trên thanh công cụ |
| **63** | **Cốc cafe khi trống đơn** | `coffee`, `cup-hot` | 48×48 | **80×80** | **SVG** hoặc **160×160 PNG** | Trạng thái "Không có đơn hàng nào cần xử lý" |
| **64** | **Nút Bắt đầu pha `(▶)`** | `play`, `play-circle` | 14×14 | **18×18** | **SVG** hoặc **36×36 PNG** | Nút trên thẻ vé đơn khi ở trạng thái `ĐANG CHỜ` |
| **65** | **Nút Hoàn tất pha `(✓)`** | `check`, `badge-check` | 14×14 | **18×18** | **SVG** hoặc **36×36 PNG** | Nút trên thẻ vé đơn khi ở trạng thái `ĐANG PHA CHẾ` |
| **66** | **Hình thức Dùng tại bàn** | `armchair`, `utensils` | 14×14 | **18×18** | **SVG** hoặc **36×36 PNG** | Thẻ thông tin `TẠI BÀN` trên góc vé KDS |
| **67** | **Hình thức Mang về** | `shopping-bag`, `package` | 14×14 | **18×18** | **SVG** hoặc **36×36 PNG** | Thẻ thông tin `MANG VỀ` trên góc vé KDS |
| **68** | **Đèn Log hệ thống** | `terminal`, `activity` | 12×12 | **15×15** | **SVG** hoặc **30×30 PNG** | Thanh nhật ký `HỆ THỐNG:` dưới đáy KDS |

---

### 3.7. Nhóm Thanh Tiêu Đề Điều Khiển Cửa Sổ (CustomHeader / Window Chrome)

| STT | Tên Icon / Ý nghĩa | Tên Icon tìm trên mạng (Lucide / Flaticon) | Size Normal (px) | Size Max Form (px) | Size Tải Về Đề Xuất (px) | Vị trí xuất hiện cụ thể |
| :---: | :--- | :--- | :---: | :---: | :---: | :--- |
| **69** | **Thu nhỏ cửa sổ (Minimize)** | `minus`, `minimize-2` | 12×12 | **15×15** | **SVG** hoặc **30×30 PNG** | Cụm nút điều khiển cửa sổ góc trên phải |
| **70** | **Phóng to cửa sổ (Maximize)** | `square`, `maximize-2` | 11×11 | **14×14** | **SVG** hoặc **28×28 PNG** | Cụm nút điều khiển cửa sổ góc trên phải |
| **71** | **Khôi phục kích thước (Restore)**| `copy`, `minimize` | 11×11 | **14×14** | **SVG** hoặc **28×28 PNG** | Khi cửa sổ đang ở trạng thái Maximize |
| **72** | **Đóng ứng dụng (Close)** | `x`, `close` | 12×12 | **15×15** | **SVG** hoặc **30×30 PNG** | Nút đỏ `✕` góc ngoài cùng bên phải |

---

## 4. HƯỚNG DẪN TÍCH HỢP CƠ CHẾ RESPONSIVE TỰ ĐỘNG TRONG JAVAFX

Để ứng dụng tự động co giãn kích thước textbox, font chữ, khung hình và icon mượt mà khi người dùng bấm nút **Phóng to (Maximize)** hoặc kéo dãn cửa sổ, bạn chỉ cần áp dụng mẫu thiết kế **Responsive Listener** đã tích hợp sẵn trong dự án:

### Cách 1: Lắng nghe trạng thái Maximize của Stage (Khuyên Dùng)
Trong `start(Stage primaryStage)` hoặc các View:
```java
primaryStage.maximizedProperty().addListener((obs, wasMax, isNowMax) -> {
    applyResponsiveScale(isNowMax);
});
```

Hàm cập nhật thông số thích ứng:
```java
public void applyResponsiveScale(boolean isMaximized) {
    if (isMaximized) {
        // --- CHẾ ĐỘ TOÀN MÀN HÌNH (MAX FORM) ---
        txtSearch.setPrefHeight(46);
        txtSearch.setStyle("-fx-font-size: 14.5px; -fx-padding: 8 16 8 16;");
        
        btnFloatingCart.setPrefHeight(46);
        lblCartBadge.setStyle("-fx-font-size: 12px; -fx-padding: 2 8 2 8;");
        
        sidebar.setPrefWidth(160);
        searchIcon.setScaleX(1.15);
        searchIcon.setScaleY(1.15);
    } else {
        // --- CHẾ ĐỘ CỬA SỔ THƯỜNG (NORMAL) ---
        txtSearch.setPrefHeight(31);
        txtSearch.setStyle("-fx-font-size: 11.5px; -fx-padding: 4 10 4 10;");
        
        btnFloatingCart.setPrefHeight(32);
        lblCartBadge.setStyle("-fx-font-size: 9.5px; -fx-padding: 1 5 1 5;");
        
        sidebar.setPrefWidth(110);
        searchIcon.setScaleX(0.85);
        searchIcon.setScaleY(0.85);
    }
}
```

### Cách 2: Tự Động Co Giãn Tỉ Lệ Vector SVG
Vì dự án dùng lớp `SvgIcons.java`, bạn có thể co giãn icon theo bất kỳ kích thước nào mà **không cần tải lại file ảnh**:
```java
// Khi cửa sổ thường (Normal): scale = 0.8 (~16px - 18px)
SVGPath normalIcon = SvgIcons.createOutlineIcon(SvgIcons.SHOPPING_BAG, 0.8, Color.web("#112823"));

// Khi phóng to Max Form: scale = 1.1 (~22px - 24px)
SVGPath maxFormIcon = SvgIcons.createOutlineIcon(SvgIcons.SHOPPING_BAG, 1.1, Color.web("#112823"));
```
