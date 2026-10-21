# KẾ HOẠCH NÂNG CẤP GIAO DIỆN ADMIN & ĐỒNG BỘ HÓA THEO CHUẨN CLIENT (PLAN UI 4.5)

> **Mục tiêu cốt lõi:**
> 1. **Loại bỏ triệt để tình trạng cắt cụt chữ với dấu ba chấm (`...`)** trên toàn bộ các thẻ, bảng và tiêu đề trong hệ thống.
> 2. **Đối chiếu và đồng bộ hóa 100% phong cách thiết kế từ phân hệ Client sang Admin**: Áp dụng sự thanh lịch, tối giản, dịu mắt của Client (`ClientView`, `CartModalDialog`, `PaymentDialog`, `OrderSuccessDialog`) vào Admin.
> 3. **Thay thế toàn bộ các nút bấm "lạc quẻ", màu sắc chắp vá, viền thô kệch** tại Admin bằng chuẩn Button đồng nhất (Evergreen `#112823`, Nền phụ `#F5EFEB`, bo góc `8px`, font chữ phân cấp rõ ràng).
> 4. **Xóa bỏ các hiển thị phi logic** (như Bánh ngọt lại có *Đá 100% • Chuẩn đường*, hay *Ghi chú đơn: Thanh toán: TIỀN MẶT* hiện trên vé bếp).

---

## PHẦN 1: MỔ XẺ CHI TIẾT CÁC ĐIỂM "LẠC QUẺ & RỐI MẮT" TỪ HÌNH ẢNH THỰC TẾ

Dựa trên hình ảnh bạn vừa cung cấp (`media_1792475982883_c5b93154.png`), giao diện Admin hiện tại đang lộ rõ 5 khuyết điểm lớn gây cảm giác rối mắt và thiếu chuyên nghiệp:

### 1.1. Lỗi vỡ giao diện & cắt cụt chữ thành dấu ba chấm (`BÀN ...`, `#10...`, `ĐANG PHA C...`)
* **Hiện tượng trên hình (Thẻ bên phải):**
  ```text
  [BÀN ...]  #10...    [ĐANG PHA C...]  24:30 (24p)
  ```
* **Nguyên nhân kỹ thuật trong code (`OrderTicketCard.java` dòng 93–128):**
  - Thẻ phiếu KDS có chiều rộng cố định khoảng `290px – 320px` với padding `14px` mỗi bên, tức không gian khả dụng bên trong chỉ còn khoảng **`262px`**.
  - Header đang cố nhồi nhét **4 phần tử riêng biệt trên cùng 1 hàng ngang**:
    1. Huy hiệu loại bàn: `[BÀN 09]` (padding `3 8`, font `12px Bold`) ~ `60px`
    2. Mã đơn: `#1004` ~ `45px`
    3. Huy hiệu trạng thái: `[ĐANG PHA CHẾ]` (padding `3 8`, font `11px Bold`) ~ `100px`
    4. Đồng hồ đo SLA: `24:30 (24p)` ~ `75px`
  - Tổng chiều rộng cần thiết: `60 + 45 + 100 + 75 = 280px` (lớn hơn không gian khả dụng `262px`).
  - Hệ quả: Bộ engine layout của JavaFX tự động co hẹp và cắt cụt toàn bộ nội dung thành: `BÀN ...`, `#10...`, `ĐANG PHA C...`. Người pha chế nhìn từ xa không thể đọc được là Bàn mấy, Mã đơn bao nhiêu, Trạng thái gì!

---

### 1.2. Hiển thị thông tin phi logic (Bánh ngọt có Đá & Đường)
* **Hiện tượng trên hình:**
  - `Cheesecake Nướng Basque` `Size M` `• 100% Đá • Chuẩn đường`
  - `Croissant Bơ Pháp` `Size M` `• 100% Đá • Chuẩn đường`
* **Nguyên nhân:**
  - Trong `OrderItem.java`, các thuộc tính `icePercent` và `sugarPercent` mặc định đều bằng `100`.
  - Khi render thẻ KDS, code duyệt qua mọi món và in đá/đường một cách máy móc, không phân biệt món đó là **Đồ uống** hay **Bánh ngọt/Món ăn**.
  - Barista và bếp nhìn vào thấy bánh sừng bò hay bánh cheesecake mà lại có *100% đá* thì rất vô lý và gây nhiễu thị giác.

---

### 1.3. Nút bấm bị phân mảnh màu sắc, lệch chuẩn Design System
* **Hiện tượng trên hình:**
  - Thẻ 1 (Đang chờ): Nút **`[ ▶ Bắt đầu pha chế ]`** dùng màu đen/xanh đậm `#112823`.
  - Thẻ 2 (Đang pha): Nút **`[ ✓ Hoàn tất món & Báo phục vụ ]`** lại nhảy sang màu xanh lá cây chói `#15803D`.
  - Hai thẻ đặt cạnh nhau tạo nên hiệu ứng bàn cờ so le (thẻ đen, thẻ xanh lá chói), hoàn toàn mất đi sự điềm đạm, sang trọng của thương hiệu Brew & Bean.
  - Trên thanh công cụ phía trên: Các nút lọc `[ Tất cả đơn ] [ Đang chờ ] [ Đang pha chế ] [ Đã xong ]` lại dùng nền trắng, viền xám mỏng `#D8D1C3`, bo góc nhọn kiểu nút bấm web cổ điển, đối chọi hoàn toàn với dải nút chọn trạm bo tròn `999px` nằm ngay bên trên!

---

### 1.4. Thông tin thanh toán chen ngang vào phiếu làm bếp
* **Hiện tượng trên hình:**
  ```text
  📌 Ghi chú đơn: Thanh toán: TIỀN MẶT
  ```
* **Nguyên nhân:**
  - Khi khách thanh toán tại quầy, chuỗi `"Thanh toán: TIỀN MẶT"` bị ghi đè vào trường `note` của đơn hàng.
  - Thẻ KDS đem nguyên trường này hiển thị thành một khung ghi chú to tướng màu vàng.
  - Nhân viên pha chế và làm bếp **chỉ quan tâm đến cách làm món** (như *Nướng giòn*, *Mang nước lọc kèm*, *Ít ngọt*), hoàn toàn không cần biết khách trả tiền mặt hay chuyển khoản. Khung ghi chú thanh toán này vừa thừa thãi vừa chiếm mất diện tích quý giá của thẻ.

---

### 1.5. Cỡ chữ và font chữ quá nhiều mức, thiếu đồng bộ
* Đang có quá nhiều cỡ chữ chen chúc trên 1 thẻ: `10.5px`, `11px`, `11.5px`, `12px`, `12.5px`, `13px`, `13.5px`, `15px`.
* Chữ quá nhỏ (10.5px – 11px) làm nhân viên phải ghé sát mắt mới đọc được, trong khi các tiêu đề lại bị co cụm.

---

## PHẦN 2: ĐỐI CHIẾU TIÊU CHUẨN THIẾT KẾ CLIENT SANG ADMIN (CLIENT BENCHMARK)

Bên phân hệ Client (`ClientView`, `CartModalDialog`, `PaymentDialog`, `OrderSuccessDialog`), giao diện được người dùng đánh giá rất cao vì tuân thủ nghiêm ngặt **3 nguyên tắc vàng**:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       CHUẨN MỰC THIẾT KẾ BÊN CLIENT                         │
├─────────────────────────────────────────────────────────────────────────────┤
│ 1. TỐI GIẢN MÀU SẮC (Color Palette):                                        │
│    - Màu chủ đạo duy nhất (Primary Brand): #112823 (Deep Evergreen)         │
│    - Nền thẻ (Card Surface):               #FFFFFF (Trắng tinh)             │
│    - Nền ứng dụng (Canvas):                #F5F0E8 (Kem ấm dịu mắt)         │
│    - Nền nút phụ (Secondary Button):       #F5EFEB (Kem nhạt không viền)    │
│    - Màu chữ chính (Primary Text):         #112823 (Xanh đen sắc nét)       │
│    - Màu chữ phụ (Secondary Text):         #4A453E (Xám nâu ấm áp)         │
│                                                                             │
│ 2. KỶ LUẬT 2 CẤP ĐỘ CHỮ (Typography Scale):                                 │
│    - STYLE_BOLD:    13px Bold (#112823)    -> Tên món, Nút bấm, Tiêu đề     │
│    - STYLE_REGULAR: 12px Regular (#4A453E) -> Tùy chọn, Mô tả, Thời gian   │
│    - DUY NHẤT Tiêu đề thương hiệu: 20px - 22px Playfair Display Bold        │
│                                                                             │
│ 3. NÚT BẤM ĐỒNG BỘ 100% (Unified Buttons):                                  │
│    - Chiều cao chuẩn: 38px - 42px                                           │
│    - Bo góc đồng nhất: 8px (Không dùng 4px sắc nhọn, không dùng viền xám)   │
│    - Không dùng màu xanh lá cây chói (#15803D / #1B5E20)                   │
└─────────────────────────────────────────────────────────────────────────────┘
```

> **Mục tiêu Plan UI 4.5:** Lấy nguyên vẹn DNA thiết kế này của Client để áp dụng cho **toàn bộ các màn hình, thẻ, nút và bảng dữ liệu của Admin**.

---

## PHẦN 3: BẢN THIẾT KẾ LẠI CHI TIẾT (REDESIGN SPECIFICATION)

### 3.1. Thiết kế lại Header Thẻ KDS — Chấm dứt 100% tình trạng cắt chữ `...`
Thay vì nhồi nhét 4 phần tử trên 1 hàng ngang, ta chia Header thành **2 tầng thông thoáng**:

```
┌──────────────────────────────────────────────────────────────────┐
│ [BÀN 05]  #1022                                   03:25 (3p trước)│ <- Tầng 1: Bàn + Mã đơn bên trái, SLA bên phải
│ ──────────────────────────────────────────────────────────────── │
│ ● ĐANG PHA CHẾ                                                   │ <- Tầng 2: Huy hiệu trạng thái thanh lịch, không bị ép
└──────────────────────────────────────────────────────────────────┘
```

* **Tầng 1 (Định danh & Thời gian):**
  - Bên trái: `[BÀN 05]` (Nền be đậm `#EAE4DA`, chữ `#112823`, font `13px Bold`, bo góc `6px`) đi liền với mã `#1022` (`12px Regular #78716C`). Không bao giờ bị co ngắn!
  - Bên phải: `03:25 (3p)` với font `12px Bold`. Dưới 5p màu `#78716C`, 5–10p màu cam `#D97706`, trên 10p màu đỏ `#DC2626`.
* **Tầng 2 (Trạng thái rõ ràng, không ép chật):**
  - Dải trạng thái dạng Pill mềm mại hoặc vạch màu nhận diện chuẩn Client:
    - `● ĐANG CHỜ`: Nền `#FEF3C7`, chữ `#B45309`, font `11.5px Bold`.
    - `● ĐANG PHA CHẾ`: Nền `#EFF6FF`, chữ `#1D4ED8`, font `11.5px Bold`.
    - `● ĐÃ HOÀN THÀNH`: Nền `#DCFCE7`, chữ `#15803D`, font `11.5px Bold`.
* **Chiều rộng thẻ KDS:** Tăng từ `290px` lên cố định **`330px – 350px`** để đảm bảo mọi tên món dài nhất trong thực đơn đều hiển thị trọn vẹn, không bao giờ bị cắt ngắn.

---

### 3.2. Chuẩn hóa hiển thị từng món (Loại bỏ Đá/Đường của Bánh & Rác ghi chú)
1. **Phân loại hiển thị thông minh:**
   - **Đối với Bánh ngọt / Món ăn nhẹ / Đồ ăn vặt:**
     - **ẨN HOÀN TOÀN** dòng `Đá`, `Đường`, `Nhiệt độ` và bỏ tag `Size M` nếu món không có phân size.
     - Chỉ hiển thị: Số lượng + Tên món + Ghi chú bếp (nếu có).
     - *Ví dụ thực tế sau sửa:*
       ```text
       [ 1x ]  Cheesecake Nướng Basque
       [ 2x ]  Croissant Bơ Pháp
               ⚡ Ghi chú: Nướng giòn
       ```
   - **Đối với Đồ uống (Cà phê, Trà, Đá xay):**
     - Chỉ hiển thị size nếu món có hỗ trợ size: `[Size L]`.
     - Dòng tùy chọn chỉ in các điểm khác biệt: `• 100% Đá  •  50% Đường` hoặc `• Dùng nóng`.
2. **Lọc sạch trường Ghi chú (Kitchen Note Filter):**
   - Lọc bỏ triệt để các chuỗi chứa:
     - `"Thanh toán:"`, `"TIỀN MẶT"`, `"VIETQR"`, `"Mã giảm giá"`, `"Voucher"`.
     - Các chuỗi tự sinh trùng lặp: `"Size L"`, `"Size M"`, `"+Topping"`.
   - Chỉ giữ lại đúng lời nhắn gửi thực tế từ khách hoặc thu ngân: `"Mang nước lọc kèm"`, `"Nướng giòn"`, `"Không lấy ống hút"`.

---

### 3.3. Đồng bộ hóa Hệ thống Nút Bấm KDS & Admin theo chuẩn Client
Loại bỏ hoàn toàn các màu xanh lá cây chói `#15803D` và các nút xám viền thô. Toàn bộ nút bấm tại Admin được quy về **3 chuẩn nút duy nhất**:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     3 CHUẨN NÚT BẤM DUY NHẤT TẠI ADMIN                      │
├─────────────────────────────────────────────────────────────────────────────┤
│ 1. NÚT CHÍNH (PRIMARY CTA BUTTON):                                          │
│    - Mã màu: -fx-background-color: #112823; -fx-text-fill: #FFFFFF;        │
│    - Kích thước: Cao 38px, font Inter 13px Bold, bo góc 8px                │
│    - Hiệu ứng hover: -fx-background-color: #1B3C35;                         │
│    - Áp dụng cho:                                                           │
│      • [ ▶ Bắt đầu pha chế ] (KDS)                                          │
│      • [ ✓ Hoàn tất món & Báo phục vụ ] (KDS - dùng Evergreen đồng bộ)      │
│      • [ + Thêm món mới ] / [ Lưu giá ] / [ Xuất báo cáo ] (Admin Tabs)     │
│                                                                             │
│ 2. NÚT PHỤ (SECONDARY / GHOST BUTTON):                                      │
│    - Mã màu: -fx-background-color: #F5EFEB; -fx-text-fill: #112823;        │
│    - Kích thước: Cao 38px (hoặc 34px), font Inter 12.5px Bold, bo góc 8px  │
│    - Hiệu ứng hover: -fx-background-color: #EAE4DA;                         │
│    - Áp dụng cho:                                                           │
│      • [ ↺ Hoàn tác ] / [ 🖨 In lại vé ] (KDS Đã xong)                       │
│      • [ Xóa đơn đã xong ] (Thay cho nút đỏ thô kệch)                       │
│      • [ Đổi bàn ] / [ Gộp bàn ] / [ Tạm ngưng ]                            │
│                                                                             │
│ 3. DẢI BỘ LỌC CHỌN (SEGMENTED FILTER PILLS):                                │
│    - Nút Chưa Chọn: Nền #F5EFEB, chữ #57534E, bo góc 8px (hoặc 999px)       │
│    - Nút Đang Chọn: Nền #112823, chữ #FFFFFF, bo góc 8px (hoặc 999px)       │
│    - TUYỆT ĐỐI KHÔNG DÙNG viền xám thô #D8D1C3 kiểu web cũ                  │
│    - Áp dụng cho:                                                           │
│      • Bộ lọc trạm KDS (Espresso / Trà / Bánh)                              │
│      • Bộ lọc trạng thái (Tất cả / Đang chờ / Đang pha / Đã xong)           │
│      • Bộ lọc danh mục Thực đơn Admin                                       │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## PHẦN 4: MA TRẬN RÀ SOÁT CÁC PHÂN HỆ ADMIN CẦN ĐỒNG BỘ THEO CLIENT

Bên cạnh phân hệ KDS, dưới đây là danh sách các nút và bảng dữ liệu tại các danh mục khác của Admin đang bị lệch phong cách và giải pháp chỉnh sửa:

| Phân hệ / Tab Admin | Hiện trạng "Lạc quẻ" | Chuẩn hóa theo phong cách Client |
| :--- | :--- | :--- |
| **Tab 4: KDS (Bếp & Bar)** | - Nút hoàn tất xanh lá chói `#15803D`.<br/>- Nút lọc trạng thái viền xám `#D8D1C3`.<br/>- Header thẻ bị cắt thành `BÀN ...`.<br/>- Bánh ngọt hiện Đá/Đường. | - Thẻ mở rộng `340px`, Header 2 tầng không cắt chữ.<br/>- Nút hành động thống nhất `#112823` bo góc `8px`.<br/>- Bộ lọc trạng thái dạng Pill mềm mại `#F5EFEB` / `#112823`.<br/>- Ẩn đá/đường đối với bánh ngọt. |
| **Tab 1: Tổng quan BI** | - Nút lọc thời gian và nút xem chi tiết dùng nhiều cỡ chữ và bo góc khác nhau (`4px`, `6px`, `999px`). | - Thống nhất toàn bộ nút bấm về bo góc `8px`, chiều cao chuẩn `36px`, font `13px Bold`. |
| **Tab 3: Đơn hàng & Lịch sử** | - Nút `[✓ Hoàn tất]` dùng nền xanh `#DCFCE7` chữ `#15803D` bo góc `6px`.<br/>- Nút `[✕ Hủy]` dùng nền đỏ `#FEE2E2` chữ `#B91C1C`. | - Đổi sang phong cách Button Client: Nền `#F5EFEB` chữ `#112823`, khi hover hiện sắc thái ngữ nghĩa nhẹ nhàng, bo góc `8px`. |
| **Tab 5: Thực đơn & Giá** | - Nút `● Còn bán` / `✕ Tạm hết` dùng bo góc viên thuốc `999px` đối lập với các nút `[Lưu giá]` bo góc `6px`. | - Thống nhất bo góc `8px`. Nút bật tắt trạng thái dùng Tag phẳng nhã nhặn, chữ `12px Bold`. |
| **Tab 6: Kho & Định lượng** | - Nút `+ Thêm ĐL` dùng font `11px` quá nhỏ, ô nhập liệu `txtQty` bị lệch chiều cao so với ComboBox. | - Chuẩn hóa chiều cao ô nhập và nút bấm đồng bộ `36px`, font `12.5px Bold`. |
| **Thanh Sidebar Trái Admin** | - Nút Active dùng nền kem `#DED5C8`, chữ xanh `#112823`, có đánh số `1.`, `2.`... `10.`. | - Đổi Active về nền xanh đậm `#112823` chữ trắng `#FFFFFF` giống 100% Client/Staff, bỏ số thứ tự thừa thãi. |

---

## PHẦN 5: BỐ CỤC MẪU THẺ KDS MỚI SAU KHI CHỈNH SỬA (BEFORE & AFTER)

Dưới đây là hình ảnh mô phỏng trực quan sự khác biệt giữa giao diện cũ (gây rối mắt) và giao diện mới (chuẩn hóa Client):

```carousel
```text
=== GIAO DIỆN CŨ (BỊ RỐI MẮT & CẮT CHỮ) ===
┌─────────────────────────────────────────────────────────────┐
│ [BÀN ...]  #10...    [ĐANG PHA C...]  24:30 (24p)           │ <- Cắt chữ ba chấm
├─────────────────────────────────────────────────────────────┤
│ [ 1x ] Cheesecake Nướng Basque [Size M]                     │
│        • 100% Đá  •  Chuẩn đường                            │ <- Bánh có đá & đường!
│ [ 2x ] Croissant Bơ Pháp [Size M]                           │
│        • 100% Đá  •  Chuẩn đường                            │
│        ⚡ Ghi chú: Nướng giòn                               │
│ 📌 Ghi chú đơn: Thanh toán: TIỀN MẶT                        │ <- Ghi chú thanh toán thừa
├─────────────────────────────────────────────────────────────┤
│ [ ✓ Hoàn tất món & Báo phục vụ ]                            │ <- Nút xanh lá chói #15803D
└─────────────────────────────────────────────────────────────┘
```
<!-- slide -->
```text
=== GIAO DIỆN MỚI (CHUẨN HÓA CLIENT - KHÔNG CẮT CHỮ) ===
┌─────────────────────────────────────────────────────────────┐
│ [BÀN 05]  #1022                             03:25 (3p trước)│ <- Rõ ràng, không cắt chữ
│ ● ĐANG PHA CHẾ                                              │ <- Dải trạng thái thanh lịch
├─────────────────────────────────────────────────────────────┤
│ [ 1x ]  Cheesecake Nướng Basque                             │ <- Bánh sạch sẽ, không đá đường
│ ─────────────────────────────────────────────────────────── │ <- Đường phân cách đứt đoạn
│ [ 2x ]  Croissant Bơ Pháp                                   │
│         ⚡ Ghi chú: Nướng giòn                              │ <- Chỉ giữ ghi chú thực tế
├─────────────────────────────────────────────────────────────┤
│ [ ✓ Hoàn tất món & Báo phục vụ ]                            │ <- Nút Evergreen #112823 đồng bộ
└─────────────────────────────────────────────────────────────┘
```
````

---

## PHẦN 6: KẾ HOẠCH TRIỂN KHAI THEO TỪNG BƯỚC (EXECUTION ROADMAP)

Để đảm bảo an toàn tuyệt đối và tính mạch lạc cho mã nguồn, quá trình chỉnh sửa sẽ được tiến hành theo 3 giai đoạn:

### Giai đoạn 1: Khắc phục triệt để KDS (`OrderTicketCard.java` & `server.css`)
1. **Thiết kế lại `buildHeader()`:** Bố trí 2 tầng thông minh, đảm bảo số bàn, mã đơn, SLA và trạng thái không bao giờ bị cắt chữ `...`.
2. **Nâng cấp `createItemBlock()`:** 
   - Kiểm tra phân loại món: Nếu là bánh/món ăn nhẹ (`category != drink`), ẩn hoàn toàn hiển thị đá, đường, size mặc định.
   - Lọc bỏ chuỗi thanh toán tiền mặt/chuyển khoản khỏi ghi chú đơn hàng.
   - Thêm đường kẻ phân cách đứt đoạn (`dashed border`) giữa các món.
3. **Đồng bộ màu sắc nút bấm:**
   - Đưa nút `[▶ Bắt đầu pha chế]` và `[✓ Hoàn tất món]` về đúng mã màu thương hiệu `#112823` (hover `#1A3B34`), chiều cao `38px`, bo góc `8px`.
   - Chuẩn hóa các nút `[↺ Hoàn tác]` và `[🖨 In lại vé]` sang nền `#F5EFEB`.

### Giai đoạn 2: Chuẩn hóa thanh công cụ KDS (`ServerKDSView.java`)
1. Thay thế các nút lọc trạng thái xám viền nhọn (`.kds-filter-btn`) bằng dải Segmented Pill đồng bộ với dải chọn trạm phía trên.
2. Sửa lỗi chính tả `Tim theo...` thành `Tìm theo số bàn hoặc mã đơn...`, bo góc ô tìm kiếm `8px`.
3. Đổi nút `[Xóa đơn đã xong]` sang phong cách nút phụ tinh tế (`#F5EFEB`).

### Giai đoạn 3: Đối chiếu & làm sạch Button trên toàn bộ Admin (`AdminSubViews.java` & `AdminShellView.java`)
1. Chuẩn hóa nút Active Sidebar Admin về nền xanh `#112823` chữ trắng `#FFFFFF`, bỏ số thứ tự `1.`, `2.`...
2. Đồng bộ các nút thao tác tại Tab 3 (Đơn hàng), Tab 5 (Thực đơn), Tab 6 (Kho) về đúng 2 loại nút chuẩn của Client.
3. Kiểm tra toàn bộ ứng dụng trên độ phân giải chuẩn (1200x760 và 1920x1080) để đảm bảo không còn bất kỳ dòng chữ nào bị cắt dấu ba chấm `...`.
