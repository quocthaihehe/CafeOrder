# AGENT TASK: TÁI TẠO COMPONENT SƠ ĐỒ BÀN 100% PIXEL-PERFECT THEO MẪU

## MỤC TIÊU
Loại bỏ hoàn toàn giao diện phẳng lỗi thời ở sơ đồ bàn (`TableMap` / `FloorPlan`). Chuyển đổi thành kiến trúc Composite Table Component nhiều lớp với bóng đổ sâu 3D (Neumorphism / Soft UI) đúng chuẩn bản vẽ gốc.

## HÀNH ĐỘNG CẦN THỰC THI

1. **Thay thế Icon Decor Cây Xanh**:
   - Gỡ bỏ hoàn toàn các Circle vẽ lá cây phẳng 2D đang dán ở các góc phòng.
   - Bổ sung file ảnh render 3D chậu cây (`plant_3d.png` - dạng transparent PNG nhìn từ trên xuống) vào `resources/images/decor/`.
   - Sử dụng `ImageView` nạp ảnh cây này với bóng đổ nhẹ `dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 2, 4)`.

2. **Tái cấu trúc 1 Bàn thành Custom Control (Composite StackPane)**:
   - Tạo cấu trúc gồm 2 tầng:
     - **Tầng nền (Chairs Layer)**: Chứa các khối ghế dạng hình viên thuốc (`-fx-background-radius: 999px; -fx-background-color: #DDD4C8;`). Ghế phải được lùi ra sau mặt bàn một khoảng hợp lý, ôm tự nhiên vào 4 cạnh của bàn vuông hoặc 6 vị trí của bàn dài.
     - **Tầng trên (Table Surface)**: Mặt bàn nằm chính giữa, bo góc tròn (`12px - 14px`), áp dụng hiệu ứng bóng đổ kép:
       - Bàn trống: Nền trắng `#FFFFFF`, bóng mềm `rgba(0,0,0,0.08)`, chữ số đậm màu `#2C2623`.
       - Bàn đang chọn / phục vụ: Nền xanh đen `#172E27`, bóng xanh sâu `rgba(23,46,39,0.35)`, chữ trắng tinh, có chấm cam nhỏ `#E2943B` ở góc trên bên trái.
   - Thêm hiệu ứng hover tương tác: Khi rê chuột vào bàn, mặt bàn hơi nhấc lên bằng cách tăng nhẹ độ lan của bóng (`DropShadow`).

3. **Responsive Grid & Spacing**:
   - Đặt sơ đồ các bàn trong một container sàn có màu `#EDE5DC` bo góc `20px`.
   - Cân đối lại khoảng cách (gap) giữa các cụm bàn để không bị dính sát vào nhau cũng không bị giãn cách quá thưa khi cửa sổ mở rộng.