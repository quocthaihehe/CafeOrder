package cafe.views;

import cafe.models.*;
import cafe.models.MenuItem;
import cafe.utils.CurrencyFormatter;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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

    public ItemDetailDialog(Stage parentStage, MenuItem item, Consumer<OrderItem> onConfirmed) {
        this.item = item;
        this.onConfirmed = onConfirmed;

        // Default size is Medium (M)
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
        VBox root = new VBox(16);
        root.getStyleClass().add("modal-canvas");
        root.setPrefWidth(580);
        root.setMinWidth(560);

        // 1. Header: Image + Basic Info & Flavor Description
        HBox headerBox = new HBox(16);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setStyle("-fx-padding: 0 0 12 0; -fx-border-color: transparent transparent #EADBCE transparent; -fx-border-width: 0 0 1 0;");

        // Product thumbnail (Center-crop bo tròn 4 phía)
        ProductImagePane thumbImg = new ProductImagePane(item, 100, 100, 14);

        VBox infoBox = new VBox(4);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label lblCategory = new Label(item.getCategory().toUpperCase());
        lblCategory.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #A89A92; -fx-letter-spacing: 0.8px;");

        Label lblName = new Label(item.getName());
        lblName.getStyleClass().add("modal-title");
        lblName.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #2C1810;");

        if (item.getDescription() != null && !item.getDescription().trim().isEmpty()) {
            Label lblDesc = new Label(item.getDescription().trim());
            lblDesc.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #7D6E68; -fx-line-spacing: 2px;");
            lblDesc.setWrapText(true);
            infoBox.getChildren().addAll(lblCategory, lblName, lblDesc);
        } else {
            infoBox.getChildren().addAll(lblCategory, lblName);
        }

        Label lblPrice = new Label(CurrencyFormatter.format(item.getPrice()));
        lblPrice.getStyleClass().add("modal-price");
        lblPrice.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #C97A44; -fx-padding: 3 0 0 0;");
        infoBox.getChildren().add(lblPrice);

        headerBox.getChildren().addAll(thumbImg, infoBox);


        // Scrollable Options Content (Generous height)
        VBox optionsList = new VBox(16);
        optionsList.setPadding(new Insets(6, 4, 6, 4));

        // 2. Size Selection (If allowed)
        if (item.isAllowSize()) {
            VBox sizeBox = new VBox(8);
            Label lblSizeTitle = new Label("1. CHỌN KÍCH CỠ (SIZE):");
            lblSizeTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #5D4037; -fx-letter-spacing: 0.5px;");

            HBox sizeButtons = new HBox(10);
            ToggleGroup sizeGroup = new ToggleGroup();

            for (SizeItem s : MenuRepository.getSizes()) {
                ToggleButton tb = new ToggleButton(s.getLabel());
                tb.setToggleGroup(sizeGroup);
                tb.getStyleClass().add("option-toggle-btn");
                boolean isDefault = s.getSizeCode().equalsIgnoreCase("M");
                tb.setSelected(isDefault);
                applyOptionToggleStyle(tb, isDefault);

                tb.selectedProperty().addListener((obs, oldV, isSel) -> applyOptionToggleStyle(tb, isSel));

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

        // 3. Hot / Cold Selection (If hot is available)
        if (item.isHotAvailable()) {
            VBox tempBox = new VBox(8);
            Label lblTempTitle = new Label("2. NHIỆT ĐỘ THƯỞNG THỨC:");
            lblTempTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #5D4037; -fx-letter-spacing: 0.5px;");

            HBox tempButtons = new HBox(10);
            ToggleGroup tempGroup = new ToggleGroup();

            ToggleButton tbCold = new ToggleButton("Dùng Đá Lạnh");
            tbCold.setToggleGroup(tempGroup);
            tbCold.getStyleClass().add("option-toggle-btn");
            tbCold.setSelected(true);
            applyOptionToggleStyle(tbCold, true);
            tbCold.selectedProperty().addListener((obs, oldV, isSel) -> applyOptionToggleStyle(tbCold, isSel));
            tbCold.setOnAction(e -> {
                if (tbCold.isSelected()) {
                    selectedTemperature = "COLD";
                    updatePrice();
                } else if (tempGroup.getSelectedToggle() == null) {
                    tbCold.setSelected(true);
                }
            });

            ToggleButton tbHot = new ToggleButton("Uống Nóng");
            tbHot.setToggleGroup(tempGroup);
            tbHot.getStyleClass().add("option-toggle-btn");
            applyOptionToggleStyle(tbHot, false);
            tbHot.selectedProperty().addListener((obs, oldV, isSel) -> applyOptionToggleStyle(tbHot, isSel));
            tbHot.setOnAction(e -> {
                if (tbHot.isSelected()) {
                    selectedTemperature = "HOT";
                    updatePrice();
                } else if (tempGroup.getSelectedToggle() == null) {
                    tbHot.setSelected(true);
                }
            });

            tempButtons.getChildren().addAll(tbCold, tbHot);
            tempBox.getChildren().addAll(lblTempTitle, tempButtons);
            optionsList.getChildren().add(tempBox);
        }

        // 4. Sugar & Ice levels (Only for beverages, not for pastries/food)
        boolean isBeverage = !item.getCategory().equalsIgnoreCase("Bánh ngọt") && !item.getCategory().equalsIgnoreCase("Món ăn nhẹ");
        if (isBeverage) {
            HBox levelsRow = new HBox(24);

            // Sugar level
            VBox sugarBox = new VBox(8);
            Label lblSugarTitle = new Label("MỨC ĐƯỜNG:");
            lblSugarTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #5D4037; -fx-letter-spacing: 0.5px;");

            HBox sugarBtns = new HBox(6);
            ToggleGroup sugarGroup = new ToggleGroup();
            int[] sugars = {0, 30, 50, 70, 100};
            for (int s : sugars) {
                ToggleButton tb = new ToggleButton(s + "%");
                tb.setToggleGroup(sugarGroup);
                tb.getStyleClass().add("option-level-btn");
                boolean isDefault = (s == 100);
                tb.setSelected(isDefault);
                applyLevelToggleStyle(tb, isDefault);
                tb.selectedProperty().addListener((obs, oldV, isSel) -> applyLevelToggleStyle(tb, isSel));

                tb.setOnAction(e -> {
                    if (tb.isSelected()) {
                        selectedSugar = s;
                    } else if (sugarGroup.getSelectedToggle() == null) {
                        tb.setSelected(true);
                    }
                });
                sugarBtns.getChildren().add(tb);
            }
            sugarBox.getChildren().addAll(lblSugarTitle, sugarBtns);

            // Ice level
            VBox iceBox = new VBox(8);
            Label lblIceTitle = new Label("MỨC ĐÁ:");
            lblIceTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #5D4037; -fx-letter-spacing: 0.5px;");

            HBox iceBtns = new HBox(6);
            ToggleGroup iceGroup = new ToggleGroup();
            int[] ices = {0, 30, 50, 70, 100};
            for (int ic : ices) {
                ToggleButton tb = new ToggleButton(ic + "%");
                tb.setToggleGroup(iceGroup);
                tb.getStyleClass().add("option-level-btn");
                boolean isDefault = (ic == 100);
                tb.setSelected(isDefault);
                applyLevelToggleStyle(tb, isDefault);
                tb.selectedProperty().addListener((obs, oldV, isSel) -> applyLevelToggleStyle(tb, isSel));

                tb.setOnAction(e -> {
                    if (tb.isSelected()) {
                        selectedIce = ic;
                    } else if (iceGroup.getSelectedToggle() == null) {
                        tb.setSelected(true);
                    }
                });
                iceBtns.getChildren().add(tb);
            }
            iceBox.getChildren().addAll(lblIceTitle, iceBtns);

            levelsRow.getChildren().addAll(sugarBox, iceBox);
            optionsList.getChildren().add(levelsRow);
        }

        // 5. Toppings Selection
        List<ToppingItem> availableToppings = MenuRepository.getToppingsForCategory(item.getCategory());
        if (!availableToppings.isEmpty()) {
            VBox topBox = new VBox(8);
            Label lblTopTitle = new Label("TOPPING CHỌN THÊM:");
            lblTopTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #5D4037; -fx-letter-spacing: 0.5px;");

            FlowPane toppingsPane = new FlowPane(10, 10);
            for (ToppingItem t : availableToppings) {
                CheckBox cb = new CheckBox(t.getName() + " (" + t.getFormattedPrice() + ")");
                cb.getStyleClass().add("topping-chip");
                applyToppingStyle(cb, false);
                cb.selectedProperty().addListener((obs, oldV, isSel) -> applyToppingStyle(cb, isSel));

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

        // 6. Note Text Field
        VBox noteBox = new VBox(8);
        Label lblNoteTitle = new Label("GHI CHÚ ĐẶC BIỆT:");
        lblNoteTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #5D4037; -fx-letter-spacing: 0.5px;");
        TextArea txtNote = new TextArea();
        txtNote.getStyleClass().add("custom-textarea");
        txtNote.setPromptText("Ví dụ: Nhiều sữa đặc, mang về, để đá riêng...");
        txtNote.setPrefRowCount(2);
        txtNote.setWrapText(true);
        noteBox.getChildren().addAll(lblNoteTitle, txtNote);
        optionsList.getChildren().add(noteBox);

        // Scroll Content with comfortable height
        ScrollPane scrollContent = new ScrollPane(optionsList);
        scrollContent.setFitToWidth(true);
        scrollContent.setPrefHeight(420);
        scrollContent.setMinHeight(360);
        scrollContent.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        Separator sep = new Separator();

        // 7. Footer: Stepper & Confirm Button
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(6, 0, 0, 0));

        Label lblQtyLabel = new Label("Số lượng:");
        lblQtyLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #2C1810;");

        Button btnMinus = new Button("-");
        btnMinus.getStyleClass().add("stepper-btn");
        btnMinus.setPrefSize(36, 36);

        Label lblQtyValue = new Label("1");
        lblQtyValue.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-min-width: 32px; -fx-alignment: center;");

        Button btnPlus = new Button("+");
        btnPlus.getStyleClass().add("stepper-btn");
        btnPlus.setPrefSize(36, 36);

        btnMinus.setOnAction(e -> {
            if (quantity > 1) {
                quantity--;
                lblQtyValue.setText(String.valueOf(quantity));
                updatePrice();
            }
        });

        btnPlus.setOnAction(e -> {
            quantity++;
            lblQtyValue.setText(String.valueOf(quantity));
            updatePrice();
        });

        Region spacerFooter = new Region();
        HBox.setHgrow(spacerFooter, Priority.ALWAYS);

        Button btnCancel = new Button("Hủy");
        btnCancel.getStyleClass().add("btn-secondary");
        btnCancel.setStyle("-fx-padding: 11 22 11 22; -fx-font-size: 13.5px; -fx-font-weight: 700;");
        btnCancel.setOnAction(e -> close());

        btnConfirm.getStyleClass().add("btn-primary");
        btnConfirm.setStyle("-fx-background-color: linear-gradient(to right, #C97A44, #B86733); -fx-text-fill: white; -fx-font-weight: 800; -fx-font-size: 13.5px; -fx-padding: 11 22 11 22; -fx-background-radius: 12; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(201, 122, 68, 0.4), 10, 0, 0, 3);");
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

        footer.getChildren().addAll(lblQtyLabel, btnMinus, lblQtyValue, btnPlus, spacerFooter, btnCancel, btnConfirm);

        updatePrice();

        root.getChildren().addAll(headerBox, scrollContent, sep, footer);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("client.css").toExternalForm());
        setScene(scene);
        setWidth(600);
        setHeight(680);
    }

    private void applyOptionToggleStyle(ToggleButton tb, boolean isSelected) {
        if (isSelected) {
            tb.setStyle(
                "-fx-background-color: #C97A44; " +
                "-fx-text-fill: #FFFFFF; " +
                "-fx-font-size: 13px; " +
                "-fx-font-weight: 800; " +
                "-fx-border-color: #8D4925; " +
                "-fx-border-radius: 12; " +
                "-fx-background-radius: 12; " +
                "-fx-border-width: 1.5; " +
                "-fx-padding: 9 18 9 18; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(201, 122, 68, 0.45), 8, 0, 0, 2);"
            );
        } else {
            tb.setStyle(
                "-fx-background-color: #FFFFFF; " +
                "-fx-text-fill: #5D4037; " +
                "-fx-font-size: 13px; " +
                "-fx-font-weight: 600; " +
                "-fx-border-color: #DCCFC6; " +
                "-fx-border-radius: 12; " +
                "-fx-background-radius: 12; " +
                "-fx-border-width: 1.5; " +
                "-fx-padding: 9 18 9 18; " +
                "-fx-cursor: hand; " +
                "-fx-effect: none;"
            );
        }
    }

    private void applyLevelToggleStyle(ToggleButton tb, boolean isSelected) {
        if (isSelected) {
            tb.setStyle(
                "-fx-background-color: #3E2723; " +
                "-fx-text-fill: #FFFFFF; " +
                "-fx-font-size: 12.5px; " +
                "-fx-font-weight: 800; " +
                "-fx-border-color: #1A0D08; " +
                "-fx-border-radius: 10; " +
                "-fx-background-radius: 10; " +
                "-fx-border-width: 2; " +
                "-fx-pref-width: 56; " +
                "-fx-pref-height: 36; " +
                "-fx-cursor: hand; " +
                "-fx-alignment: center; " +
                "-fx-effect: dropshadow(gaussian, rgba(62, 39, 35, 0.45), 6, 0, 0, 2);"
            );
        } else {
            tb.setStyle(
                "-fx-background-color: #FFFFFF; " +
                "-fx-text-fill: #6D4C41; " +
                "-fx-font-size: 12.5px; " +
                "-fx-font-weight: 600; " +
                "-fx-border-color: #DCCFC6; " +
                "-fx-border-radius: 10; " +
                "-fx-background-radius: 10; " +
                "-fx-border-width: 1.5; " +
                "-fx-pref-width: 56; " +
                "-fx-pref-height: 36; " +
                "-fx-cursor: hand; " +
                "-fx-alignment: center; " +
                "-fx-effect: none;"
            );
        }
    }

    private void applyToppingStyle(CheckBox cb, boolean isSelected) {
        if (isSelected) {
            cb.setStyle(
                "-fx-background-color: #F8EDE5; " +
                "-fx-border-color: #C97A44; " +
                "-fx-border-radius: 10; " +
                "-fx-background-radius: 10; " +
                "-fx-border-width: 1.5; " +
                "-fx-text-fill: #8D4925; " +
                "-fx-font-size: 12px; " +
                "-fx-font-weight: 700; " +
                "-fx-padding: 7 14 7 10; " +
                "-fx-cursor: hand;"
            );
        } else {
            cb.setStyle(
                "-fx-background-color: #FFFFFF; " +
                "-fx-border-color: #E2D5CA; " +
                "-fx-border-radius: 10; " +
                "-fx-background-radius: 10; " +
                "-fx-border-width: 1.2; " +
                "-fx-text-fill: #2C1810; " +
                "-fx-font-size: 12px; " +
                "-fx-font-weight: 600; " +
                "-fx-padding: 7 14 7 10; " +
                "-fx-cursor: hand;"
            );
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
        double total = Math.max(0, unit * quantity);
        btnConfirm.setText("+ Thêm vào giỏ • " + CurrencyFormatter.format(total));
    }
}
