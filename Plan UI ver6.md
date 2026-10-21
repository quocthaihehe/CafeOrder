# KẾ HOẠCH NÂNG CẤP GIAO DIỆN & HOÀN THIỆN NGHIỆP VỤ HỆ THỐNG (PLAN UI VER 6.0)
**Dự án:** Hệ thống Quản lý Bán hàng & Điều phối Đặt món Quán Cafe (CafeOrder System)  
**Tài liệu tham chiếu:** `Plan UI ver6.md`  
**Phiên bản:** UI/UX & Architecture Version 6.0  
**Đối tượng thực hiện:** Antigravity CLI / Developer  
**Nguyên tắc cốt lõi:**  
> ⚠️ **STRICT CONSTRAINT:** Tài liệu này đóng vai trò là bản thiết kế kiến trúc và giải pháp chi tiết. **TUYỆT ĐỐI KHÔNG CHỈNH SỬA BẤT KỲ DÒNG MÃ NGUỒN NÀO** trong phiên hiện tại.  
> 💡 **ADAPTIVE METRICS:** Các thông số kích thước, tọa độ, khoảng cách được thiết kế dưới dạng **tỷ lệ co giãn thích ứng (Responsive/Relative Layout)**, không dùng các hằng số cứng nhắc (hardcoded magic numbers) để AI CLI và hệ thống tự động tối ưu theo từng độ phân giải màn hình thực tế.

---

## MỤC LỤC TỔNG QUAN

1. [Bối cảnh & 5 Trọng tâm Cần xử lý](#1-bối-cảnh--5-trọng-tâm-cần-xử-lý)
2. [Vấn đề 1: Luồng Nghiệp vụ & Khắc phục Nút "Định lượng →" (Tab Thực đơn & Định mức)](#2-vấn-đề-1-luồng-nghiệp-vụ--khắc-phục-nút-định-lượng--tab-thực-đơn--định-mức)
3. [Vấn đề 2: Lỗi Socket Kick - Phân tích & Triệt tiêu Vấn đề "Kick phiên 2 nhưng vẫn chạy được"](#3-vấn-đề-2-lỗi-socket-kick---phân-tích--triệt-tiêu-vấn-đề-kick-phiên-2-nhưng-vẫn-chạy-được)
4. [Vấn đề 3: Chuẩn hóa Hiện thị Văn bản & Triệt tiêu Cắt xén `...` trên UI Nhân viên (Staff Shell)](#4-vấn-đề-3-chuẩn-hóa-hiện-thị-văn-bản--triệt-tiêu-cắt-xén--trên-ui-nhân-viên-staff-shell)
5. [Vấn đề 4: Thiết kế Logic Nghiệp vụ Toàn diện cho Thẻ Hồ Sơ Nhân viên & Bàn giao Ca trực](#5-vấn-đề-4-thiết-kế-logic-nghiệp-vụ-toàn-diện-cho-thẻ-hồ-sơ-nhân-viên--bàn-giao-ca-trực)
6. [Vấn đề 5: Mở rộng Tỷ lệ Sơ đồ Bàn, Ghế, Quầy & Xóa bỏ Hoàn toàn Khoảng trắng Rỗng](#6-vấn-đề-5-mở-rộng-tỷ-lệ-sơ-đồ-bàn-ghế-quầy--xóa-bỏ-hoàn-toàn-khoảng-trắng-rỗng)
7. [Lộ trình Triển khai & Tiêu chuẩn Nghiệm thu (Definition of Done)](#7-lộ-trình-triển-khai--tiêu-chuẩn-nghiệm-thu-definition-of-done)

---

## 1. BỐI CẢNH & 5 TRỌNG TÂM CẦN XỬ LÝ

Qua quá trình đối soát thực tế vận hành và các ảnh chụp kiểm thử trực quan trên màn hình quản trị và nhân viên, hệ thống phát sinh 5 điểm nghẽn kỹ thuật và trải nghiệm người dùng (UX):

```
+-----------------------------------------------------------------------------------------+
|                                5 ĐIỂM NGHẼN TRỌNG TÂM (VER 6.0)                         |
+---+----------------------------+--------------------------------------------------------+
| 1 | Nút "Định lượng →"         | Giao diện bị đẩy mất khung bên phải; click nút không   |
|   | (Tab 5 Admin)              | phản hồi trực quan, gây hiểu lầm là nút bị liệt.       |
+---+----------------------------+--------------------------------------------------------+
| 2 | Lỗi Socket Kick Session    | Server đóng socket nhưng không gửi lệnh ngắt phiên     |
|   | (Tab 11 Telemetry)         | sang Client, Client giữ nguyên màn hình và tự nối lại. |
+---+----------------------------+--------------------------------------------------------+
| 3 | Lỗi chữ dài bị cắt "..."   | TopBar nút quá dài nhồi nhét 1 hàng; Thẻ hồ sơ đáy     |
|   | (FloorPlanView & Sidebar)  | sidebar bị nút "Thoát" ép hẹp chiều rộng gây co chữ.   |
+---+----------------------------+--------------------------------------------------------+
| 4 | Thẻ Hồ sơ Nhân sự          | Đang là thẻ tĩnh, thiếu logic nghiệp vụ ca kíp,        |
|   | (Staff Profile Card)       | đổi mã PIN, kiểm kê doanh thu và bàn giao két ca trực. |
+---+----------------------------+--------------------------------------------------------+
| 5 | Sơ đồ Bàn & Nội thất lọt thỏm| Kích thước bàn quá nhỏ (74x60px), gom cứng trong 720px |
|   | (Floor Canvas Layout)      | để lại khoảng trắng beige mênh mông 2 bên và đáy.      |
+---+----------------------------+--------------------------------------------------------+
```

---

## 2. VẤN ĐỀ 1: LUỒNG NGHIỆP VỤ & KHẮC PHỤC NÚT "ĐỊNH LƯỢNG →" (TAB THỰC ĐƠN & ĐỊNH MỨC)

### 2.1. Phân tích Nguyên nhân Gốc rễ (Root Cause)
1. **Thiết kế Bố cục Cột cứng (Fixed Multi-Column):**
   - Trong `AdminSubViews.java` (`buildMenuAndBomView`), giao diện dùng một `HBox body` chứa:
     - `leftCard` (Bảng danh sách thực đơn): Chứa 6 cột với các chiều rộng tối thiểu gồm Mã (48px), Tên món (190px), Danh mục (125px), Giá bán (95px), Trạng thái 86 (115px), Thao tác (90px) cùng lề và khoảng cách $\rightarrow$ Chiếm tối thiểu ~750px.
     - `rightCard` (Khung chi tiết BOM): Đặt `minWidth(375)` và `prefWidth(400)`.
     - `leftRail` (Sidebar thanh điều hướng): Rộng 235px.
   - Tổng chiều rộng cần thiết: $750px + 400px + 235px + 40px\text{ (margins)} = 1425px$!
   - Khi chạy trên màn hình laptop tiêu chuẩn (1366x768, 1280x800) hoặc khi cửa sổ chưa phóng to tối đa, toàn bộ khối `rightCard` bị đẩy lọt ra ngoài mép phải của cửa sổ.
2. **Hiệu ứng Tương tác (UX Affordance) Sai lệch:**
   - Khi người dùng click nút `[ Định lượng → ]`, sự kiện chỉ gán:
     ```java
     selectedItemHolder[0] = item;
     refreshLeftRows[0].run();
     refreshRightBom[0].run();
     ```
   - Ký tự mũi tên `→` thường gợi ý hành động chuyển màn hình hoặc mở cửa sổ mới. Tuy nhiên hệ thống chỉ âm thầm vẽ lại `rightCard` (vốn đang bị khuất ra ngoài màn hình). Người dùng không nhận thấy bất kỳ thay đổi nào và kết luận *"nút định lượng chưa dùng được"*.

### 2.2. Giải pháp Kiến trúc: Chuyển đổi sang Modal Popup BOM Độc lập (`showBomRecipeModal`)

Thay vì ép 2 khung lớn nằm ngang gây vỡ khung trên màn hình nhỏ, giải pháp tối ưu nhất là chuyển chức năng soi và sửa công thức BOM thành **Cửa sổ Hộp thoại Chi tiết (Detail Modal)**, tương tự các modal chuẩn của hệ thống (`CartModalDialog`, `PaymentDialog`).

```
+-----------------------------------------------------------------------------------+
|               [MODAL] CHI TIẾT CÔNG THỨC ĐỊNH LƯỢNG NGUYÊN LIỆU (BOM)             |
+-----------------------------------------------------------------------------------+
|  [HÌNH ẢNH MÓN]   CÀ PHÊ MUỐI BIỂN HOÀNG GIA                      [ ĐANG BÁN ]   |
|  (64x64px)        Danh mục: Cà phê Pha Máy  •  Giá niêm yết: 45,000 đ             |
+-----------------------------------------------------------------------------------+
|  DANH SÁCH NGUYÊN LIỆU TIÊU HAO CHO 1 PHẦN BÁN:                                   |
|  +----------------------------------------------------+--------------+-----------+
|  | Nguyên liệu                                        | Định lượng   | Chi phí   |
|  +----------------------------------------------------+--------------+-----------+
|  | ☕ Cà phê Robusta Mộc Cầu Đất                       | 25 gram      | 4,200 đ   |
|  | 🥛 Sữa đặc có đường Laroseee                       | 30 ml        | 1,800 đ   |
|  | 🧂 Kem béo Rich's + Muối hồng Himalaya              | 40 ml        | 3,100 đ   |
|  | 🧊 Đá bi tinh khiết                                 | 150 gram     |   450 đ   |
|  | 🥤 Ly nhựa PET 500ml + Nắp cầu + Ống hút giấy      | 1 bộ         | 1,130 đ   |
|  +----------------------------------------------------+--------------+-----------+
|                                                                                   |
|  + THÊM NGUYÊN LIỆU VÀO CÔNG THỨC:                                                |
|  [ Chọn nguyên liệu từ kho... ▼ ]  [ 20 ] (ml/g)  [ + Thêm vào công thức ]        |
+-----------------------------------------------------------------------------------+
|  TỔNG KẾT TÀI CHÍNH MÓN ĂN:                                                       |
|  - Giá vốn hàng bán (COGS): 10,680 đ                                              |
|  - Lãi gộp (Gross Profit): 34,320 đ  •  Tỷ suất lợi nhuận: 76.3%                  |
+-----------------------------------------------------------------------------------+
|                                                 [ Hủy bỏ ]   [ LƯU CÔNG THỨC ]    |
+-----------------------------------------------------------------------------------+
```

#### Quy trình Xử lý cho AI CLI:
1. Khi click `btnInspect` ("Định lượng →"):
   - Gọi hàm hiển thị modal: `showBomRecipeModal(parentStage, item, onUpdatedCallback)`.
2. Trong Modal:
   - Tải danh sách nguyên liệu từ `DatabaseManager.getRecipesForProduct(item)`.
   - Hiển thị danh sách động có thể thêm bớt nguyên liệu.
   - Tự động tính toán tổng COGS (Cost of Goods Sold) và tỷ suất lợi nhuận trực tiếp khi thay đổi định lượng.
   - Khi nhấn `Lưu công thức`, lưu lại vào `customRecipesMap` hoặc bảng `ProductRecipes` trong SQL Server.
   - Bắn thông báo Toast: `"Đã lưu định lượng thành công cho món [Tên món]"`.

---

## 3. VẤN ĐỀ 2: LỖI SOCKET KICK - PHÂN TÍCH & TRIỆT TIÊU VẤN ĐỀ "KICK PHIÊN 2 NHƯNG VẪN CHẠY ĐƯỢC"

### 3.1. Phân tích Luồng Logic Hiện tại (Tạo sao Kick phiên 2 nhưng vẫn chạy?)

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Quản trị viên (Tab 11)
    participant Server as CafeServerService
    participant Socket as TCP Socket Link
    participant Client as ClientSocketService
    participant UI as ClientView / POS Window

    Admin->>Server: Click [ ✕ Kick ] phiên #2
    Note over Server: Server chỉ chạy: activeSessions.remove("2"); socket.close();
    Server--xSocket: Đóng kênh TCP (Không gửi thông báo ngắt kết nối)
    
    Socket-->>Client: Phát hiện Socket Closed / EOF trên in.readLine()
    Note over Client: Client hiểu nhầm là rớt mạng tạm thời (IOException)!
    Client->>UI: showToast("Mất kết nối với máy chủ pha chế!")
    
    Note over UI: UI Client VẪN ĐỨNG NGUYÊN trên màn hình! Không bị đá ra Login!
    actor User2 as Khách / Thu ngân phiên #2
    User2->>UI: Bấm giỏ hàng / Thao tác đặt món mới
    UI->>Client: sendOrder(...) -> Socket chưa nối -> Gọi tryConnectToServer()
    Client->>Server: Gửi gói tin HELLO mới toanh (Port 5000)
    Server-->>Client: Chấp nhận kết nối, cấp phiên làm việc MỚI!
    Note over Admin,Server: Phiên 2 vừa bị kick biến mất, nhưng phiên mới lại mọc lên ngay lập tức!
```

**Kết luận nguyên nhân:**
1. **Thiếu gói tin Protocol thông báo KICK:** Server ngắt socket ngang xương mà không truyền lý do hay tín hiệu `SESSION_KICKED`.
2. **Thiếu cơ chế Chấm dứt Vòng đời (Termination Handler) phía Client:** Client không hề có hàm đóng băng ứng dụng, không xóa phiên, không trở về màn hình đăng nhập `LoginView`.
3. **Cơ chế Reconnect/Warmup ngầm tự phát:** Ngay khi có tương tác tiếp theo, Client mở lại socket và gửi lệnh `HELLO` như một máy mới tinh.

### 3.2. Thiết kế Luồng Triệt tiêu Kick Đạt chuẩn Socket Production

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Quản trị viên
    participant Server as CafeServerService
    participant Client as ClientSocketService
    participant UI as Client Shell / LoginView

    Admin->>Server: Yêu cầu Kick phiên #2 (Kèm lý do)
    Server->>Client: Gửi gói tin SESSION_KICKED { "reason": "Admin đã ngắt phiên này", "banMs": 60000 }
    Note over Server: Đưa ID/IP vào Blacklist tạm thời 60 giây
    Server->>Server: unbindTableSubscriber() & close() socket
    
    Client->>Client: Nhận diện type == SESSION_KICKED
    Note over Client: Đặt running = false; dừng heartbeat; chặn tự động reconnect!
    Client->>UI: Kích hoạt onSessionKickedCallback
    UI->>UI: Hiển thị Modal Cảnh báo Đỏ (Modal Alert Lockout)
    Note over UI: "Phiên làm việc của bạn đã bị ngắt bởi Quản trị viên!"
    UI->>UI: Xóa sạch dữ liệu giỏ hàng (Cart.clear())
    UI->>UI: Ép chuyển Stage về LoginView ngay lập tức!
```

#### Các Bước Cải tiến Kỹ thuật:
1. **Mở rộng `MessageType.java`:**
   ```java
   public enum MessageType {
       // ... các type cũ ...
       SESSION_KICKED,       // Server gửi cho Client khi Admin kick
       FORCE_LOGOUT          // Lệnh bắt buộc đăng xuất
   }
   ```
2. **Xây dựng DTO Message trong `MessageProtocol.java`:**
   ```java
   public static class SessionKickedMessage {
       public String type = MessageType.SESSION_KICKED.name();
       public String sessionId;
       public String reason;
       public long timestamp;
   }
   ```
3. **Cập nhật `kickSession` tại `CafeServerService.java`:**
   - Trước khi đóng Socket: Gửi `SessionKickedMessage` qua `session.out.print(...)`.
   - `flush()` dữ liệu xuống đường truyền mạng.
   - Cho socket nghỉ 50ms để gói tin chắc chắn đến Card mạng của Client, sau đó mới `close()`.
4. **Xử lý tại `ClientSocketService.java`:**
   - Khi nhận `SESSION_KICKED`: Thiết lập cờ `isKickedByAdmin = true`. Vô hiệu hóa mọi tiến trình tự động kết nối lại (`reconnectThread`, `warmupConnectAsync`).
   - Gọi giao diện: `Platform.runLater(() -> onKickedHandler.run())`.
   - Điều hướng người dùng về `LoginView` với thông báo lý do rõ ràng.

---

## 4. VẤN ĐỀ 3: CHUẨN HÓA HIỆN THỊ VĂN BẢN & TRIỆT TIÊU CẮT XÉN `...` TRÊN UI NHÂN VIÊN (STAFF SHELL)

### 4.1. Khắc phục Thanh công cụ TopBar (`FloorPlanView.java`)

#### Hiện trạng:
Hàng công cụ tầng và bộ lọc nhồi nhét tới 9 nút bấm có nhãn dài trên cùng 1 hàng ngang `HBox`:
- `Tầng 1 (Sảnh chính)`, `Tầng 2 (Ban công)`, `Tầng 3 (Rooftop)`
- `🪑 Tất cả bàn`, `◯ Bàn trống`, `✔ Có khách`
- `⟳ Làm mới`, `⇄ Chuyển bàn`, `⧉ Gộp bàn`
Trong khi chiều rộng của vùng làm việc chỉ có khoảng 750px (do bị trừ đi Sidebar 235px và Inspector Drawer 295px), khiến tất cả nút đồng loạt biến thành `Tầng 1 (Sảnh ch...`, `🪑 Tất...`, `◯ Bàn tr...`, `⇄ Chuyển ...`.

#### Giải pháp Cải tiến (2-Tier Compact Toolbar Layout):
Thay vì kéo dãn vô tận trên 1 hàng, chia thanh công cụ thành **2 tầng rõ ràng hoặc sử dụng nhãn ngắn gọn chuẩn POS quốc tế**:

```
[BỐ CỤC ĐỀ XUẤT: PHÂN TẦNG HOẶC RÚT GỌN TỐI ƯU]

+-----------------------------------------------------------------------------------------+
| [ Tầng 1 ]  [ Tầng 2 ]  [ Tầng 3 ]  |  [ Tất cả ] [ Trống ] [ Có khách ]  |  [⟳] [⇄] [⧉] |
+-----------------------------------------------------------------------------------------+
```

1. **Rút gọn Nhãn Nút Bấm:**
   - `Tầng 1 (Sảnh chính)` $\rightarrow$ **`Tầng 1`** (Chi tiết sảnh đặt vào `Tooltip: Sảnh chính & Barista`).
   - `Tầng 2 (Ban công)` $\rightarrow$ **`Tầng 2`** (Tooltip: `Ban công ngoài trời & Phòng đọc sách`).
   - `Tầng 3 (Rooftop)` $\rightarrow$ **`Tầng 3`** (Tooltip: `Sân thượng Rooftop Zen Garden`).
   - `🪑 Tất cả bàn` $\rightarrow$ **`Tất cả`** (Kèm số đếm dạng badge: `[ Tất cả 12 ]`).
   - `◯ Bàn trống` $\rightarrow$ **`Bàn trống`** (Kèm số đếm: `[ Trống 7 ]`).
   - `✔ Có khách` $\rightarrow$ **`Đang phục vụ`** (Kèm số đếm: `[ Đang phục vụ 5 ]`).
2. **Nhóm Thao tác Nhanh (Action Tools):**
   - Đổi các nút dài dòng thành nút Icon-Text tinh tế với kích thước cố định vừa vặn:
     - `[ ⟳ Làm mới ]` $\rightarrow$ Nút vuông icon xoay `[ ⟳ ]` (PrefWidth 36px, tooltip "Làm mới dữ liệu bàn").
     - `[ ⇄ Chuyển bàn ]` $\rightarrow$ `[ ⇄ Chuyển ]`.
     - `[ ⧉ Gộp bàn ]` $\rightarrow$ `[ ⧉ Gộp ]`.

### 4.2. Khắc phục Thẻ Hồ Sơ Nhân viên dưới đáy Sidebar (`FloorPlanView.java` & `AdminShellView.java`)

#### Hiện trạng:
Sidebar cố định rộng 235px. Khối `profileCard` chứa:
- Padding trong: 20px
- Avatar: 34px
- Khoảng cách gap: 10px
- Nút "Thoát" màu đỏ: ~55px
- Chiều rộng còn lại cho phần chữ tên và vai trò: $235 - 24 - 20 - 34 - 10 - 55 \approx 92px$!
Khi tên nhân viên là `"Trần Minh Anh"`, chiều dài chữ thực tế vượt quá 120px dẫn đến việc bị ép thành `"Trần Minh ..."` và vai trò `"Thu ngân"` bị ép thành `"Th..."`.

#### Giải pháp Thiết kế: Tái cấu trúc Thẻ Hồ Sơ Dọc 2 Tầng (Vertical Stacked Profile Card)

```
+---------------------------------------------+
| CẤU TRÚC HIỆN TẠI (BỊ ÉP HẸP):              |
| [AVT] Trần Minh ... [Thoát]                 |
|       Th...                                 |
+---------------------------------------------+
                       ▼
+---------------------------------------------+
| CẤU TRÚC ĐỀ XUẤT (VER 6.0 HIỂN THỊ ĐỦ 100%):|
| +-----------------------------------------+ |
| | [ AVT ]  Trần Minh Anh                  | |
| |  (MA)    Thu ngân • Ca sáng             | |
| +-----------------------------------------+ |
| [ ⚙ Ca trực & Bàn giao ]     [ Đăng xuất ]  |
+---------------------------------------------+
```

- **Hàng 1 (Thông tin nhận diện):** Avatar tròn 36px + Cột chữ Tên đầy đủ (`Trần Minh Anh`, font 13px bold, không giới hạn maxWidth) + Nhãn vai trò với badge màu sắc trang nhã (`Thu ngân`).
- **Hàng 2 (Thanh công cụ tác vụ):** Tách nút "Ca trực / Bàn giao" và nút "Đăng xuất" xuống hàng dưới hoặc bố trí nút đăng xuất dạng icon nhỏ gọn ở góc trên phải của Card.
- Chiều rộng dành cho chữ tăng từ 92px lên **165px**, loại bỏ 100% hiện tượng nhảy chữ `...`.

---

## 5. VẤN ĐỀ 4: THIẾT KẾ LOGIC NGHIỆP VỤ TOÀN DIỆN CHO THẺ HỒ SƠ NHÂN VIÊN & BÀN GIAO CA TRỰC

### 5.1. Mô hình Nghiệp vụ Ca trực (Shift Life-Cycle Management)

Thẻ hồ sơ nhân sự không chỉ đơn thuần là thông tin trang trí, mà là trung tâm điều phối **Vòng đời Ca trực (Shift Session)** trong vận hành quán F&B chuyên nghiệp:

```
[ ĐĂNG NHẬP VÀO CA ] ──> [ KHAI BÁO QUỸ ĐẦU CA ] ──> [ THỰC HIỆN BÁN HÀNG ]
                                                            │
[ ĐĂNG XUẤT / RA CA ] <── [ IN BÁO CÁO Z-REPORT ] <── [ ĐỐI SOÁT & KẾT CA ]
```

### 5.2. Chi tiết Modal Nghiệp vụ: "Hồ Sơ Nhân Viên & Kết Toán Ca Trực"

Khi nhân viên click vào Thẻ Hồ Sơ ở Sidebar, hệ thống mở Modal tương tác chuyên sâu gồm 3 phân khu nghiệp vụ:

```
+-----------------------------------------------------------------------------------+
|               HỒ SƠ NHÂN VIÊN & QUẢN LÝ CA TRỰC (STAFF SHIFT DRAWER)              |
+-----------------------------------------------------------------------------------+
|  [AVATAR]   TRẦN MINH ANH (Mã NV: NV-003)                                         |
|   (MA)      Chức danh: Thu ngân chính (Cashier)  •  Trạng thái: Đang hoạt động    |
|             Tài khoản: cashier1  •  Đăng nhập lúc: 06:45 - 21/10/2026             |
+-----------------------------------------------------------------------------------+
|  📊 ĐỐI SOÁT DOANH THU CA HIỆN TẠI (CA SÁNG: 06:30 - 14:30):                      |
|                                                                                   |
|  1. Tiền mặt ban đầu trong két (Cash Float):                1,000,000 đ          |
|  2. Tổng số hóa đơn đã xử lý thành công:                    28 đơn hàng          |
|  3. Doanh số thanh toán TIỀN MẶT trong ca:                  2,450,000 đ          |
|  4. Doanh số thanh toán CHUYỂN KHOẢN (VietQR):              3,820,000 đ          |
|  -----------------------------------------------------------------------          |
|  => TỔNG TIỀN MẶT LÝ THUYẾT PHẢI CÓ TRONG KÉT:              3,450,000 đ          |
+-----------------------------------------------------------------------------------+
|  💵 BÀN GIAO TIỀN MẶT KHI RA CA:                                                  |
|  Số tiền mặt thực tế kiểm đếm: [ 3,450,000           ] đ                          |
|  Chênh lệch két:               [ 0 đ (Khớp 100%)    ]                             |
|  Ghi chú ca trực:              [ Ca sáng êm đẹp, bàn 05 đã đặt trước lúc 14h... ] |
+-----------------------------------------------------------------------------------+
|  [ 🔑 Đổi mã PIN ]   [ 🖨 In Báo Cáo Ca (Z-Report) ]   [ 🚪 KẾT CA & ĐĂNG XUẤT ]   |
+-----------------------------------------------------------------------------------+
```

#### Các Chức năng Nghiệp vụ Cụ thể:
1. **Đổi Mật khẩu / Mã PIN Cá nhân (`showChangePasswordModal`):**
   - Cho phép nhân viên tự đổi mật khẩu tài khoản trực tiếp ngay trên POS mà không cần nhờ Admin.
   - Kiểm tra mật khẩu cũ, mật khẩu mới tối thiểu 4-6 ký tự.
2. **Kiểm kê Két tiền (Cash Drawer Count):**
   - Tự động cộng dồn doanh số tiền mặt thực thu từ các hóa đơn có `staff_id` tương ứng trong ca.
   - So sánh với số tiền thực đếm của thu ngân để phát hiện lệch két (Thừa/Thiếu).
3. **In Báo Cáo Ca (Z-Report Print / Preview):**
   - Kết xuất file hóa đơn tổng kết ca gửi máy in hóa đơn nhiệt 80mm hoặc xuất ra màn hình tóm tắt.
4. **Bàn giao Ca & Đăng xuất An toàn (Handover & Safe Sign-out):**
   - Lưu biên bản bàn giao ca vào bảng `ShiftHandovers` (hoặc Audit Log hệ thống).
   - Đóng socket, xóa session trên bộ nhớ và điều hướng an toàn về màn hình `LoginView`.

---

## 6. VẤN ĐỀ 5: MỞ RỘNG TỶ LỆ SƠ ĐỒ BÀN, GHẾ, QUẦY & XÓA BỎ HOÀN TOÀN KHOẢNG TRẮNG RỖNG

### 6.1. Phân tích Nguyên nhân Lọt Thỏm (Canvas Bị Thu Hẹp)

1. **Kích thước Phần tử Quá Nhỏ:**
   - Trong `TableWithChairsNode.java:49`:
     - Bàn tiêu chuẩn 4 chỗ: `tableW = 74px`, `tableH = 60px`!
     - Ghế: Kích thước viên thuốc chỉ `34x12px`.
     - Cỡ chữ số bàn: 12px, nhãn doanh thu "150k • 2 món" bị nén chặt.
   - Một chiếc bàn 74px trên màn hình độ phân giải Full HD (1920x1080) chỉ nhỏ bằng một chiếc tem thư!
2. **Gán Tọa độ Cứng trong Khung 720px (`FloorPlanView.java:1396`):**
   - Code hiện tại tính:
     ```java
     double canvasWidth = floorCanvas.getWidth();
     double offsetX = canvasWidth > 720 ? (canvasWidth - 720) / 2.0 : 20;
     ```
   - Tất cả 12 bàn chỉ được đặt trong khoảng cách 720px từ `offsetX + 35` đến `offsetX + 485`.
   - Khi cửa sổ phóng to (Canvas rộng 1000px - 1100px), công thức trên chia đều khoảng dư ra 2 bên, tạo ra hai vùng beige rỗng khổng lồ mỗi bên rộng 150px - 200px.
   - Về chiều cao: Hàng bàn cuối cùng chỉ nằm ở tọa độ $y = 368px$, trong khi chiều cao Canvas lên tới $550px - 650px$, để lại khoảng trống mênh mông ở nửa dưới đáy màn hình!

### 6.2. Thiết kế Giải pháp Mở rộng Tỷ lệ Thích ứng (Responsive Adaptive Grid)

#### 1. Phóng to Kích thước Bàn Ghế & Nội thất (`TableWithChairsNode`):
Thay vì kích thước 74x60px tí hon, nâng tỷ lệ linh hoạt theo bảng tham chiếu sau:

```
+---------------------+-----------------------+-----------------------+
| Thành phần          | Kích thước Cũ (Ver 5) | Kích thước Mới Đề xuất|
+---------------------+-----------------------+-----------------------+
| Mặt bàn đơn (4 ghế) | 74 x 60 px            | 115 x 85 px (+55%)    |
| Mặt bàn dài (8 ghế) | 148 x 60 px           | 210 x 95 px (+50%)    |
| Ghế tựa viên thuốc  | 34 x 12 px            | 50 x 16 px            |
| Cỡ chữ số bàn       | 12px Regular          | 16px Extra Bold       |
| Nhãn tóm tắt đơn    | 10px (khó đọc)        | 12.5px Bold Hiển thị rõ|
| Chậu cây 3D decor   | 68 x 68 px            | 90 x 90 px            |
| Quầy Barista & POS  | Cao 32px              | Cao 48px, Trải rộng   |
+---------------------+-----------------------+-----------------------+
```

#### 2. Thuật toán Phân bổ Lưới Động theo Kích thước Khung chứa (Dynamic Canvas Distribution):
Không dùng con số cứng `offsetX = (canvasWidth - 720) / 2.0`. Sử dụng tỷ lệ phân bổ động tự động co giãn theo `canvasWidth` và `canvasHeight`:

```
+---------------------------------------------------------------------------------------------+
|                                    BỐ CỤC LƯỚI TẦNG 1 CO GIÃN ĐẦY ĐỦ                         |
+---------------------------------------------------------------------------------------------+
|  [ ☕ QUẦY PHA CHẾ BARISTA & THU NGÂN POS ] (Trải rộng 70% bề ngang)   [ 🚪 LỐI VÀO CHÍNH ]   |
|                                                                                             |
|   +---------------+     +---------------+     +---------------+     +---------------+       |
|   |    BÀN 01     |     |    BÀN 02     |     |    BÀN 03     |     |    BÀN 04     |       |
|   |  (115x85px)   |     |  (115x85px)   |     |  (115x85px)   |     |  (115x85px)   |       |
|   +---------------+     +---------------+     +---------------+     +---------------+       |
|                                                                                             |
|   +---------------+     +---------------+     +---------------+     +---------------+       |
|   |    BÀN 05     |     |    BÀN 06     |     |    BÀN 07     |     |    BÀN 08     |       |
|   |  (115x85px)   |     |  (115x85px)   |     |  (115x85px)   |     |  (115x85px)   |       |
|   +---------------+     +---------------+     +---------------+     +---------------+       |
|                                                                                             |
|   +---------------+     +---------------+     +-------------------------------------+       |
|   |    BÀN 09     |     |    BÀN 10     |     |         BÀN DÀI 11 • SOFA           |       |
|   |  (115x85px)   |     |  (115x85px)   |     |             (210x95px)              |       |
|   +---------------+     +---------------+     +-------------------------------------+       |
|                                                                                             |
|   +-------------------------------------+     [ 🌿 CHẬU CÂY ]                               |
|   |       BÀN DÀI 12 • SẢNH KÍNH        |                                                   |
|   |             (210x95px)              |     [ 🪟 HÀNH LANG KÍNH NHÌN RA PHỐ (Trải đáy) ]  |
|   +-------------------------------------+                                                   |
+---------------------------------------------------------------------------------------------+
```

- **Tính toán Tọa độ Trục X:**
  $$\text{colWidth} = \frac{\text{canvasWidth} - 2 \times \text{marginSide}}{4}$$
  $$\text{col}_i = \text{marginSide} + i \times \text{colWidth} + \frac{\text{colWidth} - \text{tableWidth}}{2}$$
- **Tính toán Tọa độ Trục Y:**
  $$\text{rowHeight} = \frac{\text{canvasHeight} - \text{headerSpace} - \text{footerSpace}}{4}$$
  $$\text{row}_j = \text{headerSpace} + j \times \text{rowHeight} + \frac{\text{rowHeight} - \text{tableHeight}}{2}$$
- Nhờ công thức này, khi kích thước màn hình thay đổi (thu nhỏ cửa sổ hoặc phóng to Full HD), toàn bộ 12 bàn, quầy thu ngân, lối vào và hành lang kính sẽ **tự động dàn đều 100% diện tích**, không để thừa khoảng trống rỗng ở bất kỳ mép nào.

---

## 7. LỘ TRÌNH TRIỂN KHAI & TIÊU CHUẨN NGHIỆM THU (DEFINITION OF DONE)

### 7.1. Bảng Đối Soát Trạng Thái Nghiệm Thu

```
+------------------------------------+---------------------------------------+---------------------+
| Hạng mục Kiểm tra                  | Hành vi Kỳ vọng Đạt chuẩn             | Trạng thái Thiết kế |
+------------------------------------+---------------------------------------+---------------------+
| 1. Nút "Định lượng →"              | Mở Modal chi tiết BOM độc lập, hiển   | Đã hoàn thiện thiết |
|    (Tab 5 Admin)                   | thị đủ nguyên liệu, COGS & Margin.    | kế kiến trúc.       |
+------------------------------------+---------------------------------------+---------------------+
| 2. Luồng Kick Socket               | Client nhận SESSION_KICKED, khóa nút, | Đã hoàn thiện quy   |
|    (Tab 11 Telemetry)              | hiện thông báo đỏ & văng về LoginView.| trình Protocol.     |
+------------------------------------+---------------------------------------+---------------------+
| 3. Triệt tiêu `...` Staff          | TopBar rút gọn tầng & bộ lọc; Card hồ | Đã hoàn thiện giải  |
|    (FloorPlanView & Sidebar)       | sơ xếp 2 tầng hiển thị trọn vẹn tên.  | pháp layout.        |
+------------------------------------+---------------------------------------+---------------------+
| 4. Thẻ Hồ sơ Nhân sự               | Bấm thẻ mở Modal ca trực, đối soát két| Đã hoàn thiện toàn  |
|    (Business Logic)                | tiền mặt, đổi mã PIN & in Z-Report.   | bộ logic nghiệp vụ. |
+------------------------------------+---------------------------------------+---------------------+
| 5. Lấp đầy Sơ đồ Bàn               | Mặt bàn to rõ (+55%), chia lưới động  | Đã hoàn thiện công  |
|    (Floor Canvas Layout)           | theo canvas, triệt tiêu khoảng trắng. | thức toán tỷ lệ.    |
+------------------------------------+---------------------------------------+---------------------+
```

### 7.2. Hướng Dẫn Dành Cho Antigravity CLI / Developer

1. Khi được cấp lệnh triển khai code thực tế (Implementation Phase), đọc tài liệu này theo từng chương để thực hiện tuần tự:
   - File can thiệp Vấn đề 1: `src/cafe/views/AdminSubViews.java`
   - File can thiệp Vấn đề 2: `src/cafe/models/MessageType.java`, `src/cafe/models/MessageProtocol.java`, `src/cafe/services/CafeServerService.java`, `src/cafe/services/ClientSocketService.java`, `src/cafe/controllers/ClientController.java`
   - File can thiệp Vấn đề 3: `src/cafe/views/FloorPlanView.java`
   - File can thiệp Vấn đề 4: `src/cafe/views/FloorPlanView.java` (thêm `StaffProfileShiftDialog`)
   - File can thiệp Vấn đề 5: `src/cafe/views/TableWithChairsNode.java` và `src/cafe/views/FloorPlanView.java`
2. Giữ nguyên toàn bộ cấu trúc dự án, mã nguồn hiện tại không bị ảnh hưởng ngoài 5 phân vùng trên.
3. Luôn đảm bảo tương thích đa nền tảng và duy trì tính ổn định của giao tiếp mạng TCP Socket.
