# PLAN LOGIC FINAL — ĐẶC TẢ TÁI CẤU TRÚC LUỒNG CLIENT (3 ROLE) & SERVER (QUẦY PHA CHẾ KDS)

**Dự án:** Hệ thống Quản lý & Đặt món Quán Cafe (`Brew & Bean — CafeOrder`)  
**Tài liệu:** `Plan Logic Final.md` (Bản chốt tập trung 100% vào Luồng Kiến trúc, Cấu hình Kết nối Đa Máy & Đồng bộ Socket)  
**Đối tượng đọc & thực thi:** `agy cli`  
**Ràng buộc nghiêm ngặt:**
1. **Không chạy code ở bước lập kế hoạch này.**
2. **TUYỆT ĐỐI KHÔNG dùng icon mặc định ("icon lỏ") của Windows / NetBeans / JavaFX (`Alert` mặc định, unicode emoji thô như `⚙️`, `🔄`, `🚫`, `📡`).** Bắt buộc sử dụng bộ icon `.png` đã được thêm sẵn trong `src/cafe/resources/images/icons/{admin,staff,client}/` và `src/cafe/resources/images/placeholders/` thông qua `IconHelper.getIcon(...)` (hoặc `SvgIcons` vector chuẩn).

---

## 1. TRỌNG TÂM YÊU CẦU & MỤC TIÊU THAY ĐỔI (CORE FLOW OBJECTIVES)

### 1.1. Tách bạch Luồng Chạy: `ServerMain` (Quầy Pha Chế KDS) & `ClientMain` (3 Role)
1. **`ServerMain.java` CHỈ chạy Quầy Pha Chế (`ServerKDSView`) & Máy chủ Socket trung tâm:**
   - Gỡ bỏ `AdminShellView` khỏi `ServerMain.java` để chấm dứt tình trạng trùng lặp giao diện Admin giữa Server và Client.
   - Đưa **`ServerKDSView` (Màn hình Điều phối Pha chế - KDS)** lên làm giao diện chính toàn màn hình của `ServerMain.java`.
   - Trang bị trên thanh Header của `ServerKDSView`:
     - **Chỉ báo trạng thái mạng & IP LAN thực tế:** Hiển thị chấm tròn `Circle` màu xanh `#16A34A` kèm `IP LAN: <IP_Máy_B> • Port: 8888 • Đang kết nối: X phiên`.
     - **Nút Đổi Cổng Mạng ngay tại Server:** Dùng icon `ha-tang-mang.png` (hoặc `cai-dat.png` trong `icons/admin/`), mở hộp thoại Custom Modal đổi Port trực tiếp trên `ServerMain` mà không cần vào Admin.
     - **Nút Báo Tạm Hết Món (86) ngay tại Quầy Bar:** Dùng icon `thuc-don-gia.png` trong `icons/admin/`, mở hộp thoại Custom Modal cho phép Pha chế bật/tắt trạng thái món còn/hết và phát sóng `STOCK_STATUS_BROADCAST` tức thời tới mọi Client.
     - **Bộ lọc trạm pha chế:** `[ Tất cả ]`, `[ Cà phê / Espresso ]`, `[ Trà & Đá xay ]`, `[ Bánh ngọt ]` dùng icon chuẩn từ thư mục `icons/`.
     - **Hộp thoại xác nhận đóng Server:** Thay thế `Alert` mặc định của JavaFX bằng Custom Modal bo góc chuẩn thương hiệu (dùng icon `man-pha-che.png` / `coffee-beans.png`) khi bấm nút thoát.

2. **`ClientMain.java` đảm nhận trọn vẹn 3 Role (`Khách hàng` — `Nhân viên` — `Admin`) giao tiếp 100% qua Socket với `ServerMain`:**
   - Khi mở `ServerMain` (Máy B / Tiến trình 1) và mở `ClientMain` (Máy A / Tiến trình 2), mọi thao tác của cả 3 Role trên `ClientMain` đều đi qua đường truyền TCP Socket (`ClientSocketService` $\leftrightarrow$ `CafeServerService`) tới `ServerMain`:
     - **Role 1 — Khách hàng (`ClientView`):** Chọn bàn $\rightarrow$ Đặt món qua Socket $\rightarrow$ Nhận trạng thái pha chế & món tạm hết (86) thời gian thực.
     - **Role 2 — Nhân viên (`FloorPlanView`):** Đăng nhập tài khoản Staff (`cashier1`, `waiter`) $\rightarrow$ Nhận đồng bộ trạng thái 36 bàn & danh sách đơn hàng từ `ServerMain` qua Socket $\rightarrow$ Đặt món, Chuyển/Gộp bàn, Thanh toán, Chốt ca Z-Report.
     - **Role 3 — Admin (`AdminShellView`):** Đăng nhập tài khoản Quản lý (`admin`, `manager`) trên `ClientMain` $\rightarrow$ Điều khiển toàn diện hệ thống và đặc biệt là **Tab 11 (Hạ tầng & Mạng)**: Xem danh sách phiên kết nối thật của `ServerMain`, bấm **Ping đo RTT**, **Kick thiết bị**, **Duyệt/Từ chối yêu cầu xin cấp lại phiên** hoàn toàn qua gói tin Socket.

---

### 1.2. Cơ Chế Đổi IP & Cổng Mạng Khi Test Trên 1 Máy Hoặc 2 Máy (LAN / Wi-Fi)
Để giải quyết triệt để bài toán **Máy A chạy `ClientMain` kết nối sang Máy B chạy `ServerMain`** mà không cần phải đăng nhập vào Admin mới đổi được cổng:

1. **Tại Màn hình Đăng nhập `LoginView.java` (Trên Máy A — Client):**
   - Ngay trên thanh trạng thái máy chủ (`serverStatusBadge`), bên cạnh nút `[ Thử lại ]` (dùng icon `lam-moi.png`), bổ sung nút **`[ Cấu hình mạng ]`** sử dụng icon `ha-tang-mang.png` (hoặc `cai-dat.png` từ `src/cafe/resources/images/icons/admin/`).
   - Khi bấm vào nút **`[ Cấu hình mạng ]`** ngay tại màn hình Login, hiển thị một **Custom Modal Cấu Hình Kết Nối Máy Chủ** (tuyệt đối không dùng dialog mặc định của Windows/NetBeans):
     - Ô nhập **Địa chỉ IP Máy chủ (Server Host):** Mặc định `127.0.0.1` (nếu chạy chung 1 máy) hoặc nhập IP LAN của Máy B (ví dụ: `192.168.1.15`). Có nút chọn nhanh `[ Localhost 127.0.0.1 ]`.
     - Ô nhập **Cổng kết nối (TCP Port):** Mặc định `8888` (đồng bộ với cổng trên Máy B).
     - Nút **`[ Lưu & Kết nối ngay ]`**: Lưu vào `NetworkConfig.saveConfig(host, port)`, tự động ngắt socket cũ và gọi `socketService.tryConnectToServer()` tới IP:Port mới, cập nhật đèn xanh và độ trễ `RTT` ngay lập tức trên `LoginView`.

2. **Tại Màn hình Quầy Pha Chế `ServerKDSView.java` (Trên Máy B — Server):**
   - Tự động phát hiện và hiển thị **Địa chỉ IPv4 LAN thật của Máy B** (ví dụ `192.168.1.15`) cùng với `Port` hiện tại ngay trên thanh Header để người test nhìn vào Máy B là biết ngay cần nhập IP/Port nào trên Máy A.
   - Có nút **`[ Đổi Cổng ]`** (dùng icon `ha-tang-mang.png`) ngay trên Header `ServerKDSView`: Khi đổi sang cổng mới (ví dụ `9000`), `CafeServerService.rebindPort(9000)` mở ngay cổng mới trên Máy B.

---

### 1.3. Quy Chuẩn Bắt Buộc Về Bộ Icon (Zero Default OS/Emoji Icons)
Mọi nút bấm và chỉ báo mới thêm trong kế hoạch này phải tuân thủ:
- **Sử dụng `cafe.utils.IconHelper.getIcon(folder, fileName, svgFallback, textFallback, size, color)`** nạp từ các thư mục icon có sẵn:
  - `src/cafe/resources/images/icons/admin/ha-tang-mang.png` $\rightarrow$ Dùng cho nút Cấu hình IP/Port, Đổi cổng mạng, Chỉ báo Hạ tầng Socket.
  - `src/cafe/resources/images/icons/admin/cai-dat.png` $\rightarrow$ Dùng cho nút Cài đặt / Tùy chỉnh thông số.
  - `src/cafe/resources/images/icons/admin/thuc-don-gia.png` $\rightarrow$ Dùng cho nút Báo Tạm Hết Món (86) và quản lý món.
  - `src/cafe/resources/images/icons/admin/man-pha-che.png` $\rightarrow$ Dùng cho biểu tượng Quầy Pha Chế KDS & bộ lọc trạm pha chế.
  - `src/cafe/resources/images/icons/admin/don-hang.png` $\rightarrow$ Dùng cho thẻ đơn hàng & bộ lọc trạng thái đơn.
  - `src/cafe/resources/images/icons/staff/lam-moi.png` $\rightarrow$ Dùng cho nút Làm mới / Thử lại kết nối.
  - `src/cafe/resources/images/placeholders/coffee-beans.png` $\rightarrow$ Dùng cho thương hiệu Header / Modal xác nhận.
- **Đèn trạng thái (Online / Offline / Latency):** Dùng `javafx.scene.shape.Circle` (bán kính `4.5px - 5px`) tô màu chuẩn (`#16A34A` xanh lá, `#D97706` vàng cam, `#DC2626` đỏ) thay vì ký tự emoji `🟢`, `🔴`.

---

## 2. SƠ ĐỒ LUỒNG KIẾN TRÚC & KẾT NỐI ĐA MÁY (ARCHITECTURE FLOW)

```
+=========================================================================================+
|                  MÁY B: TRẠM MÁY CHỦ — src/cafe/ServerMain.java                         |
|               [ QUẦY PHA CHẾ TRUNG TÂM (ServerKDSView) & SOCKET ENGINE ]                |
+=========================================================================================+
|  - Header hiển thị rõ: [● Online | IP LAN: 192.168.1.x | Port: 8888 | X phiên]          |
|  - Nút Header (dùng icon PNG chuẩn):                                                    |
|    + [ha-tang-mang.png  Đổi Cổng] -> Mở Custom Modal đổi Port lắng nghe tại chỗ.        |
|    + [thuc-don-gia.png  Báo Tạm Hết Món (86)] -> Khóa/mở món & Broadcast toàn mạng.     |
|  - Nhận đơn từ mọi máy Client -> Pha chế bấm [Bắt đầu pha] / [Hoàn tất] -> Broadcast.   |
+=========================================================================================+
                                             ▲
                                             │  TCP Socket qua LAN / Localhost (IP:Port)
                                             ▼
+=========================================================================================+
|                  MÁY A: ỨNG DỤNG ĐẦU CUỐI — src/cafe/ClientMain.java                    |
|   [ LoginView có nút (ha-tang-mang.png Cấu hình IP/Port) -> PHÂN LUỒNG 3 ROLE ]         |
+=========================================================================================+
         │
         ├──► ROLE 1: KHÁCH HÀNG (ClientView — Nhập Số bàn 1..12)
         │     - Đặt món qua Socket -> Nhận trạng thái pha chế & món hết (86) thời gian thực.
         │
         ├──► ROLE 2: NHÂN VIÊN (FloorPlanView — Tài khoản: cashier1, waiter)
         │     - Đồng bộ trạng thái 36 bàn & danh sách đơn hàng từ ServerMain qua Socket.
         │     - Đặt món, Chuyển/Gộp bàn, Thanh toán, Chốt ca Z-Report.
         │
         └──► ROLE 3: ADMIN / QUẢN LÝ (AdminShellView — Tài khoản: admin, manager)
               - Điều khiển ServerMain từ xa 100% qua Socket:
                 + Tab 1, 3, 4, 9: Đồng bộ danh sách đơn hàng & KDS live từ ServerMain.
                 + Tab 5 & Tab 8: Bật/tắt món 86, sửa giá, khóa nhân sự -> Broadcast qua Server.
                 + Tab 11 (Hạ tầng & Mạng): Xem danh sách phiên thật trên ServerMain,
                   bấm Ping đo RTT, Kick phiên, Duyệt/Từ chối yêu cầu xin cấp lại phiên.
```

---

## 3. ĐẶC TẢ GÓI TIN SOCKET CẦN BỔ SUNG (`MessageType` & `MessageProtocol`)

Bổ sung vào `src/cafe/models/MessageType.java` và `src/cafe/models/MessageProtocol.java` 5 loại gói tin để đồng bộ giữa `ClientMain` và `ServerMain`:

1. **`ADMIN_GET_TELEMETRY` (`Client -> Server`):**
   - Yêu cầu `ServerMain` gửi toàn bộ thông tin phiên kết nối, lưu lượng mạng và danh sách yêu cầu xin cấp lại phiên đang chờ duyệt.
2. **`ADMIN_TELEMETRY_SNAPSHOT` (`Server -> Client`):**
   - Chứa `List<SessionSnapshotDto> sessions`, `List<ReconnectRequestDto> pendingRequests`, `int activePort`, `String lanIp`, `long avgRttMs`, `long totalBytes`.
3. **`ADMIN_COMMAND` (`Client -> Server`):**
   - Gửi lệnh điều khiển từ Admin trên `ClientMain` xuống `ServerMain`:
     - `action = "PING_SINGLE"` (`targetId = sessionId`)
     - `action = "PING_ALL"`
     - `action = "KICK_SESSION"` (`targetId = sessionId`, `param1 = reason`)
     - `action = "APPROVE_RECONNECT"` (`targetId = deviceId`)
     - `action = "REJECT_RECONNECT"` (`targetId = deviceId`, `param1 = reason`)
     - `action = "TOGGLE_STOCK_86"` (`targetId = productName`, `flag = outOfStock`)
     - `action = "REBIND_PORT"` (`targetId = String.valueOf(newPort)`)
4. **`ADMIN_PING_RESULT` (`Server -> Client`):**
   - Trả kết quả đo Ping thực tế từ `ServerMain` về ngay cho màn hình Admin trên `ClientMain` (`sessionId`, `rttMs`, `avgRttMs`, `success`).
5. **`ORDERS_SNAPSHOT_SYNC` (`Server -> Client`):**
   - Đồng bộ toàn bộ danh sách đơn hàng (`orderHistory`) đang có trên `ServerMain` sang `ClientMain` khi Nhân viên hoặc Admin đăng nhập, hoặc khi có đơn mới/cập nhật trạng thái đơn.

---

## 4. HƯỚNG THAY ĐỔI MÃ NGUỒN CHI TIẾT THEO TỪNG FILE (FILE-BY-FILE PLAN CHO AGY CLI)

### Bước 1: Chuyển `ServerMain.java` sang Quầy Pha Chế KDS & Thay Alert Mặc Định
- **File:** `src/cafe/ServerMain.java`
- **Thay đổi:**
  1. Gỡ bỏ việc khởi tạo `AdminShellView` trong `ServerMain.java`.
  2. Khởi tạo `ServerKDSView kdsView = new ServerKDSView();` và `new ServerController(kdsView, serverService);`.
  3. Đặt `kdsView` làm root của `WindowChromeHelper.createWindowScene(...)` với tiêu đề `"Brew & Bean CAFE • Quầy Điều Phối Pha Chế Trung Tâm (Server KDS)"`.
  4. Thay thế `javafx.scene.control.Alert` khi đóng cửa sổ `ServerMain` bằng một **Custom Modal Xác Nhận Đóng Máy Chủ** (`StageStyle.TRANSPARENT`, bo góc `16px`, dùng icon `man-pha-che.png` qua `IconHelper`).

### Bước 2: Nâng cấp `ServerKDSView.java` & `ServerController.java` (IP LAN, Đổi Cổng, Báo Hết Món 86)
- **File:** `src/cafe/views/ServerKDSView.java`, `src/cafe/controllers/ServerController.java`, `src/cafe/utils/NetworkConfig.java`
- **Thay đổi:**
  1. Trong `NetworkConfig.java`: Thêm hàm tiện ích `getLocalLanIp()` lấy địa chỉ IPv4 LAN thực tế của máy đang chạy (ví dụ `192.168.1.x`, bỏ qua loopback/virtual adapter) để hiển thị trên Header của Server.
  2. Trên thanh Header (`buildHeader()`) của `ServerKDSView.java`:
     - Hiển thị huy hiệu trạng thái mạng gồm `Circle(4.5, Color.web("#16A34A"))` + nhãn: `"IP LAN: " + NetworkConfig.getLocalLanIp() + " : " + port + " • " + count + " phiên"`.
     - Thêm nút **`[ Đổi Cổng ]`** dùng icon `IconHelper.getIcon("admin", "ha-tang-mang.png", SvgIcons.SETTINGS, "", 16, Color.WHITE)` mở Custom Modal đổi cổng trực tiếp tại Server.
     - Thêm nút **`[ Báo Tạm Hết Món (86) ]`** dùng icon `IconHelper.getIcon("admin", "thuc-don-gia.png", SvgIcons.MENU, "", 16, Color.WHITE)` mở Custom Modal danh sách món cho phép Barista gạt bật/tắt trạng thái hết hàng và gọi `serverService.broadcastStockStatus(...)`.
  3. Trên thanh công cụ lọc đơn của `ServerKDSView.java`:
     - Bổ sung các nút lọc theo nhóm trạm pha chế (`Tất cả`, `Cà phê / Espresso`, `Trà & Đá xay`, `Bánh ngọt`) và thay thế các ký tự icon rời rạc trên nút bằng icon từ `IconHelper`.

### Bước 3: Thêm Nút Cấu Hình IP / Port Ngay Trên `LoginView.java` & `LoginController.java`
- **File:** `src/cafe/views/LoginView.java`, `src/cafe/controllers/LoginController.java`
- **Thay đổi:**
  1. Trong `LoginView.java`:
     - Tại thanh `serverStatusBadge`, dùng `Circle` làm chấm trạng thái.
     - Cập nhật nút `btnRetryConnection` dùng icon `IconHelper.getIcon("staff", "lam-moi.png", SvgIcons.REFRESH, "", 13, Color.web("#112823"))` (bỏ ký tự unicode `↻`).
     - Thêm nút mới `btnConfigServer` bên cạnh `btnRetryConnection` dùng icon `IconHelper.getIcon("admin", "ha-tang-mang.png", SvgIcons.SETTINGS, "", 14, Color.web("#112823"))` kèm chữ `"Cấu hình"`.
  2. Trong `LoginController.java`:
     - Khi bấm `btnConfigServer`, mở **Custom Modal Cấu Hình Kết Nối Máy Chủ** (bo góc `16px`, dùng icon `ha-tang-mang.png`):
       - Cho phép người dùng nhập **IP Máy chủ (`Host`)** và **Cổng (`Port`)**, kèm nút chọn nhanh `[ Đặt về Localhost 127.0.0.1 ]`.
       - Khi bấm **`[ Lưu & Kết nối ]`**: Gọi `NetworkConfig.saveConfig(host, port)`, ngắt kết nối cũ, kết nối lại tới `host:port` mới và cập nhật ngay trạng thái đèn xanh/đỏ trên `LoginView`.

### Bước 4: Mở rộng Giao thức & Xử lý Đồng Bộ Socket (`MessageType`, `MessageProtocol`, `CafeServerService`, `ClientSocketService`)
- **File:** `src/cafe/models/MessageType.java`, `src/cafe/models/MessageProtocol.java`, `src/cafe/services/CafeServerService.java`, `src/cafe/services/ClientSocketService.java`
- **Thay đổi:**
  1. Khai báo 5 `MessageType` mới (`ADMIN_GET_TELEMETRY`, `ADMIN_TELEMETRY_SNAPSHOT`, `ADMIN_COMMAND`, `ADMIN_PING_RESULT`, `ORDERS_SNAPSHOT_SYNC`) và các DTO JSON trong `MessageProtocol.java`.
  2. Trong `CafeServerService.java`:
     - Xử lý `ADMIN_GET_TELEMETRY` và `ADMIN_COMMAND` (`PING_SINGLE`, `PING_ALL`, `KICK_SESSION`, `APPROVE_RECONNECT`, `REJECT_RECONNECT`, `TOGGLE_STOCK_86`, `REBIND_PORT`), gửi kết quả `ADMIN_TELEMETRY_SNAPSHOT` và `ADMIN_PING_RESULT` về cho Admin Client.
     - Tự động gửi `ORDERS_SNAPSHOT_SYNC` khi Staff/Admin kết nối hoặc khi có thay đổi đơn hàng trên `ServerMain`.
  3. Trong `ClientSocketService.java`:
     - Bổ sung hàm gửi `requestAdminTelemetry()`, `sendAdminCommand(...)` và các callback lắng nghe `ADMIN_TELEMETRY_SNAPSHOT`, `ADMIN_PING_RESULT`, `ORDERS_SNAPSHOT_SYNC`.

### Bước 5: Kết nối `AdminSubViews.java`, `AdminShellView.java` & `FloorPlanView.java` với Dữ Liệu Socket Từ Xa
- **File:** `src/cafe/views/AdminSubViews.java`, `src/cafe/views/AdminShellView.java`, `src/cafe/views/FloorPlanView.java`
- **Thay đổi:**
  1. Trong `AdminSubViews.buildNetworkTelemetryView(...)` (Tab 11):
     - Thay thế các emoji trên nút (`📡`, `⚙️`, `🔄`, `⚡`, `✕`, `✔`) bằng icon nạp từ `IconHelper` (`ha-tang-mang.png`, `cai-dat.png`, `lam-moi.png` và `SvgIcons`).
     - Lấy danh sách phiên kết nối, KPI lưu lượng và danh sách `pendingReconnectRequests` từ snapshot của `ClientSocketService` (do `ServerMain` gửi về).
     - Khi Admin bấm **Ping**, **Ping toàn bộ**, **Kick**, **Phê duyệt**, **Từ chối**, hoặc **Đổi cổng mạng**: Gửi `ADMIN_COMMAND` qua `ClientSocketService` tới `ServerMain` và cập nhật trực tiếp lên giao diện ngay khi nhận phản hồi.
  2. Trong `AdminShellView.java` & `FloorPlanView.java`:
     - Khi nhận `ORDERS_SNAPSHOT_SYNC` hoặc `ORDER_STATUS_UPDATE` từ `ClientSocketService`, cập nhật ngay danh sách đơn hàng, trạng thái bàn và màn hình giám sát.
