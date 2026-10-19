# BỘ QUY CHUẨN ĐỒNG BỘ GIAO DIỆN CLIENT & STAFF THEO BENCHMARK ADMIN/SERVER
**Hệ thống CafeOrder (Brew & Bean CoffeePOS)**

---

## MỤC LỤC
1. [Hệ quy chiếu chuẩn (Benchmark từ AdminShellView)](#1-hệ-quy-chiếu-chuẩn-benchmark-từ-adminshellview)
2. [Cấu trúc thư mục tài nguyên & Quy tắc đặt tên Icon](#2-cấu-trúc-thư-mục-tài-nguyên--quy-tắc-đặt-tên-icon)
3. [BẢNG DANH MỤC ICON VÀ TÊN NÚT CHI TIẾT ĐỂ TÌM KIẾM TRÊN MẠNG](#3-bảng-danh-mục-icon-và-tên-nút-chi-tiết-để-tìm-kiếm-trên-mạng)
   - 3.1. Phân hệ Nhân viên (Staff UI - Sơ đồ bàn, Đơn hàng, Quầy Order)
   - 3.2. Phân hệ Khách hàng (Client UI - Gọi món, Giỏ hàng, Thanh toán QR)
   - 3.3. Danh mục Menu món ăn & Đồ uống (Categories)
   - 3.4. Phân hệ Quản lý / Server (Admin UI - Bổ sung tham chiếu)
4. [Kế hoạch điều chỉnh Layout (Before vs After)](#4-kế-hoạch-điều-chỉnh-layout-before-vs-after)
5. [Prompt tinh chỉnh mã nguồn dành riêng cho AGY (Antigravity Agent)](#5-prompt-tinh-chỉnh-mã-nguồn-dành-riêng-cho-agy)

---

## 1. HỆ QUY CHIẾU CHUẨN (BENCHMARK TỪ ADMINSHELLVIEW)

Admin/Server (`AdminShellView.java`) đã được tinh chỉnh hoàn hảo về thị giác, kích thước và độ thoải mái khi nhìn ở màn hình lớn hoặc thao tác cảm ứng POS. Toàn bộ UI của **Nhân viên (Staff)** và **Khách hàng (Client)** bắt buộc phải lấy các thông số sau làm quy chuẩn:

| Thành phần UI | Thông số chuẩn Admin (Mốc Benchmark) | Áp dụng cho Staff / Client | Mục tiêu & Lợi ích |
| :--- | :--- | :--- | :--- |
| **Độ rộng Sidebar** | `Pref: 235px` (Min: 220px, Max: 255px) | Đồng bộ **235px** (thay cho 110px - 130px cũ) | Chữ không bị co cụm, đọc trọn vẹn tên tính năng và danh mục món. |
| **Padding Sidebar** | `Insets(20, 12, 20, 12)` | Đồng bộ `Insets(20, 12, 20, 12)` | Tạo khoảng thở thông thoáng, chuẩn thiết kế hiện đại. |
| **Khung chứa Icon (Container)** | **`StackPane` cố định 26×26 px** | **`StackPane` cố định 26×26 px** (`min/pref/max = 26`) | **Khắc phục triệt để lỗi icon thụt ra thụt vào**, đảm bảo thẳng tắp 1 trục dọc. |
| **Kích thước Icon hiển thị** | `fitWidth: 20px`, `fitHeight: 20px` (`smooth = true`) | `fitWidth: 20px`, `fitHeight: 20px` | Icon hiển thị thanh thoát, không lấn át nhãn chữ. |
| **Kích thước tải về từ Web** | **24×24 px** hoặc **32×32 px** (PNG trong suốt) | **24×24 px** hoặc **32×32 px** PNG | JavaFX downsample từ 24/32px xuống 20px sẽ đạt độ nét Retina, không vỡ hạt. |
| **Khoảng cách Icon - Chữ** | `HBox spacing = 12px` | `HBox spacing = 12px` | Tách bạch rõ giữa biểu tượng và nhãn nội dung. |
| **Padding nút bấm Sidebar** | `Insets(10, 14, 10, 14)` | `Insets(10, 14, 10, 14)` | Chiều cao chạm đạt chuẩn Touch POS (44px – 46px). |
| **Typography Nút Sidebar** | `13.5px`, Weight 700 (Inactive) / 800 (Active) | `13.5px`, Weight 700 / 800 | To, đậm, đọc cực kỳ rõ từ khoảng cách 60cm – 1m. |
| **Màu sắc trạng thái Nút** | Active: Nền `#DED5C8`, Chữ `#112823`<br>Inactive: Nền `transparent`, Hover `#EFE8DD` | Giữ nguyên bộ màu nhận diện Warm Coffee POS | Tạo tính liền mạch thương hiệu toàn chuỗi cửa hàng. |
| **Brand Header Logo** | "Brew & Bean" 17.5px Playfair Serif Weight 800<br>Subtitle: 10px Weight 800 letter-spacing 1.5px | Đồng bộ 100% trên cả 3 giao diện | Nhất quán thương hiệu quầy và bàn khách. |
| **Cơ chế nạp Icon (Fallback)** | 1. Đọc PNG tài nguyên → 2. Đọc PNG ổ đĩa Dev → 3. Fallback SVG Vector → 4. Emoji | Đồng bộ vào mọi view | Đảm bảo hệ thống không bao giờ bị crash hoặc trắng icon. |

---

## 2. CẤU TRÚC THƯ MỤC TÀI NGUYÊN & QUY TẮC ĐẶT TÊN ICON

Toàn bộ icon PNG được tổ chức quy củ theo phân hệ trong mã nguồn Java:

```
c:\Users\ACER\Documents\NetBeansProjects\CafeOrder\
 └── src\cafe\resources\images\icons\
      ├── admin\          # Icon phân hệ Quản trị / Admin Server
      ├── staff\          # Icon phân hệ Nhân viên Order & Sơ đồ bàn
      ├── client\         # Icon phân hệ Khách hàng (Giỏ hàng, Bàn, Thanh toán...)
      └── categories\     # Icon danh mục thực đơn (Cà phê, Trà, Bánh...)
```

### Hướng dẫn tải Icon từ các kho trực tuyến:
* **Các trang web tìm icon chuẩn nhất:** [Flaticon.com](https://www.flaticon.com), [Lucide.dev](https://lucide.dev/icons), [Icons8.com](https://icons8.com), [SVGRepo.com](https://www.svgrepo.com).
* **Định dạng file:** Tải file định dạng **PNG**, chọn nền **Trong suốt (Transparent)**.
* **Kích thước tải về khuyến nghị:** Chọn **`24px`** hoặc **`32px`** (Web chỉ hỗ trợ 16, 24, 32, 64px - tải 24/32px là tối ưu nhất cho màn hình Desktop & POS).
* **Phong cách (Style):** Chọn kiểu **Line Art / Outline** hoặc **Monochrome Solid**, màu xám than `#112823` hoặc đen để hòa hợp với tông màu ấm của quán cà phê.

---

## 3. BẢNG DANH MỤC ICON VÀ TÊN NÚT CHI TIẾT ĐỂ TÌM KIẾM TRÊN MẠNG

### 3.1. Phân hệ Nhân viên (Staff UI)
*Thư mục lưu file:* `src/cafe/resources/images/icons/staff/`

| STT | TÊN NÚT TRÊN GIAO DIỆN | TÊN TÌM KIẾM TRÊN MẠNG (Search Keywords) | TÊN FILE LƯU (.png) | KÍCH THƯỚC TẢI | KÍCH THƯỚC HIỂN THỊ | VỊ TRÍ XUẤT HIỆN TRÊN HỆ THỐNG |
|:---:|:---|:---|:---|:---:|:---:|:---|
| 1 | **Nút "Bàn" / "Sơ đồ bàn"** | `table restaurant`, `dining table`, `layout grid` | `so-do-ban.png` | **24×24** hoặc **32×32** | 20×20 px | Nút 1 trên Sidebar `FloorPlanView` |
| 2 | **Nút "Thực đơn"** | `utensils`, `coffee menu`, `book open`, `restaurant menu` | `thuc-don.png` | **24×24** hoặc **32×32** | 20×20 px | Nút 2 trên Sidebar `FloorPlanView` (Chuyển sang quầy gọi món) |
| 3 | **Nút "Đơn hàng"** | `receipt`, `clipboard list`, `bill orders`, `invoice` | `don-hang.png` | **24×24** hoặc **32×32** | 20×20 px | Nút 3 trên Sidebar (Mở popup `OrdersDialog` xem tiến độ bàn) |
| 4 | **Nút "Thanh toán"** | `wallet`, `credit card`, `cash payment`, `hand coins` | `thanh-toan.png` | **24×24** hoặc **32×32** | 20×20 px | Nút 4 trên Sidebar (Chức năng tính tiền nhanh) |
| 5 | **Nút "Cài đặt"** | `settings`, `gear`, `cog`, `sliders` | `cai-dat.png` | **24×24** hoặc **32×32** | 20×20 px | Nút 5 trên Sidebar (Cài đặt trạm nhân viên) |
| 6 | **Nút "Đăng xuất"** | `log out`, `exit`, `power off`, `door open` | `dang-xuat.png` | **24×24** hoặc **32×32** | 20×20 px | Nút chân Sidebar nhân viên (Thoát ca làm) |
| 7 | **Nút "Làm mới bàn"** | `rotate cw`, `refresh`, `reload`, `sync` | `lam-moi.png` | **24×24** hoặc **32×32** | 18×18 px | Góc trên bên phải màn hình Sơ đồ bàn (Cập nhật realtime) |
| 8 | **Nút "Chuyển bàn"** | `arrow left right`, `exchange table`, `move arrows` | `chuyen-ban.png` | **24×24** hoặc **32×32** | 18×18 px | Thanh công cụ thao tác bàn (Đổi bàn cho khách) |
| 9 | **Nút "Gộp bàn"** | `merge`, `combine tables`, `group objects` | `tach-gop-ban.png`| **24×24** hoặc **32×32** | 18×18 px | Thanh công cụ thao tác bàn (Gộp nhiều bàn đi đoàn đông) |
| 10 | **Nút lọc "Tất cả bàn"** | `grid 2x2`, `layers`, `all items` | `tat-ca-ban.png` | **24×24** hoặc **32×32** | 16×16 px | Tab lọc trạng thái bàn ở đỉnh màn hình |
| 11 | **Nút lọc "Bàn trống"** | `check circle`, `circle dot`, `available seat` | `ban-trong.png` | **24×24** hoặc **32×32** | 16×16 px | Tab lọc bàn chưa có khách |
| 12 | **Nút lọc "Có khách"** | `user check`, `coffee cup`, `occupied seat` | `co-khach.png` | **24×24** hoặc **32×32** | 16×16 px | Tab lọc bàn đang dùng món |
| 13 | **Nút "In tạm tính / Hóa đơn"** | `printer`, `receipt print`, `print document` | `in-hoa-don.png` | **24×24** hoặc **32×32** | 18×18 px | Nút trong popup `OrdersDialog` khi khách yêu cầu thanh toán |
| 14 | **Nút "Hủy đơn / Hủy món"** | `trash 2`, `x circle`, `cancel order` | `huy-mon.png` | **24×24** hoặc **32×32** | 18×18 px | Nút thao tác hủy món bị lỗi hoặc khách đổi ý |

---

### 3.2. Phân hệ Khách hàng (Client UI)
*Thư mục lưu file:* `src/cafe/resources/images/icons/client/`

| STT | TÊN NÚT TRÊN GIAO DIỆN | TÊN TÌM KIẾM TRÊN MẠNG (Search Keywords) | TÊN FILE LƯU (.png) | KÍCH THƯỚC TẢI | KÍCH THƯỚC HIỂN THỊ | VỊ TRÍ XUẤT HIỆN TRÊN HỆ THỐNG |
|:---:|:---|:---|:---|:---:|:---:|:---|
| 1 | **Nút "Sơ đồ bàn"** | `table`, `restaurant table`, `cafe chair`, `layout` | `ban-an.png` | **24×24** hoặc **32×32** | 20×20 px | Nút đầu tiên trên đỉnh Sidebar Khách hàng (`ClientView`) |
| 2 | **Nút "Giỏ hàng" (Floating Cart)** | `shopping bag`, `cart shopping`, `basket`, `bag` | `gio-hang.png` | **24×24** hoặc **32×32** | 20×20 px | Nút giỏ hàng nổi góc phải Header kèm huy hiệu đếm số lượng |
| 3 | **Ô "Tìm kiếm món..."** | `search`, `magnifying glass`, `search bar` | `tim-kiem.png` | **24×24** hoặc **32×32** | 18×18 px | Icon nằm bên trong thanh Search Bar ở Header |
| 4 | **Nút "+ Thêm món"** | `plus`, `plus circle`, `add item`, `plus round` | `them-mon.png` | **24×24** hoặc **32×32** | 18×18 px | Nút góc dưới mỗi thẻ món ăn ([ItemCard.java](file:///c:/Users/ACER/Documents/NetBeansProjects/CafeOrder/src/cafe/views/ItemCard.java)) |
| 5 | **Nút "- Giảm số lượng"** | `minus`, `minus circle`, `remove item` | `giam-sl.png` | **24×24** hoặc **32×32** | 16×16 px | Nút tăng giảm số lượng trong Giỏ hàng và Dialog chi tiết |
| 6 | **Nút "+ Tăng số lượng"** | `plus`, `plus circle`, `add quantity` | `tang-sl.png` | **24×24** hoặc **32×32** | 16×16 px | Nút tăng giảm số lượng trong Giỏ hàng và Dialog chi tiết |
| 7 | **Nút "Xem chi tiết món"** | `eye`, `info circle`, `sliders`, `customize` | `chi-tiet-mon.png` | **24×24** hoặc **32×32** | 16×16 px | Nút mở cửa sổ chọn Đường / Đá / Topping |
| 8 | **Nút "Xóa món"** | `trash 2`, `delete item`, `remove bin` | `xoa-mon.png` | **24×24** hoặc **32×32** | 16×16 px | Nút xóa món khỏi giỏ trong `CartModalDialog` |
| 9 | **Nút "Ghi chú món"** | `edit 3`, `pencil`, `note sticky`, `message square` | `ghi-chu.png` | **24×24** hoặc **32×32** | 16×16 px | Ô nhập lưu ý cho Bar/Bếp (Ít ngọt, không đá,...) |
| 10 | **Nút "Quét mã VietQR"** | `qr code`, `scan qr`, `banking qr`, `scan` | `thanh-toan-qr.png`| **32×32** hoặc **64×64** | 22×22 px | Nút chọn thanh toán quét mã ngân hàng trong `PaymentDialog` |
| 11 | **Nút "Tiền mặt tại quầy"** | `banknote`, `cash`, `money dollar`, `coins` | `tien-mat.png` | **24×24** hoặc **32×32** | 20×20 px | Nút chọn thanh toán trả tiền mặt tại quầy thu ngân |
| 12 | **Nút "Xác nhận gửi đơn"** | `send`, `check circle 2`, `coffee cup`, `submit` | `dong-y-dat.png` | **24×24** hoặc **32×32** | 20×20 px | Nút to nổi bật dưới chân giỏ hàng để gửi đơn sang Bếp/Bar |
| 13 | **Thẻ "Theo dõi đơn hàng"** | `clock`, `timer`, `hourglass`, `bell ring` | `theo-doi-don.png` | **24×24** hoặc **32×32** | 20×20 px | Card cập nhật trạng thái đơn realtime ở chân Sidebar |
| 14 | **Nút "Đóng popup"** | `x`, `close`, `cross` | `dong-cua-so.png` | **24×24** hoặc **32×32** | 16×16 px | Nút [X] đóng hộp thoại giỏ hàng và thanh toán |

---

### 3.3. Danh mục Menu món ăn & Đồ uống (Categories)
*Thư mục lưu file:* `src/cafe/resources/images/icons/categories/`

| STT | TÊN NÚT DANH MỤC TRÊN MENU | TÊN TÌM KIẾM TRÊN MẠNG (Search Keywords) | TÊN FILE LƯU (.png) | KÍCH THƯỚC TẢI | KÍCH THƯỚC HIỂN THỊ | VỊ TRÍ XUẤT HIỆN TRÊN HỆ THỐNG |
|:---:|:---|:---|:---|:---:|:---:|:---|
| 1 | **Nút "Tất cả"** | `grid 2x2`, `menu square`, `all categories`, `layers` | `tat-ca.png` | **24×24** hoặc **32×32** | 20×20 px | Nút danh mục 1 trên Sidebar Khách hàng |
| 2 | **Nút "Cà phê"** | `coffee cup`, `espresso`, `cappuccino`, `coffee beans` | `ca-phe.png` | **24×24** hoặc **32×32** | 20×20 px | Danh mục các dòng Cà phê Việt & Máy |
| 3 | **Nút "Trà" / "Trà sữa"** | `leaf`, `tea bag`, `cup hot`, `bubble tea` | `tra.png` | **24×24** hoặc **32×32** | 20×20 px | Danh mục Trà trái cây, Trà sen, Trà sữa |
| 4 | **Nút "Đá xay"** | `ice cream`, `smoothie cup`, `frappe`, `blender` | `da-xay.png` | **24×24** hoặc **32×32** | 20×20 px | Danh mục Freeze, Matcha đá xay, Socola đá xay |
| 5 | **Nút "Bánh ngọt"** | `cake`, `croissant`, `pastry`, `cookie` | `banh-ngot.png` | **24×24** hoặc **32×32** | 20×20 px | Danh mục Tiramisu, Croissant, Mousse |
| 6 | **Nút "Đồ ăn vặt"** | `cookie`, `popcorn`, `snack package`, `chips` | `mon-an-vat.png` | **24×24** hoặc **32×32** | 20×20 px | Danh mục Hạt hướng dương, khô gà, khoai tây |
| 7 | **Nút "Nước ép" / Sinh tố** | `citrus`, `glass juice`, `orange fruit`, `drink` | `nuoc-ep.png` | **24×24** hoặc **32×32** | 20×20 px | Danh mục Nước ép cam, dưa hấu, sinh tố bơ |
| 8 | **Nút "Topping"** | `sparkles`, `cherry`, `plus star`, `stars` | `topping.png` | **24×24** hoặc **32×32** | 20×20 px | Danh mục trân châu, thạch, kem cheese |

---

### 3.4. Phân hệ Quản lý / Server (Admin UI - Bổ sung tham chiếu)
*Thư mục lưu file:* `src/cafe/resources/images/icons/admin/` *(Đã cấu hình trong `AdminShellView.java`)*

| STT | TÊN NÚT TRÊN SIDEBAR ADMIN | TÊN TÌM KIẾM TRÊN MẠNG (Search Keywords) | TÊN FILE ĐÃ CẤU HÌNH | KÍCH THƯỚC TẢI | KÍCH THƯỚC HIỂN THỊ |
|:---:|:---|:---|:---|:---:|:---:|
| 1 | **Nút "Tổng quan"** | `layout dashboard`, `chart bar`, `dashboard` | `tong-quan.png` | **24×24** hoặc **32×32** | 20×20 px |
| 2 | **Nút "Gọi món"** | `shopping cart`, `pos terminal`, `cashier bag` | `goi-mon.png` | **24×24** hoặc **32×32** | 20×20 px |
| 3 | **Nút "Đơn hàng"** | `receipt`, `clipboard list`, `orders bill` | `don-hang.png` | **24×24** hoặc **32×32** | 20×20 px |
| 4 | **Nút "Màn pha chế"** | `coffee cup`, `cup hot`, `kitchen bar` | `man-pha-che.png` | **24×24** hoặc **32×32** | 20×20 px |
| 5 | **Nút "Món & giá"** | `tag`, `price tag`, `menu price` | `mon-va-gia.png` | **24×24** hoặc **32×32** | 20×20 px |
| 6 | **Nút "Nhân viên"** | `users`, `id card`, `employees` | `nhan-vien.png` | **24×24** hoặc **32×32** | 20×20 px |
| 7 | **Nút "Tồn kho"** | `package`, `box`, `inventory warehouse` | `kho.png` | **24×24** hoặc **32×32** | 20×20 px |
| 8 | **Nút "Báo cáo"** | `trending up`, `analytics`, `bar chart` | `bao-cao.png` | **24×24** hoặc **32×32** | 20×20 px |
| 9 | **Nút "Cài đặt"** | `settings`, `gear`, `cog`, `sliders` | `cai-dat.png` | **24×24** hoặc **32×32** | 20×20 px |

---

## 4. KẾ HOẠCH ĐIỀU CHỈNH LAYOUT (BEFORE VS AFTER)

### 4.1. Phân hệ Nhân viên (`FloorPlanView.java`):
* **Trước điều chỉnh:** `rail.setPrefWidth(130)`, padding nút `9, 12, 9, 12`, font 12.5px nhỏ, dùng trực tiếp `SVGPath` đơn lẻ không có container cố định khiến các icon có độ rộng khác nhau bị giật hàng lộn xộn.
* **Sau điều chỉnh (Theo chuẩn Admin):**
  - Mở rộng sidebar: `prefWidth = 235`, `minWidth = 220`, `maxWidth = 255`.
  - Padding `Insets(20, 12, 20, 12)`.
  - Brand Logo nâng cấp lên `Playfair Display 17.5px Weight 800` giống hệt Admin.
  - Sử dụng hàm nạp linh hoạt `getIconNode()` ưu tiên ảnh PNG từ `staff/` và fallback về SVG.
  - Mỗi icon được bọc trong `StackPane` kích thước chuẩn `26×26 px`, căn giữa hoàn hảo.
  - Nhãn chữ nút nâng lên `13.5px`, Weight 700/800, padding nút `10, 14, 10, 14`.
  - Góc chân trang thêm Profile nhân viên trực ca (Avatar tròn 32px, tên nhân viên, chức vụ "Nhân viên Order").

### 4.2. Phân hệ Khách hàng (`ClientView.java`):
* **Trước điều chỉnh:** Sidebar danh mục co rút còn `110px`, nút bấm bé tẹo (34px), khung icon chỉ 22px, Search bar hẹp (240px, cao 38px), chữ danh mục 12.5px.
* **Sau điều chỉnh (Theo chuẩn Admin):**
  - Mở rộng sidebar danh mục: `prefWidth = 235`, `minWidth = 220`, `maxWidth = 255`.
  - Brand Header to đẹp: "Brew & Bean" 17.5px Playfair Display.
  - Nút danh mục chuyển sang container icon `26×26 px`, hiển thị icon 20×20 px từ `categories/`, nhãn chữ `13.5px` weight 700.
  - Header Utility Bar: Search Bar tăng chiều rộng lên `280px - 320px`, chiều cao `42px`, font `13.5px`, có icon kính lúp to rõ.
  - Nút Giỏ hàng nổi (Floating Cart) nâng chiều cao lên `42px`, padding rộng rãi, badge số lượng món đỏ/đen nổi bật to rõ.
  - Responsive Grid món ăn: Điều chỉnh tỷ lệ cột tối ưu từ 3 - 5 cột tùy theo kích thước cửa sổ mở rộng tối đa (Maximized Form).

---

## 5. PROMPT TINH CHỈNH MÃ NGUỒN DÀNH RIÊNG CHO AGY

> [!TIP]
> Bạn chỉ cần copy nguyên văn đoạn prompt dưới đây và dán vào khung chat với AGY / Antigravity để hệ thống tự động refactor mã nguồn khớp 100% với AdminShellView.

```markdown
### NHIỆM VỤ DÀNH CHO AGY: ĐỒNG BỘ TOÀN DIỆN UI STAFF & CLIENT THEO BENCHMARK ADMINSHELLVIEW

Bạn là kỹ sư JavaFX cao cấp. Hãy lấy file `src/cafe/views/AdminShellView.java` làm MỐC CHUẨN DUY NHẤT (Benchmark) về kích thước, tỷ lệ, font chữ và cơ chế nạp icon để tiến hành tái cấu trúc (refactor) lại 2 giao diện:
1. `src/cafe/views/FloorPlanView.java` (Giao diện Nhân viên / Sơ đồ bàn)
2. `src/cafe/views/ClientView.java` (Giao diện Khách hàng / Gọi món)

---

### YÊU CẦU CHI TIẾT CẦN THỰC HIỆN:

#### 1. ĐỒNG BỘ THÔNG SỐ SIDEBAR THEO CHUẨN ADMIN:
- Độ rộng Sidebar: Đặt `setPrefWidth(235)`, `setMinWidth(220)`, `setMaxWidth(255)`.
- Padding Sidebar: Đặt `setPadding(new Insets(20, 12, 20, 12))`.
- Màu nền Sidebar: `#F7F2EA`, viền phải 1px `#DED7CD` (`-fx-background-color: #F7F2EA; -fx-border-color: transparent #DED7CD transparent transparent; -fx-border-width: 0 1 0 0;`).
- Brand Logo: 
  - Tiêu đề: "Brew & Bean" với font `Playfair Display`, serif, 17.5px, weight 800, text `#112823`.
  - Phụ đề: Subtitle chữ in hoa, 10px, weight 800, text `#563521`, letter-spacing `1.5px`.

#### 2. CHUẨN HÓA KHUNG CHỨA VÀ HIỂN THỊ ICON (KHẮC PHỤC TRIỆT ĐỂ LỆCH TRỤC DỌC):
- Mọi nút bấm trên Sidebar (cả nút điều hướng và nút danh mục món) đều PHẢI bọc icon trong một `StackPane iconContainer`:
  ```java
  StackPane iconContainer = new StackPane();
  iconContainer.setPrefSize(26, 26);
  iconContainer.setMinSize(26, 26);
  iconContainer.setMaxSize(26, 26);
  iconContainer.setAlignment(Pos.CENTER);
  ```
- Thêm phương thức nạp icon đa tầng `getIconNode(String iconFileName, String folderType, String svgFallback, String fallbackEmoji, boolean isActive)`:
  1. Thử nạp ảnh PNG từ classpath tài nguyên: `/cafe/resources/images/icons/{folderType}/` (với `folderType` là `staff`, `client`, hoặc `categories`).
  2. Thử nạp ảnh PNG từ thư mục dev trên đĩa: `src/cafe/resources/images/icons/{folderType}/`.
  3. Khi tìm thấy ảnh PNG: tạo `ImageView` với `setFitWidth(20); setFitHeight(20); setPreserveRatio(true); setSmooth(true);`.
  4. Nếu không có PNG: Fallback vẽ `SVGPath` vector nét mảnh (`SvgIcons.createOutlineIcon(...)`).
  5. Cuối cùng fallback bằng Label Emoji.

#### 3. QUY CHUẨN NÚT BẤM VÀ TYPOGRAPHY:
- Nút bấm Sidebar:
  - Căn lề: `Pos.CENTER_LEFT`, `btn.setMaxWidth(Double.MAX_VALUE)`.
  - Padding: `Insets(10, 14, 10, 14)`.
  - Khoảng cách HBox row: `12px`.
  - Label text: `-fx-font-size: 13.5px; -fx-font-weight: 700; -fx-text-fill: #3F3931;` (nếu Active thì weight `800` và text `#112823`).
  - Style Active: `-fx-background-color: #DED5C8; -fx-background-radius: 8px;`.
  - Style Inactive: `-fx-background-color: transparent; -fx-background-radius: 8px;`, hover vào `#EFE8DD`.
  - Áp dụng `AnimationHelper.applyButtonPress(btn)`.

#### 4. TINH CHỈNH RIÊNG CHO STAFF (`FloorPlanView.java`):
- Nâng cấp 5 nút điều hướng chính:
  1. "Bàn" (`so-do-ban.png`)
  2. "Thực đơn" (`thuc-don.png`)
  3. "Đơn hàng" (`don-hang.png`)
  4. "Thanh toán" (`thanh-toan.png`)
  5. "Cài đặt" (`cai-dat.png`)
- Dưới chân Sidebar thêm Profile nhân viên trực quầy (Avatar tròn r=16 `#112823`, chữ cái đầu, Tên nhân viên 13.5px bold, vai trò "Nhân viên phục vụ").
- Giữ nút "Đăng xuất" (`dang-xuat.png`) tách biệt ở phía dưới.

#### 5. TINH CHỈNH RIÊNG CHO CLIENT (`ClientView.java`):
- Sidebar danh mục:
  - Nút đầu tiên: "Sơ đồ bàn" (`ban-an.png`) với giao diện to rõ, chiều cao chuẩn.
  - Các nút danh mục trong `renderCategories`: Load từ folder `categories/` (`tat-ca.png`, `ca-phe.png`, `tra.png`, `da-xay.png`, `banh-ngot.png`, v.v.), icon 20px, container 26px, font chữ danh mục `13.5px`.
- Header Utility Bar:
  - Search Bar: Chiều cao `40px` (min/pref `40px`), chiều rộng `280px`, font `13px`, padding `8 14 8 14`.
  - Floating Cart Button: Chiều cao `40px`, padding `6 16 6 14`, font `13px` bold, icon giỏ hàng to rõ kèm badge đếm số lượng nổi bật.
- Giữ vững cơ chế Responsive Grid cho danh sách món ăn `ItemCard` để khi phóng to toàn màn hình (Maximized Form), các thẻ món tự giãn đều 3 - 5 cột mượt mà, không bị vỡ bố cục.

---
Hãy kiểm tra kỹ cú pháp JavaFX, không gây lỗi biên dịch (Compile error), và giữ nguyên toàn bộ các event handler, callback logic hiện có.
```
