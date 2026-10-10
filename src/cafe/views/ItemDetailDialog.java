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

    private final Button btnConfirm = new Button("Thêm vào giỏ hàng");
    private final Label lblPrice = new Label();
    private final Label lblQty = new Label("1");

    public ItemDetailDialog(Stage parentStage, MenuItem item, Consumer<OrderItem> onConfirmed) {
        this.item = item;
        this.onConfirmed = onConfirmed;

        List<SizeItem> sizes = MenuRepository.getSizes();
        this.selectedSize = sizes.size() > 1 ? sizes.get(1) : (sizes.isEmpty() ? new SizeItem("M", "Vừa (400ml)", 400, 0) : sizes.get(0));

        initOwner(parentStage);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.UNDECORATED);
        setTitle("Chi Tiết Món • " + item.getName());
        setResizable(false);

        initUI();
    }

    private void initUI() {
        HBox root = new HBox(16);
        root.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12px; -fx-border-color: #E7E2DA; -fx-border-radius: 12px; -fx-border-width: 1px; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.18), 24, 0.2, 0, 8);");
        root.setPadding(new Insets(16));
        root.setPrefWidth(580);
        root.setPrefHeight(390);
        cafe.utils.WindowChromeHelper.makeDraggable(this, root);

        // Entrance animation
        AnimationHelper.playDialogEntrance(root);

        // =====================================================================
        // 1. CỘT TRÁI (38%): ẢNH LỚN CẬN CẢNH (RADIUS 6PX)
        // =====================================================================
        VBox leftCol = new VBox();
        leftCol.setAlignment(Pos.CENTER);
        leftCol.setPrefWidth(210);
        leftCol.setMinWidth(200);
        leftCol.setMaxWidth(220);

        ProductImagePane largeImg = new ProductImagePane(item, 210, 330, 6);
        leftCol.getChildren().add(largeImg);

        // =====================================================================
        // 2. CỘT PHẢI (62%): NỘI DUNG TÙY BIẾN CHUẨN SCREEN 4
        // =====================================================================
        VBox rightCol = new VBox(8);
        HBox.setHgrow(rightCol, Priority.ALWAYS);

        // Header: Close Button X ở góc phải
        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_RIGHT);

        Button btnClose = new Button("✕");
        btnClose.setStyle("-fx-background-color: transparent; -fx-text-fill: #817A70; -fx-font-size: 13px; -fx-cursor: hand; -fx-padding: 0;");
        btnClose.setOnAction(e -> close());
        topRow.getChildren().add(btnClose);

        // Tên món (Playfair Display) & Giá
        Label lblName = new Label(item.getName());
        lblName.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: #292722;");

        lblPrice.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #563521;");

        // Mô tả món
        Label lblDesc = new Label(item.getDescription() != null && !item.getDescription().isEmpty() ? item.getDescription() : "Cà phê espresso kết hợp cùng sữa tươi thượng hạng, tạo nên hương vị béo ngậy, êm dịu.");
        lblDesc.setWrapText(true);
        lblDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #817A70; -fx-line-spacing: 1.5px;");
        lblDesc.setMaxHeight(40);

        // Scrollable Options Area
        VBox optionsList = new VBox(10);
        optionsList.setPadding(new Insets(2, 4, 4, 0));

        // 1. Size Selector: [ S ]  [ M ]  [ L ]
        VBox sizeSection = new VBox(4);
        Label lblSizeTitle = new Label("Size");
        lblSizeTitle.setStyle("-fx-font-size: 10.5px; -fx-font-weight: 700; -fx-text-fill: #292722;");

        HBox sizeButtons = new HBox(6);
        List<SizeItem> sizes = MenuRepository.getSizes();
        List<Button> sizeBtnList = new ArrayList<>();

        for (SizeItem s : sizes) {
            Button btnSize = new Button(s.getSizeCode());
            btnSize.setPrefWidth(42);
            btnSize.setPrefHeight(28);
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

        // 2. Checklist Tùy chọn thêm (Toppings / Options)
        VBox toppingSection = new VBox(4);
        Label lblToppingTitle = new Label("Tùy chọn thêm");
        lblToppingTitle.setStyle("-fx-font-size: 10.5px; -fx-font-weight: 700; -fx-text-fill: #292722;");

        VBox toppingChecks = new VBox(4);
        List<ToppingItem> availableToppings = MenuRepository.getToppingsForCategory(item.getCategory());
        if (availableToppings.isEmpty()) {
            availableToppings = MenuRepository.getAllToppings();
        }

        for (int i = 0; i < Math.min(4, availableToppings.size()); i++) {
            ToppingItem t = availableToppings.get(i);
            CheckBox cb = new CheckBox(t.getName() + " (+" + CurrencyFormatter.format(t.getPrice()) + ")");
            cb.setStyle("-fx-font-size: 11px; -fx-text-fill: #292722; -fx-cursor: hand;");
            if (i == 0) {
                cb.setSelected(true);
                selectedToppings.add(t);
            }
            cb.setOnAction(e -> {
                if (cb.isSelected()) {
                    selectedToppings.add(t);
                } else {
                    selectedToppings.remove(t);
                }
                updateTotalPrice();
            });
            toppingChecks.getChildren().add(cb);
        }

        toppingSection.getChildren().addAll(lblToppingTitle, toppingChecks);
        optionsList.getChildren().add(toppingSection);

        ScrollPane scrollOptions = new ScrollPane(optionsList);
        scrollOptions.setFitToWidth(true);
        scrollOptions.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scrollOptions, Priority.ALWAYS);

        // Bottom Action Bar: Stepper [-] 1 [+] & Button "Thêm vào giỏ hàng"
        HBox bottomBar = new HBox(8);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(6, 0, 0, 0));

        // Stepper chữ nhật bo góc 5px cao 28px
        HBox stepper = new HBox();
        stepper.setAlignment(Pos.CENTER);
        stepper.setStyle("-fx-background-color: #FBF9F5; -fx-border-color: #DED7CD; -fx-border-radius: 5px; -fx-background-radius: 5px;");
        stepper.setPrefHeight(28);

        Button btnMinus = new Button("-");
        btnMinus.setStyle("-fx-background-color: transparent; -fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #292722; -fx-cursor: hand; -fx-padding: 2 8 2 8;");
        AnimationHelper.applyButtonPress(btnMinus);

        lblQty.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #292722; -fx-min-width: 20px; -fx-alignment: center;");

        Button btnPlus = new Button("+");
        btnPlus.setStyle("-fx-background-color: transparent; -fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #292722; -fx-cursor: hand; -fx-padding: 2 8 2 8;");
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

        // Primary Button "Thêm vào giỏ hàng" (#112823, cao 32px, radius 5px)
        btnConfirm.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-size: 11.5px; -fx-font-weight: 600; -fx-background-radius: 5px; -fx-pref-height: 30px; -fx-cursor: hand;");
        btnConfirm.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(btnConfirm, Priority.ALWAYS);
        AnimationHelper.applyButtonPress(btnConfirm);

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
                ""
            );

            if (onConfirmed != null) {
                onConfirmed.accept(orderItem);
            }
            close();
        });

        bottomBar.getChildren().addAll(stepper, btnConfirm);

        rightCol.getChildren().addAll(topRow, lblName, lblPrice, lblDesc, scrollOptions, bottomBar);

        root.getChildren().addAll(leftCol, rightCol);

        Scene scene = new Scene(root);
        cafe.utils.WindowChromeHelper.applyTheme(scene);

        setScene(scene);
        updateTotalPrice();
    }

    private void applySizeBtnStyle(Button btn, boolean isSelected) {
        if (isSelected) {
            btn.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-size: 11px; -fx-font-weight: 600; -fx-background-radius: 5px; -fx-cursor: hand;");
        } else {
            btn.setStyle("-fx-background-color: #FBF9F5; -fx-text-fill: #292722; -fx-font-size: 11px; -fx-font-weight: 500; -fx-background-radius: 5px; -fx-border-color: #DED7CD; -fx-border-radius: 5px; -fx-border-width: 1px; -fx-cursor: hand;");
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
        btnConfirm.setText("Thêm vào giỏ hàng • " + CurrencyFormatter.format(total));
    }
}
