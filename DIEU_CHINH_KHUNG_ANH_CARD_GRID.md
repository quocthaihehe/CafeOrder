# KẾ HOẠCH TỐI ƯU HÓA HÌNH ẢNH MÓN TRÊN LƯỚI GRID (GIỮ NGUYÊN BỘ ẢNH)
**Hệ thống:** CafeOrder (Brew & Bean CoffeePOS)  
**Quyết định thiết kế:** Giữ nguyên 100% bộ ảnh thực tế hiện tại, điều chỉnh thông số bố cục giao diện để hiển thị trọn vẹn, đều đặn và cân đối.

---

## 1. NGUYÊN NHÂN LỖI TRƯỚC ĐÓ
1. **Khung ảnh bị bẹp ngang (125px):** Chiều cao khung ảnh cũ trên [`ItemCard.java`](file:///c:/Users/ACER/Documents/NetBeansProjects/CafeOrder/src/cafe/views/ItemCard.java) chỉ có **`125px`**, trong khi chiều ngang thẻ lên tới **`215px – 225px`** (Tỷ lệ bẹt 16:9).
2. **Ảnh nguồn là ảnh dọc (3:4 hoặc 2:3):** Các ly cà phê cao (*Sữa đá, Bạc xỉu, Muối, Cold brew*) chiếm gần hết chiều cao của ảnh. Khi bị ép vào khung 125px, hệ thống bắt buộc phải phóng to chiều ngang, khiến hơn một nửa chiều cao bức ảnh bị xén mất (cụt cả miệng ly và đáy ly).

---

## 2. GIẢI PHÁP ĐÃ THỰC THI TRỰC TIẾP VÀO CODE

Để giữ nguyên bộ ảnh hiện tại mà mọi ly/tách đều hiển thị **đầy đủ, không bị cắt ngọn, không bị mất đáy**, 3 điều chỉnh sau đã được áp dụng đồng bộ:

### 1. Nâng chiều cao khung ảnh trên thẻ lên 175px ([`ItemCard.java`](file:///c:/Users/ACER/Documents/NetBeansProjects/CafeOrder/src/cafe/views/ItemCard.java)):
* Chiều cao `imageWrapper` và `ProductImagePane` tăng từ `125px` $\rightarrow$ **`175px`** (bo góc 8px).
* Tỷ lệ khung chuyển từ "chữ nhật ngang bẹp" sang dạng **gần vuông (1.1 : 1)**, giúp diện tích chứa ảnh tăng thêm 40%, ôm trọn chiều cao của ly nước.

### 2. Thuật toán neo tỷ lệ vàng 0.35 ([`ProductImagePane.java`](file:///c:/Users/ACER/Documents/NetBeansProjects/CafeOrder/src/cafe/views/ProductImagePane.java)):
* Thay vì cắt đều 50% đỉnh và 50% đáy (`0.5`), hoặc dịch quá cao lên đỉnh (`0.25`), hệ thống áp dụng tỷ lệ vàng **`0.35`**:
  ```java
  double x = (w - targetW) / 2.0;
  double y = (targetH > h) ? (h - targetH) * 0.35 : (h - targetH) / 2.0;
  imageView.relocate(x, y);
  ```
* **Hiệu quả:**
  * Giữ nguyên vẹn 100% phần đỉnh: Miệng ly, đá, lát cam, bọt sữa, latte art.
  * Giữ nguyên vẹn 100% phần đáy: Đĩa lót ly, chân đế ly sứ/thủy tinh.
  * Chỉ xén bớt phần mặt bàn phụ ở dưới cùng.

### 3. Tối ưu kích thước chia cột Grid ([`ClientView.java`](file:///c:/Users/ACER/Documents/NetBeansProjects/CafeOrder/src/cafe/views/ClientView.java)):
* Mẫu số tính cột đổi từ `215` $\rightarrow$ **`195`**:
  ```java
  private int calculateOptimalColumns(double availableWidth) {
      if (availableWidth <= 0) return 3;
      int cols = (int) ((availableWidth - 32) / 195);
      return Math.max(3, Math.min(cols, 6));
  }
  ```
* Khi mở toàn màn hình, lưới tự động chia **5 đến 6 cột**. Chiều rộng mỗi thẻ đạt khoảng `185px – 195px`.
* Khung ảnh đạt kích thước lý tưởng `~190px × 175px` (chuẩn tỷ lệ Kiosk POS hiện đại), giúp các thẻ món thẳng hàng tăm tắp.

---

## 3. KẾT QUẢ ĐẠT ĐƯỢC
* **Toàn bộ 15 món cà phê:** Hiển thị rõ nét, thấy trọn vẹn cả ly nước và đĩa lót.
* **Không làm vỡ layout:** Giao diện chi tiết món ([`ItemDetailDialog.java`](file:///c:/Users/ACER/Documents/NetBeansProjects/CafeOrder/src/cafe/views/ItemDetailDialog.java)) vẫn giữ nguyên kích thước lớn 840×560px sang trọng.
