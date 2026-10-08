package cafe.views;

import cafe.models.MenuItem;
import cafe.models.MenuRepository;
import cafe.models.OrderItem;
import cafe.models.SizeItem;
import cafe.models.ToppingItem;
import cafe.utils.CurrencyFormatter;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * ItemDetailDialog - Hộp thoại tùy chọn món ăn 2 cột theo Screen 4 (UI_mong_muon.png).
 * Cột trái: Ảnh sản phẩm lớn sắc nét bo góc 10px.
 * Cột phải: Tiêu đề Serif, Đơn giá, Chọn Size (S/M/L), Topping checkboxes, Stepper [-] 1 [+] và nút Thêm vào giỏ.
 */
public class ItemDetailDialog extends Stage {
    private final MenuItem item;
    private final Consumer<OrderItem> onConfirmed;

    private int quantity = 1;
    private SizeItem selectedSize;
    private String selectedTemperature = "COLD";
    private int selectedSugar = 100;
    private int selectedIce = 100;
    private final List<ToppingItem> selectedToppings = new ArrayList<>();

    private final Button btnConfirm = new Button();
    private final Label lblPrice = new Label();

    public ItemDetailDialog(Stage parentStage, MenuItem item, Consumer<OrderItem> onConfirmed) {
        this.item = item;
        this.onConfirmed = onConfirmed;

        List<SizeItem> sizes = MenuRepository.getSizes();
        this.selectedSize = sizes.size() > 1 ? sizes.get(1) : (sizes.isEmpty() ? new SizeItem("M", "Vừa", 400, 0) : sizes.get(0));

        initOwner(parentStage);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.UTILITY);
        setTitle("Tùy Chọn Món • " + item.getName());
        setResizable(false);

        initUI();
    }

    private void initUI() {
        HBox root = new HBox(18);
        root.setStyle("-fx-background-color: #FBF9F4; -fx-padding: 20;");
        root.setPrefWidth(660);
        root.setPrefHeight(490);

        // =====================================================================
        // CỘT TRÁI: ẢNH SẢN PHẨM LỚN
        // =====================================================================
        VBox leftCol = new VBox(10);
        leftCol.setAlignment(Pos.TOP_CENTER);
        leftCol.setPrefWidth(240);
        leftCol.setMinWidth(240);

        ProductImagePane largeImg = new ProductImagePane(item, 240, 240, 10);

        // Huy hiệu đặc biệt (nếu có)
        HBox badgeBox = new HBox(6);
        badgeBox.setAlignment(Pos.CENTER);
        if (item.isBestseller()) {
            Label b = new Label("★ BÁN CHẠY NHẤT");
            b.setStyle("-fx-background-color: #183C32; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: 800; -fx-background-radius: 4px; -fx-padding: 3 8 3 8;");
            badgeBox.getChildren().add(b);
        } else if (item.isNew()) {
            Label b = new Label("● MÓN MỚI");
            b.setStyle("-fx-background-color: #A66A42; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: 800; -fx-background-radius: 4px; -fx-padding: 3 8 3 8;");
            badgeBox.getChildren().add(b);
        }

        leftCol.getChildren().addAll(largeImg, badgeBox);

        // =====================================================================
        // CỘT PHẢI: TIÊU ĐỀ, TÙY CHỌN & NÚT THÊM
        // =====================================================================
        VBox rightCol = new VBox(12);
        HBox.setHgrow(rightCol, Priority.ALWAYS);

        // 1. Header (Tên món, Nút đóng ✕, Giá tiền)
        HBox headerRow = new HBox(8);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Label lblName = new Label(item.getName());
        lblName.setStyle("-fx-font-family: 'Playfair Display', 'Georgia', serif; -fx-font-size: 21px; -fx-font-weight: 700; -fx-text-fill: #292722;");

        lblPrice.setStyle("-fx-font-size: 17px; -fx-font-weight: 800; -fx-text-fill: #A66A42;");
        titleBox.getChildren().addAll(lblName, lblPrice);

        Button btnClose = new Button("✕");
        btnClose.setStyle("-fx-background-color: transparent; -fx-text-fill: #817B70; -fx-font-size: 14px; -fx-cursor: hand; -fx-font-weight: 700;");
        btnClose.setOnAction(e -> close());

        headerRow.getChildren().addAll(titleBox, btnClose);

        if (item.getDescription() != null && !item.getDescription().trim().isEmpty()) {
            Label lblDesc = new Label(item.getDescription().trim());
            lblDesc.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #817B70; -fx-line-spacing: 1.5px;");
            lblDesc.setWrapText(true);
            rightCol.getChildren().addAll(headerRow, lblDesc);
        } else {
            rightCol.getChildren().addAll(headerRow);
        }

        // 2. Danh sách tùy chọn cuộn mượt
        VBox optionsList = new VBox(12);
        optionsList.setPadding(new Insets(4, 2, 4, 2));

        // Size Selection (S / M / L)
        if (item.isAllowSize()) {
            VBox sizeBox = new VBox(6);
            Label lblSizeTitle = new Label("SIZE");
            lblSizeTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #292722; -fx-letter-spacing: 0.5px;");

            HBox sizeButtons = new HBox(8);
            ToggleGroup sizeGroup = new ToggleGroup();

            for (SizeItem s : MenuRepository.getSizes()) {
                ToggleButton tb = new ToggleButton(s.getSizeCode());
                tb.setToggleGroup(sizeGroup);
                boolean isDefault = s.getSizeCode().equalsIgnoreCase("M");
                tb.setSelected(isDefault);
                applySizeToggleStyle(tb, isDefault);

                tb.selectedProperty().addListener((obs, oldV, isSel) -> applySizeToggleStyle(tb, isSel));

                tb.setOnAction(e -> {
                    if (tb.isSelected()) {
                        selectedSize = s;
                        updatePrice();
                    } else if (sizeGroup.getSelectedToggle() == null) {
                        tb.setSelected(true);
                    }
                });
                sizeButtons.getChildren().add(tb);
            }
            sizeBox.getChildren().addAll(lblSizeTitle, sizeButtons);
            optionsList.getChildren().add(sizeBox);
        }

        // Temperature (Nóng / Lạnh)
        if (item.isHotAvailable()) {
            VBox tempBox = new VBox(6);
            Label lblTempTitle = new Label("NHIỆT ĐỘ");
            lblTempTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #292722; -fx-letter-spacing: 0.5px;");

            HBox tempButtons = new HBox(8);
            ToggleGroup tempGroup = new ToggleGroup();

            ToggleButton tbCold = new ToggleButton("🧊 Đá lạnh");
            tbCold.setToggleGroup(tempGroup);
            tbCold.setSelected(true);
            applySizeToggleStyle(tbCold, true);

            ToggleButton tbHot = new ToggleButton("🔥 Uống nóng");
            tbHot.setToggleGroup(tempGroup);
            applySizeToggleStyle(tbHot, false);

            tbCold.selectedProperty().addListener((obs, oldV, isSel) -> applySizeToggleStyle(tbCold, isSel));
            tbHot.selectedProperty().addListener((obs, oldV, isSel) -> applySizeToggleStyle(tbHot, isSel));

            tbCold.setOnAction(e -> {
                selectedTemperature = "COLD";
                tbCold.setSelected(true);
            });
            tbHot.setOnAction(e -> {
                selectedTemperature = "HOT";
                tbHot.setSelected(true);
            });

            tempButtons.getChildren().addAll(tbCold, tbHot);
            tempBox.getChildren().addAll(lblTempTitle, tempButtons);
            optionsList.getChildren().add(tempBox);
        }

        // Topping Add-ons
        List<ToppingItem> allowedToppings = MenuRepository.getToppingsForCategory(item.getCategory());
        if (allowedToppings != null && !allowedToppings.isEmpty()) {
            VBox topBox = new VBox(6);
            Label lblTopTitle = new Label("TÙY CHỌN THÊM (TOPPINGS)");
            lblTopTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #292722; -fx-letter-spacing: 0.5px;");

            VBox toppingsPane = new VBox(6);
            for (ToppingItem t : allowedToppings) {
                CheckBox cb = new CheckBox(t.getName() + " (+" + CurrencyFormatter.format(t.getPrice()) + ")");
                cb.setStyle("-fx-font-size: 12px; -fx-text-fill: #292722; -fx-cursor: hand;");
                cb.setOnAction(e -> {
                    if (cb.isSelected()) {
                        selectedToppings.add(t);
                    } else {
                        selectedToppings.remove(t);
                    }
                    updatePrice();
                });
                toppingsPane.getChildren().add(cb);
            }
            topBox.getChildren().addAll(lblTopTitle, toppingsPane);
            optionsList.getChildren().add(topBox);
        }

        // Ghi chú
        VBox noteBox = new VBox(6);
        Label lblNoteTitle = new Label("GHI CHÚ ĐẶC BIỆT");
        lblNoteTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #292722; -fx-letter-spacing: 0.5px;");
        TextField txtNote = new TextField();
        txtNote.setPromptText("Ví dụ: Ít ngọt, không đá, để riêng...");
        txtNote.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 6px; -fx-border-color: #D8D1C3; -fx-border-radius: 6px; -fx-font-size: 12px; -fx-padding: 6 10 6 10;");
        noteBox.getChildren().addAll(lblNoteTitle, txtNote);
        optionsList.getChildren().add(noteBox);

        ScrollPane scrollContent = new ScrollPane(optionsList);
        scrollContent.setFitToWidth(true);
        scrollContent.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scrollContent, Priority.ALWAYS);

        // 3. Footer (Stepper [-] 1 [+] & Nút "Thêm vào giỏ hàng" Dark Emerald)
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(10, 0, 0, 0));
        footer.setStyle("-fx-border-color: #E6DFD4 transparent transparent transparent; -fx-border-width: 1 0 0 0;");

        HBox stepper = new HBox(6);
        stepper.setAlignment(Pos.CENTER_LEFT);

        Button btnMinus = new Button("-");
        btnMinus.setStyle("-fx-background-color: #E8E2D8; -fx-text-fill: #292722; -fx-font-size: 13px; -fx-font-weight: 700; -fx-background-radius: 6px; -fx-min-width: 32px; -fx-min-height: 32px; -fx-cursor: hand;");

        Label lblQtyValue = new Label("1");
        lblQtyValue.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #292722; -fx-padding: 0 8 0 8;");

        Button btnPlus = new Button("+");
        btnPlus.setStyle("-fx-background-color: #E8E2D8; -fx-text-fill: #292722; -fx-font-size: 13px; -fx-font-weight: 700; -fx-background-radius: 6px; -fx-min-width: 32px; -fx-min-height: 32px; -fx-cursor: hand;");

        btnMinus.setOnAction(e -> {
            if (quantity > 1) {
                quantity--;
                lblQtyValue.setText(String.valueOf(quantity));
                updatePrice();
            }
        });

        btnPlus.setOnAction(e -> {
            if (quantity < 99) {
                quantity++;
                lblQtyValue.setText(String.valueOf(quantity));
                updatePrice();
            }
        });

        stepper.getChildren().addAll(btnMinus, lblQtyValue, btnPlus);

        Region spacerFooter = new Region();
        HBox.setHgrow(spacerFooter, Priority.ALWAYS);

        btnConfirm.setStyle(
            "-fx-background-color: #183C32; -fx-text-fill: white; -fx-font-weight: 700; -fx-font-size: 13.5px; " +
            "-fx-padding: 9 20 9 20; -fx-background-radius: 8px; -fx-cursor: hand; " +
            "-fx-effect: dropshadow(gaussian, rgba(24, 60, 50, 0.25), 6, 0, 0, 2);"
        );
        btnConfirm.setOnMouseEntered(e -> btnConfirm.setStyle("-fx-background-color: #102C25; -fx-text-fill: white; -fx-font-weight: 700; -fx-font-size: 13.5px; -fx-padding: 9 20 9 20; -fx-background-radius: 8px; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(16, 44, 37, 0.35), 8, 0, 0, 3);"));
        btnConfirm.setOnMouseExited(e -> btnConfirm.setStyle("-fx-background-color: #183C32; -fx-text-fill: white; -fx-font-weight: 700; -fx-font-size: 13.5px; -fx-padding: 9 20 9 20; -fx-background-radius: 8px; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(24, 60, 50, 0.25), 6, 0, 0, 2);"));

        btnConfirm.setOnAction(e -> {
            if (onConfirmed != null) {
                double sizeDelta = item.isAllowSize() && selectedSize != null ? selectedSize.getPriceDelta() : 0.0;
                String sizeCode = item.isAllowSize() && selectedSize != null ? selectedSize.getSizeCode() : "M";

                OrderItem orderItem = new OrderItem(
                    item.getId(),
                    item.getName(),
                    quantity,
                    item.getPrice(),
                    sizeCode,
                    sizeDelta,
                    selectedSugar,
                    selectedIce,
                    selectedTemperature,
                    new ArrayList<>(selectedToppings),
                    txtNote.getText().trim()
                );
                onConfirmed.accept(orderItem);
            }
            close();
        });

        footer.getChildren().addAll(stepper, spacerFooter, btnConfirm);

        updatePrice();

        rightCol.getChildren().addAll(scrollContent, footer);
        root.getChildren().addAll(leftCol, rightCol);

        Scene scene = new Scene(root);
        setScene(scene);
        setWidth(680);
        setHeight(500);
    }

    private void applySizeToggleStyle(ToggleButton tb, boolean isSelected) {
        if (isSelected) {
            tb.setStyle("-fx-background-color: #183C32; -fx-text-fill: #FFFFFF; -fx-font-weight: 700; -fx-font-size: 12px; -fx-background-radius: 6px; -fx-padding: 6 16 6 16; -fx-cursor: hand;");
        } else {
            tb.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #292722; -fx-font-weight: 600; -fx-font-size: 12px; -fx-background-radius: 6px; -fx-border-color: #D8D1C3; -fx-border-radius: 6px; -fx-padding: 6 16 6 16; -fx-cursor: hand;");
        }
    }

    private void updatePrice() {
        double unit = item.getPrice();
        if (item.isAllowSize() && selectedSize != null) {
            unit += selectedSize.getPriceDelta();
        }
        for (ToppingItem t : selectedToppings) {
            unit += t.getPrice();
        }
        double total = unit * quantity;
        lblPrice.setText(CurrencyFormatter.format(unit));
        btnConfirm.setText("Thêm vào giỏ • " + CurrencyFormatter.format(total));
    }
}
