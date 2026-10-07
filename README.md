# ☕ CafeOrder - Hệ Thống Đặt Món Quán Cafe Chuẩn MVC & JavaFX

> **Hệ thống quản lý đặt món tại bàn (Kiosk) và điều phối quầy pha chế (Kitchen Display System - KDS) thời gian thực.**  
> Dự án kết hợp công nghệ **JavaFX hiện đại**, mô hình mạng **TCP Socket hai chiều**, định dạng dữ liệu **JSON** và được thiết kế theo chuẩn kiến trúc **MVC (Model - View - Controller)**.

---

## 📖 Mục Lục
1. [Giới Thiệu Tổng Quan](#-giới-thiệu-tổng-quan)
2. [Triết Lý & Phong Cách Thiết Kế (UI/UX)](#-triết-lý--phong-cách-thiết-kế-uiux)
3. [Tính Năng Nổi Bật](#-tính-năng-nổi-bật)
4. [Kiến Trúc Dự Án (Mô Hình MVC)](#-kiến-trúc-dự-án-mô-hình-mvc)
5. [Yêu Cầu Hệ Thống & Cài Đặt](#-yêu-cầu-hệ-thống--cài-đặt)
6. [Hướng Dẫn Khởi Chạy Ứng Dụng](#-hướng-dẫn-khởi-chạy-ứng-dụng)
   * [Cách 1: Sử dụng Visual Studio Code (1-Click Run)](#cách-1-khởi-chạy-trong-visual-studio-code)
   * [Cách 2: Sử dụng NetBeans IDE](#cách-2-khởi-chạy-trong-apache-netbeans)
   * [Cách 3: Chạy trực tiếp qua Scripts (.bat) hoặc Terminal](#cách-3-khởi-chạy-bằng-scripts-hoặc-terminal)
7. [Hướng Dẫn Sử Dụng Chi Tiết](#-hướng-dẫn-sử-dụng-chi-tiết)
   * [Dành cho Khách Hàng (Màn hình Đặt Món Kiosk)](#1-dành-cho-khách-hàng-màn-hình-kiosk-tại-bàn)
   * [Dành cho Barista / Quản Lý (Màn hình Quầy Pha Chế KDS)](#2-dành-cho-barista--nhân-viên-quầy-pha-chế-kds)
8. [Giao Thức Trao Đổi Dữ Liệu (Network Protocol)](#-giao-thức-trao-đổi-dữ-liệu-network-protocol)
9. [Kiểm Thử Tự Động (Automated Testing)](#-kiểm-thử-tự-động-automated-testing)
10. [Giải Quyết Sự Cố Thường Gặp (Troubleshooting)](#-giải-quyết-sự-cố-thường-gặp-troubleshooting)

---

## 🌟 Giới Thiệu Tổng Quan

**CafeOrder** là giải pháp số hóa toàn diện quy trình gọi món cho các quán cà phê theo mô hình bán tự động (Kiosk tại bàn) và hiển thị đơn hàng cho quầy pha chế (KDS):
* **Màn hình Kiosk tại bàn (Client):** Khách hàng tự do duyệt menu, xem mô tả chi tiết, tùy biến hương vị (ít ngọt, không đá, thêm sữa...), theo dõi hóa đơn và gửi đơn hàng trực tiếp vào bếp.
* **Màn hình Quầy Pha Chế (Server KDS):** Nhân viên Barista nhận đơn tức thì không cần ghi giấy, xem vé đơn hàng trực quan, chuyển trạng thái chế biến và hệ thống tự động phát thông báo ngược về bàn của khách.

---

## 🎨 Triết Lý & Phong Cách Thiết Kế (UI/UX)

Giao diện được lấy cảm hứng từ các chuỗi cà phê boutique đương đại (*Blue Bottle Coffee*, *Starbucks Reserve*, *Toast POS*):
* **Chủ đề:** *Artisanal Coffeehouse & Relaxed Modern Lounge* (ấm cúng, thư giãn, hiện đại).
* **Bảng màu nhận diện:**
  * **Nền Linen ngà ấm (`#FAF7F2`):** Mang lại sự dịu mắt, sạch sẽ, tôn vinh hình ảnh món ăn.
  * **Espresso rang mộc (`#2C1810`, `#3E2723`):** Tạo độ tương phản cao, chuẩn mực cho chữ và tiêu đề.
  * **Hạt màu Caramel nướng / Terracotta (`#C97A44`):** Điểm nhấn bắt mắt cho nút hành động chính (Call-To-Action) và giá tiền.
  * **Matcha & Sage Green (`#4A7C59`):** Biểu thị trạng thái hoàn tất đơn hàng tươi mới, an tâm.
* **Tương tác vi mô (Micro-interactions):** Thẻ món ăn bo góc mềm mại (16px), hiệu ứng nổi bóng đổ nhẹ khi rê chuột (hover elevation), thanh điều khiển số lượng (Stepper) chuẩn màn hình cảm ứng.

---

## 🚀 Tính Năng Nổi Bật

* ⚡ **Giao tiếp Real-time hai chiều qua TCP Socket:** Tốc độ phản hồi dưới 50ms, độ ổn định cao, luồng đọc ngầm không gây đơ giật giao diện.
* 📦 **Gói tin chuẩn hóa JSON:** Bóc tách và đóng gói dữ liệu an toàn thông qua thư viện Google `Gson`.
* 🛒 **Giỏ hàng Reactive Observable:** Tự động tính toán thành tiền, cập nhật tổng hóa đơn theo thời gian thực khi thêm/bớt/sửa món.
* 🏷️ **Gợi ý ghi chú nhanh (Smart Chips):** Cho phép khách chọn nhanh *"Ít ngọt"*, *"Không đá"*, *"Nhiều sữa"*, *"Mang về"* chỉ với 1 chạm.
* 📊 **Bảng điều phối KDS chuyên nghiệp:** Phân loại vé đơn hàng theo mã màu (*Đang chờ*, *Đang pha chế*, *Đã xong*), tích hợp bộ đếm giờ và thanh thống kê số lượng đơn trực quan.
* 🔇 **Lọc sạch cảnh báo đỏ Console (Zero-Warning Output):** Tích hợp bộ triệt tiêu cảnh báo `WarningSuppressor`, loại bỏ hoàn toàn các thông báo `unnamed module` và `sun.misc.Unsafe` trên JDK 21+.

---

## 🏗️ Kiến Trúc Dự Án (Mô Hình MVC)

Toàn bộ mã nguồn cũ đã được thanh lọc, tổ chức chuẩn mực theo cấu trúc **MVC** dưới cây thư mục **Source Packages**:

```text
CafeOrder/
├── .vscode/                        # Cấu hình 1-Click Run & Debug cho Visual Studio Code
│   ├── launch.json                 # Cấu hình chạy Server, Client, Unit Test
│   ├── settings.json               # Cấu hình Classpath nạp thư viện JAR tự động
│   └── tasks.json                  # Tác vụ biên dịch và đồng bộ tài nguyên CSS
│
├── lib/                            # Thư viện phụ thuộc (Standalone Offline)
│   ├── gson-2.10.1.jar             # Google Gson xử lý JSON
│   ├── javafx-base-21.0.2*.jar     # JavaFX Core & Properties
│   ├── javafx-controls-21.0.2*.jar # JavaFX Controls (Button, Table, Label...)
│   ├── javafx-fxml-21.0.2*.jar     # JavaFX FXML Engine
│   └── javafx-graphics-21.0.2*.jar # JavaFX Graphics & Hardware Acceleration
│
├── src/                            # Thư mục mã nguồn chính (Source Packages)
│   └── cafe/
│       ├── ClientMain.java         # [ENTRY POINT] Khởi động Màn hình Đặt Món (Kiosk)
│       ├── ServerMain.java         # [ENTRY POINT] Khởi động Quầy Pha Chế (KDS)
│       │
│       ├── controllers/            # [C - CONTROLLER] Xử lý tương tác & điều phối State
│       │   ├── ClientController.java   # Điều phối catalog, tìm kiếm, giỏ hàng & gửi đơn
│       │   ├── LoginController.java    # Xác thực số bàn, chuyển tiếp vào Kiosk
│       │   └── ServerController.java   # Điều phối danh sách đơn hàng KDS, đổi trạng thái
│       │
│       ├── models/                 # [M - MODEL] Thực thể dữ liệu & Reactive State
│       │   ├── MenuItem.java           # Thực thể món (Tên, Giá, Nhóm, Mô tả, Icon)
│       │   ├── OrderItem.java          # Chi tiết dòng món (Số lượng, Ghi chú, Thành tiền)
│       │   ├── Order.java              # Đơn hàng (Mã đơn, Số bàn, Danh sách món, Trạng thái, Giờ)
│       │   ├── CartModel.java          # State giỏ hàng Reactive Observable (JavaFX Properties)
│       │   ├── MenuRepository.java     # Danh mục thực đơn chuẩn 16 món phong phú
│       │   ├── MessageProtocol.java    # Giao thức đóng/mở JSON DTO
│       │   └── MessageType.java        # Enum loại thông điệp mạng (HELLO, NEW_ORDER, ACK...)
│       │
│       ├── views/                  # [V - VIEW] Giao diện đồ họa JavaFX & CSS Stylesheets
│       │   ├── ClientView.java         # Bố cục màn hình Kiosk (Catalog dạng lưới + Drawer giỏ hàng)
│       │   ├── LoginView.java          # Màn hình đăng nhập số bàn với chip chọn nhanh
│       │   ├── ItemDetailDialog.java   # Hộp thoại popup chọn số lượng Stepper & ghi chú
│       │   ├── ServerKDSView.java      # Màn hình KDS quầy pha chế thời gian thực
│       │   ├── ItemCard.java           # Component thẻ món ăn hiện đại
│       │   ├── CartItemRow.java        # Component dòng món ăn trong giỏ hàng
│       │   ├── OrderTicketCard.java    # Component vé order KDS cho Barista
│       │   ├── client.css              # Bộ stylesheet phong cách Cafe ấm cúng cho Kiosk
│       │   └── server.css              # Bộ stylesheet chuyên dụng cho KDS Barista
│       │
│       ├── services/               # [SERVICES & NETWORKING] Xử lý Socket & Đa luồng
│       │   ├── ClientSocketService.java# Quản lý Socket kết nối máy chủ cho Kiosk
│       │   └── CafeServerService.java  # Socket Server quản lý kết nối nhiều bàn đồng thời
│       │
│       └── utils/                  # [UTILS] Tiện ích bổ trợ
│           ├── CurrencyFormatter.java  # Tiện ích định dạng tiền VND (VD: 29.000 đ)
│           └── WarningSuppressor.java  # Bộ lọc triệt tiêu cảnh báo console vô hại
│
├── test/                           # Bộ kiểm thử tự động
│   └── SystemVerificationTest.java # Kiểm thử End-to-End toàn bộ luồng Socket & Model
│
├── run-server.bat                  # Script 1-click khởi động Server trên Windows
├── run-client.bat                  # Script 1-click khởi động Client trên Windows
└── README.md                       # Tài liệu hướng dẫn sử dụng dự án
```

---

## 💻 Yêu Cầu Hệ Thống & Cài Đặt

* **Hệ điều hành:** Windows 10/11, macOS, hoặc Linux.
* **Java Development Kit (JDK):** Khuyên dùng **JDK 21 LTS** trở lên.
* **Môi trường phát triển (IDE):**
  * **Visual Studio Code** (khuyên dùng kèm extension *Extension Pack for Java*).
  * Hoặc **Apache NetBeans 17+**.
* **Thư viện đi kèm:** Đã tích hợp sẵn toàn bộ JAR JavaFX 21 và Gson trong thư mục `lib/`, không cần tải thêm bất cứ thư viện bên ngoài nào.

---

## 🚀 Hướng Dẫn Khởi Chạy Ứng Dụng

### Cách 1: Khởi chạy trong Visual Studio Code (Tiện lợi nhất)
1. Mở thư mục dự án `CafeOrder` trong **VS Code**.
2. Nhấn tổ hợp phím `Ctrl + Shift + D` để mở menu **Run and Debug**.
3. Chọn cấu hình từ danh sách thả xuống ở góc trên cùng bên trái:
   * **Bước 1:** Chọn **`☕ 1. Khởi động Server (Quầy Pha Chế KDS)`** ➔ Nhấn **Play** (màu xanh) hoặc phím `F5`. Cửa sổ KDS sẽ hiện lên và Server sẵn sàng đón khách tại cổng `5000`.
   * **Bước 2:** Chọn **`📱 2. Khởi động Client (Đặt Món Kiosk)`** ➔ Nhấn **Play**. Cửa sổ đăng nhập số bàn sẽ hiện lên.
   * *(Tùy chọn: Chọn `🧪 3. Chạy Kiểm Thử (Verification Test)` để chạy kiểm thử tự động).*

### Cách 2: Khởi chạy trong Apache NetBeans
1. Mở dự án `CafeOrder` trong **NetBeans**.
2. **Khởi động Server:** Nhấp chuột phải vào file `src/cafe/ServerMain.java` ➔ Chọn **Run File** (hoặc nhấn `Shift + F6`).
3. **Khởi động Client:** Nhấp chuột phải vào file `src/cafe/ClientMain.java` ➔ Chọn **Run File** (hoặc nhấn `Shift + F6`).

### Cách 3: Khởi chạy bằng Scripts hoặc Terminal
* **Khởi động Server:** Nhấp đúp chuột vào file `run-server.bat` hoặc gõ lệnh:
  ```powershell
  java "-Dfile.encoding=UTF-8" -XX:-PrintWarnings -cp "build/classes;lib/*" cafe.ServerMain
  ```
* **Khởi động Client:** Nhấp đúp chuột vào file `run-client.bat` hoặc gõ lệnh:
  ```powershell
  java "-Dfile.encoding=UTF-8" -XX:-PrintWarnings -cp "build/classes;lib/*" cafe.ClientMain
  ```

---

## 📱 Hướng Dẫn Sử Dụng Chi Tiết

### 1. Dành cho Khách Hàng (Màn hình Kiosk tại bàn)

```text
[Đăng Nhập Số Bàn] ➔ [Duyệt Menu & Tìm Kiếm] ➔ [Tùy Biến Món & Ghi Chú] ➔ [Kiểm Tra Giỏ Hàng] ➔ [Bấm Gửi Order]
```

1. **Đăng nhập số bàn:**
   * Nhập số bàn vào ô văn bản hoặc bấm chọn nhanh các chip bàn có sẵn (*Bàn 01*, *Bàn 02*,... *Bàn 08*).
   * Bấm nút **BẮT ĐẦU GỌI MÓN →**. Hệ thống sẽ tự động bắt tay với Server qua gói tin `HELLO`.
2. **Duyệt thực đơn & Tìm kiếm:**
   * Bấm vào các thẻ danh mục (*Tất cả*, *Cà phê*, *Trà & Trái cây*, *Đá xay & Sinh tố*, *Bánh ngọt*) để lọc món.
   * Gõ tên món vào thanh tìm kiếm (hỗ trợ tìm kiếm không dấu/có dấu tức thì).
3. **Tùy biến món ăn (Customization Dialog):**
   * Bấm vào thẻ món hoặc nút **+ Chọn**.
   * Dùng nút `-` hoặc `+` để chỉnh số lượng (tổng tiền món cập nhật ngay bên cạnh).
   * Bấm các chip gợi ý: *"Ít ngọt"*, *"Không đá"*, *"Nhiều sữa"*, *"Mang về"* hoặc tự nhập ghi chú riêng vào ô văn bản.
   * Bấm **✓ Thêm vào giỏ**.
4. **Kiểm tra giỏ hàng & Gửi đơn:**
   * Theo dõi danh sách món, số lượng, ghi chú và tổng thanh toán tại khung bên phải.
   * Có thể chỉnh tăng/giảm số lượng hoặc bấm `✕` để xóa món khỏi giỏ.
   * Bấm nút **☕ GỬI BẾP ORDER**. Hệ thống sẽ đóng gói đơn và gửi về quầy pha chế.
   * Một thông báo toast màu xanh sẽ hiện lên xác nhận đơn đặt thành công kèm mã Order ID. Khi Barista đổi trạng thái đơn, màn hình tại bàn sẽ nhận thông báo cập nhật tức thì.

---

### 2. Dành cho Barista / Nhân Viên Quầy Pha Chế (KDS)

```text
[Nhận Đơn Mới (ĐANG CHỜ)] ➔ [Bấm "▶ PHA CHẾ"] ➔ [Pha Xong Bấm "✓ HOÀN TẤT & GỬI BÀN"]
```

1. **Bảng vé đơn hàng thời gian thực:**
   * Mỗi khi có bàn gửi đơn, vé order mới sẽ tự động xuất hiện ở vị trí đầu tiên kèm hiệu ứng âm thanh và thẻ trạng thái **ĐANG CHỜ** (màu vàng hổ phách).
   * Vé hiển thị rõ: Số bàn (*BÀN 03*), Mã đơn (*#1002*), thời gian đặt, số lượng từng món và các dòng ghi chú nổi bật.
2. **Chuyển tiến độ pha chế:**
   * Khi bắt đầu làm món, Barista bấm **▶ PHA CHẾ**. Trạng thái chuyển sang **ĐANG PHA CHẾ** (màu xanh dương). Đồng thời, máy chủ gửi tín hiệu báo cho khách tại bàn biết món đang được chuẩn bị.
   * Khi làm xong món, bấm **✓ HOÀN TẤT & GỬI BÀN**. Đơn chuyển sang **ĐÃ XONG** (màu xanh lá) và thông báo gọi khách nhận đồ/phục vụ bàn.
3. **Thanh thống kê & Bộ lọc:**
   * Barista có thể bấm các nút lọc trên thanh công cụ để xem riêng đơn: *Tất cả đơn*, *Đang chờ*, *Đang pha chế*, *Đã xong*.
   * Ô tìm kiếm hỗ trợ lọc nhanh theo số bàn hoặc mã đơn.
   * Bấm nút **🗑 Xóa đơn đã xong** để dọn dẹp các vé đã hoàn tất phục vụ cho thoáng bảng.

---

## 📡 Giao Thức Trao Đổi Dữ Liệu (Network Protocol)

Hệ thống giao tiếp hai chiều qua cổng TCP `5000`, mỗi thông điệp là một dòng văn bản JSON kết thúc bằng ký tự xuống dòng `\n`:

### 1. Khách báo danh bàn (`HELLO`):
```json
{"type":"HELLO","table":3}
```

### 2. Khách gửi đơn đặt món (`NEW_ORDER`):
```json
{
  "type": "NEW_ORDER",
  "table": 3,
  "items": [
    {"name": "Bạc Xỉu Kem Béo", "qty": 2, "note": "Ít ngọt, nhiều đá", "unitPrice": 32000.0},
    {"name": "Croissant Bơ Pháp", "qty": 1, "note": "Làm nóng", "unitPrice": 32000.0}
  ]
}
```

### 3. Server xác nhận đơn (`ORDER_ACK`):
```json
{"type":"ORDER_ACK","orderId":1001,"status":"QUEUED","position":1}
```

### 4. Server cập nhật trạng thái đơn (`ORDER_STATUS_UPDATE`):
```json
{"type":"ORDER_STATUS_UPDATE","orderId":1001,"status":"PREPARING"}
```

---

## 🧪 Kiểm Thử Tự Động (Automated Testing)

Dự án có sẵn kịch bản kiểm thử tích hợp tự động kiểm tra trọn vẹn từ tầng Model, giỏ hàng đến kết nối Socket hai chiều:

```powershell
java "-Dfile.encoding=UTF-8" -XX:-PrintWarnings -cp "build/classes;lib/*" test.SystemVerificationTest
```

**Kết quả kiểm thử xác nhận:**
* ✅ **[TEST 1]** Nạp danh mục 16 món ăn và các nhóm sản phẩm từ `MenuRepository` thành công.
* ✅ **[TEST 2]** Reactive State `CartModel` cộng dồn số lượng và tính tổng tiền chính xác 100%.
* ✅ **[TEST 3]** Kết nối Socket Port giả lập, gửi gói `HELLO`, đẩy `NEW_ORDER`, nhận phản hồi `ORDER_ACK` và cập nhật trạng thái `ORDER_STATUS_UPDATE` diễn ra trơn tru không lỗi.

---

## ❓ Giải Quyết Sự Cố Thường Gặp (Troubleshooting)

| Tình huống | Nguyên nhân | Cách khắc phục |
| :--- | :--- | :--- |
| **"Không thể kết nối đến quầy pha chế"** | Chưa bật Server hoặc Server đang bị chiếm cổng | Khởi động `ServerMain` trước khi mở `ClientMain`. Đảm bảo cổng `5000` không bị ứng dụng khác chiếm giữ. |
| **Lỗi hiển thị tiếng Việt bị dấu hỏi `?`** | Terminal hoặc JVM chưa cấu hình UTF-8 | Thêm tham số JVM `-Dfile.encoding=UTF-8` khi chạy (các file script `.bat` và VS Code `launch.json` đã được cấu hình sẵn). |
| **Cảnh báo chữ đỏ khi khởi động ứng dụng** | JDK 21 cảnh báo module và bộ nhớ Unsafe | Đã được xử lý tự động bởi lớp `WarningSuppressor` và cờ `-XX:-PrintWarnings` tích hợp sẵn trong dự án. |

---

<p align="center">
  <b>☕ CafeOrder — Trải Nghiệm Thưởng Thức Cà Phê Hiện Đại & Tinh Tế</b><br>
  <i>Phát triển bởi đội ngũ kỹ sư Java & UI/UX Specialist</i>
</p>
