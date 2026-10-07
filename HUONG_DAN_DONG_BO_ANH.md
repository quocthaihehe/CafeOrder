# QUY CHUẨN ĐỒNG BỘ VÀ QUẢN LÝ HÌNH ẢNH MÓN ĂN (DÀNH CHO NHÓM 3 NGƯỜI)

Tài liệu này hướng dẫn cách 3 thành viên trong nhóm cùng thêm món và hình ảnh song song mà **không bao giờ bị xung đột (conflict) Git**, **không làm nặng repository**, và **không bị lỗi hiển thị**.

---

## 1. Cấu trúc thư mục hình ảnh
Toàn bộ ảnh được lưu trực tiếp trong mã nguồn Java để NetBeans tự động đóng gói:
```text
CafeOrder/
└── src/
    └── cafe/
        └── resources/
            └── images/
                ├── placeholders/          <-- Ảnh mặc định dự phòng
                │   ├── default_drink.png   (Dành cho đồ uống)
                │   └── default_food.png    (Dành cho bánh & món ăn)
                └── products/              <-- Chứa toàn bộ ảnh của 67+ món
                    ├── ca-phe_ca-phe-den-phin.png
                    ├── ca-phe_ca-phe-sua-da.png
                    ├── tra-trai-cay_tra-dao-cam-sa.png
                    └── ...
```

---

## 2. Quy tắc đặt tên file ảnh (BẮT BUỘC)
Để tránh trường hợp 2 người cùng tạo món mới và đặt trùng số ID (ví dụ cùng đặt `68.jpg`), **tuyệt đối không đặt tên ảnh theo số ID tự tăng**.

* **Cú pháp chuẩn**: `[danh-muc-slug]_[ten-mon-slug].png` (hoặc `.jpg`)
* **Cách viết slug**: Chữ thường, không dấu, các từ nối nhau bằng dấu gạch ngang `-`.
* **Ví dụ thực tế**:
  - Món "Cà phê Muối Cố Đô" thuộc danh mục "Cà phê" -> `ca-phe_ca-phe-muoi-co-do.png`
  - Món "Trà Sen Vàng Kem Cheese" thuộc danh mục "Trà & Trái cây" -> `tra-trai-cay_tra-sen-vang-kem-cheese.png`
  - Món "Bánh Mì Que Pate" thuộc danh mục "Món ăn nhẹ" -> `mon-an-nhe_banh-mi-que-pate.png`

> 💡 **Mẹo**: Bạn có thể gọi `ImageManager.toSlug("Tên món")` hoặc dùng thuộc tính `item.getImageFileName()` trong code để lấy chính xác tên file chuẩn.

---

## 3. Quy chuẩn nén ảnh trước khi Commit lên Git
Để tránh làm Git repo bị nặng (Git Bloat), mỗi khi chụp hoặc tải ảnh mới về:
1. **Cắt ảnh tỉ lệ 1:1 (Vuông)**: Kích thước khuyến nghị là **400 x 400 px** hoặc **500 x 500 px**.
2. **Nén giảm dung lượng**:
   - Truy cập trang web miễn phí: [TinyPNG](https://tinypng.com/) hoặc [Squoosh](https://squoosh.app/).
   - Dung lượng mỗi ảnh phải **< 60 KB** (tránh commit file 3MB-5MB từ điện thoại).
3. Copy ảnh đã nén vào thư mục `src/cafe/resources/images/products/`.

---

## 4. Công cụ tự động sinh ảnh mẫu (AssetGenerator)
Nếu một thành viên vừa thêm món mới vào Database nhưng **chưa kịp chụp ảnh thật**, hãy chạy công cụ tự động sinh ảnh có sẵn trong dự án:
```powershell
# Chạy class sinh ảnh mẫu (tự động tạo ảnh 400x400 theo màu sắc danh mục)
java -cp "build/classes;lib/*" cafe.utils.AssetGenerator
```
File ảnh mẫu sẽ được tự động tạo ngay trong `src/cafe/resources/images/products/` với thiết kế nhận diện thương hiệu L'Amour Artisan Cafe.

---

## 5. Cơ chế Fallback an toàn (Chống Crash ứng dụng)
Lớp [`ImageManager.java`](src/cafe/utils/ImageManager.java) đã được lập trình với cơ chế bảo vệ 3 lớp:
1. **Lớp 1**: Tìm ảnh theo Slug chuẩn trong `resources/images/products/`.
2. **Lớp 2**: Tìm ảnh theo ID hoặc đường dẫn cục bộ ngoài ổ đĩa.
3. **Lớp 3 (Fallback)**: Nếu hoàn toàn không tìm thấy ảnh, hệ thống tự động gán `default_drink.png` hoặc `default_food.png`.
➡️ **Cam kết**: Bất kỳ thành viên nào pull code về mà thiếu ảnh, ứng dụng vẫn chạy mượt mà 60 FPS, không bao giờ bị lỗi đen màn hình hay NullPointerException.
