# BÁO CÁO THẨM ĐỊNH & KẾ HOẠCH TÁI CẤU TRÚC TOÀN DIỆN UI/UX
### TRIỂN KHAI THEO YÊU CẦU TỪ `cần chỉnh sửa.docx`, ĐỐI CHIẾU `UI_mong_muon.png` & HỆ THỐNG JSON ĐẶC TRƯNG
> **Chuyên gia Thẩm định:** Senior Creative Director & Lead UI/UX Architect (20 năm kinh nghiệm thiết kế Hệ thống POS/F&B Cao cấp)  
> **Thương hiệu:** Brew & Bean CAFE — Modern Editorial POS System  
> **Tài liệu đối chiếu:** `cần chỉnh sửa.docx`, `UI_mong_muon.png`, `json Hình mẫu.txt`, `json quan trọng.txt`  
> **Ngày lập kế hoạch:** 08/10/2026  

---

## ☕ LỜI MỞ ĐẦU: TRIẾT LÝ THIẾT KẾ CỐT LÕI (DESIGN DNA)

Qua việc giải phẫu chi tiết file tài liệu `cần chỉnh sửa.docx`, đối chiếu trực tiếp trên từng tọa độ pixel của bản thiết kế mẫu `UI_mong_muon.png` (kích thước 1312 × 1199 px) và 2 file cấu trúc `json Hình mẫu.txt`, `json quan trọng.txt`, hệ thống được định vị chính xác theo phong cách:

**Japanese Specialty Coffee Shop × Modern Editorial POS**  
*(Tối giản • Ấm áp • Tinh tế • Sang trọng tĩnh lặng — Quiet Luxury)*

### 5 Nguyên Tắc Bất Di Bất Dịch (Critical Design DNA):
1. **Color (Bảng màu tự nhiên):** Nền giấy Washi ấm (`#F5F0E8`), mặt thẻ trắng kem (`#FBF9F5`), xanh thông trầm (`#112823`), nâu cafe mộc (`#563521`), viền hairline (`#DED7CD`).
2. **Typography (Tương phản cao cấp):** Tiêu đề trang trọng dùng font có chân cổ điển (*Playfair Display*), toàn bộ nhãn thao tác, bảng biểu, danh mục dùng font không chân thanh mảnh (*Inter*).
3. **Shape (Quy chuẩn bo góc nghiêm ngặt):** Bo góc tinh tế từ **`4px – 8px`** (Subtle Rectangular Rounding). **Tuyệt đối không dùng bo góc tròn lớn** (như card 16–24px kiểu Bootstrap/Mobile app phổ thông làm vỡ tính editorial).
4. **Imagery (Nhiếp ảnh chân thực):** Ánh sáng tự nhiên xiên góc (warm directional morning light), bề mặt gỗ mộc, cây xanh, chiều sâu quang học mềm mại. Tránh hoàn toàn ảnh hoạt hình 3D hay vector tách nền vô hồn.
5. **Layout (Mật độ thông tin chuẩn POS):** Lưới bố cục 4px base grid, phân cấp thị giác mạch lạc, tỷ lệ 70% công năng rõ ràng + 30% bản sắc nghệ thuật.

---

## BẢNG THÔNG SỐ DESIGN TOKENS CHUẨN HỆ THỐNG

### 1. Bảng Màu Hệ Thống (Color Tokens)
| Tên Token | Mã Hex | RGB | Vai trò & Ứng dụng trong giao diện |
| :--- | :--- | :--- | :--- |
| `color-canvas` | `#F5F0E8` | 245, 240, 232 | Nền chính của toàn bộ cửa sổ ứng dụng (Ấm, giấy Washi). |
| `color-surface` | `#FBF9F5` | 251, 249, 245 | Nền thẻ sản phẩm, panel bên phải, nền card giỏ hàng, form. |
| `color-primary` | `#112823` | 17, 40, 35 | Nút bấm chính (CTA), mục Category đang Active, Bàn đang chọn. |
| `color-primary-hover` | `#183B34` | 24, 59, 52 | Trạng thái di chuột (Hover) của nút chính. |
| `color-primary-pressed` | `#0B1D19` | 11, 29, 25 | Trạng thái nhấn giữ (Pressed) của nút chính. |
| `color-accent-brown` | `#563521` | 86, 53, 33 | Nâu hạt cafe rang mộc, dùng cho nhãn giá tiền, phụ đề thương hiệu. |
| `color-text-primary` | `#292722` | 41, 39, 34 | Màu than đen ấm cho toàn bộ tiêu đề, nhãn chính, tên sản phẩm. |
| `color-text-secondary` | `#817A70` | 129, 122, 112 | Màu xám tro ấm cho mô tả món, placeholder, phụ đề. |
| `color-border` | `#DED7CD` | 222, 215, 205 | Viền mỏng 1px cho inputs, thẻ card, đường phân cách (Hairline). |
| `color-chair` | `#E8DED0` | 232, 222, 208 | Khối ghế xung quanh bàn trên sơ đồ mặt bằng 2.5D. |
| `color-sidebar-active` | `#DED5C8` | 222, 213, 200 | Nền mục Active trên Left Navigation Rail của nhân viên. |
| `color-status-serving` | `#E5A04D` | 229, 160, 77 | Chấm tròn bàn Đang phục vụ / Tag chú giải. |
| `color-status-badge-bg` | `#DCE8D9` | 220, 232, 217 | Nền pill badge "Đang phục vụ" trên header & thẻ đơn hàng. |
| `color-status-badge-fg` | `#597256` | 89, 114, 86 | Màu chữ pill badge "Đang phục vụ". |
| `color-status-reserved` | `#86A88A` | 134, 168, 138 | Chấm tròn bàn Đã đặt trước. |
| `color-status-maint` | `#4B1717` | 75, 23, 23 | Chấm tròn bàn Đang bảo trì / Hủy đơn. |

---

### 2. Quy Chuẩn Typography (Hệ Thống Phông Chữ)
* **Heading / Editorial Font:** `Playfair Display` (Serif cổ điển, thanh lịch).
* **Functional / UI Font:** `Inter` hoặc `Segoe UI` (Sans-serif trung tính, độ đọc tối ưu ở kích thước nhỏ).

| Cấp bậc | Font Family | Size | Weight | Line Height | Ứng dụng |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Hero Title** | Playfair Display | 31px – 32px | Medium (500) | 1.05 | Headline poster "Good Coffee Better Moments" |
| **Page Title** | Playfair Display | 20px – 22px | Semi-Bold (600)| 1.25 | Tiêu đề "Bàn 05", "Latte", "Thanh toán", "Đơn hàng" |
| **Section Title** | Inter | 14px – 15px | Semi-Bold (600)| 1.30 | "Chào mừng trở lại!", "Phương thức thanh toán" |
| **Body / Labels** | Inter | 11px – 12px | Regular / Med  | 1.40 | Nhãn form, danh mục sidebar, mô tả món |
| **Button Text** | Inter | 11px | Medium (500) | 1.00 | Nút "Đăng nhập", "Thêm vào giỏ", "Thanh toán →" |
| **Price Text** | Inter | 11px | Bold (700) | 1.00 | Hiển thị giá tiền `65.000đ` |
| **Small / Badges**| Inter | 9px – 10px | Medium / Bold  | 1.20 | Badge trạng thái, tên món trên card menu, footnote |

---

# PHẦN 1: BÓC TÁCH CHI TIẾT & GIẢI PHÁP THAY ĐỔI CHO 7 TRANG MẪU

```
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│                                   TỔNG QUAN 7 MÀN HÌNH MẪU                                  │
├──────────────────────────────┬──────────────────────────────┬───────────────────────────────┤
│ [1. Trang Login]             │ [2. Sơ đồ bàn Nhân viên]     │ [3. Thực đơn Khách hàng]      │
│ • Poster nghệ thuật bên trái │ • Sidebar đúng 5 mục         │ • Sidebar 7 mục căn thẳng hàng│
│ • Form tài khoản/mật khẩu    │ • Bàn có ghế xung quanh      │ • Grid 3 cột + nút (+) tròn   │
├──────────────────────────────┼──────────────────────────────┼───────────────────────────────┤
│ [4. Chi tiết món ItemDetail] │ [5. Giỏ hàng Cart]           │ [6. Thanh toán Payment]       │
│ • Layout ngang (Ảnh trái)    │ • Danh sách món + ghi chú    │ • 3 phương thức + Tóm tắt tiền│
│ • Size pills + Toppings check│ • Nút [Thêm món] [Thanh toán]│ • Nút [Xác nhận thanh toán]   │
├──────────────────────────────┴──────────────────────────────┴───────────────────────────────┤
│ [7. Quản lý Đơn hàng Orders (Nhân viên)]                                                    │
│ • Header có nút xóa • 3 Tabs lọc trạng thái • Cards đơn hàng hiển thị bàn, tiền, giờ        │
└─────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 1.1 TRANG ĐĂNG NHẬP (`LoginView`) — YÊU CẦU 1.1 TRONG DOCX

### A. Đánh Giá Hiện Trạng & Điểm Cần Chỉnh Sửa
* **Hiện trạng dự án:** Form đăng nhập chỉ có 1 ô nhập số bàn đơn sơ kèm chip chọn nhanh, thiếu hẳn hệ thống đăng nhập tài khoản/mật khẩu theo chuẩn POS cao cấp.
* **Cải tạo cần đạt 100% theo mẫu:**
  * **Bố cục Split-Screen (50/50):**
    * Cột trái: Poster nghệ thuật tràn viền, tông màu ấm sâu, chiều sâu quang học.
    * Cột phải: Form đăng nhập nền `#FBF9F5`, căn giữa theo trục dọc.
  * **Hệ thống form bên phải:**
    1. Icon đỉnh: Biểu tượng 2 hạt cà phê mộc màu xanh đậm `#112823`.
    2. Headline: `Chào mừng trở lại!` (Inter Semi-bold 18px).
    3. Phụ đề: `Đăng nhập để bắt đầu phục vụ khách hàng.` (11.5px, `#817A70`).
    4. Ô `Tài khoản`: Nhãn có icon `👤`, placeholder `Nhập tên đăng nhập`. Chiều cao **32px**, bo góc **5px**, viền `#DED7CD`.
    5. Ô `Mật khẩu`: Nhãn có icon `🔒`, placeholder `Nhập mật khẩu`, đuôi ô có icon con mắt ẩn/hiện mật khẩu.
    6. Nút chính `Đăng nhập`: Nền `#112823`, chữ trắng 11px, cao **32px**, bo góc **5px**.
    7. Phân cách: Dòng chữ `Hoặc đăng nhập bằng` kèm 2 đường kẻ hairline mảnh hai bên.
    8. Nút phân quyền nhanh: 2 nút song song `[ 👤 Nhân viên ]` và `[ 💼 Quản lý ]` nền trắng kem viền `#DED7CD`.
    9. Footer: `Brew & Bean © 2025` (9px).

---

### B. Prompt AI Tạo Ảnh & Cấu Trúc JSON Trích Xuất Đặc Trưng Poster (Yêu cầu 1.1)

#### 1. Bảng Dữ Liệu JSON Trích Xuất Đặc Trưng (Image Feature Extraction JSON):
```json
{
  "poster_feature_extraction": {
    "dimensions": {
      "aspect_ratio": "9:16",
      "recommended_resolution": "1080x1920"
    },
    "scene_composition": {
      "genre": "Editorial lifestyle photography for Japanese artisanal coffee house",
      "main_subject": "Handcrafted coffee service on dark rustic solid oak tabletop",
      "foreground": {
        "item": "Warm stoneware ceramic cappuccino cup with latte art on matching ceramic saucer",
        "position": "Lower right quadrant",
        "texture": "Matte speckled warm grey ceramic"
      },
      "midground": {
        "item": "Tall ribbed iced latte glass with delicate milk gradient and straw",
        "position": "Middle left quadrant",
        "texture": "Chilled glass with subtle condensation droplets"
      },
      "background": {
        "environment": "Minimalist Kyoto cafe interior, textured beige wabi-sabi plaster wall",
        "plants": "Soft out-of-focus indoor ficus foliage in terracotta pot",
        "depth_of_field": "Shallow, creamy bokeh blur"
      }
    },
    "lighting_and_atmosphere": {
      "light_source": "Strong warm morning sunlight entering horizontally from side window",
      "shadows": "Long cinematic soft shadows casting diagonally across wooden surface",
      "color_temperature": "Warm golden hour (approx. 3200K - 3800K)",
      "mood": "Quiet luxury, serene, meditative, inviting, tactile"
    },
    "color_grading": {
      "dominant_palette": ["#2C1B10", "#5A3825", "#8E5E3C", "#C49A6C"],
      "highlight_tones": ["#EAD8C3", "#F5ECE1"],
      "film_simulation": "Kodak Portra 400 with soft highlight roll-off and organic fine grain"
    },
    "typography_overlay_spec": {
      "top_logo": {
        "text": "Brew & Bean CAFE",
        "position": "Top-left, 36px margin",
        "font": "Inter Regular 11px, letter-spacing 2px, white"
      },
      "headline": {
        "lines": ["Good", "Coffee", "Better", "Moments"],
        "position": "Upper-left quadrant (y: 22% - 42%)",
        "font": "Playfair Display 32px, font-weight 500, color #D2A679 (warm caramel-cream)"
      },
      "bottom_slogan": {
        "lines": ["Order tại bàn. Nhanh hơn.", "Tiện hơn. Trọn vẹn hơn."],
        "position": "Bottom-left, 36px margin",
        "font": "Inter Regular 10.5px, line-height 1.4, color #FFFFFF"
      }
    }
  }
}
```

#### 2. Prompt Tạo Ảnh (Dành cho Midjourney v6 / Flux.1 / DALL-E 3):
```text
Editorial lifestyle advertising photography for a luxury Japanese specialty coffee brand. A vertical poster composition (aspect ratio 9:16). A ceramic stoneware cup of hot latte with intricate foam art sits beside a tall elegant fluted glass of iced cafe au lait with condensation drops on a dark rustic solid oak cafe table. Warm natural morning sunlight raking from a side window creating deep moody shadows and warm golden reflections. Subtle indoor green plants in the blurred background. Moody, cozy, quiet luxury, cinematic atmosphere, 85mm lens, f/2.0, Kodak Portra 400 color tones, ultra-realistic texture --ar 9:16 --style raw --v 6.0
```

---

## 1.2 TRANG GIAO DIỆN NHÂN VIÊN — SƠ ĐỒ BÀN (`FloorPlanView`) — YÊU CẦU 1.2

### A. Sidebar Đúng Đủ 5 Mục Chuẩn Bản Mẫu
Thanh Left Rail phải có chiều rộng cố định **105px**, nền `#F7F2EA`, viền phải `#DED7CD`:
1. `[ 🪑 Bàn ]` — **Mục Active:** Nền be đậm `#DED5C8`, chữ & icon màu `#292722`.
2. `[ 📋 Thực đơn ]` — Nền trong suốt, chữ `#6F695F`, hover `#EFE8DD`.
3. `[ 🧾 Đơn hàng ]` — Quản lý toàn bộ phiếu gọi món trong ca.
4. `[ 💳 Thanh toán ]` — Quản lý lịch sử hóa đơn thanh toán.
5. `[ ⚙️ Cài đặt ]` — Cấu hình hệ thống bàn, máy in bill.

---

### B. Giải Pháp Kỹ Thuật: Dựng Bàn Có Ghế Bao Quanh & Cây Trang Trí (100% Giống Mẫu)
Trong ảnh mẫu, các bàn được mô phỏng theo **bản vẽ mặt bằng kiến trúc nội thất (Top-Down Architectural Plan)**:
* Trung tâm là mặt bàn hình chữ nhật bo góc `8px`.
* Bốn phía quanh bàn là **các khối ghế (Chair Blocks)** bo góc `3px`, màu be nhạt `#E8DED0`, có hiệu ứng bóng đổ như mẫu.
* Bàn đang chọn (Bàn 05) đổi sang màu **Deep Green `#112823`**, chữ số màu trắng, góc trên bên trái có **chấm tròn cam `#E5A04D`** biểu thị trạng thái đang phục vụ.

```
            ┌──────────────┐
            │  GHẾ TRÊN    │  (28 × 6 px, bo góc 3px, #E8DED0)
     ┌──────┴──────────────┴──────┐
┌────┤                            ├────┐
│GHẾ │           BÀN 05           │GHẾ │
│TRÁI│     ● Đang phục vụ         │PHẢI│  (Mặt bàn: 52 × 42 px, bo góc 8px)
│    │      (Nền #112823)         │    │
└────┤                            ├────┘
     └──────┬──────────────┬──────┘
            │  GHẾ DƯỚI    │  (28 × 6 px, bo góc 3px, #E8DED0)
            └──────────────┘
```

#### Kiến Trúc Code Component JavaFX `TableWithChairsNode`:
```java
package cafe.views.components;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class TableWithChairsNode extends StackPane {
    private final Button btnTable = new Button();

    public TableWithChairsNode(String tableNum, boolean isSelected, String status, Runnable onClick) {
        setPrefSize(74, 62);
        setMinSize(74, 62);

        // 1. Tạo 4 khối ghế bao quanh (#E8DED0)
        Region chairTop = createChair(28, 6);
        StackPane.setAlignment(chairTop, Pos.TOP_CENTER);

        Region chairBottom = createChair(28, 6);
        StackPane.setAlignment(chairBottom, Pos.BOTTOM_CENTER);

        Region chairLeft = createChair(6, 22);
        StackPane.setAlignment(chairLeft, Pos.CENTER_LEFT);

        Region chairRight = createChair(6, 22);
        StackPane.setAlignment(chairRight, Pos.CENTER_RIGHT);

        // 2. Mặt bàn trung tâm (52 x 42px)
        btnTable.setPrefSize(52, 42);
        btnTable.setText(tableNum);
        applyTableStyle(isSelected);

        // 3. Chấm cam phục vụ nếu là bàn active
        if (isSelected && "serving".equalsIgnoreCase(status)) {
            Circle dot = new Circle(3.5, Color.web("#E5A04D"));
            StackPane.setAlignment(dot, Pos.TOP_LEFT);
            dot.setTranslateX(14);
            dot.setTranslateY(13);
            getChildren().addAll(chairTop, chairBottom, chairLeft, chairRight, btnTable, dot);
        } else {
            getChildren().addAll(chairTop, chairBottom, chairLeft, chairRight, btnTable);
        }

        btnTable.setOnAction(e -> {
            if (onClick != null) onClick.run();
        });
    }

    private Region createChair(double w, double h) {
        Region chair = new Region();
        chair.setPrefSize(w, h);
        chair.setMaxSize(w, h);
        chair.setStyle("-fx-background-color: #E8DED0; -fx-background-radius: 3px;");
        return chair;
    }

    private void applyTableStyle(boolean isSelected) {
        if (isSelected) {
            btnTable.setStyle(
                "-fx-background-color: #112823; " +
                "-fx-text-fill: #FFFFFF; " +
                "-fx-font-size: 13px; " +
                "-fx-font-weight: 700; " +
                "-fx-background-radius: 8px; " +
                "-fx-cursor: hand;"
            );
        } else {
            btnTable.setStyle(
                "-fx-background-color: #FBF9F5; " +
                "-fx-text-fill: #292722; " +
                "-fx-font-size: 13px; " +
                "-fx-font-weight: 600; " +
                "-fx-background-radius: 8px; " +
                "-fx-border-color: #DED7CD; " +
                "-fx-border-radius: 8px; " +
                "-fx-border-width: 1px; " +
                "-fx-cursor: hand;"
            );
        }
    }
}
```

* **Cây trang trí góc phòng (`🪴`):** Sử dụng asset hình ảnh cây trầu bà/monstera đặt trong chậu gốm màu be ấm đặt ở góc trên-phải và dưới-phải của sàn, đổ bóng mờ nhẹ (`blur=12, opacity=0.05`).
* **Legend thanh trạng thái đáy sàn:** 4 mục màu chuẩn hex:
  * ● Trống: `#D9D0C3`
  * ● Đang phục vụ: `#E5A04D`
  * ● Đặt trước: `#86A88A`
  * ● Bảo trì: `#4B1717`

---

## 1.3 TRANG GIAO DIỆN NGƯỜI DÙNG — MENU KIOSK (`ClientView`) — YÊU CẦU 1.3

### A. Khắc Phục Lệch Chữ Tại Sidebar (Chữ Phải Thẳng Trục Tuyệt Đối)
* **Nguyên nhân cốt lõi gây thụt thò chữ:** Kích thước của các icon không bằng nhau (ví dụ: icon túi mua hàng có tỉ lệ hình vuông, trong khi icon ly trà hay lát cam có bề ngang hẹp hơn). Nếu đưa thẳng icon và nhãn vào `HBox` mặc định, lề trái của chữ sẽ phụ thuộc vào độ rộng của icon đứng trước, tạo cảm giác lượn sóng, mất đối xứng.
* **Giải pháp kỹ thuật chuẩn mực:**
  * Mỗi icon được bọc trong một container `StackPane` cố định bề rộng **`width = 24px`**, căn giữa tuyệt đối (`Pos.CENTER`).
  * Text nhãn danh mục đặt ngay sau container với lề trái cố định `8px`.
  * **Kết quả:** Tất cả chữ nhãn (*Tất cả, Cà phê, Trà, Đá xay, Nước ép, Bánh ngọt, Món ăn nhẹ*) luôn bắt đầu chính xác tại cùng một tọa độ $X = 36px$, tạo thành đường biên thẳng đứng 100%.

```java
private Button createCategoryButton(String label, String iconSvg, boolean isActive) {
    Button btn = new Button();
    btn.setMaxWidth(Double.MAX_VALUE);
    btn.setPrefHeight(34);
    btn.setAlignment(Pos.CENTER_LEFT);
    btn.setPadding(new Insets(0, 10, 0, 10));

    HBox box = new HBox(8);
    box.setAlignment(Pos.CENTER_LEFT);

    // Container cố định kích thước cho Icon
    StackPane iconContainer = new StackPane();
    iconContainer.setPrefSize(24, 24);
    iconContainer.setMinSize(24, 24);
    iconContainer.setMaxSize(24, 24);
    iconContainer.setAlignment(Pos.CENTER);

    SVGPath icon = SvgIcons.createOutlineIcon(iconSvg, 0.82, isActive ? Color.WHITE : Color.web("#6F695F"));
    iconContainer.getChildren().add(icon);

    Label lblText = new Label(label);
    lblText.setStyle("-fx-font-size: 11.5px; -fx-font-weight: " + (isActive ? "600" : "500") + "; -fx-text-fill: " + (isActive ? "#FFFFFF" : "#292722") + ";");

    box.getChildren().addAll(iconContainer, lblText);
    btn.setGraphic(box);

    if (isActive) {
        btn.setStyle("-fx-background-color: #112823; -fx-background-radius: 6px; -fx-cursor: hand;");
    } else {
        btn.setStyle("-fx-background-color: transparent; -fx-background-radius: 6px; -fx-cursor: hand;");
    }
    return btn;
}
```

---

### B. Quy Chuẩn Lưới Sản Phẩm 3 Cột (Product Grid)
* **Số cột:** Cố định 3 cột, khoảng cách giữa các thẻ `gap = 7px`.
* **Thẻ món ăn (`ItemCard`):**
  * Nền: `#FBF9F5`, bo góc chuẩn **`6px`**, viền mỏng `#DED7CD`.
  * Ảnh sản phẩm: Chiếm 67% chiều cao thẻ, bo góc **`5px`**, chế độ cắt ảnh `cover`.
  * **Nút bấm thêm nhanh `(+)`:** Nút tròn đường kính **`20px`**, màu đen xanh `#112823`, dấu cộng trắng mảnh, đặt nổi đè ở góc dưới-phải của bức ảnh (Overlay bottom-right, cách mép 4px).
  * Tên món & Giá: Cỡ chữ 9.5px – 10px, giá tiền màu nâu cafe `#563521`.

---

## 1.4 TRANG CHI TIẾT MÓN (`ItemDetailDialog`) — YÊU CẦU 1.4

### A. Thay Đổi Hoàn Toàn Giao Diện NetBeans Cũ Sang Dạng Ngang (Horizontal Modal)
* Bỏ toàn bộ các dialog dọc kiểu NetBeans/Swing xám xịt cũ kỹ.
* Dựng modal dạng **Ngang (Horizontal Split)** theo tỷ lệ vàng của mẫu:
  * **Bên Trái (38% bề rộng):** Ảnh sản phẩm cận cảnh chất lượng cao (ví dụ: ly Latte phân tầng hấp dẫn), bo góc **`6px`**.
  * **Bên Phải (62% bề rộng):** Toàn bộ hệ thống tùy biến món:
    1. **Nút đóng `✕`:** Nhỏ gọn, thanh mảnh ở góc trên cùng bên phải.
    2. **Tên sản phẩm:** `Latte` (Playfair Display 20px, `#292722`).
    3. **Giá bán:** `65.000đ` (Inter Bold 15px, màu nâu `#563521`).
    4. **Mô tả món:** 2 dòng ngắn gọn giới thiệu hương vị, màu `#817A70`.
    5. **Bộ chọn Size (Segmented Pill Buttons):** 3 nút liền kề `[ S ]` `[ M ]` `[ L ]`. Nút `M` đang chọn có nền xanh đậm `#112823`, chữ trắng; hai nút còn lại nền trắng viền `#DED7CD`.
    6. **Checklist Tùy chọn thêm (Toppings):** Checkbox tùy biến cao cấp:
       * `☑ Thêm shot (+10.000đ)` (Mặc định tích chọn, ô vuông xanh `#112823`)
       * `☐ Ít đường`
       * `☐ Không đường`
       * `☐ Sữa hạnh nhân (+10.000đ)`
    7. **Cụm Tăng/Giảm số lượng:** Khung chữ nhật cao 28px gồm nút `[-]`, số lượng `1`, và nút `[+]`.
    8. **Nút CTA Thêm vào giỏ:** `Thêm vào giỏ hàng` — Nền `#112823`, chữ trắng, cao 34px, bo góc 5px.
    9. **Họa tiết đáy:** Dấu ấn vector 2 hạt cà phê nhỏ mờ ở góc dưới bên trái panel.

---

## 1.5 TRANG THANH TOÁN (`PaymentDialog`) — YÊU CẦU 1.5

* **Header:** Nút mũi tên quay lại `←`, tiêu đề `Thanh toán`, bên phải có tag `Bàn 05`.
* **3 Thẻ Phương Thức Thanh Toán:**
  1. `[ 💵 Tiền mặt ]` — **Trạng thái Đang chọn:** Viền xanh đậm `#112823`, bên phải có icon tích chọn `✔`.
  2. `[ 💳 Thẻ ngân hàng ]` — Viền be `#DED7CD`, bên phải có mũi tên `>`.
  3. `[ 👛 Ví điện tử ]` — Viền be `#DED7CD`, bên phải có mũi tên `>`.
* **Bảng Tóm Tắt Chi Phí:**
  * Tổng tiền: `110.000đ`
  * Giảm giá: `0đ`
  * **Thành tiền:** `110.000đ` (Inter Bold 16px, `#292722`).
* **Nút Xác Nhận:** Nút `Xác nhận thanh toán` chiếm trọn chiều ngang, cao **40px**, bo góc **5px**, nền `#112823`.

---

## 1.6 TRANG ĐƠN HÀNG NHÂN VIÊN (`OrdersView`) — YÊU CẦU 1.6

* **Mục đích:** Quản lý toàn bộ danh sách đơn hàng gửi từ các bàn về quầy điều phối theo thời gian thực.
* **Header:** Nút `← Quay lại`, tiêu đề `Đơn hàng`, bên phải có icon thùng rác `🗑️` để xóa/lưu trữ các đơn đã xong.
* **Bộ Lọc Trạng Thái (3 Tabs):**
  * `[ Đang phục vụ ]` (Active — có gạch chân hoặc nền sẫm).
  * `[ Hoàn thành ]`
  * `[ Hủy ]`
* **Danh Sách Thẻ Đơn Hàng (Order Cards - Cao 64px, Bo góc 6px, Nền `#FBF9F5`):**
  * Mỗi thẻ gồm: Icon bàn `🪑`, Số bàn (`Bàn 03`, `Bàn 05`, `Bàn 08`), Tóm tắt (`2 món · 120.000đ`), Giờ gọi món (`10:24`, `10:18`).
  * **Tag trạng thái màu chuẩn:** Badge mềm màu xanh rêu nhạt `#DCE8D9`, chữ `#597256` ghi `Đang phục vụ`.

---

# PHẦN 2: RÀ SOÁT ANIMATION & CÁC LỖI THỊ GIÁC

1. **Hiệu Ứng Chuyển Động (Animations):**
   * **Badge Bump:** Giữ nguyên hiệu ứng nảy nhẹ của icon giỏ hàng khi bấm `(+)` để tạo phản hồi xúc giác (tactile feedback).
   * **Modal Transition:** Chuyển từ hiệu ứng phóng to thông thường sang **Fade & Slight Rise** (mờ dần và trượt nhẹ 10px từ dưới lên với curve `EASE_OUT_QUART`), đúng chất ứng dụng cao cấp.
   * **Tactile Button Press:** Khi click nút, nút hạ độ sâu (Scale 0.985 và chuyển nhẹ tone `#0B1D19`).
2. **Khắc Phục Triệt Để Lỗi Bo Góc:**
   * Thay toàn bộ các radius 12–16px ở các nút và thẻ sản phẩm về đúng chuẩn **`5px – 6px`** theo đúng quy định tại `json quan trọng.txt`.

---

# PHẦN 3: CHIẾN LƯỢC RESPONSIVE & MOBILE-FIRST

Theo yêu cầu mục [10] trong file docx: *"Ưu tiên làm đúng kích thước ở trên điện thoại trước và sau đó là resize tùy ý theo kích thước to của máy tính."*

```
📱 MOBILE (375px - 430px)             💻 DESKTOP (1100px - 1440px)
┌───────────────────────────────┐     ┌───────────┬────────────────────────────────────────┐
│ [Top Bar: Logo + Bàn 05 + 🛒] │     │ SIDEBAR   │ TOP BAR: Bàn 05 | Profile | Nút Menu   │
├───────────────────────────────┤     │ (105px)   ├────────────────────────────────────────┤
│ [Search Bar]                  │     │           │ [Floor Tabs: Tầng 1 | Tầng 2 | Tầng 3] │
├───────────────────────────────┤ ──> │ • Bàn     │ ┌────────────────────────────────────┐ │
│ [Tab danh mục trượt ngang]    │     │ • Menu    │ │  Mặt bằng bàn 2.5D (Isometric)     │ │
├───────────────────────────────┤     │ • Đơn hàng│ │  [01]   [02]   [03]        🪴      │ │
│ LƯỚI SẢN PHẨM 2 CỘT           │     │ • Thanh to│ │  [05●]  [09]   [07]                │ │
│ ┌──────────┐ ┌──────────┐     │     │ • Cài đặt │ └────────────────────────────────────┘ │
│ │  Món 1   │ │  Món 2   │     │     │           ├────────────────────────────────────────┤
│ └──────────┘ └──────────┘     │     │           │ Status Legend: Trống | Đang phục vụ... │
└───────────────────────────────┘     └───────────┴────────────────────────────────────────┘
```

### 1. Cấp Độ Mobile (Điện Thoại: 375px – 430px):
* Sidebar dọc tự động chuyển thành **Bottom Navigation Bar** hoặc thanh cuộn ngang (Horizontal Category Pills).
* Lưới thực đơn chuyển thành **2 cột** (thay vì 3 cột).
* Modal chi tiết món chuyển thành **Bottom Sheet** trượt từ mép dưới màn hình lên.

### 2. Cấp Độ Desktop / Tablet (Máy Tính: 1100px – 1440px):
* Mở rộng layout 2 panel và 3 cột lưới thoáng đãng.
* Modal hiển thị dạng Dialog chính giữa màn hình với kích thước cố định, bóng đổ mờ êm dịu.

---

# PHẦN 4: LỘ TRÌNH TRIỂN KHAI TỪNG BƯỚC (IMPLEMENTATION ROADMAP)

| Giai đoạn | Nội dung công việc cụ thể | File tác động | Trạng thái |
| :--- | :--- | :--- | :---: |
| **Phase 1: Design Tokens** | Cập nhật toàn bộ bảng màu chuẩn (`#112823`, `#F5F0E8`, `#DED7CD`), đưa radius toàn hệ thống về **4–8px**. | `cafe-theme.css`, `client.css` | ⏳ Sẵn sàng |
| **Phase 2: Trang Login** | Tích hợp prompt ảnh poster bên trái + hoàn thiện form Tài khoản/Mật khẩu/Phân quyền. | `LoginView.java`, `LoginController.java` | ⏳ Sẵn sàng |
| **Phase 3: Sơ đồ bàn** | Bổ sung đủ 5 menu sidebar, tạo component `TableWithChairsNode` (bàn có ghế bao quanh), chậu cây decor. | `FloorPlanView.java` | ⏳ Sẵn sàng |
| **Phase 4: Menu & Sidebar** | Cố định container icon để chữ danh mục thẳng tắp 1 hàng; tinh chỉnh nút tròn `(+)` đè góc ảnh. | `ClientView.java` | ⏳ Sẵn sàng |
| **Phase 5: Dialogs** | Tái cấu trúc `ItemDetailDialog` (bố cục ngang ảnh trái), `PaymentDialog` (3 thẻ phương thức), `CartModalDialog`. | `ItemDetailDialog.java`, `PaymentDialog.java` | ⏳ Sẵn sàng |
| **Phase 6: Màn hình Đơn hàng** | Dựng mới màn hình `OrdersView` cho nhân viên với 3 tab lọc trạng thái và danh sách thẻ đơn 64px. | `OrdersView.java`, `ServerKDSView.java` | ⏳ Sẵn sàng |
| **Phase 7: Responsive** | Thiết lập cơ chế layout co giãn linh hoạt từ mobile-first lên desktop. | Toàn bộ controller & layout panes | ⏳ Sẵn sàng |

---

*Báo cáo được biên soạn và bảo chứng bởi Chuyên gia Thiết kế Hệ thống UI/UX cao cấp.*
