# ĐẶC TẢ THIẾT KẾ PHIẾU THANH TOÁN (EDITORIAL RECEIPT SLIP)
**Hệ thống:** CafeOrder (Brew & Bean CoffeePOS)  
**Tài liệu:** Thiết kế giao diện Xác nhận thanh toán & Phiếu thu chuẩn chuỗi F&B  
**Trạng thái:** Bản đặc tả thiết kế (Design Specification - Chưa triển khai code)

---

## 1. BỐI CẢNH & MỤC TIÊU THIẾT KẾ
* **Vấn đề của UI cũ:** Hộp thoại xác nhận đơn hàng cũ chỉ là một Alert Modal thông báo ngắn (`Mã đơn`, `Hình thức`, `Tổng tiền`), thiếu hoàn toàn danh sách món chi tiết, thiếu nhận diện thương hiệu và không mang lại cảm giác an tâm của một hóa đơn thanh toán thực thụ.
* **Mục tiêu mới:** Chuyển đổi thành một **Phiếu thu / Hóa đơn điện tử dạng cuộn (Editorial Receipt Slip)** theo chuẩn mực của các chuỗi F&B hàng đầu (*Highlands Coffee, Starbucks, Katinat, The Coffee House*):
  1. Minh bạch 100% danh mục món gọi (Tên món, Size, Mức đường/đá, Topping, Đơn giá, Thành tiền).
  2. Nâng tầm trải nghiệm thị giác với phong cách giấy in nhiệt cà phê thủ công cao cấp.
  3. Cung cấp tiện ích gia tăng cho khách hàng: Mã tra cứu tiến độ Bếp/Bar, thông tin Wi-Fi, lời cảm ơn.

---

## 2. BẢN PHÁC THẢO GIAO DIỆN TRỰC QUAN (VISUAL WIREFRAME MOCKUP)

```
┌────────────────────────────────────────────────────────────────────────┐
│                                 ( ✓ )                                  │  <- Icon tích xanh tròn (Emerald Green)
│                         THANH TOÁN THÀNH CÔNG                          │  <- Font Inter 17px Bold, #112823
│                  Cảm ơn bạn! Đơn hàng đã được gửi tới quầy.            │  <- Font Inter 12.5px, #6F695F
│                                                                        │
│  ┌──────────────────────────────────────────────────────────────────┐  │  <- KHUNG PHIẾU THU (RECEIPT CANVAS)
│  │                                                                  │  │     (Nền giấy ngà #FBF9F5, bo góc 12px)
│  │                           Brew & Bean                            │  │  <- Logo Playfair Display 19px Bold
│  │                      COFFEE ROASTERY & BAR                       │  │  <- Subtitle 9.5px, tracking 2px
│  │                 123 Nguyễn Thị Minh Khai, Quận 3                 │  │  <- Hotline & Địa chỉ quán
│  │                                                                  │  │
│  │  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -   │  │  <- Đường đứt nét (Dashed Divider)
│  │  Số HĐ:   #1001                              Bàn: BÀN 05         │  │  <- Metadata định danh
│  │  Ngày:    19/10/2026 16:20                   Thu ngân: Kiosk     │  │
│  │  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -   │  │
│  │  TÊN MÓN                             SL               THÀNH TIỀN │  │  <- Tiêu đề cột (Table Header)
│  │                                                                  │  │
│  │  Cà phê Sữa Đá                        1                 29.000 đ │  │  <- Món 1 (Tên in đậm)
│  │    • Size L (+7.000 đ)                                   7.000 đ │  │  <- Tùy chọn Size
│  │    • Thạch cà phê                                        8.000 đ │  │  <- Topping đính kèm
│  │    [ 70% Đường • 100% Đá ]                                       │  │  <- Ghi chú chế biến
│  │                                                                  │  │
│  │  Bạc Xỉu Kem Béo                      1                 32.000 đ │  │  <- Món 2
│  │    • Size M (Tiêu chuẩn)                                     0 đ │  │
│  │    [ 50% Đường • Ít đá ]                                         │  │
│  │                                                                  │  │
│  │  Croissant Bơ Pháp                    1                 32.000 đ │  │  <- Món 3 (Bánh ngọt)
│  │    [ Hâm nóng giòn ]                                             │  │
│  │  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -   │  │
│  │  Tạm tính (3 món):                                     108.000 đ │  │  <- Chi tiết tài chính
│  │  Khuyến mãi (Voucher GIAM10):                          - 10.800 đ │  │  <- Giảm giá (màu xanh lá)
│  │  --------------------------------------------------------------  │  │
│  │  TỔNG THANH TOÁN:                                       97.200 đ │  │  <- Số tiền to nổi bật #A66A42
│  │  Phương thức:                                      VIETQR / MOMO │  │
│  │  Trạng thái:                                       ĐÃ THANH TOÁN │  │  <- Badge xanh lá
│  │  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -   │  │
│  │                         ||| | |||| || | ||                       │  │  <- Barcode / QR tra cứu
│  │                            ORDER-1001-KDS                        │  │
│  │               Wi-Fi: BrewBean_Guest  •  Pass: 88888888           │  │  <- Tiện ích quán
│  │           Chúc quý khách một ngày làm việc tràn đầy năng lượng!  │  │  <- Lời chúc chân thành
│  └──────────────────────────────────────────────────────────────────┘  │
│                                                                        │
│   [ 🧾 Theo dõi tiến độ đơn ]            [ ✕ Quay lại Thực đơn ]      │  <- Nút bấm kép (Primary & Secondary)
└────────────────────────────────────────────────────────────────────────┘
```

---

## 3. ĐẶC TẢ CHI TIẾT 5 TẦNG DỮ LIỆU (5-TIER RECEIPT ARCHITECTURE)

### TẦNG 1: BRAND HEADER (NHẬN DIỆN THƯƠNG HIỆU)
* **Tên thương hiệu:** `Brew & Bean`
  * Font chữ: `'Playfair Display', serif`, Size `19px`, Weight `800`, Màu `#112823`.
* **Phụ đề:** `COFFEE ROASTERY & BAR`
  * Font chữ: `'Inter', sans-serif`, Size `9.5px`, Weight `800`, Màu `#563521`, Letter-spacing `2px`.
* **Thông tin liên hệ chi nhánh:**
  * Địa chỉ: `123 Nguyễn Thị Minh Khai, Phường Bến Thành, Quận 3, TP.HCM`.
  * Hotline: `1900 6868` • Giờ mở cửa: `07:00 - 22:30`.
  * Màu chữ: `#8C8275`, Size `11px`.

---

### TẦNG 2: METADATA ĐƠN HÀNG (ORDER & TABLE INFO)
Bố trí 2 cột đối xứng với đường kẻ phân cách nét đứt (`dashed border`):
* **Cột trái:**
  * Mã đơn hàng: `MÃ ĐƠN: #1001` (Font đậm `13px`, màu `#112823`).
  * Thời gian giao dịch: `19/10/2026 - 16:20:45` (Size `11.5px`, màu `#6F695F`).
* **Cột phải:**
  * Vị trí phục vụ: `BÀN 05` (In hoa nổi bật trên nền tag bo góc `#EAE2D5`, màu `#183C32`, font `13px Bold`).
  * Trạm phục vụ: `Kiosk Tự Phục Vụ #01` (Size `11.5px`, màu `#6F695F`).

---

### TẦNG 3: BẢNG KÊ CHI TIẾT MÓN ĂN (LINE ITEMS BREAKDOWN)
Được đặt trong một `VBox` cuộn linh hoạt (`ScrollPane` tối đa 4 món hiển thị trực tiếp, vượt quá sẽ cuộn mượt):
* **Cột Header:** `TÊN MÓN`, `SL`, `THÀNH TIỀN` (Font `11px SemiBold`, màu `#8C8275`, in hoa).
* **Cấu trúc mỗi dòng món (`ItemRow`):**
  1. **Dòng chính:**
     * Tên món: Ví dụ `Cà phê Sữa Đá` (Font `Inter 13px Bold`, `#1C1917`).
     * Số lượng: `1` (Căn giữa, Font `12.5px Bold`).
     * Đơn giá gốc: `29.000 đ` (Căn phải, Font `13px Bold`).
  2. **Dòng chi tiết bổ sung (Indent lùi vào 12px):**
     * Kích cỡ chọn: `• Size L (+7.000 đ)` $\rightarrow$ `7.000 đ`.
     * Topping thêm: `• Thạch cà phê (+8.000 đ)` $\rightarrow$ `8.000 đ`.
     * Ghi chú khẩu vị: `[ 70% Đường • 100% Đá ]` (Font `11px Italic`, màu xám dịu `#6E675F`).

---

### TẦNG 4: CHI TIẾT TÀI CHÍNH & THANH TOÁN (FINANCIAL SUMMARY)
Phân đoạn tính toán minh bạch, tạo lòng tin tuyệt đối cho khách hàng:
* **Tạm tính món:** Tổng giá gốc của toàn bộ sản phẩm (Ví dụ: `108.000 đ`).
* **Chiết khấu / Ưu đãi (Voucher):** Hiển thị số âm màu xanh lá cây hoặc cam nổi bật (Ví dụ: `- 10.800 đ (Voucher GIAM10)`).
* **Thuế VAT (8% hoặc 10% nếu có):** Đã bao gồm trong giá niêm yết.
* **Đường phân cách kép hoặc nét đậm:** `-fx-border-color: #112823; -fx-border-width: 1.5px;`
* **DÒNG TỔNG THANH TOÁN (TOTAL AMOUNT):**
  * Nhãn: `TỔNG THANH TOÁN:` (Font `14px Bold`, màu `#112823`).
  * Số tiền: `97.200 đ` (Font `18px ExtraBold`, màu nâu cà phê `#A66A42` hoặc xanh đậm `#112823`).
* **Phương thức thanh toán:** `VIETQR / NGÂN HÀNG` hoặc `TIỀN MẶT TẠI QUẦY` (Font `12px SemiBold`).
* **Trạng thái giao dịch:** Badge màu xanh lá nhạt bo tròn: `● ĐÃ THANH TOÁN THÀNH CÔNG`.

---

### TẦNG 5: TIỆN ÍCH QUÁN & TRA CỨU ĐƠN (FOOTER & VALUE-ADDED UTILITIES)
* **Mã vạch tra cứu KDS:** Minh họa Barcode giả lập hoặc QR Code để nhân viên quầy Bar quét nhanh khi trả đồ cho khách.
* **Tiện ích Khách hàng (Tại bàn):**
  * Wi-Fi Quán: `BrewBean_Guest` • Mật khẩu: `88888888`.
* **Lời cảm ơn thương hiệu:**
  * *"Cảm ơn quý khách đã lựa chọn Brew & Bean! Thức uống của bạn đang được pha chế với tất cả sự tận tâm."*
  * Font `11px Italic`, căn giữa, màu `#8C8275`.

---

## 4. QUY CHUẨN THIẾT KẾ ĐỒ HỌA & STYLE GUIDE

| Thuộc tính UI | Thông số chuẩn hóa | Mục tiêu thẩm mỹ |
| :--- | :--- | :--- |
| **Độ rộng cửa sổ (Modal Width)** | **`460px`** (Cũ: 420px) | Đủ rộng rãi để tên món và giá tiền không bị đè chữ. |
| **Chiều cao cửa sổ (Height)** | Tự động co giãn theo nội dung (`Min: 580px`, `Max: 720px`) | Vừa vặn trên mọi độ phân giải màn hình Kiosk. |
| **Nền cửa sổ ngoài (Backdrop)** | `rgba(0, 0, 0, 0.45)` với hiệu ứng làm mờ nền nhẹ | Tập trung toàn bộ sự chú ý của khách vào hóa đơn. |
| **Nền Phiếu Thu (`Receipt Canvas`)** | `#FBF9F5` (Màu giấy kem in nhiệt ấm áp) | Tạo cảm giác cầm trên tay một cuộn hóa đơn thật. |
| **Viền Phiếu Thu** | `-fx-border-color: #E2DDD5; -fx-border-radius: 12px;` | Mềm mại, cao cấp, không bị thô cứng. |
| **Đường ngăn cách (Dividers)** | Nét đứt `-fx-border-style: dashed; -fx-border-color: #D6CEBF;` | Giả lập đường xé răng cưa của máy in bill nhiệt. |
| **Đổ bóng (Drop Shadow)** | `dropshadow(gaussian, rgba(0, 0, 0, 0.16), 24, 0.15, 0, 8)` | Tạo chiều sâu 3D nổi hẳn lên khỏi màn hình nền. |

---

## 5. THIẾT KẾ BỘ ĐÔI NÚT HÀNH ĐỘNG (ACTION BUTTONS)

Dưới đáy Modal bố trí **2 nút bấm với mức độ ưu tiên thị giác rõ ràng**:

1. **Nút Chính (Primary Action) - "Theo dõi tiến độ đơn":**
   * Màu nền: `#112823` (Xanh than thương hiệu), chữ trắng `#FFFFFF`, font `13.5px Bold`.
   * Chiều cao: `42px`, bo góc `8px`.
   * Icon: Icon chiếc đồng hồ hoặc hóa đơn `🧾`.
   * Hành vi: Chuyển trực tiếp sang thẻ đơn hàng hoặc mở popup theo dõi tiến độ món đang ở khâu: *Đã nhận* $\rightarrow$ *Đang pha chế* $\rightarrow$ *Sẵn sàng nhận*.
2. **Nút Phụ (Secondary Action) - "Quay lại Thực đơn":**
   * Màu nền: `#FFFFFF`, viền xám mờ `#DED7CD`, chữ xám đen `#3F3931`, font `13.5px SemiBold`.
   * Chiều cao: `42px`, bo góc `8px`.
   * Hành vi: Đóng modal, reset giỏ hàng và đưa khách về màn hình danh mục gọi món.

---

## 6. MÔ HÌNH DỮ LIỆU ĐẦU VÀO ĐỂ TRIỂN KHAI (DATA MAPPING CHO DEVELOPER)

Khi đội ngũ kỹ thuật bắt đầu triển khai code JavaFX, đối tượng truyền vào `OrderSuccessDialog` sẽ bao gồm:

```java
public class ReceiptData {
    int orderId;                // Ví dụ: 1001
    int tableNumber;            // Ví dụ: 5
    String storeName;           // "Brew & Bean • CN Quận 3"
    LocalDateTime orderTime;    // 2026-10-19 16:20:45
    List<OrderItem> items;      // Danh sách món gồm tên, size, topping, ghi chú
    double subtotal;            // Tạm tính
    double discountAmount;      // Giảm giá voucher
    double finalTotal;          // Tổng tiền thanh toán
    String paymentMethod;       // "VIETQR", "TIỀN MẶT", "VÍ ĐIỆN TỬ"
    String paymentStatus;       // "ĐÃ THANH TOÁN"
    String cashierName;         // "Kiosk Tự Phục Vụ"
    String wifiInfo;            // "BrewBean_Guest / pass: 88888888"
}
```

---
*Tài liệu này là căn cứ kỹ thuật chuẩn để AGY hoặc lập trình viên tiến hành refactor lớp `OrderSuccessDialog.java` khi bước vào giai đoạn nâng cấp mã nguồn.*
