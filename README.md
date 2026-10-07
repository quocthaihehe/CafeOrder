# ☕ CafeOrder - Hệ Thống Quản Lý Đặt Món Quán Cafe

Dự án phần mềm quản lý quán Cafe mô hình **Client - Server** sử dụng **TCP Socket** thuần và giao diện đồ họa **Java Swing (FlatLaf)**. Dự án được thiết kế theo chuẩn Kiosk (tại bàn) và KDS (Kitchen Display System - màn hình bếp/pha chế).

---

## 🌟 Tính Năng Nổi Bật
*   **Giao thức Mạng (Network):** Giao tiếp hai chiều Real-time qua TCP/IP.
*   **Gói tin chuẩn JSON:** Sử dụng thư viện `Gson` để đóng gói và bóc tách dữ liệu (`HelloMessage`, `NewOrderMessage`, `OrderStatusUpdateMessage`).
*   **Giao diện Tương thích (Responsive):** Ứng dụng công nghệ `ScrollableTracksViewport` kết hợp `GridLayout` ép các thẻ món ăn (Cards) tự động co giãn vừa khít màn hình khi phóng to.
*   **Tính tiền Real-time:** Giá tiền giỏ hàng cập nhật trực tiếp từng giây mà không bị đơ giao diện.
*   **Theme Hiện Đại:** Sử dụng giao diện [FlatLaf](https://www.formdev.com/flatlaf/) mang lại trải nghiệm Flat Design mượt mà.

---

## 🏗️ Cấu Trúc Dự Án Hiện Tại (Giai đoạn 1 hoàn tất)

```text
CafeOrder/
├── src/
│   ├── cafe.models/          # Các thực thể dữ liệu (Order, OrderItem, MenuItem) và Giao thức JSON (MessageProtocol)
│   ├── cafe.utils/           # Tiện ích bổ trợ (ConsoleHelper...)
│   ├── cafe.client/          # Tầng Client: Quản lý Network (NetworkManager), Load UI (UIHelper)
│   │   └── ui/               # Chứa LoginUI, MainClientUI, ItemDetailUI
│   └── cafe.server/          # Tầng Server: Socket Server Core (CafeServer), Phân luồng (ClientHandler)
│       └── ui/               # Chứa MainServerUI (KDS System)
```

---

## 🚀 Hướng Dẫn Chạy Dự Án

### Yêu cầu hệ thống:
*   JDK 17 trở lên (Hỗ trợ tốt nhất trên JDK 21+).
*   Đã thêm thư viện `flatlaf-3.4.1.jar` và `gson-2.11.0.jar` vào mục Libraries.
*   Đã cấu hình JVM Arguments: `-Dfile.encoding=UTF-8 --enable-native-access=ALL-UNNAMED`.

### Các bước khởi động:
1. **Khởi động Server:** Mở file `src/cafe/server/ui/MainServerUI.java` và chạy (`Shift + F6`). Cửa sổ Quầy Pha Chế sẽ hiện lên và Server Socket tự động mở cổng 5000.
2. **Khởi động Client:** Mở file `src/cafe/client/ui/LoginUI.java` và chạy (`Shift + F6`). 
3. **Thao tác:** Nhập số bàn (VD: 1) -> Chọn Món -> Chọn số lượng & ghi chú -> Bấm Gửi Order. Đơn sẽ nhảy lập tức sang màn hình ServerUI.

---

## 🛣️ Lộ Trình Phát Triển (Roadmap Giai đoạn 2)
*   **[BƯỚC 0]** Đồng bộ dữ liệu tĩnh (Menu & Ảnh) bằng file `menu.json` qua Git.
*   **[BƯỚC 1]** Refactor: Dọn dẹp nợ kỹ thuật, xử lý triệt để Exception rớt mạng.
*   **[BƯỚC 2]** Gom State Management vào `OrderService`.
*   **[BƯỚC 3]** Nâng cấp kiến trúc Event-Driven (Dùng PropertyChangeListener thay vì vòng lặp Timer).
*   **[BƯỚC 4]** Lưu trữ lịch sử bán hàng ra file JSON (`orders_history.json`) hoặc SQLite.
