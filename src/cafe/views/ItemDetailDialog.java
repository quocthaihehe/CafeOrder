package cafe.views;

import cafe.models.MenuItem;
import cafe.models.MenuRepository;
import cafe.models.OrderItem;
import cafe.models.SizeItem;
import cafe.models.ToppingItem;
import cafe.utils.AnimationHelper;
import cafe.utils.CurrencyFormatter;
import cafe.utils.SvgIcons;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * ItemDetailDialog - Modal Chi tiết món (Screen 4 trong UI_mong_muon.png):
 * - Bố cục ngang (Horizontal Split): Ảnh lớn bên trái (38%), Tùy chọn bên phải (62%)
 * - Tiêu đề Playfair Display 20px, Giá Inter Bold #563521
 * - Bộ chọn Size dạng Segmented Buttons: [ S ] [ M ] [ L ] (Nút M đang chọn #112823)
 * - Tùy chọn thêm: Checkbox thanh lịch (Thêm shot, Ít đường, Không đường, Sữa hạnh nhân)
 * - Cụm [-] 1 [+] và nút CTA "Thêm vào giỏ hàng" #112823
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
    private final boolean isEditMode;
    private final OrderItem existingOrderItem;

    private final Button btnConfirm = new Button("Thêm vào giỏ hàng");
    private final Label lblPrice = new Label();
    private final Label lblQty = new Label("1");

    public ItemDetailDialog(Stage parentStage, MenuItem item, Consumer<OrderItem> onConfirmed) {
        this(parentStage, item, null, onConfirmed);
    }

    public ItemDetailDialog(Stage parentStage, MenuItem item, OrderItem existingOrderItem, Consumer<OrderItem> onConfirmed) {
        this.item = item;
        this.existingOrderItem = existingOrderItem;
        this.isEditMode = (existingOrderItem != null);
        this.onConfirmed = onConfirmed;

        List<SizeItem> sizes = MenuRepository.getSizes();
        if (item.isAllowSize()) {
            this.selectedSize = sizes.size() > 1 ? sizes.get(1) : (sizes.isEmpty() ? new SizeItem("M", "Vừa (400ml)", 400, 0) : sizes.get(0));
            if (isEditMode && existingOrderItem.getSizeCode() != null) {
                for (SizeItem s : sizes) {
                    if (s.getSizeCode().equalsIgnoreCase(existingOrderItem.getSizeCode())) {
                        this.selectedSize = s;
                        break;
                    }
                }
            }
        } else {
            this.selectedSize = null;
        }

        if (isEditMode) {
            this.quantity = Math.max(1, existingOrderItem.getQty());
            this.lblQty.setText(String.valueOf(this.quantity));
            this.selectedSugar = existingOrderItem.getSugarPercent();
            this.selectedIce = existingOrderItem.getIcePercent();
            if (existingOrderItem.getTemperature() != null) {
                this.selectedTemperature = existingOrderItem.getTemperature();
            }
        }

        initOwner(parentStage);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.TRANSPARENT);
        setTitle((isEditMode ? "Đổi Tùy Chọn • " : "Chi Tiết Món • ") + item.getName());
        setResizable(false);

        initUI();
    }

    private void initUI() {
        HBox root = new HBox(24);
        root.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16px; -fx-border-color: #E2DDD5; -fx-border-radius: 16px; -fx-border-width: 1px; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.22), 28, 0.12, 0, 8);");
        root.setPadding(new Insets(20, 24, 20, 24));
        root.setPrefWidth(840);
        root.setPrefHeight(560);

        // Entrance animation
        AnimationHelper.playDialogEntrance(root);

        // =====================================================================
        // 1. CỘT TRÁI (35%): ẢNH LỚN DẠNG DỌC CẬN CẢNH (RADIUS 12PX)
        // =====================================================================
        VBox leftCol = new VBox();
        leftCol.setAlignment(Pos.CENTER);
        leftCol.setPrefWidth(290);
        leftCol.setMinWidth(290);
        leftCol.setMaxWidth(300);

        ProductImagePane largeImg = new ProductImagePane(item, 290, 485, 12);
        leftCol.getChildren().add(largeImg);

        // =====================================================================
        // 2. CỘT PHẢI (65%): NỘI DUNG TÙY BIẾN THOÁNG ĐÃNG
        // =====================================================================
        VBox rightCol = new VBox(12);
        HBox.setHgrow(rightCol, Priority.ALWAYS);

        // Header: Drag handle & Close Button [X]
        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_RIGHT);
        topRow.setStyle("-fx-cursor: move; -fx-padding: 0 0 2 0;");
        cafe.utils.WindowChromeHelper.makeDraggable(this, topRow);

        Region dragFiller = new Region();
        HBox.setHgrow(dragFiller, Priority.ALWAYS);

        Button btnClose = new Button();
        btnClose.setGraphic(cafe.utils.IconHelper.getIcon("client", "dong-cua-so.png", SvgIcons.CLOSE, "✕", 13, Color.web("#563521")));
        btnClose.setStyle("-fx-background-color: #F5EFEB; -fx-background-radius: 15px; -fx-min-width: 30px; -fx-min-height: 30px; -fx-pref-width: 30px; -fx-pref-height: 30px; -fx-padding: 0; -fx-cursor: hand; -fx-alignment: center;");
        btnClose.setOnMouseEntered(e -> btnClose.setStyle("-fx-background-color: #EAE4DA; -fx-background-radius: 15px; -fx-min-width: 30px; -fx-min-height: 30px; -fx-pref-width: 30px; -fx-pref-height: 30px; -fx-padding: 0; -fx-cursor: hand; -fx-alignment: center;"));
        btnClose.setOnMouseExited(e -> btnClose.setStyle("-fx-background-color: #F5EFEB; -fx-background-radius: 15px; -fx-min-width: 30px; -fx-min-height: 30px; -fx-pref-width: 30px; -fx-pref-height: 30px; -fx-padding: 0; -fx-cursor: hand; -fx-alignment: center;"));
        btnClose.setOnMousePressed(javafx.event.Event::consume);
        btnClose.setOnAction(e -> close());
        topRow.getChildren().addAll(dragFiller, btnClose);

        // Tên món (Playfair Display) & Giá
        Label lblName = new Label(item.getName());
        lblName.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 24px; -fx-font-weight: 800; -fx-text-fill: #112823;");

        lblPrice.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #563521;");

        // Mô tả món
        Label lblDesc = new Label(item.getDescription() != null && !item.getDescription().isEmpty() ? item.getDescription() : "Cà phê espresso thượng hạng hòa quyện cùng sữa tươi tinh khiết, mang lại cảm giác thơm ngon, tinh tế.");
        lblDesc.setWrapText(true);
        lblDesc.setStyle("-fx-font-size: 13px; -fx-text-fill: #6E675F; -fx-line-spacing: 3px;");
        lblDesc.setMaxHeight(50);

        // Scrollable Options Area
        VBox optionsList = new VBox(14);
        optionsList.setPadding(new Insets(4, 6, 6, 2));

        // 1. Size Selector: [ S ]  [ M ]  [ L ] (Chỉ hiện với món hỗ trợ chọn Size)
        if (item.isAllowSize() && selectedSize != null) {
            VBox sizeSection = new VBox(6);
            Label lblSizeTitle = new Label("Kích cỡ (Size)");
            lblSizeTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #112823;");

            HBox sizeButtons = new HBox(10);
            List<SizeItem> sizes = MenuRepository.getSizes();
            List<Button> sizeBtnList = new ArrayList<>();

            for (SizeItem s : sizes) {
                Button btnSize = new Button(s.getSizeCode() + (s.getPriceDelta() > 0 ? " (+" + CurrencyFormatter.format(s.getPriceDelta()) + ")" : ""));
                btnSize.setPrefHeight(34);
                btnSize.setMinWidth(64);
                AnimationHelper.applyButtonPress(btnSize);

                boolean isCurrent = s.getSizeCode().equalsIgnoreCase(selectedSize.getSizeCode());
                applySizeBtnStyle(btnSize, isCurrent);

                btnSize.setOnAction(e -> {
                    selectedSize = s;
                    for (int i = 0; i < sizes.size(); i++) {
                        applySizeBtnStyle(sizeBtnList.get(i), sizes.get(i).getSizeCode().equalsIgnoreCase(selectedSize.getSizeCode()));
                    }
                    updateTotalPrice();
                });

                sizeBtnList.add(btnSize);
                sizeButtons.getChildren().add(btnSize);
            }
            sizeSection.getChildren().addAll(lblSizeTitle, sizeButtons);
            optionsList.getChildren().add(sizeSection);
        }

        // 2. Độ ngọt (Sugar) & Lượng đá (Ice) cho đồ uống
        boolean isDrink = item.getCategory() == null ||
            (!item.getCategory().equalsIgnoreCase("BÁNH NGỌT") &&
             !item.getCategory().equalsIgnoreCase("ĐỒ ĂN VẶT") &&
             !item.getCategory().equalsIgnoreCase("MÓN ĂN NHẸ"));

        if (isDrink) {
            // Sweetness pills
            VBox sweetSection = new VBox(6);
            Label lblSweetTitle = new Label("Mức đường");
            lblSweetTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #112823;");

            HBox sweetRow = new HBox(10);
            sweetRow.setAlignment(Pos.CENTER_LEFT);
            int[] sweetLevels = {100, 70, 50, 0};
            String[] sweetNames = {"100% Đường", "70% Đường", "50% Đường", "Không đường"};
            List<Button> sweetBtns = new ArrayList<>();
            for (int i = 0; i < sweetLevels.length; i++) {
                final int lvl = sweetLevels[i];
                Button b = new Button(sweetNames[i]);
                b.setPrefHeight(34);
                b.setMinWidth(Region.USE_PREF_SIZE);
                applySizeBtnStyle(b, lvl == selectedSugar);
                b.setOnAction(e -> {
                    selectedSugar = lvl;
                    for (int j = 0; j < sweetLevels.length; j++) {
                        applySizeBtnStyle(sweetBtns.get(j), sweetLevels[j] == selectedSugar);
                    }
                });
                sweetBtns.add(b);
                sweetRow.getChildren().add(b);
            }
            sweetSection.getChildren().addAll(lblSweetTitle, sweetRow);
            optionsList.getChildren().add(sweetSection);

            // Ice pills (Ẩn với các món phục vụ nóng đặc thù như Espresso, Cà phê Trứng, Chocolate Nóng)
            String nameLower = item.getName() != null ? item.getName().toLowerCase() : "";
            boolean isHotOnly = nameLower.contains("nóng") || nameLower.equals("espresso") || nameLower.contains("cà phê trứng");
            if (isHotOnly) {
                selectedIce = 0;
                selectedTemperature = "HOT";
            } else {
                VBox iceSection = new VBox(6);
                Label lblIceTitle = new Label("Lượng đá");
                lblIceTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #112823;");

                HBox iceRow = new HBox(10);
                iceRow.setAlignment(Pos.CENTER_LEFT);
                int[] iceLevels = {100, 50, 0};
                String[] iceNames = {"100% Đá", "50% Đá", "Không đá"};
                List<Button> iceBtns = new ArrayList<>();
                for (int i = 0; i < iceLevels.length; i++) {
                    final int lvl = iceLevels[i];
                    Button b = new Button(iceNames[i]);
                    b.setPrefHeight(34);
                    b.setMinWidth(Region.USE_PREF_SIZE);
                    applySizeBtnStyle(b, lvl == selectedIce);
                    b.setOnAction(e -> {
                        selectedIce = lvl;
                        for (int j = 0; j < iceLevels.length; j++) {
                            applySizeBtnStyle(iceBtns.get(j), iceLevels[j] == selectedIce);
                        }
                    });
                    iceBtns.add(b);
                    iceRow.getChildren().add(b);
                }
                iceSection.getChildren().addAll(lblIceTitle, iceRow);
                optionsList.getChildren().add(iceSection);
            }
        }

        // 3. Tùy chọn thêm (Toppings) theo từng món - KHÔNG tích mặc định khi thêm mới, chỉ giữ topping đã chọn khi sửa món
        List<ToppingItem> availableToppings = MenuRepository.getToppingsForItem(item);
        if (!availableToppings.isEmpty()) {
            VBox toppingSection = new VBox(6);
            Label lblToppingTitle = new Label("Topping & Tùy chọn thêm");
            lblToppingTitle.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #112823;");

            VBox toppingCards = new VBox(6);
            for (ToppingItem t : availableToppings) {
                boolean wasSelected = false;
                if (isEditMode && existingOrderItem != null && existingOrderItem.getToppings() != null) {
                    for (ToppingItem ext : existingOrderItem.getToppings()) {
                        if (ext.getName().equalsIgnoreCase(t.getName()) || (ext.getToppingId() > 0 && ext.getToppingId() == t.getToppingId())) {
                            wasSelected = true;
                            break;
                        }
                    }
                }
                Node toppingRow = createToppingOptionRow(t, wasSelected);
                toppingCards.getChildren().add(toppingRow);
            }

            toppingSection.getChildren().addAll(lblToppingTitle, toppingCards);
            optionsList.getChildren().add(toppingSection);
        } else {
            VBox noToppingCard = new VBox(4);
            noToppingCard.setPadding(new Insets(12, 14, 12, 14));
            noToppingCard.setStyle("-fx-background-color: #FAF7F2; -fx-background-radius: 8px; -fx-border-color: #E6E0D8; -fx-border-radius: 8px; -fx-border-width: 1px;");
            Label lblNoToppingTitle = new Label(isDrink ? "Thưởng thức nguyên bản" : "Phục vụ tiêu chuẩn tại quầy");
            lblNoToppingTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #563521;");
            Label lblNoToppingDesc = new Label(isDrink
                ? "Món này không kèm tùy chọn topping thêm để giữ trọn vẹn hương vị đặc trưng."
                : "Món dùng trực tiếp chuẩn định lượng bếp • Thời gian chuẩn bị dự kiến ~" + item.getPrepMinutes() + " phút.");
            lblNoToppingDesc.setWrapText(true);
            lblNoToppingDesc.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #6E675F;");
            noToppingCard.getChildren().addAll(lblNoToppingTitle, lblNoToppingDesc);
            optionsList.getChildren().add(noToppingCard);
        }

        ScrollPane scrollOptions = new ScrollPane(optionsList);
        scrollOptions.setFitToWidth(true);
        scrollOptions.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollOptions.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollOptions.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scrollOptions, Priority.ALWAYS);

        // Bottom Action Bar: Stepper [-] 1 [+] & Button "Thêm vào giỏ hàng"
        HBox bottomBar = new HBox(12);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(8, 0, 0, 0));

        // Stepper
        HBox stepper = new HBox();
        stepper.setAlignment(Pos.CENTER);
        stepper.setStyle("-fx-background-color: #FBF9F5; -fx-border-color: #DED7CD; -fx-border-radius: 6px; -fx-background-radius: 6px;");
        stepper.setPrefHeight(42);

        Button btnMinus = new Button();
        btnMinus.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 6 12 6 12;");
        btnMinus.setGraphic(cafe.utils.IconHelper.getIcon("client", "giam-sl.png", null, "-", 13, Color.web("#292722")));
        AnimationHelper.applyButtonPress(btnMinus);

        lblQty.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #112823; -fx-min-width: 28px; -fx-alignment: center;");

        Button btnPlus = new Button();
        btnPlus.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 6 12 6 12;");
        btnPlus.setGraphic(cafe.utils.IconHelper.getIcon("client", "tang-sl.png", null, "+", 13, Color.web("#292722")));
        AnimationHelper.applyButtonPress(btnPlus);

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

        // Primary Button "Thêm vào giỏ hàng" (#112823, cao 42px)
        btnConfirm.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-size: 14px; -fx-font-weight: 700; -fx-background-radius: 6px; -fx-pref-height: 42px; -fx-cursor: hand;");
        btnConfirm.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnConfirm, Priority.ALWAYS);
        AnimationHelper.applyButtonPress(btnConfirm);

        btnConfirm.setOnAction(e -> {
            double sizeDelta = (selectedSize != null ? selectedSize.getPriceDelta() : 0.0);

            OrderItem orderItem = new OrderItem(
                item.getId(),
                item.getName(),
                quantity,
                item.getPrice(),
                selectedSize != null ? selectedSize.getSizeCode() : "",
                sizeDelta,
                selectedSugar,
                selectedIce,
                selectedTemperature,
                new ArrayList<>(selectedToppings),
                isEditMode && existingOrderItem != null ? existingOrderItem.getNote() : ""
            );

            if (onConfirmed != null) {
                onConfirmed.accept(orderItem);
            }
            close();
        });

        bottomBar.getChildren().addAll(stepper, btnConfirm);

        rightCol.getChildren().addAll(topRow, lblName, lblPrice, lblDesc, scrollOptions, bottomBar);

        root.getChildren().addAll(leftCol, rightCol);

        StackPane sceneRoot = new StackPane(root);
        sceneRoot.setStyle("-fx-background-color: transparent;");
        sceneRoot.setPadding(new Insets(16));

        Scene scene = new Scene(sceneRoot);
        scene.setFill(Color.TRANSPARENT);
        cafe.utils.WindowChromeHelper.applyTheme(scene);

        setScene(scene);
        updateTotalPrice();
    }

    private Node createToppingOptionRow(ToppingItem t, boolean initialSelected) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 14, 10, 14));
        row.setCursor(javafx.scene.Cursor.HAND);
        AnimationHelper.applyButtonPress(row);

        StackPane checkIndicator = new StackPane();
        checkIndicator.setMinSize(20, 20);
        checkIndicator.setPrefSize(20, 20);
        checkIndicator.setMaxSize(20, 20);

        Label lblCheckmark = new Label("✓");
        lblCheckmark.setStyle("-fx-font-size: 11px; -fx-text-fill: #FFFFFF; -fx-font-weight: 800;");

        Label lblName = new Label(t.getName());
        lblName.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 13.5px; -fx-font-weight: 600; -fx-text-fill: #1C1917;");
        HBox.setHgrow(lblName, Priority.ALWAYS);

        Label lblPrice = new Label("+" + CurrencyFormatter.format(t.getPrice()));
        lblPrice.setStyle("-fx-background-color: #EFE9E0; -fx-text-fill: #563521; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 4px; -fx-padding: 3 8 3 8;");

        row.getChildren().addAll(checkIndicator, lblName, lblPrice);

        Runnable updateState = () -> {
            boolean isSel = selectedToppings.contains(t);
            if (isSel) {
                checkIndicator.setStyle("-fx-background-color: #112823; -fx-background-radius: 5px; -fx-border-color: #112823; -fx-border-radius: 5px; -fx-border-width: 1.5px;");
                checkIndicator.getChildren().setAll(lblCheckmark);
                row.setStyle("-fx-background-color: #F8F4EE; -fx-background-radius: 8px; -fx-border-color: #112823; -fx-border-radius: 8px; -fx-border-width: 1.5px;");
            } else {
                checkIndicator.setStyle("-fx-background-color: #FAF7F2; -fx-background-radius: 5px; -fx-border-color: #D2C9BD; -fx-border-radius: 5px; -fx-border-width: 1.5px;");
                checkIndicator.getChildren().clear();
                row.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 8px; -fx-border-color: #E6E0D8; -fx-border-radius: 8px; -fx-border-width: 1px;");
            }
        };

        if (initialSelected) {
            selectedToppings.add(t);
        }
        updateState.run();

        row.setOnMouseClicked(e -> {
            if (selectedToppings.contains(t)) {
                selectedToppings.remove(t);
            } else {
                selectedToppings.add(t);
            }
            updateState.run();
            updateTotalPrice();
        });

        return row;
    }

    private void applySizeBtnStyle(Button btn, boolean isSelected) {
        btn.setFocusTraversable(false);
        if (isSelected) {
            btn.setStyle(
                "-fx-background-color: #112823; " +
                "-fx-text-fill: #FFFFFF; " +
                "-fx-font-size: 13px; " +
                "-fx-font-weight: 700; " +
                "-fx-background-radius: 6px; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 6 12 6 12; " +
                "-fx-border-color: transparent; " +
                "-fx-focus-color: transparent; " +
                "-fx-faint-focus-color: transparent;"
            );
        } else {
            btn.setStyle(
                "-fx-background-color: #FBF9F5; " +
                "-fx-text-fill: #292722; " +
                "-fx-font-size: 13px; " +
                "-fx-font-weight: 600; " +
                "-fx-background-radius: 6px; " +
                "-fx-border-color: #DED7CD; " +
                "-fx-border-radius: 6px; " +
                "-fx-border-width: 1px; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 6 12 6 12; " +
                "-fx-focus-color: transparent; " +
                "-fx-faint-focus-color: transparent;"
            );
        }
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
        String actionPrefix = isEditMode ? "Cập nhật tùy chọn • " : "Thêm vào giỏ hàng • ";
        btnConfirm.setText(actionPrefix + CurrencyFormatter.format(total));
    }
}
