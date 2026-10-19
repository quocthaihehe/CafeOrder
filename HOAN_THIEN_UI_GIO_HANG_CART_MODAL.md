# BẢN ĐẶC TẢ HOÀN THIỆN TOÀN DIỆN UI GIỎ HÀNG (CART MODAL)
**Hệ thống:** CafeOrder (Brew & Bean CoffeePOS)  
**Tài liệu:** Đánh giá & Thiết kế nâng cấp Giỏ hàng từ 70% lên chuẩn mực 100% F&B  
**Trạng thái:** Kế hoạch & Đặc tả kỹ thuật (Chưa triển khai code)

---

## 1. CHẨN ĐOÁN UX: TẠI SAO UI HIỆN TẠI CHỈ ĐẠT 70%?

Quan sát giao diện thực tế bạn gửi, giao diện giỏ hàng hiện tại đang mắc phải **7 điểm yếu về mặt trải nghiệm và thẩm mỹ**:

```
[ THỰC TRẠNG GIAO DIỆN HIỆN TẠI (70%) ]
┌──────────────────────────────────────────────┐
│  Giỏ hàng                         Bàn 05 [X] │
│                                              │
│  ┌───┐  Mocha (M)                            │
│  │ẢNH│  48.000 đ                 [-] 1 [+] [🗑]
│  └───┘  Size M • Đá 100%                     │
│    ▲                                         │
│  (ẢNH BỊ BÓP MÉO DẸP LÙN DO KHUNG VUÔNG 52x52)
│                                              │
│  [ KHOẢNG TRẮNG THÊNH THANG VÔ VỊ 50% ]      │ <- Chiều cao cố định (620px)
│                                              │    khiến 1 món bị lọt thỏm
│                                              │
│  ──────────────────────────────────────────  │
│  Tổng cộng (1 món)                  48.000 đ │ <- Chỉ có 1 dòng duy nhất
│  📝 Ghi chú đơn hàng                          │
│  [ Ví dụ: Không đá, ít đường,...           ] │
│                                              │
│  [ + Thêm món Thêm... ] [ 💳 Thanh toá... ]  │ <- LỖI TRÀN CHỮ VÀ LẶP TEXT!
└──────────────────────────────────────────────┘
```

1. **Lỗi ảnh sản phẩm bị bóp méo, co dẹp lùn tịt (Image Distortion Bug):**
   * *Hiện trạng:* Chiếc ly cà phê trong giỏ hàng bị bóp nghẽn ngang và ép dọc, nhìn biến dạng méo mó rất xấu.
   * *Nguyên nhân code:* Trong [`CartItemRow.java`](file:///c:/Users/ACER/Documents/NetBeansProjects/CafeOrder/src/cafe/views/CartItemRow.java) dòng 44 đang khai báo:
     ```java
     ivThumb.setFitWidth(52);
     ivThumb.setFitHeight(52);
     ivThumb.setPreserveRatio(false); // <- THỦ PHẠM CHÍNH!
     ```
   * Thuộc tính `setPreserveRatio(false)` cưỡng bức JavaFX phải bóp méo một bức ảnh chụp dọc (tỉ lệ 2:3 hoặc 3:4) nhét vào một ô vuông `52×52px`, phá hủy hoàn toàn tỉ lệ gốc của bức ảnh.
2. **Lỗi nút bấm bị lặp chữ và tràn mép (`...`):**
   * Nút trái: `[ + Thêm món Thêm ... ]`
   * Nút phải: `[ 💳 Thanh toán -> Thanh toá... ]`
   * *Nguyên nhân:* Nút `Button` vừa có text mặc định ("Thêm món"), vừa gắn thêm `setGraphic(HBox)` chứa nhãn "Thêm món". JavaFX ghép đôi thành *"Thêm món Thêm món"*, dẫn đến chuỗi dài gấp đôi và bị xén thành `...`.
3. **Khoảng trắng trống trải vô lý (Empty Void Space):**
   * Modal đang bị ép chiều cao cố định **`620px`**. Khi khách mới gọi 1 – 2 món, danh sách chỉ chiếm 80px, để lại một khoảng trống khổng lồ ở giữa, tạo cảm giác giao diện sơ sài, thiếu hoàn thiện.
4. **Thiếu tính năng "Chỉnh sửa món" (Edit / Re-customization):**
   * Nếu khách muốn đổi từ Size M sang Size L, hoặc muốn thêm Thạch cà phê: khách **bắt buộc phải bấm xóa món rồi ra ngoài menu tìm lại để đặt lại từ đầu**.
5. **Bảng tính tiền quá nghèo nàn:**
   * Chỉ có duy nhất 1 dòng `Tổng cộng (1 món) ... 48.000 đ`.
   * Thiếu hoàn toàn: *Tạm tính món*, *Ô nhập mã giảm giá / Voucher*, *Số tiền được giảm*, *Thuế VAT*.
6. **Nguy cơ bấm nhầm xóa món (Mis-click Danger):**
   * Nút Xóa `[🗑]` đang nằm ngay sát cạnh nút Tăng số lượng `[+]`. Trên màn hình cảm ứng POS hoặc điện thoại, khách muốn tăng số lượng rất dễ chạm nhầm vào nút xóa.
7. **Khu vực Ghi chú đơn điệu:**
   * Chỉ có một ô nhập văn bản thô. Không có các nút tag gợi ý nhanh (Quick-chips) như: `[Mang về]`, `[Uống tại quán]`, `[Lấy thêm đường]`, `[Ít ngọt]`.

---

## 2. BẢN PHÁC THẢO WIREFRAME MỚI (CHUẨN MỰC 100% F&B)

```
┌────────────────────────────────────────────────────────────────────────┐
│  Giỏ hàng (2 món)               [ 🏷️ BÀN 05 ]           [ Xóa hết ] [✕] │  <- Header đầy đủ tiện ích
│                                                                        │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ ┌─────┐  Mocha                                                   │  │  <- DÒNG MÓN NÂNG CẤP
│  │ │ ẢNH │  48.000 đ   [ ✏️ Đổi tùy chọn ]          ┌──────────────┐ │  │     - Ảnh dọc 3:4 thanh thoát
│  │ │ DỌC │  • Size M • 100% Đá • Ít ngọt            │ [ - ] 1 [ + ]│ │  │     - KHÔNG BỊ BÓP MÉO
│  │ │ 3:4 │  • Thạch cà phê (+8.000 đ)               └──────────────┘ │  │     - Stepper dạng viên thuốc (Pill)
│  │ └─────┘                                                      [🗑]│  │     - Nút xóa tách biệt bên phải
│  └──────────────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │ ┌─────┐  Croissant Bơ Pháp                                       │  │
│  │ │ ẢNH │  32.000 đ                                ┌──────────────┐ │  │
│  │ │ DỌC │  • Hâm nóng giòn                         │ [ - ] 1 [ + ]│ │  │
│  │ └─────┘                                                      [🗑]│  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                                                        │
│  ┌─ Ưu đãi & Voucher ────────────────────────────────────────────────┐  │  <- TÍNH NĂNG VOUCHER MỚI
│  │ [ 🎟️ Nhập mã giảm giá...          ]  [ Áp dụng ]                  │  │     (Kích thích thanh toán)
│  │ ✓ Đã áp dụng mã GIAM10 (-10%)                                    │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                                                        │
│  Ghi chú nhanh cho Bar/Bếp:                                            │  <- QUICK CHIPS 1 CHẠM
│  [ Uống tại bàn ]  [ Mang về ]  [ Ít ngọt ]  [ Không lấy ống hút ]     │
│  [ 📝 Nhập ghi chú thêm cho quán...                                  ] │
│                                                                        │
│  ────────────────────────────────────────────────────────────────────  │  <- CHI TIẾT TÀI CHÍNH ĐA TẦNG
│  Tạm tính (2 món):                                            88.000 đ │
│  Khuyến mãi voucher:                                         - 8.800 đ │  <- Số tiền giảm màu cam/xanh
│  --------------------------------------------------------------------  │
│  TỔNG THANH TOÁN:                                             79.200 đ │  <- Số tiền to nổi bật 19px
│                                                                        │
│  [ + Thêm món khác ]                 [ 💳 Tiến hành Thanh toán → ]     │  <- NÚT BẤM CÂN ĐỐI, HẾT TRÀN CHỮ
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. ĐẶC TẢ CHI TIẾT CÁC GIẢI PHÁP HOÀN THIỆN

### 1. Giải pháp hiển thị ảnh thumbnail nguyên tỉ lệ (Không bị kéo giãn hay bóp dẹp):
* **Nguyên tắc cốt lõi:** Bắt buộc thiết lập `ivThumb.setPreserveRatio(true);`. Tuyệt đối không để `false`.
* **Cấu hình khung ảnh tối ưu:**
  * **Cách A (Khuyên dùng nhất - Khung chữ nhật đứng tỉ lệ 3:4):**
    - Đặt kích thước khung thumbnail: **Rộng `54px` × Cao `72px`** (hoặc `48px × 64px`).
    - Bo góc cong nhẹ: `Radius 8px`.
    - **Hiệu quả:** Vì bộ ảnh của quán vốn là ảnh chụp dọc (2:3 hoặc 3:4), khung thumbnail đứng 3:4 sẽ **ôm vừa khít tỉ lệ của chiếc ly**, ly nước đứng thẳng tự nhiên, thanh thoát, sắc nét và tôn vinh được trọn vẹn vẻ đẹp của thức uống.
  * **Cách B (Khung bo tròn thẻ kem Soft Letterbox):**
    - Đặt khung thumbnail là hộp màu kem nhạt `#F8F4F0` kích thước `60×68px`.
    - Ảnh đặt `PreserveRatio = true` co dãn vừa khít bên trong, căn giữa hoàn hảo. Bất kể ảnh vuông hay ảnh dọc đều hiển thị chuẩn xác 100%, không bao giờ bị méo mó.

### 2. Khắc phục triệt để lỗi nút bấm tràn chữ:
* **Nguyên nhân code:** Thuộc tính `setText("Thêm món")` và `setGraphic(new Label("Thêm món"))` bị xung đột.
* **Quy chuẩn sửa:** Đặt `setText(null)` khi dùng Graphic, chia tỷ lệ nút `38% - 62%`:
  - Nút **"Thêm món khác"** (Secondary): Chiếm 38% chiều rộng, nền trắng kem `#FBF9F5`, viền bo góc 8px.
  - Nút **"Tiến hành Thanh toán →"** (Primary): Chiếm 62% chiều rộng, nền xanh than thương hiệu `#112823`, chữ trắng đậm, hiệu ứng chuyển màu khi hover.

### 3. Chiều cao co dãn động thông minh (Dynamic Height Sizing):
* Thay vì gán cứng `PrefHeight = 620px`:
  - Khi giỏ hàng có **1 – 2 món**: Modal co gọn gàng ở mức **`460px – 500px`**, loại bỏ hoàn toàn khoảng trắng vô vị.
  - Khi giỏ hàng có **từ 3 món trở lên**: Modal mở rộng tối đa **`640px`**, danh sách món tự động xuất hiện thanh cuộn mượt mà.

### 4. Nâng cấp trải nghiệm thẻ món (`CartItemRow`):
* **Nút "Đổi tùy chọn" (Quick Edit):** Bổ sung nút chữ nhỏ màu nâu `#A66A42` kèm icon bút chì `✏️`. Khi bấm vào, hệ thống mở lại bảng chi tiết để khách đổi Size / Đá / Đường / Topping ngay lập tức mà không phải xóa món.
* **Stepper dạng viên thuốc (Pill Stepper):** Gộp `[-]` `[Số lượng]` `[+]` thành một khối thống nhất có viền bo tròn mềm mại.
* **Nút xóa món:** Chuyển thành icon thùng rác màu đỏ xám tinh tế, đặt riêng biệt ở góc trên bên phải để tránh bấm nhầm.

### 5. Bổ sung Ô nhập mã Ưu đãi & Giảm giá (Voucher Section):
* Cung cấp ô nhập mã khuyến mãi (ví dụ: `GIAM10`, `FREESHIP`).
* Nút "Áp dụng" nhanh. Khi nhập đúng, dòng tiền giảm giá lập tức hiển thị màu xanh lá `- 8.800 đ`, kích thích khách hàng hoàn tất đơn hàng ngay lập tức.

### 6. Cụm gợi ý ghi chú nhanh (Quick Note Chips):
* Thay vì bắt khách phải gõ từng chữ trên bàn phím ảo, hiển thị 4 tag bấm chọn nhanh:
  * `[ Uống tại bàn ]` · `[ Mang về (Take away) ]` · `[ Ít ngọt ]` · `[ Không ống hút nhựa ]`.
* Khách chỉ cần chạm vào tag là ghi chú tự động điền vào đơn hàng gửi xuống Bar/Bếp.

### 7. Bổ sung nút "Xóa tất cả" trên Header:
* Cạnh nút đóng `[X]`, thêm nhãn *"Xóa tất cả"* màu đỏ nhạt/xám. Khi khách muốn làm mới giỏ hàng, họ chỉ cần 1 thao tác xác nhận thay vì phải bấm xóa từng món một.

---

## 4. BẢNG SO SÁNH TRỰC QUAN: 70% VS 100%

| Tiêu chí trải nghiệm | Giao diện hiện tại (70%) | Giao diện nâng cấp (100%) |
| :--- | :--- | :--- |
| **Hình ảnh món trong giỏ** | **Bị bóp méo, dẹp lùn (do `preserveRatio=false`)** | **Nguyên tỉ lệ gốc (PreserveRatio=true), khung dọc 3:4 thanh thoát** |
| **Nút bấm đáy** | Bị tràn chữ, lặp từ `Thêm món Thêm...` | Chữ hiển thị trọn vẹn, căn lề hoàn hảo, phân cấp nút chính/phụ rõ ràng |
| **Bố cục khi ít món** | Thủng một khoảng trắng to ở giữa | Tự co dãn chiều cao vừa khít nội dung, gọn gàng, ấm cúng |
| **Thay đổi Size / Topping** | Phải xóa món, ra menu tìm lại | Bấm nút `[ ✏️ Đổi tùy chọn ]` sửa ngay tại chỗ |
| **Khuyến mãi / Voucher** | Hoàn toàn không có | Có ô nhập Voucher và tính tiền giảm giá trực tiếp |
| **Nhập ghi chú** | Bắt buộc phải gõ chữ thủ công | Có sẵn 4 chip 1 chạm (*Mang về, Ít đá, Ít ngọt...*) |
| **Tránh bấm nhầm** | Nút xóa nằm sát nút tăng số lượng | Nút xóa tách biệt, Stepper dạng viên thuốc độc lập |
| **Minh bạch tài chính** | Chỉ có 1 dòng tổng tiền | Tách bạch: Tạm tính $\rightarrow$ Giảm giá $\rightarrow$ Tổng thanh toán |

---

## 5. HƯỚNG DẪN KỸ THUẬT CHO DEVELOPER KHI TRIỂN KHAI

* **File 1: [`CartItemRow.java`](file:///c:/Users/ACER/Documents/NetBeansProjects/CafeOrder/src/cafe/views/CartItemRow.java)**
  ```java
  // Thay thế đoạn ép méo cũ (dòng 42 - 45):
  ivThumb.setFitWidth(54);
  ivThumb.setFitHeight(72);
  ivThumb.setPreserveRatio(true); // BẮT BUỘC: Giữ nguyên tỉ lệ ảnh dọc, không bóp méo
  ivThumb.setSmooth(true);

  Rectangle clip = new Rectangle(54, 72);
  clip.setArcWidth(12);
  clip.setArcHeight(12);
  ivThumb.setClip(clip);
  ```
* **File 2: [`CartModalDialog.java`](file:///c:/Users/ACER/Documents/NetBeansProjects/CafeOrder/src/cafe/views/CartModalDialog.java)**
  * Xóa bỏ việc khai báo text lặp trong `new Button(...)` khi đã có Graphic.
  * Tích hợp khối Voucher Input và Quick Chips.
  * Thiết lập `root.setMinHeight(Region.USE_PREF_SIZE)` và `root.setMaxHeight(640)` thay vì ép cứng `PrefHeight = 620`.
