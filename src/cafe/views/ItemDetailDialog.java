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
 * ItemDetailDialog - Product Customization Dialog.
 * Hallmark Discipline:
 * - 2-column editorial layout
 * - Left column: large cover image (4px radius)
 * - Right column: upright serif title, clean size tags, toppings, stepper, dark emerald CTA
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
        this.selectedSize = sizes.size() > 1 ? sizes.get(1) : (sizes.isEmpty() ? new SizeItem("M", "Vừa (400ml)", 400, 0) : sizes.get(0));

        initOwner(parentStage);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.UTILITY);
        setTitle("Tùy Chọn Món • " + item.getName());
        setResizable(false);

        initUI();
    }

    private void initUI() {
        HBox root = new HBox(16);
        root.getStyleClass().add("modal-canvas");
        root.setPadding(new Insets(18));
        root.setPrefWidth(660);
        root.setPrefHeight(490);

        // =====================================================================
        // 1. LEFT COLUMN: LARGE PRODUCT IMAGE
        // =====================================================================
        VBox leftCol = new VBox(10);
        leftCol.setAlignment(Pos.TOP_CENTER);
        leftCol.setPrefWidth(240);
        leftCol.setMinWidth(240);

        ProductImagePane largeImg = new ProductImagePane(item, 240, 240, 4);

        HBox badgeBox = new HBox(6);
        badgeBox.setAlignment(Pos.CENTER);
        if (item.isBestseller()) {
            Label b = new Label("BESTSELLER");
            b.getStyleClass().add("badge-bestseller");
            badgeBox.getChildren().add(b);
        } else if (item.isNew()) {
            Label b = new Label("MÓN MỚI");
            b.getStyleClass().add("badge-new");
            badgeBox.getChildren().add(b);
        }

        leftCol.getChildren().addAll(largeImg, badgeBox);

        // =====================================================================
        // 2. RIGHT COLUMN: CUSTOMIZATION OPTIONS
        // =====================================================================
        VBox rightCol = new VBox(10);
        HBox.setHgrow(rightCol, Priority.ALWAYS);

        // Header (Name + Price + Close)
        HBox headerRow = new HBox(8);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Label lblName = new Label(item.getName());
        lblName.getStyleClass().add("modal-dialog-title");

        lblPrice.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #A66A42;");
        titleBox.getChildren().addAll(lblName, lblPrice);

        Button btnClose = new Button("✕");
        btnClose.getStyleClass().add("btn-delete-item");
        btnClose.setOnAction(e -> close());

        headerRow.getChildren().addAll(titleBox, btnClose);

        if (item.getDescription() != null && !item.getDescription().trim().isEmpty()) {
            Label lblDesc = new Label(item.getDescription().trim());
            lblDesc.getStyleClass().add("editorial-subtitle");
            lblDesc.setWrapText(true);
            titleBox.getChildren().add(lblDesc);
        }

        // Scrollable Options Area
        VBox optionsList = new VBox(12);
        optionsList.setPadding(new Insets(4, 8, 8, 0));

        // Size Selector
        if (item.isAllowSize()) {
            VBox sizeSection = new VBox(6);
            Label lblSizeTitle = new Label("CHỌN KÍCH CỠ (SIZE)");
            lblSizeTitle.getStyleClass().add("modal-section-title");

            HBox sizeButtons = new HBox(8);
            ToggleGroup sizeGroup = new ToggleGroup();
            for (SizeItem s : MenuRepository.getSizes()) {
                String labelText = s.getDisplayName() + " (" + s.getSizeCode() + ")";
                if (s.getPriceDelta() > 0) {
                    labelText += " +" + CurrencyFormatter.format(s.getPriceDelta());
                }
                ToggleButton tb = new ToggleButton(labelText);
                tb.getStyleClass().add("btn-option-tag");
                tb.setToggleGroup(sizeGroup);
                if (s.getSizeCode().equalsIgnoreCase(selectedSize.getSizeCode())) {
                    tb.setSelected(true);
                }
                tb.setOnAction(e -> {
                    selectedSize = s;
                    updateTotalPrice();
                });
                sizeButtons.getChildren().add(tb);
            }
            sizeSection.getChildren().addAll(lblSizeTitle, sizeButtons);
            optionsList.getChildren().add(sizeSection);
        }

        // Temperature (Nóng / Đá)
        if (item.isHotAvailable()) {
            VBox tempSection = new VBox(6);
            Label lblTempTitle = new Label("NHIỆT ĐỘ");
            lblTempTitle.getStyleClass().add("modal-section-title");

            HBox tempButtons = new HBox(8);
            ToggleGroup tempGroup = new ToggleGroup();

            ToggleButton tbCold = new ToggleButton("🧊 Đá (Cold)");
            tbCold.getStyleClass().add("btn-option-tag");
            tbCold.setToggleGroup(tempGroup);
            tbCold.setSelected(true);
            tbCold.setOnAction(e -> selectedTemperature = "COLD");

            ToggleButton tbHot = new ToggleButton("☕ Nóng (Hot)");
            tbHot.getStyleClass().add("btn-option-tag");
            tbHot.setToggleGroup(tempGroup);
            tbHot.setOnAction(e -> selectedTemperature = "HOT");

            tempButtons.getChildren().addAll(tbCold, tbHot);
            tempSection.getChildren().addAll(lblTempTitle, tempButtons);
            optionsList.getChildren().add(tempSection);
        }

        // Sugar & Ice percentage (Drinks only)
        if (!item.getCategory().equalsIgnoreCase("Món ăn nhẹ")) {
            VBox levelSection = new VBox(6);
            Label lblLevelTitle = new Label("MỨC ĐƯỜNG & ĐÁ");
            lblLevelTitle.getStyleClass().add("modal-section-title");

            HBox sugarBox = new HBox(6);
            sugarBox.setAlignment(Pos.CENTER_LEFT);
            Label lblSugar = new Label("Đường:");
            lblSugar.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #817B70; -fx-min-width: 50px;");
            ToggleGroup sugarGroup = new ToggleGroup();
            int[] sugarLevels = {100, 70, 50, 30, 0};
            for (int lvl : sugarLevels) {
                ToggleButton tb = new ToggleButton(lvl + "%");
                tb.getStyleClass().add("btn-option-tag");
                tb.setToggleGroup(sugarGroup);
                if (lvl == 100) tb.setSelected(true);
                tb.setOnAction(e -> selectedSugar = lvl);
                sugarBox.getChildren().add(tb);
            }

            HBox iceBox = new HBox(6);
            iceBox.setAlignment(Pos.CENTER_LEFT);
            Label lblIce = new Label("Đá:");
            lblIce.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #817B70; -fx-min-width: 50px;");
            ToggleGroup iceGroup = new ToggleGroup();
            int[] iceLevels = {100, 70, 50, 0};
            for (int lvl : iceLevels) {
                ToggleButton tb = new ToggleButton(lvl == 0 ? "Không đá" : lvl + "%");
                tb.getStyleClass().add("btn-option-tag");
                tb.setToggleGroup(iceGroup);
                if (lvl == 100) tb.setSelected(true);
                tb.setOnAction(e -> selectedIce = lvl);
                iceBox.getChildren().add(tb);
            }

            levelSection.getChildren().addAll(lblLevelTitle, sugarBox, iceBox);
            optionsList.getChildren().add(levelSection);
        }

        // Toppings Checkboxes
        List<ToppingItem> availableToppings = MenuRepository.getToppingsForCategory(item.getCategory());
        if (availableToppings.isEmpty()) {
            availableToppings = MenuRepository.getAllToppings();
        }

        if (!availableToppings.isEmpty() && !item.getCategory().equalsIgnoreCase("Món ăn nhẹ")) {
            VBox toppingSection = new VBox(6);
            Label lblToppingTitle = new Label("TOPPING THÊM");
            lblToppingTitle.getStyleClass().add("modal-section-title");

            FlowPane toppingPane = new FlowPane(8, 8);
            for (ToppingItem t : availableToppings) {
                CheckBox cb = new CheckBox(t.getName() + " (+" + CurrencyFormatter.format(t.getPrice()) + ")");
                cb.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #292722; -fx-cursor: hand;");
                cb.setOnAction(e -> {
                    if (cb.isSelected()) {
                        selectedToppings.add(t);
                    } else {
                        selectedToppings.remove(t);
                    }
                    updateTotalPrice();
                });
                toppingPane.getChildren().add(cb);
            }
            toppingSection.getChildren().addAll(lblToppingTitle, toppingPane);
            optionsList.getChildren().add(toppingSection);
        }

        // Special Note Input
        VBox noteSection = new VBox(4);
        Label lblNote = new Label("GHI CHÚ ĐẶC BIỆT");
        lblNote.getStyleClass().add("modal-section-title");
        TextField txtNote = new TextField();
        txtNote.getStyleClass().add("text-input-field");
        txtNote.setPromptText("VD: Ít ngọt, dùng ly giấy, ít đá...");
        noteSection.getChildren().addAll(lblNote, txtNote);
        optionsList.getChildren().add(noteSection);

        ScrollPane scrollOptions = new ScrollPane(optionsList);
        scrollOptions.setFitToWidth(true);
        VBox.setVgrow(scrollOptions, Priority.ALWAYS);

        // Bottom Action Bar: Stepper [-] Qty [+] & Add Button
        HBox bottomBar = new HBox(12);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(10, 0, 0, 0));
        bottomBar.setStyle("-fx-border-color: #D8D1C3 transparent transparent transparent; -fx-border-width: 1 0 0 0;");

        HBox stepper = new HBox(4);
        stepper.setAlignment(Pos.CENTER_LEFT);

        Button btnMinus = new Button("-");
        btnMinus.getStyleClass().add("stepper-btn");
        Label lblQty = new Label(String.valueOf(quantity));
        lblQty.getStyleClass().add("stepper-value");
        Button btnPlus = new Button("+");
        btnPlus.getStyleClass().add("stepper-btn");

        btnMinus.setOnAction(e -> {
            if (quantity > 1) {
                quantity--;
                lblQty.setText(String.valueOf(quantity));
                updateTotalPrice();
            }
        });
        btnPlus.setOnAction(e -> {
            quantity++;
            lblQty.setText(String.valueOf(quantity));
            updateTotalPrice();
        });

        stepper.getChildren().addAll(btnMinus, lblQty, btnPlus);

        btnConfirm.getStyleClass().add("btn-primary");
        btnConfirm.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnConfirm, Priority.ALWAYS);
        btnConfirm.setOnAction(e -> {
            double extra = (selectedSize != null ? selectedSize.getPriceDelta() : 0.0);
            for (ToppingItem t : selectedToppings) {
                extra += t.getPrice();
            }

            OrderItem orderItem = new OrderItem(
                item.getId(),
                item.getName(),
                quantity,
                item.getPrice(),
                selectedSize != null ? selectedSize.getSizeCode() : "M",
                extra,
                selectedSugar,
                selectedIce,
                selectedTemperature,
                new ArrayList<>(selectedToppings),
                txtNote.getText().trim()
            );

            if (onConfirmed != null) {
                onConfirmed.accept(orderItem);
            }
            close();
        });

        bottomBar.getChildren().addAll(stepper, btnConfirm);

        rightCol.getChildren().addAll(headerRow, scrollOptions, bottomBar);

        root.getChildren().addAll(leftCol, rightCol);

        Scene scene = new Scene(root);
        try {
            scene.getStylesheets().add(getClass().getResource("/cafe/resources/css/cafe-theme.css").toExternalForm());
        } catch (Exception ignored) {}

        setScene(scene);
        updateTotalPrice();
    }

    private void updateTotalPrice() {
        double unitPrice = item.getPrice();
        if (selectedSize != null) {
            unitPrice += selectedSize.getPriceDelta();
        }
        for (ToppingItem t : selectedToppings) {
            unitPrice += t.getPrice();
        }
        double total = unitPrice * quantity;

        lblPrice.setText(CurrencyFormatter.format(unitPrice));
        btnConfirm.setText("Thêm vào đơn hàng • " + CurrencyFormatter.format(total));
    }
}
