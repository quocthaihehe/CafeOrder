package cafe.views;

import cafe.models.Order;
import cafe.models.OrderItem;
import cafe.models.ToppingItem;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * OrderTicketCard - Card phiếu đơn hàng trong quầy KDS (Kitchen Display System).
 * Chuẩn hóa toàn diện theo Plan UI 4.5:
 * 1. Tiêu đề 2 tầng thông minh: Tầng 1 (Bàn + #Mã đơn bên trái, SLA bên phải); Tầng 2 (Dải trạng thái thanh lịch).
 *    -> Chấm dứt 100% hiện tượng cắt chữ thành dấu ba chấm (...).
 * 2. Phân loại hiển thị thông minh: Bánh ngọt / Món ăn tự động ẩn hoàn toàn dòng Đá/Đường và tag Size M mặc định.
 * 3. Lọc sạch thông tin thanh toán: Không in các chuỗi "Thanh toán: TIỀN MẶT", voucher, mã giảm giá lên vé bếp.
 * 4. Đồng bộ màu sắc nút bấm: Evergreen #112823 cho cả Bắt đầu & Hoàn tất, nút phụ #F5EFEB chuẩn Client.
 * 5. Pinned Action Bar: Cố định sát đáy thẻ (Priority.ALWAYS trên danh sách món), thẳng hàng tắp trên toàn bộ lưới.
 */
public class OrderTicketCard extends VBox {
    private final Order order;
    private final BiConsumer<Order, String> onStatusChange;
    private final Label lblSlaTime = new Label();

    public OrderTicketCard(Order order, BiConsumer<Order, String> onStatusChange) {
        this.order = order;
        this.onStatusChange = onStatusChange;

        initUI();
    }

    public void updateElapsedTime() {
        long elapsedMillis = Math.max(0, System.currentTimeMillis() - order.getTimestamp());
        long elapsedMinutes = elapsedMillis / 60000;
        long elapsedSeconds = (elapsedMillis % 60000) / 1000;

        String timeFormatted = String.format("%02d:%02d", elapsedMinutes, elapsedSeconds);
        if (elapsedMinutes > 0) {
            timeFormatted += " (" + elapsedMinutes + "p trước)";
        }
        lblSlaTime.setText(timeFormatted);

        if (elapsedMinutes >= 10) {
            // SLA > 10 phút: Đỏ cảnh báo khẩn cấp
            lblSlaTime.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #DC2626;");
        } else if (elapsedMinutes >= 5) {
            // SLA 5 - 10 phút: Cam hổ phách
            lblSlaTime.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #D97706;");
        } else {
            // SLA < 5 phút: Xám trung tính
            lblSlaTime.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #78716C;");
        }
    }

    private void initUI() {
        getStyleClass().add("ticket-card");
        setSpacing(10);
        setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10px; -fx-border-color: #D8D1C3; -fx-border-radius: 10px; -fx-border-width: 1px; -fx-padding: 12 14 12 14;");

        // 1. TICKET HEADER (Bố cục 2 tầng không bị cắt chữ)
        VBox header = buildHeader();
        getChildren().add(header);

        // 2. ITEMS LIST (Co gọn tự nhiên theo số lượng món, triệt tiêu khoảng trống rỗng)
        VBox itemsBox = buildItemsBox();
        VBox.setVgrow(itemsBox, Priority.ALWAYS);
        getChildren().add(itemsBox);

        // 3. GENERAL ORDER NOTE (Lọc bỏ các thông tin thanh toán không liên quan đến bếp)
        String cleanOrderNote = filterKitchenOrderNote(order.getNote());
        if (!cleanOrderNote.isEmpty()) {
            HBox orderNoteBox = new HBox(6);
            orderNoteBox.setAlignment(Pos.CENTER_LEFT);
            orderNoteBox.setStyle("-fx-background-color: #FBF4E6; -fx-border-color: #EBD9BC; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 6 10 6 10;");
            Label iconOrderNote = new Label("📌");
            iconOrderNote.setStyle("-fx-font-size: 11.5px;");
            Label lblOrderNote = new Label("Ghi chú đơn: " + cleanOrderNote);
            lblOrderNote.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #854D0E;");
            lblOrderNote.setWrapText(true);
            HBox.setHgrow(lblOrderNote, Priority.ALWAYS);
            orderNoteBox.getChildren().addAll(iconOrderNote, lblOrderNote);
            getChildren().add(orderNoteBox);
        }

        // 4. ACTION BAR SÁT ĐÁY THẺ
        HBox actionsBar = buildActionBar();
        getChildren().add(actionsBar);
    }

    private VBox buildHeader() {
        VBox header = new VBox(6);
        header.setStyle("-fx-padding: 0 0 8 0; -fx-border-color: transparent transparent #EAE4DA transparent; -fx-border-width: 0 0 1 0;");

        // TẦNG 1: [BÀN 05]  #1022               03:25 (3p trước)
        HBox row1 = new HBox(8);
        row1.setAlignment(Pos.CENTER_LEFT);

        boolean isTakeaway = "TAKEAWAY".equalsIgnoreCase(order.getOrderType()) || order.getTable() <= 0;
        Label lblTypeBadge = new Label();
        if (isTakeaway) {
            lblTypeBadge.setText("MANG VỀ");
            lblTypeBadge.setStyle("-fx-background-color: #FFF3E0; -fx-text-fill: #C05621; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 800; -fx-padding: 3 8 3 8; -fx-background-radius: 6px;");
        } else {
            int tbl = order.getTable();
            lblTypeBadge.setText("BÀN " + (tbl < 10 ? "0" + tbl : String.valueOf(tbl)));
            lblTypeBadge.setStyle("-fx-background-color: #EAE4DA; -fx-text-fill: #112823; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 800; -fx-padding: 3 8 3 8; -fx-background-radius: 6px;");
        }

        Label lblOrderId = new Label("#" + order.getOrderId());
        lblOrderId.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #57534E;");

        HBox leftRow1 = new HBox(6, lblTypeBadge, lblOrderId);
        leftRow1.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        updateElapsedTime();
        row1.getChildren().addAll(leftRow1, spacer, lblSlaTime);

        // TẦNG 2: ● ĐANG PHA CHẾ (Thanh trạng thái riêng biệt, không bị co cụm)
        HBox row2 = new HBox(6);
        row2.setAlignment(Pos.CENTER_LEFT);
        Label lblStatus = new Label();
        applyStatusBadgeStyle(lblStatus, order.getStatus());
        row2.getChildren().add(lblStatus);

        header.getChildren().addAll(row1, row2);
        return header;
    }

    private VBox buildItemsBox() {
        VBox itemsBox = new VBox(6);
        itemsBox.setPadding(new Insets(6, 0, 8, 0));

        List<OrderItem> items = order.getItems();
        if (items != null && !items.isEmpty()) {
            for (int i = 0; i < items.size(); i++) {
                OrderItem oi = items.get(i);
                VBox itemBlock = createItemBlock(oi);
                itemsBox.getChildren().add(itemBlock);

                // Đường phân cách đứt đoạn giữa các món (Dashed Divider)
                if (i < items.size() - 1) {
                    Region divider = new Region();
                    divider.setStyle("-fx-border-style: dashed; -fx-border-color: #E6E0D6 transparent transparent transparent; -fx-border-width: 1 0 0 0; -fx-min-height: 1px; -fx-pref-height: 1px;");
                    VBox.setMargin(divider, new Insets(6, 0, 6, 0));
                    itemsBox.getChildren().add(divider);
                }
            }
        }
        return itemsBox;
    }

    private boolean isFoodOrPastry(OrderItem oi) {
        if (oi == null || oi.getName() == null) return false;
        String n = oi.getName().toLowerCase();
        return n.contains("bánh") || n.contains("cheesecake") || n.contains("croissant")
            || n.contains("tiramisu") || n.contains("mousse") || n.contains("sandwich")
            || n.contains("hướng dương") || n.contains("hạt điều") || n.contains("snack")
            || n.contains("khô bò") || n.contains("khô gà") || n.contains("nem")
            || n.contains("phô mai nướng") || n.contains("sừng bò");
    }

    private VBox createItemBlock(OrderItem oi) {
        VBox block = new VBox(3);
        boolean isFood = isFoodOrPastry(oi);

        // Dòng 1: [ 2x ]  Tên Món                             [ Size L ] (nếu có size)
        HBox headline = new HBox(8);
        headline.setAlignment(Pos.CENTER_LEFT);

        Label lblQty = new Label(oi.getQty() + "x");
        lblQty.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 800; -fx-padding: 2 7 2 7; -fx-background-radius: 5px; -fx-min-width: 28px; -fx-alignment: center;");

        String cleanName = cleanProductName(oi.getName());
        Label lblName = new Label(cleanName);
        lblName.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 13.5px; -fx-font-weight: 700; -fx-text-fill: #112823;");
        lblName.setWrapText(true);
        HBox.setHgrow(lblName, Priority.ALWAYS);

        headline.getChildren().addAll(lblQty, lblName);

        // Đối với bánh ngọt/món ăn: ẨN tag size M mặc định. Chỉ hiện size nếu là đồ uống hoặc size khác M
        String size = resolveItemSize(oi);
        if (!isFood || (!"M".equalsIgnoreCase(size) && !size.isEmpty())) {
            Label lblSize = new Label("Size " + size);
            lblSize.setStyle("-fx-background-color: #F5F0E8; -fx-border-color: #DFD7CA; -fx-border-radius: 5px; -fx-background-radius: 5px; -fx-text-fill: #563521; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 1 6 1 6;");
            headline.getChildren().add(lblSize);
        }

        block.getChildren().add(headline);

        // Dòng 2: Tùy chọn pha chế (Đá, Đường, Nhiệt độ) - ẨN HOÀN TOÀN ĐỐI VỚI BÁNH NGỌT / ĐỒ ĂN
        if (!isFood) {
            String specs = buildDrinkSpecs(oi);
            if (!specs.isEmpty()) {
                Label lblSpecs = new Label(specs);
                lblSpecs.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 11.5px; -fx-text-fill: #57534E; -fx-padding: 0 0 0 36;");
                lblSpecs.setWrapText(true);
                block.getChildren().add(lblSpecs);
            }
        }

        // Dòng 3: Topping thêm (nếu có)
        List<String> toppings = resolveToppings(oi);
        if (!toppings.isEmpty()) {
            Label lblTopping = new Label("• +Topping: " + String.join(", ", toppings));
            lblTopping.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 11.5px; -fx-font-weight: 600; -fx-text-fill: #15803D; -fx-padding: 0 0 0 36;");
            lblTopping.setWrapText(true);
            block.getChildren().add(lblTopping);
        }

        // Dòng 4: Ghi chú đặc biệt của món (Kitchen Note Alert Box)
        String cleanNote = cleanKitchenNote(oi);
        if (!cleanNote.isEmpty()) {
            HBox noteBox = new HBox(5);
            noteBox.setAlignment(Pos.CENTER_LEFT);
            noteBox.setStyle("-fx-background-color: #FEF3C7; -fx-border-color: #FDE68A; -fx-border-radius: 5px; -fx-background-radius: 5px; -fx-padding: 3 8 3 8;");
            VBox.setMargin(noteBox, new Insets(2, 0, 0, 36));

            Label lblIcon = new Label("⚡");
            lblIcon.setStyle("-fx-font-size: 11px;");
            Label lblNoteText = new Label("Ghi chú: " + cleanNote);
            lblNoteText.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #92400E;");
            lblNoteText.setWrapText(true);
            HBox.setHgrow(lblNoteText, Priority.ALWAYS);

            noteBox.getChildren().addAll(lblIcon, lblNoteText);
            block.getChildren().add(noteBox);
        }

        return block;
    }

    private String cleanProductName(String name) {
        if (name == null) return "";
        return name.replaceAll("(?i)\\s*\\([SMLsml]\\)", "")
                   .replaceAll("(?i)\\s*\\(Size\\s+[SMLsml]\\)", "")
                   .replaceAll("(?i)\\s*-\\s*Size\\s+[SMLsml]", "")
                   .trim();
    }

    private String resolveItemSize(OrderItem oi) {
        String code = oi.getSizeCode();
        if (code != null && !code.trim().isEmpty()) {
            return code.trim().toUpperCase();
        }
        if (oi.getNote() != null) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)size\\s*([SML])").matcher(oi.getNote());
            if (m.find()) {
                return m.group(1).toUpperCase();
            }
        }
        return "M";
    }

    private String buildDrinkSpecs(OrderItem oi) {
        List<String> specs = new ArrayList<>();

        if ("HOT".equalsIgnoreCase(oi.getTemperature())) {
            specs.add("• Nóng");
        } else {
            int ice = oi.getIcePercent();
            if (ice == 0) {
                specs.add("• Không đá");
            } else {
                specs.add("• " + ice + "% Đá");
            }
        }

        int sugar = oi.getSugarPercent();
        if (sugar == 100) {
            specs.add("• Chuẩn đường");
        } else if (sugar == 0) {
            specs.add("• Không đường");
        } else {
            specs.add("• " + sugar + "% Đường");
        }

        return String.join("  ", specs);
    }

    private List<String> resolveToppings(OrderItem oi) {
        List<String> list = new ArrayList<>();
        if (oi.getToppings() != null && !oi.getToppings().isEmpty()) {
            for (ToppingItem t : oi.getToppings()) {
                if (t != null && t.getName() != null && !t.getName().trim().isEmpty()) {
                    list.add(t.getName().trim());
                }
            }
        } else if (oi.getNote() != null && oi.getNote().contains("+")) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\+([^•«»\n\r]+)").matcher(oi.getNote());
            while (m.find()) {
                String top = m.group(1).trim();
                if (!top.isEmpty() && !list.contains(top)) {
                    list.add(top);
                }
            }
        }
        return list;
    }

    private String cleanKitchenNote(OrderItem oi) {
        if (oi.getNote() == null) return "";
        String n = oi.getNote().trim();
        if (n.isEmpty()) return "";

        // Lọc bỏ triệt để các ghi chú thanh toán, phương thức trả tiền hoặc voucher
        if (n.matches("(?i).*(thanh toán|tiền mặt|vietqr|chuyển khoản|voucher|mã giảm giá|đã thanh toán|giam\\d+k).*")) {
            return "";
        }

        // Xóa dấu ngoặc nhọn hoặc ngoặc kép «...»
        n = n.replaceAll("^[«\"']+|[»\"']+$", "").trim();

        // Nếu chuỗi chỉ ghi Size (VD: "Size L", "Size M", "Size: L")
        if (n.matches("(?i)^size\\s*[:\\-]?\\s*[sml]$")) {
            return "";
        }

        // Nếu chuỗi là "Size L • +Đào miếng"
        if (n.matches("(?i)^size\\s*[sml]\\s*•\\s*.*$")) {
            n = n.replaceAll("(?i)^size\\s*[sml]\\s*•\\s*", "").trim();
        }

        // Nếu là Topping đơn thuần (đã hiển thị ở dòng topping)
        if (n.startsWith("+")) {
            return "";
        }

        // Xóa các đặc tính đá/đường thừa trong note
        n = n.replaceAll("(?i)(?:•\\s*)?(?:đá\\s*)?\\d+%\\s*(?:đá)?", "")
             .replaceAll("(?i)(?:•\\s*)?\\d+%\\s*đường", "")
             .replaceAll("(?i)(?:•\\s*)?nóng", "")
             .replaceAll("(?i)^•\\s*|\\s*•$", "")
             .trim();

        if (n.equalsIgnoreCase("Size " + oi.getSizeCode()) || n.equalsIgnoreCase(oi.getSizeCode())) {
            return "";
        }

        return n;
    }

    private String filterKitchenOrderNote(String rawNote) {
        if (rawNote == null) return "";
        String n = rawNote.trim();
        if (n.isEmpty()) return "";

        // Bỏ các cụm từ liên quan đến thanh toán/voucher
        String[] parts = n.split("[•\n\r,]");
        List<String> cleanParts = new ArrayList<>();
        for (String p : parts) {
            String token = p.trim();
            if (token.isEmpty()) continue;
            if (token.matches("(?i).*(thanh toán|tiền mặt|vietqr|chuyển khoản|voucher|mã giảm giá|đã thanh toán|giam\\d+k).*")) {
                continue;
            }
            cleanParts.add(token);
        }
        return String.join(" • ", cleanParts).trim();
    }

    private HBox buildActionBar() {
        HBox actionsBar = new HBox(8);
        actionsBar.setAlignment(Pos.CENTER);
        actionsBar.setStyle("-fx-padding: 10 0 0 0; -fx-border-color: #EAE4DA transparent transparent transparent; -fx-border-width: 1 0 0 0;");

        String st = order.getStatus() != null ? order.getStatus().toUpperCase() : "QUEUED";

        if ("QUEUED".equals(st)) {
            Button btnStart = new Button("▶ Bắt đầu pha chế");
            btnStart.setMaxWidth(Double.MAX_VALUE);
            btnStart.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 38px; -fx-cursor: hand;");
            btnStart.setOnMouseEntered(e -> btnStart.setStyle("-fx-background-color: #1A3B34; -fx-text-fill: #FFFFFF; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 38px; -fx-cursor: hand;"));
            btnStart.setOnMouseExited(e -> btnStart.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 38px; -fx-cursor: hand;"));
            btnStart.setOnAction(e -> onStatusChange.accept(order, "PREPARING"));
            HBox.setHgrow(btnStart, Priority.ALWAYS);
            actionsBar.getChildren().add(btnStart);
        } else if ("PREPARING".equals(st)) {
            // Nút hoàn tất đồng bộ với màu Evergreen #112823 (Thay vì xanh lá chói #15803D)
            Button btnDone = new Button("✓ Hoàn tất món & Báo phục vụ");
            btnDone.setMaxWidth(Double.MAX_VALUE);
            btnDone.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 38px; -fx-cursor: hand;");
            btnDone.setOnMouseEntered(e -> btnDone.setStyle("-fx-background-color: #1A3B34; -fx-text-fill: #FFFFFF; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 38px; -fx-cursor: hand;"));
            btnDone.setOnMouseExited(e -> btnDone.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 38px; -fx-cursor: hand;"));
            btnDone.setOnAction(e -> onStatusChange.accept(order, "DONE"));
            HBox.setHgrow(btnDone, Priority.ALWAYS);
            actionsBar.getChildren().add(btnDone);
        } else {
            // DONE: 2 nút phụ nền #F5EFEB đồng bộ chuẩn Client Secondary Button
            Button btnUndo = new Button("↺ Hoàn tác");
            btnUndo.setMaxWidth(Double.MAX_VALUE);
            btnUndo.setStyle("-fx-background-color: #F5EFEB; -fx-border-color: #DFD7CA; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-text-fill: #112823; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-pref-height: 36px; -fx-cursor: hand;");
            btnUndo.setOnMouseEntered(e -> btnUndo.setStyle("-fx-background-color: #EAE4DA; -fx-border-color: #DFD7CA; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-text-fill: #112823; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-pref-height: 36px; -fx-cursor: hand;"));
            btnUndo.setOnMouseExited(e -> btnUndo.setStyle("-fx-background-color: #F5EFEB; -fx-border-color: #DFD7CA; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-text-fill: #112823; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-pref-height: 36px; -fx-cursor: hand;"));
            btnUndo.setOnAction(e -> onStatusChange.accept(order, "PREPARING"));
            HBox.setHgrow(btnUndo, Priority.ALWAYS);

            Button btnPrint = new Button("🖨 In lại vé");
            btnPrint.setMaxWidth(Double.MAX_VALUE);
            btnPrint.setStyle("-fx-background-color: #F5EFEB; -fx-border-color: #DFD7CA; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-text-fill: #112823; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-pref-height: 36px; -fx-cursor: hand;");
            btnPrint.setOnMouseEntered(e -> btnPrint.setStyle("-fx-background-color: #EAE4DA; -fx-border-color: #DFD7CA; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-text-fill: #112823; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-pref-height: 36px; -fx-cursor: hand;"));
            btnPrint.setOnMouseExited(e -> btnPrint.setStyle("-fx-background-color: #F5EFEB; -fx-border-color: #DFD7CA; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-text-fill: #112823; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-pref-height: 36px; -fx-cursor: hand;"));
            btnPrint.setOnAction(e -> {
                btnPrint.setText("✓ Đã in!");
                javafx.animation.PauseTransition p = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
                p.setOnFinished(ev -> btnPrint.setText("🖨 In lại vé"));
                p.play();
            });
            HBox.setHgrow(btnPrint, Priority.ALWAYS);

            actionsBar.getChildren().addAll(btnUndo, btnPrint);
        }

        return actionsBar;
    }

    private void applyStatusBadgeStyle(Label lbl, String status) {
        String st = status != null ? status.toUpperCase() : "QUEUED";
        switch (st) {
            case "QUEUED":
                lbl.setText("● ĐANG CHỜ");
                lbl.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #B45309; -fx-border-color: #FDE68A; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 3 10 3 10; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 11.5px; -fx-font-weight: 800;");
                break;
            case "PREPARING":
                lbl.setText("● ĐANG PHA CHẾ");
                lbl.setStyle("-fx-background-color: #EFF6FF; -fx-text-fill: #1D4ED8; -fx-border-color: #BFDBFE; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 3 10 3 10; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 11.5px; -fx-font-weight: 800;");
                break;
            case "DONE":
                lbl.setText("● ĐÃ HOÀN THÀNH");
                lbl.setStyle("-fx-background-color: #DCFCE7; -fx-text-fill: #15803D; -fx-border-color: #BBF7D0; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 3 10 3 10; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 11.5px; -fx-font-weight: 800;");
                break;
            default:
                lbl.setText("● " + st);
                lbl.setStyle("-fx-background-color: #F5F5F5; -fx-text-fill: #616161; -fx-border-radius: 6px; -fx-background-radius: 6px; -fx-padding: 3 10 3 10; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 11.5px; -fx-font-weight: 800;");
                break;
        }
    }
}
