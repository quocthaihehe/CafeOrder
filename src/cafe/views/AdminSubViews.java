package cafe.views;

import cafe.models.CustomerFeedback;

import cafe.models.MenuItem;
import cafe.models.MenuRepository;
import cafe.models.Order;
import cafe.models.OrderItem;
import cafe.models.StaffModel;
import cafe.models.Voucher;
import cafe.services.CafeServerService;
import cafe.services.DatabaseManager;
import cafe.utils.AnimationHelper;
import cafe.utils.CurrencyFormatter;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.text.SimpleDateFormat;
import java.util.Date;
import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * AdminSubViews - Các phân hệ Quản trị Chuyên sâu (Tabs 5, 6, 7, 8, 9, 10) của AdminShellView (Sprint 4.3 & 4.4):
 * - Tab 5: Thực đơn & Định lượng BOM (Master-Detail 2 cột: 69 món + Bảng công thức BOM & Giá vốn COGS + 86-ing).
 * - Tab 6: Tồn kho Nguyên liệu (Đồng bộ 25 nguyên vật liệu chuẩn từ setup_database.sql + Lọc nhóm + Modal Nhập kho).
 * - Tab 7: Khuyến mãi & Voucher (Quản lý mã giảm giá đồng bộ trực tiếp với Giỏ hàng Kiosk/Staff).
 * - Tab 8: Nhân sự & Phân quyền RBAC (Quản lý tài khoản nhân viên, thêm mới, đổi vai trò, khóa/mở khóa).
 * - Tab 9: Báo cáo Tài chính Động (Lọc theo Hôm nay / 7 ngày / Tháng này / Quý này + Xuất file CSV thực tế).
 * - Tab 10: Cài đặt Hệ thống 2 Cột (Thông tin thương hiệu, Mẫu phiếu in, Trạng thái Server/DB, Reset Seed Data & Đăng xuất).
 */
public class AdminSubViews {

    // =========================================================================
    // TAB 5: THỰC ĐƠN & ĐỊNH LƯỢNG BOM (2-COLUMN MASTER-DETAIL)
    // =========================================================================
    public static VBox buildMenuAndBomView(Stage parentStage, Consumer<String> toastCallback, Runnable onMenuUpdated) {
        VBox root = new VBox(14);
        VBox.setVgrow(root, Priority.ALWAYS);

        List<MenuItem> allItems = MenuRepository.getAllItems();
        final MenuItem[] selectedItemHolder = new MenuItem[]{allItems.isEmpty() ? null : allItems.get(0)};
        final String[] searchFilter = new String[]{""};
        final String[] catFilter = new String[]{"Tất cả"};

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox titleBox = new VBox(2);
        Label lblTitle = new Label("Quản Lý Thực Đơn & Định Lượng Pha Chế (BOM)");
        lblTitle.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #112823;");
        Label lblSub = new Label("Tổng cộng " + allItems.size() + " món  •  Nhấn vào từng món để xem công thức định lượng nguyên liệu, giá vốn (COGS) và bật/tắt trạng thái còn bán (86-ing)");
        lblSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #78716C;");
        titleBox.getChildren().addAll(lblTitle, lblSub);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        TextField txtSearch = new TextField();
        txtSearch.setPromptText("Tìm tên món...");
        txtSearch.setPrefWidth(200);
        txtSearch.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #DFD8CE; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 7 12 7 12; -fx-font-size: 12px;");

        ComboBox<String> cboCat = new ComboBox<>();
        cboCat.getItems().addAll(MenuRepository.getCategories());
        cboCat.getSelectionModel().selectFirst();
        cboCat.setPrefHeight(36);
        cboCat.setStyle(COMBO_STYLE);

        Button btnAddDish = new Button("+ Thêm món mới");
        btnAddDish.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 36px; -fx-padding: 7 16 7 16; -fx-cursor: hand;");

        header.getChildren().addAll(titleBox, sp, txtSearch, cboCat, btnAddDish);

        // Master-Detail 2-Column Body
        HBox body = new HBox(16);
        VBox.setVgrow(body, Priority.ALWAYS);

        // Left Column (63%): Menu Items Table
        VBox leftCard = new VBox(8);
        HBox.setHgrow(leftCard, Priority.ALWAYS);
        leftCard.setPadding(new Insets(14));
        leftCard.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12px; -fx-border-color: #E6E0D6; -fx-border-radius: 12px;");

        HBox tableHeader = new HBox(10);
        tableHeader.setPadding(new Insets(8, 12, 8, 12));
        tableHeader.setStyle("-fx-background-color: #F4EFE6; -fx-background-radius: 8px;");
        tableHeader.getChildren().addAll(
            createColHeader("MÃ", 48),
            createColHeader("TÊN MÓN TRÊN MENU", 190),
            createColHeader("DANH MỤC", 125),
            createColHeader("GIÁ BÁN", 95),
            createColHeader("TRẠNG THÁI (86)", 115),
            createColHeader("THAO TÁC", 90)
        );

        VBox rowsBox = new VBox(4);
        ScrollPane leftScroll = new ScrollPane(rowsBox);
        leftScroll.setFitToWidth(true);
        leftScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(leftScroll, Priority.ALWAYS);
        leftCard.getChildren().addAll(tableHeader, leftScroll);

        // Right Column (37%): BOM Recipe & Costing Inspector
        VBox rightCard = new VBox(12);
        rightCard.setPrefWidth(400);
        rightCard.setMinWidth(375);
        rightCard.setMaxWidth(425);
        rightCard.setPadding(new Insets(18));
        rightCard.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12px; -fx-border-color: #E6E0D6; -fx-border-radius: 12px;");

        final Runnable[] refreshRightBom = new Runnable[1];
        final Runnable[] refreshLeftRows = new Runnable[1];

        refreshRightBom[0] = () -> {
            rightCard.getChildren().clear();
            MenuItem sel = selectedItemHolder[0];
            if (sel == null) return;

            Label lblBomHeader = new Label("CÔNG THỨC ĐỊNH LƯỢNG (BOM)");
            lblBomHeader.setStyle("-fx-font-size: 10.5px; -fx-font-weight: 800; -fx-text-fill: #78716C; -fx-letter-spacing: 0.8px;");

            HBox itemSummary = new HBox(12);
            itemSummary.setAlignment(Pos.CENTER_LEFT);
            itemSummary.setPadding(new Insets(10, 12, 10, 12));
            itemSummary.setStyle("-fx-background-color: #F8F5F0; -fx-background-radius: 10px;");
            ProductImagePane thumb = new ProductImagePane(sel, 54, 8);
            thumb.setMinSize(54, 54);
            thumb.setMaxSize(54, 54);

            VBox namePriceCol = new VBox(3);
            Label lName = new Label(sel.getName());
            lName.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: #112823;");
            lName.setWrapText(true);
            Label lCatPrice = new Label(sel.getCategory() + "  •  Giá bán: " + CurrencyFormatter.formatVND(sel.getPrice()));
            lCatPrice.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #B45309;");
            namePriceCol.getChildren().addAll(lName, lCatPrice);
            itemSummary.getChildren().addAll(thumb, namePriceCol);

            // Quick Edit Selling Price Row
            HBox priceEditRow = new HBox(8);
            priceEditRow.setAlignment(Pos.CENTER_LEFT);
            Label lblEditPrice = new Label("Điều chỉnh giá bán (đ):");
            lblEditPrice.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #57534E;");
            TextField txtNewPrice = new TextField(String.valueOf((long) sel.getPrice()));
            txtNewPrice.setPrefWidth(105);
            txtNewPrice.setStyle("-fx-background-color: #FAF8F5; -fx-border-color: #D8CEBE; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 6 10 6 10; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-pref-height: 34px;");
            Button btnSavePrice = new Button("Lưu giá");
            btnSavePrice.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 34px; -fx-padding: 6 14 6 14; -fx-cursor: hand;");
            btnSavePrice.setOnAction(e -> {
                try {
                    double np = Double.parseDouble(txtNewPrice.getText().trim().replace(".", "").replace(",", ""));
                    if (np >= 5000) {
                        sel.setPrice(np);
                        refreshLeftRows[0].run();
                        refreshRightBom[0].run();
                        if (onMenuUpdated != null) onMenuUpdated.run();
                        if (toastCallback != null) toastCallback.accept("Đã cập nhật giá bán '" + sel.getName() + "' -> " + CurrencyFormatter.formatVND(np));
                    }
                } catch (NumberFormatException ignored) {}
            });
            priceEditRow.getChildren().addAll(lblEditPrice, txtNewPrice, btnSavePrice);

            // BOM Ingredients List
            List<DatabaseManager.RecipeEntry> recipes = DatabaseManager.getRecipesForProduct(sel);
            VBox bomList = new VBox(6);
            double totalCogs = 0;
            for (DatabaseManager.RecipeEntry r : recipes) {
                totalCogs += r.estimatedCost;
                HBox rRow = new HBox(8);
                rRow.setAlignment(Pos.CENTER_LEFT);
                rRow.setPadding(new Insets(8, 10, 8, 10));
                rRow.setStyle("-fx-background-color: #FAF8F5; -fx-background-radius: 8px; -fx-border-color: #EFEAE2; -fx-border-radius: 8px;");

                VBox rInfo = new VBox(2);
                HBox.setHgrow(rInfo, Priority.ALWAYS);
                Label lIng = new Label(r.ingredientName);
                lIng.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #1C1917;");
                Label lCost = new Label("Chi phí ước tính: " + CurrencyFormatter.formatVND(r.estimatedCost));
                lCost.setStyle("-fx-font-size: 11px; -fx-text-fill: #78716C;");
                rInfo.getChildren().addAll(lIng, lCost);

                Label lQty = new Label(String.format("%.0f %s", r.quantityNeeded, r.unit));
                lQty.setStyle("-fx-background-color: #EAE4DA; -fx-text-fill: #112823; -fx-font-size: 11.5px; -fx-font-weight: 800; -fx-background-radius: 6px; -fx-padding: 4 10 4 10;");

                rRow.getChildren().addAll(rInfo, lQty);
                bomList.getChildren().add(rRow);
            }

            ScrollPane bomScroll = new ScrollPane(bomList);
            bomScroll.setFitToWidth(true);
            bomScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
            VBox.setVgrow(bomScroll, Priority.ALWAYS);

            // Add ingredient to BOM inline
            HBox addBomRow = new HBox(6);
            addBomRow.setAlignment(Pos.CENTER_LEFT);
            ComboBox<String> cboIng = new ComboBox<>();
            for (DatabaseManager.IngredientItem ing : DatabaseManager.getAllIngredients()) {
                cboIng.getItems().add(ing.name + " (" + ing.unit + ")");
            }
            cboIng.getSelectionModel().selectFirst();
            cboIng.setPrefWidth(175);
            cboIng.setPrefHeight(36);
            cboIng.setStyle(COMBO_STYLE);

            TextField txtQty = new TextField("20");
            txtQty.setPrefWidth(60);
            txtQty.setPrefHeight(36);
            txtQty.setPromptText("ĐL");
            txtQty.setStyle("-fx-background-color: #FAF8F5; -fx-border-color: #D8CEBE; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-padding: 6 8 6 8;");

            Button btnAddBom = new Button("+ Thêm ĐL");
            btnAddBom.setStyle("-fx-background-color: #F5EFEB; -fx-text-fill: #112823; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 36px; -fx-padding: 6 12 6 12; -fx-cursor: hand;");
            btnAddBom.setOnMouseEntered(e -> btnAddBom.setStyle("-fx-background-color: #EAE4DA; -fx-text-fill: #112823; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 36px; -fx-padding: 6 12 6 12; -fx-cursor: hand;"));
            btnAddBom.setOnMouseExited(e -> btnAddBom.setStyle("-fx-background-color: #F5EFEB; -fx-text-fill: #112823; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 36px; -fx-padding: 6 12 6 12; -fx-cursor: hand;"));
            btnAddBom.setOnAction(e -> {
                String chosen = cboIng.getValue();
                if (chosen != null) {
                    try {
                        double q = Double.parseDouble(txtQty.getText().trim());
                        String cleanName = chosen.contains(" (") ? chosen.substring(0, chosen.indexOf(" (")) : chosen;
                        String unit = chosen.contains("(") && chosen.contains(")") ? chosen.substring(chosen.indexOf("(") + 1, chosen.indexOf(")")) : "gram";
                        recipes.add(new DatabaseManager.RecipeEntry(cleanName, q, unit, Math.max(500, q * 45)));
                        refreshRightBom[0].run();
                        if (toastCallback != null) toastCallback.accept("Đã thêm '" + cleanName + "' vào công thức món " + sel.getName());
                    } catch (NumberFormatException ignored) {}
                }
            });
            addBomRow.getChildren().addAll(cboIng, txtQty, btnAddBom);

            // Cost & Gross Margin Summary Card
            double profit = Math.max(0, sel.getPrice() - totalCogs);
            double marginPct = sel.getPrice() > 0 ? (profit / sel.getPrice()) * 100.0 : 0;

            VBox marginCard = new VBox(5);
            marginCard.setPadding(new Insets(12));
            marginCard.setStyle("-fx-background-color: #112823; -fx-background-radius: 10px;");
            Label lblCogs = new Label("Giá vốn nguyên liệu (COGS): " + CurrencyFormatter.formatVND(totalCogs));
            lblCogs.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #D6D3CD;");
            Label lblMargin = new Label(String.format("Lãi gộp: %s (Biên lợi nhuận: %.1f%%)", CurrencyFormatter.formatVND(profit), marginPct));
            lblMargin.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #E8C798;");
            marginCard.getChildren().addAll(lblCogs, lblMargin);

            rightCard.getChildren().addAll(lblBomHeader, itemSummary, priceEditRow, bomScroll, addBomRow, marginCard);
        };

        refreshLeftRows[0] = () -> {
            rowsBox.getChildren().clear();
            for (MenuItem item : MenuRepository.getAllItems()) {
                if (!"Tất cả".equals(catFilter[0]) && !catFilter[0].equalsIgnoreCase(item.getCategory())) {
                    continue;
                }
                if (!searchFilter[0].isEmpty() && !item.getName().toLowerCase().contains(searchFilter[0])) {
                    continue;
                }

                boolean isSelected = (selectedItemHolder[0] != null && selectedItemHolder[0].getId() == item.getId());
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(9, 12, 9, 12));
                row.setStyle(
                    "-fx-background-color: " + (isSelected ? "#EFE8DD" : "#FFFFFF") + "; " +
                    "-fx-background-radius: 8px; " +
                    "-fx-border-color: " + (isSelected ? "#112823" : "#F2ECE4") + "; " +
                    "-fx-border-radius: 8px; -fx-border-width: 1px; -fx-cursor: hand;"
                );

                Label lId = new Label("#" + item.getId());
                lId.setPrefWidth(48);
                lId.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #78716C;");

                Label lName = new Label(item.getName());
                lName.setPrefWidth(190);
                lName.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #112823;");

                Label lCat = new Label(item.getCategory());
                lCat.setPrefWidth(125);
                lCat.setStyle("-fx-font-size: 12px; -fx-text-fill: #57534E;");

                Label lPrice = new Label(CurrencyFormatter.formatVND(item.getPrice()));
                lPrice.setPrefWidth(95);
                lPrice.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 800; -fx-text-fill: #B45309;");

                boolean inStock = item.isInStock();
                Button btnStockToggle = new Button(inStock ? "● Còn bán" : "✕ Tạm hết (86)");
                btnStockToggle.setPrefWidth(115);
                btnStockToggle.setStyle(
                    "-fx-background-color: " + (inStock ? "#DCFCE7" : "#FEE2E2") + "; " +
                    "-fx-text-fill: " + (inStock ? "#15803D" : "#B91C1C") + "; " +
                    "-fx-font-family: 'Inter', sans-serif; -fx-font-size: 11.5px; -fx-font-weight: 800; -fx-background-radius: 8px; -fx-pref-height: 32px; -fx-padding: 4 10 4 10; -fx-cursor: hand;"
                );
                btnStockToggle.setOnAction(ev -> {
                    ev.consume();
                    item.setInStock(!inStock);
                    CafeServerService.getInstance().broadcastStockStatus(item.getId(), item.isInStock(), item.getName());
                    refreshLeftRows[0].run();
                    if (onMenuUpdated != null) onMenuUpdated.run();
                    if (toastCallback != null) {
                        toastCallback.accept(item.isInStock()
                            ? "Đã mở bán lại món '" + item.getName() + "' trên toàn bộ Kiosk!"
                            : "Đã tạm khóa (86-ing) món '" + item.getName() + "' do hết nguyên liệu!");
                    }
                });

                Button btnInspect = new Button("Định lượng →");
                btnInspect.setPrefWidth(95);
                btnInspect.setStyle("-fx-background-color: #F5EFEB; -fx-text-fill: #112823; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 11.5px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 32px; -fx-padding: 4 8 4 8; -fx-cursor: hand;");
                btnInspect.setOnMouseEntered(e -> btnInspect.setStyle("-fx-background-color: #EAE4DA; -fx-text-fill: #112823; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 11.5px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 32px; -fx-padding: 4 8 4 8; -fx-cursor: hand;"));
                btnInspect.setOnMouseExited(e -> btnInspect.setStyle("-fx-background-color: #F5EFEB; -fx-text-fill: #112823; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 11.5px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 32px; -fx-padding: 4 8 4 8; -fx-cursor: hand;"));
                btnInspect.setOnAction(ev -> {
                    selectedItemHolder[0] = item;
                    refreshLeftRows[0].run();
                    refreshRightBom[0].run();
                    showBomRecipeModal(parentStage, item, () -> {
                        refreshLeftRows[0].run();
                        refreshRightBom[0].run();
                        if (onMenuUpdated != null) onMenuUpdated.run();
                        if (toastCallback != null) toastCallback.accept("Đã lưu định lượng (BOM) thành công cho món '" + item.getName() + "'!");
                    });
                });

                row.setOnMouseClicked(ev -> {
                    selectedItemHolder[0] = item;
                    refreshLeftRows[0].run();
                    refreshRightBom[0].run();
                });

                row.getChildren().addAll(lId, lName, lCat, lPrice, btnStockToggle, btnInspect);
                rowsBox.getChildren().add(row);
            }
        };

        txtSearch.textProperty().addListener((obs, o, n) -> {
            searchFilter[0] = n != null ? n.trim().toLowerCase() : "";
            refreshLeftRows[0].run();
        });
        cboCat.setOnAction(e -> {
            catFilter[0] = cboCat.getValue() != null ? cboCat.getValue() : "Tất cả";
            refreshLeftRows[0].run();
        });

        btnAddDish.setOnAction(e -> showAddMenuItemModal(parentStage, () -> {
            refreshLeftRows[0].run();
            if (onMenuUpdated != null) onMenuUpdated.run();
            if (toastCallback != null) toastCallback.accept("Đã thêm món mới vào Thực đơn hệ thống!");
        }));

        refreshLeftRows[0].run();
        refreshRightBom[0].run();

        body.getChildren().addAll(leftCard, rightCard);
        root.getChildren().addAll(header, body);
        return root;
    }

    public static void showBomRecipeModal(Stage owner, MenuItem item, Runnable onUpdated) {
        Stage modal = createModal(owner, "Chi Tiết Định Lượng (BOM) - " + item.getName());
        VBox root = new VBox(14);
        root.setPadding(new Insets(22));
        root.setPrefWidth(580);
        root.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16px; -fx-border-color: #DFD7CA; -fx-border-radius: 16px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 24, 0.15, 0, 8);");

        Label lblTitle = new Label("🧪 Công Thức Định Lượng Nguyên Liệu (BOM)");
        lblTitle.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #112823;");

        // Item Header Banner
        HBox itemHeader = new HBox(12);
        itemHeader.setAlignment(Pos.CENTER_LEFT);
        itemHeader.setPadding(new Insets(10, 14, 10, 14));
        itemHeader.setStyle("-fx-background-color: #FAF8F5; -fx-background-radius: 10px; -fx-border-color: #EFEAE2; -fx-border-radius: 10px;");

        ProductImagePane thumb = new ProductImagePane(item, 56, 8);
        thumb.setMinSize(56, 56);
        thumb.setMaxSize(56, 56);

        VBox itemTexts = new VBox(2);
        HBox.setHgrow(itemTexts, Priority.ALWAYS);
        Label lName = new Label(item.getName());
        lName.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: #112823;");
        Label lCatPrice = new Label("Danh mục: " + item.getCategory() + "  •  Giá niêm yết: " + CurrencyFormatter.formatVND(item.getPrice()));
        lCatPrice.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #B45309;");
        itemTexts.getChildren().addAll(lName, lCatPrice);

        Label badgeStatus = new Label(item.isInStock() ? "● Đang bán" : "✕ Tạm hết (86)");
        badgeStatus.setStyle(
            "-fx-background-color: " + (item.isInStock() ? "#DCFCE7" : "#FEE2E2") + "; " +
            "-fx-text-fill: " + (item.isInStock() ? "#15803D" : "#B91C1C") + "; " +
            "-fx-font-size: 11px; -fx-font-weight: 800; -fx-background-radius: 6px; -fx-padding: 4 8 4 8;"
        );
        itemHeader.getChildren().addAll(thumb, itemTexts, badgeStatus);

        // Recipe Table
        List<DatabaseManager.RecipeEntry> currentRecipes = new ArrayList<>(DatabaseManager.getRecipesForProduct(item));
        VBox rowsBox = new VBox(6);
        rowsBox.setPadding(new Insets(4, 0, 4, 0));

        HBox tblHead = new HBox(8);
        tblHead.setPadding(new Insets(6, 10, 6, 10));
        tblHead.setStyle("-fx-background-color: #F4EFE6; -fx-background-radius: 6px;");
        Label hName = createColHeader("NGUYÊN LIỆU TIÊU HAO CHO 1 PHẦN", 210);
        HBox.setHgrow(hName, Priority.ALWAYS);
        hName.setMaxWidth(Double.MAX_VALUE);
        tblHead.getChildren().addAll(
            hName,
            createColHeader("ĐỊNH LƯỢNG", 100),
            createColHeader("CHI PHÍ", 110),
            createColHeader("", 40)
        );

        ScrollPane scroll = new ScrollPane(rowsBox);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(180);
        scroll.setMinHeight(140);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        // Financial Summary Box
        HBox summaryBox = new HBox(12);
        summaryBox.setAlignment(Pos.CENTER_LEFT);
        summaryBox.setPadding(new Insets(10, 14, 10, 14));
        summaryBox.setStyle("-fx-background-color: #F8F5F0; -fx-background-radius: 10px;");

        Label lblCogs = new Label();
        lblCogs.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #B45309;");
        Label lblProfit = new Label();
        lblProfit.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #15803D;");
        Label lblMargin = new Label();
        lblMargin.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #112823;");
        summaryBox.getChildren().addAll(lblCogs, new Label(" • "), lblProfit, new Label(" • "), lblMargin);

        final Runnable[] renderRows = new Runnable[1];
        renderRows[0] = () -> {
            rowsBox.getChildren().clear();
            double totalCost = 0;
            for (int i = 0; i < currentRecipes.size(); i++) {
                final int idx = i;
                DatabaseManager.RecipeEntry re = currentRecipes.get(i);
                totalCost += re.estimatedCost;

                HBox r = new HBox(8);
                r.setAlignment(Pos.CENTER_LEFT);
                r.setPadding(new Insets(6, 10, 6, 10));
                r.setStyle("-fx-background-color: #FAF8F5; -fx-background-radius: 6px; -fx-border-color: #EFEAE2; -fx-border-radius: 6px;");

                Label lIngName = new Label("☕ " + re.ingredientName);
                lIngName.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #1C1917;");
                HBox.setHgrow(lIngName, Priority.ALWAYS);
                lIngName.setMaxWidth(Double.MAX_VALUE);

                Label lIngQty = new Label(String.format("%.0f %s", re.quantityNeeded, re.unit));
                lIngQty.setPrefWidth(100);
                lIngQty.setStyle("-fx-background-color: #EAE4DA; -fx-text-fill: #112823; -fx-font-size: 11px; -fx-font-weight: 800; -fx-background-radius: 6px; -fx-padding: 3 8 3 8; -fx-alignment: center;");

                Label lIngCost = new Label(CurrencyFormatter.formatVND(re.estimatedCost));
                lIngCost.setPrefWidth(110);
                lIngCost.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #B45309; -fx-alignment: center-right;");

                Button btnDel = new Button("✕");
                btnDel.setPrefSize(28, 28);
                btnDel.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-font-size: 10px; -fx-font-weight: 800; -fx-background-radius: 6px; -fx-cursor: hand;");
                btnDel.setOnAction(e -> {
                    currentRecipes.remove(idx);
                    renderRows[0].run();
                });

                r.getChildren().addAll(lIngName, lIngQty, lIngCost, btnDel);
                rowsBox.getChildren().add(r);
            }

            double grossProfit = item.getPrice() - totalCost;
            double marginPct = item.getPrice() > 0 ? (grossProfit / item.getPrice()) * 100.0 : 0.0;
            lblCogs.setText("Giá vốn (COGS): " + CurrencyFormatter.formatVND(totalCost));
            lblProfit.setText("Lãi gộp: " + CurrencyFormatter.formatVND(grossProfit));
            lblMargin.setText(String.format("Tỷ suất lợi nhuận: %.1f%%", marginPct));
        };

        renderRows[0].run();

        // Add Ingredient Row
        HBox addRow = new HBox(8);
        addRow.setAlignment(Pos.CENTER_LEFT);

        ComboBox<DatabaseManager.IngredientItem> cboNew = new ComboBox<>();
        for (DatabaseManager.IngredientItem ing : DatabaseManager.getAllIngredients()) {
            cboNew.getItems().add(ing);
        }
        cboNew.getSelectionModel().selectFirst();
        cboNew.setPrefHeight(36);
        cboNew.setStyle(COMBO_STYLE);
        HBox.setHgrow(cboNew, Priority.ALWAYS);
        cboNew.setConverter(new javafx.util.StringConverter<DatabaseManager.IngredientItem>() {
            @Override
            public String toString(DatabaseManager.IngredientItem ing) {
                return ing == null ? "" : ing.name + " (" + ing.unit + " - " + CurrencyFormatter.formatVND(ing.unitCost) + ")";
            }
            @Override
            public DatabaseManager.IngredientItem fromString(String string) { return null; }
        });

        TextField txtNewQty = new TextField("20");
        txtNewQty.setPrefWidth(65);
        txtNewQty.setPrefHeight(36);
        txtNewQty.setStyle(INPUT_STYLE);

        Button btnAdd = new Button("+ Thêm nguyên liệu");
        btnAdd.setPrefHeight(36);
        btnAdd.setStyle("-fx-background-color: #F5EFEB; -fx-text-fill: #112823; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 6 14 6 14; -fx-cursor: hand;");
        btnAdd.setOnAction(e -> {
            DatabaseManager.IngredientItem selectedIng = cboNew.getValue();
            if (selectedIng != null) {
                try {
                    double q = Double.parseDouble(txtNewQty.getText().trim());
                    if (q > 0) {
                        double estCost = Math.round(selectedIng.unitCost * q);
                        currentRecipes.add(new DatabaseManager.RecipeEntry(selectedIng.name, q, selectedIng.unit, estCost));
                        renderRows[0].run();
                    }
                } catch (NumberFormatException ignored) {}
            }
        });
        addRow.getChildren().addAll(cboNew, txtNewQty, btnAdd);

        // Actions Bottom Row
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnCancel = new Button("Hủy bỏ");
        btnCancel.setStyle("-fx-background-color: #EAE4DA; -fx-text-fill: #1C1917; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 8 16 8 16; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> modal.close());

        Button btnSave = new Button("💾 LƯU CÔNG THỨC");
        btnSave.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 800; -fx-background-radius: 8px; -fx-padding: 8 20 8 20; -fx-cursor: hand;");
        btnSave.setOnAction(e -> {
            DatabaseManager.saveRecipeForProduct(item.getId(), currentRecipes);
            if (onUpdated != null) onUpdated.run();
            modal.close();
        });
        actions.getChildren().addAll(btnCancel, btnSave);

        root.getChildren().addAll(lblTitle, itemHeader, tblHead, scroll, summaryBox, addRow, actions);
        showModal(modal, root);
    }

    private static void showAddMenuItemModal(Stage owner, Runnable onAdded) {
        Stage modal = createModal(owner, "Thêm Món Mới Vào Thực Đơn");
        VBox root = new VBox(12);
        root.setPadding(new Insets(22));
        root.setPrefWidth(420);
        root.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16px; -fx-border-color: #DFD7CA; -fx-border-radius: 16px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 24, 0.15, 0, 8);");

        Label lblTitle = new Label("☕ Thêm Món Mới Vào Thực Đơn");
        lblTitle.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #112823;");

        TextField txtName = new TextField();
        txtName.setPromptText("Tên món mới (VD: Cold Brew Mơ Đào)...");

        ComboBox<String> cboCategory = new ComboBox<>();
        for (String c : MenuRepository.getCategories()) {
            if (!"Tất cả".equals(c)) cboCategory.getItems().add(c);
        }
        cboCategory.getSelectionModel().selectFirst();
        cboCategory.setMaxWidth(Double.MAX_VALUE);

        TextField txtPrice = new TextField("45000");
        txtPrice.setPromptText("Giá bán tiêu chuẩn (VNĐ)...");

        TextField txtDesc = new TextField("Hương vị đặc trưng thượng hạng của Brew & Bean.");

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button btnCancel = new Button("Hủy");
        btnCancel.setStyle("-fx-background-color: #EAE4DA; -fx-text-fill: #1C1917; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 8 16 8 16; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> modal.close());

        Button btnSave = new Button("Lưu Món Mới");
        btnSave.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 8 18 8 18; -fx-cursor: hand;");
        btnSave.setOnAction(e -> {
            String name = txtName.getText().trim();
            if (!name.isEmpty()) {
                double price = 45000;
                try {
                    price = Double.parseDouble(txtPrice.getText().trim());
                } catch (NumberFormatException ignored) {}
                MenuItem newItem = new MenuItem(0, name, price, cboCategory.getValue(), txtDesc.getText().trim(), "☕", true, true, false, true, 5);
                MenuRepository.addMenuItem(newItem);
                modal.close();
                if (onAdded != null) onAdded.run();
            }
        });
        actions.getChildren().addAll(btnCancel, btnSave);

        root.getChildren().addAll(
            lblTitle,
            new Label("Tên món:"), txtName,
            new Label("Danh mục:"), cboCategory,
            new Label("Giá bán (VNĐ):"), txtPrice,
            new Label("Mô tả hương vị:"), txtDesc,
            actions
        );
        showModal(modal, root);
    }

    // =========================================================================
    // TAB 6: TỒN KHO NGUYÊN LIỆU (25 NGUYÊN LIỆU CHUẨN + LỌC NHÓM + MODAL NHẬP KHO)
    // =========================================================================
    public static VBox buildInventoryView(Stage parentStage, Consumer<String> toastCallback) {
        VBox root = new VBox(14);
        VBox.setVgrow(root, Priority.ALWAYS);

        final String[] currentCategory = new String[]{"Tất cả"};
        final String[] searchKeyword = new String[]{""};

        // Top Header & Action Buttons
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox titleCol = new VBox(2);
        Label lblTitle = new Label("Quản Lý Tồn Kho Nguyên Vật Liệu (25 Mặt Hàng Chuẩn)");
        lblTitle.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #112823;");
        Label lblSub = new Label("Đồng bộ định lượng tự động (BOM Deduction) từ bảng Ingredients & Recipes trong SQL Server");
        lblSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #78716C;");
        titleCol.getChildren().addAll(lblTitle, lblSub);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        TextField txtSearch = new TextField();
        txtSearch.setPromptText("Tìm nguyên liệu hoặc nhà cung cấp...");
        txtSearch.setPrefWidth(240);
        txtSearch.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #DFD8CE; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 7 12 7 12; -fx-font-size: 12px;");

        Button btnRestockModal = new Button("+ Nhập kho nguyên liệu");
        btnRestockModal.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 36px; -fx-padding: 7 16 7 16; -fx-cursor: hand;");

        header.getChildren().addAll(titleCol, sp, txtSearch, btnRestockModal);

        // KPI Summary Row
        HBox kpiRow = new HBox(14);
        Label lblTotalCount = new Label();
        Label lblLowStockCount = new Label();
        Label lblTotalValue = new Label();
        kpiRow.getChildren().addAll(
            createMiniKpiCard("TỔNG DANH MỤC VẬT TƯ", lblTotalCount, "#112823"),
            createMiniKpiCard("CẢNH BÁO SẮP HẾT HÀNG", lblLowStockCount, "#B91C1C"),
            createMiniKpiCard("GIÁ TRỊ TỒN KHO ƯỚC TÍNH", lblTotalValue, "#B45309")
        );

        // Category Filter Chips
        HBox filterBar = new HBox(8);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        String[] cats = {"Tất cả", "Cảnh báo sắp hết", "Hạt & Trà", "Sữa & Kem", "Trái cây & Mứt", "Topping & Syrup", "Bao bì & Khác"};
        List<Button> chipBtns = new ArrayList<>();

        VBox rowsContainer = new VBox(6);
        final Runnable[] renderInventory = new Runnable[1];

        for (String cat : cats) {
            Button chip = new Button(cat);
            chip.setOnAction(e -> {
                currentCategory[0] = cat;
                for (Button b : chipBtns) {
                    boolean active = b.getText().equals(cat);
                    b.setStyle(active
                        ? "-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 34px; -fx-padding: 6 14 6 14; -fx-cursor: hand;"
                        : "-fx-background-color: #F5EFEB; -fx-text-fill: #57534E; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 600; -fx-background-radius: 8px; -fx-pref-height: 34px; -fx-padding: 6 14 6 14; -fx-cursor: hand;");
                }
                renderInventory[0].run();
            });
            chipBtns.add(chip);
            filterBar.getChildren().add(chip);
        }

        // Table Card
        VBox tableCard = new VBox(8);
        VBox.setVgrow(tableCard, Priority.ALWAYS);
        tableCard.setPadding(new Insets(14));
        tableCard.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12px; -fx-border-color: #E6E0D6; -fx-border-radius: 12px;");

        HBox tableHeader = new HBox(10);
        tableHeader.setPadding(new Insets(8, 12, 8, 12));
        tableHeader.setStyle("-fx-background-color: #F4EFE6; -fx-background-radius: 8px;");
        Label hName = createColHeader("TÊN NGUYÊN VẬT LIỆU", 195);
        HBox.setHgrow(hName, Priority.ALWAYS);
        hName.setMaxWidth(Double.MAX_VALUE);

        Label hSup = createColHeader("NHÀ CUNG CẤP", 145);
        HBox.setHgrow(hSup, Priority.ALWAYS);
        hSup.setMaxWidth(Double.MAX_VALUE);

        tableHeader.getChildren().addAll(
            createColHeader("MÃ", 45),
            hName,
            createColHeader("NHÓM VẬT TƯ", 125),
            createColHeader("TỒN KHO HIỆN TẠI", 145),
            createColHeader("ĐỊNH MỨC TỐI THIỂU", 125),
            hSup,
            createColHeader("TRẠNG THÁI", 110),
            createColHeader("NHẬP NHANH", 110)
        );

        ScrollPane scroll = new ScrollPane(rowsContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        tableCard.getChildren().addAll(tableHeader, scroll);

        renderInventory[0] = () -> {
            rowsContainer.getChildren().clear();
            List<DatabaseManager.IngredientItem> list = DatabaseManager.getAllIngredients();
            int lowCount = 0;
            double totalVal = 0;

            for (DatabaseManager.IngredientItem ing : list) {
                if (ing.isLowStock()) lowCount++;
                totalVal += ing.currentStock * ing.unitCost;

                if ("Cảnh báo sắp hết".equals(currentCategory[0]) && !ing.isLowStock()) continue;
                else if (!"Tất cả".equals(currentCategory[0]) && !"Cảnh báo sắp hết".equals(currentCategory[0])
                        && !currentCategory[0].equalsIgnoreCase(ing.category)) {
                    continue;
                }

                if (!searchKeyword[0].isEmpty()) {
                    String kw = searchKeyword[0];
                    if (!ing.name.toLowerCase().contains(kw) && !ing.supplier.toLowerCase().contains(kw)) {
                        continue;
                    }
                }

                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(9, 12, 9, 12));
                row.setStyle("-fx-background-color: " + (ing.isLowStock() ? "#FFFBEB" : "#FFFFFF") + "; -fx-background-radius: 8px; -fx-border-color: #F2ECE4; -fx-border-radius: 8px;");

                Label lId = new Label("#" + ing.id);
                lId.setPrefWidth(45);
                lId.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #78716C;");

                Label lName = new Label(ing.name);
                lName.setPrefWidth(195);
                lName.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(lName, Priority.ALWAYS);
                lName.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #112823;");

                Label lCat = new Label(ing.category);
                lCat.setPrefWidth(125);
                lCat.setStyle("-fx-font-size: 12px; -fx-text-fill: #57534E;");

                Label lStock = new Label(String.format("%,.0f %s", ing.currentStock, ing.unit));
                lStock.setPrefWidth(145);
                lStock.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: " + (ing.isLowStock() ? "#B91C1C" : "#112823") + ";");

                Label lMin = new Label(String.format("%,.0f %s", ing.minThreshold, ing.unit));
                lMin.setPrefWidth(125);
                lMin.setStyle("-fx-font-size: 12px; -fx-text-fill: #78716C;");

                Label lSup = new Label(ing.supplier);
                lSup.setPrefWidth(145);
                lSup.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(lSup, Priority.ALWAYS);
                lSup.setStyle("-fx-font-size: 12px; -fx-text-fill: #57534E;");

                Label badge = new Label(ing.isLowStock() ? "⚠ Sắp hết" : "● Đủ dùng");
                badge.setPrefWidth(110);
                badge.setStyle(
                    "-fx-background-color: " + (ing.isLowStock() ? "#FEE2E2" : "#DCFCE7") + "; " +
                    "-fx-text-fill: " + (ing.isLowStock() ? "#B91C1C" : "#15803D") + "; " +
                    "-fx-font-family: 'Inter', sans-serif; -fx-font-size: 11.5px; -fx-font-weight: 800; -fx-background-radius: 8px; -fx-padding: 4 10 4 10; -fx-alignment: center;"
                );

                Button btnQuickImport = new Button("+ Nhập thêm");
                btnQuickImport.setPrefWidth(110);
                btnQuickImport.setStyle("-fx-background-color: #F5EFEB; -fx-text-fill: #112823; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 32px; -fx-padding: 4 10 4 10; -fx-cursor: hand;");
                btnQuickImport.setOnMouseEntered(e -> btnQuickImport.setStyle("-fx-background-color: #EAE4DA; -fx-text-fill: #112823; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 32px; -fx-padding: 4 10 4 10; -fx-cursor: hand;"));
                btnQuickImport.setOnMouseExited(e -> btnQuickImport.setStyle("-fx-background-color: #F5EFEB; -fx-text-fill: #112823; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 32px; -fx-padding: 4 10 4 10; -fx-cursor: hand;"));
                btnQuickImport.setOnAction(ev -> showRestockIngredientModal(parentStage, ing, () -> {
                    renderInventory[0].run();
                    if (toastCallback != null) toastCallback.accept("Đã nhập kho bổ sung cho '" + ing.name + "'!");
                }));

                row.getChildren().addAll(lId, lName, lCat, lStock, lMin, lSup, badge, btnQuickImport);
                rowsContainer.getChildren().add(row);
            }

            lblTotalCount.setText(list.size() + " nguyên vật liệu");
            lblLowStockCount.setText(lowCount + " mặt hàng cần nhập");
            lblTotalValue.setText(CurrencyFormatter.formatVND(totalVal));
        };

        // Initialize chip styles
        chipBtns.get(0).fire();

        txtSearch.textProperty().addListener((obs, o, n) -> {
            searchKeyword[0] = n != null ? n.trim().toLowerCase() : "";
            renderInventory[0].run();
        });

        btnRestockModal.setOnAction(e -> {
            List<DatabaseManager.IngredientItem> list = DatabaseManager.getAllIngredients();
            DatabaseManager.IngredientItem defaultTarget = list.get(0);
            for (DatabaseManager.IngredientItem it : list) {
                if (it.isLowStock()) {
                    defaultTarget = it;
                    break;
                }
            }
            showRestockIngredientModal(parentStage, defaultTarget, () -> {
                renderInventory[0].run();
                if (toastCallback != null) toastCallback.accept("Đã tạo phiếu nhập kho nguyên vật liệu thành công!");
            });
        });

        root.getChildren().addAll(header, kpiRow, filterBar, tableCard);
        return root;
    }

    private static void showRestockIngredientModal(Stage owner, DatabaseManager.IngredientItem initialIng, Runnable onRestocked) {
        Stage modal = createModal(owner, "Phiếu Nhập Kho Nguyên Vật Liệu");
        VBox root = new VBox(12);
        root.setPadding(new Insets(22));
        root.setPrefWidth(430);
        root.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16px; -fx-border-color: #DFD7CA; -fx-border-radius: 16px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 24, 0.15, 0, 8);");

        Label lblTitle = new Label("📦 Lập Phiếu Nhập Kho Nguyên Vật Liệu");
        lblTitle.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #112823;");

        ComboBox<DatabaseManager.IngredientItem> cboIng = new ComboBox<>();
        cboIng.getItems().addAll(DatabaseManager.getAllIngredients());
        cboIng.setMaxWidth(Double.MAX_VALUE);
        cboIng.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(DatabaseManager.IngredientItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : "#" + item.id + " - " + item.name + " (Tồn: " + (long) item.currentStock + " " + item.unit + ")");
            }
        });
        cboIng.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(DatabaseManager.IngredientItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : "#" + item.id + " - " + item.name + " (Tồn: " + (long) item.currentStock + " " + item.unit + ")");
            }
        });
        if (initialIng != null) {
            cboIng.setValue(initialIng);
        } else {
            cboIng.getSelectionModel().selectFirst();
        }

        double defaultAdd = (initialIng != null && initialIng.minThreshold >= 100) ? 1000 : 50;
        TextField txtAddQty = new TextField(String.valueOf((long) defaultAdd));
        TextField txtSupplier = new TextField(initialIng != null ? initialIng.supplier : "Nhà cung cấp chính");

        cboIng.setOnAction(e -> {
            DatabaseManager.IngredientItem sel = cboIng.getValue();
            if (sel != null) {
                txtSupplier.setText(sel.supplier);
                txtAddQty.setText(String.valueOf((long) (sel.minThreshold >= 100 ? 1000 : 50)));
            }
        });

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button btnCancel = new Button("Hủy");
        btnCancel.setStyle("-fx-background-color: #EAE4DA; -fx-text-fill: #1C1917; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 8 16 8 16; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> modal.close());

        Button btnSave = new Button("✓ Xác Nhận Nhập Kho");
        btnSave.setStyle("-fx-background-color: #15803D; -fx-text-fill: #FFFFFF; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 8 18 8 18; -fx-cursor: hand;");
        btnSave.setOnAction(e -> {
            DatabaseManager.IngredientItem sel = cboIng.getValue();
            if (sel != null) {
                try {
                    double added = Double.parseDouble(txtAddQty.getText().trim());
                    if (added > 0) {
                        DatabaseManager.restockIngredient(sel.id, added, txtSupplier.getText().trim());
                        modal.close();
                        if (onRestocked != null) onRestocked.run();
                    }
                } catch (NumberFormatException ignored) {}
            }
        });

        actions.getChildren().addAll(btnCancel, btnSave);
        root.getChildren().addAll(
            lblTitle,
            new Label("Chọn nguyên vật liệu nhập:"), cboIng,
            new Label("Số lượng nhập thêm:"), txtAddQty,
            new Label("Nhà cung cấp:"), txtSupplier,
            actions
        );
        showModal(modal, root);
    }

    // =========================================================================
    // TAB 7: KHUYẾN MÃI & VOUCHER (QUẢN LÝ MÃ GIẢM GIÁ THỜI GIAN THỰC)
    // =========================================================================
    public static VBox buildPromotionsView(Stage parentStage, Consumer<String> toastCallback) {
        VBox root = new VBox(14);
        VBox.setVgrow(root, Priority.ALWAYS);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox titleCol = new VBox(2);
        Label lblTitle = new Label("Chiến Dịch Khuyến Mãi & Mã Giảm Giá (Voucher)");
        lblTitle.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #112823;");
        Label lblSub = new Label("Mọi thay đổi hoặc mã mới tạo tại đây sẽ có hiệu lực tức thì trên Giỏ hàng của Khách (Kiosk) và Nhân viên");
        lblSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #78716C;");
        titleCol.getChildren().addAll(lblTitle, lblSub);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        VBox listBox = new VBox(8);
        final Runnable[] renderVouchers = new Runnable[1];

        Button btnCreate = new Button("+ Tạo mã Voucher mới");
        btnCreate.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 36px; -fx-padding: 7 16 7 16; -fx-cursor: hand;");
        btnCreate.setOnAction(e -> showCreateVoucherModal(parentStage, () -> {
            renderVouchers[0].run();
            if (toastCallback != null) toastCallback.accept("Đã phát hành mã khuyến mãi mới trên toàn hệ thống!");
        }));

        header.getChildren().addAll(titleCol, sp, btnCreate);

        VBox card = new VBox(10);
        VBox.setVgrow(card, Priority.ALWAYS);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12px; -fx-border-color: #E6E0D6; -fx-border-radius: 12px;");

        HBox tableHeader = new HBox(10);
        tableHeader.setPadding(new Insets(8, 14, 8, 14));
        tableHeader.setStyle("-fx-background-color: #F4EFE6; -fx-background-radius: 8px;");
        Label hType = createColHeader("LOẠI GIẢM GIÁ / MÔ TẢ ĐIỀU KIỆN", 140);
        HBox.setHgrow(hType, Priority.ALWAYS);
        hType.setMaxWidth(Double.MAX_VALUE);

        tableHeader.getChildren().addAll(
            createColHeader("MÃ VOUCHER", 140),
            hType,
            createColHeader("MỨC GIẢM", 120),
            createColHeader("ĐƠN TỐI THIỂU", 135),
            createColHeader("LƯỢT DÙNG", 100),
            createColHeader("HẠN SỬ DỤNG", 115),
            createColHeader("TRẠNG THÁI", 125),
            createColHeader("THAO TÁC", 100)
        );

        ScrollPane scroll = new ScrollPane(listBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        card.getChildren().addAll(tableHeader, scroll);

        renderVouchers[0] = () -> {
            listBox.getChildren().clear();
            for (Voucher v : MenuRepository.getAllVouchers()) {
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(10, 14, 10, 14));
                row.setStyle("-fx-background-color: #FAF8F5; -fx-background-radius: 8px; -fx-border-color: #EFEAE2; -fx-border-radius: 8px;");

                Label lCode = new Label(v.getCode());
                lCode.setPrefWidth(140);
                lCode.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 800; -fx-text-fill: #112823;");

                boolean isPct = "PERCENT".equalsIgnoreCase(v.getDiscountType());
                Label lType = new Label(isPct ? "Giảm theo % (Toàn menu)" : "Giảm tiền mặt trực tiếp");
                lType.setPrefWidth(140);
                lType.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(lType, Priority.ALWAYS);
                lType.setStyle("-fx-font-size: 12px; -fx-text-fill: #57534E;");

                Label lVal = new Label(isPct ? String.format("-%.0f%%", v.getDiscountValue()) : "-" + CurrencyFormatter.formatVND(v.getDiscountValue()));
                lVal.setPrefWidth(120);
                lVal.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #B45309;");

                Label lMin = new Label(CurrencyFormatter.formatVND(v.getMinOrderAmount()));
                lMin.setPrefWidth(135);
                lMin.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #1C1917;");

                Label lUsage = new Label(v.getUsageCount() + " lượt");
                lUsage.setPrefWidth(100);
                lUsage.setStyle("-fx-font-size: 12px; -fx-text-fill: #57534E;");

                Label lExp = new Label(v.getExpiryDate());
                lExp.setPrefWidth(115);
                lExp.setStyle("-fx-font-size: 12px; -fx-text-fill: #78716C;");

                boolean act = v.isActive();
                Button btnToggle = new Button(act ? "● Đang chạy" : "⏸ Tạm dừng");
                btnToggle.setPrefWidth(125);
                btnToggle.setStyle(
                    "-fx-background-color: " + (act ? "#DCFCE7" : "#FEE2E2") + "; " +
                    "-fx-text-fill: " + (act ? "#15803D" : "#B91C1C") + "; " +
                    "-fx-font-family: 'Inter', sans-serif; -fx-font-size: 11.5px; -fx-font-weight: 800; -fx-background-radius: 8px; -fx-pref-height: 32px; -fx-padding: 4 10 4 10; -fx-cursor: hand;"
                );
                btnToggle.setOnAction(ev -> {
                    v.setActive(!act);
                    renderVouchers[0].run();
                    if (toastCallback != null) {
                        toastCallback.accept("Đã chuyển mã " + v.getCode() + " sang trạng thái: " + (v.isActive() ? "Đang chạy" : "Tạm dừng"));
                    }
                });

                Button btnDelete = new Button("Xóa mã");
                btnDelete.setPrefWidth(100);
                btnDelete.setStyle("-fx-background-color: #F5EFEB; -fx-text-fill: #B91C1C; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 32px; -fx-cursor: hand;");
                btnDelete.setOnMouseEntered(e -> btnDelete.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 32px; -fx-cursor: hand;"));
                btnDelete.setOnMouseExited(e -> btnDelete.setStyle("-fx-background-color: #F5EFEB; -fx-text-fill: #B91C1C; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 32px; -fx-cursor: hand;"));
                btnDelete.setOnAction(ev -> {
                    MenuRepository.removeVoucher(v.getCode());
                    renderVouchers[0].run();
                    if (toastCallback != null) toastCallback.accept("Đã xóa mã khuyến mãi " + v.getCode());
                });

                row.getChildren().addAll(lCode, lType, lVal, lMin, lUsage, lExp, btnToggle, btnDelete);
                listBox.getChildren().add(row);
            }
        };

        renderVouchers[0].run();
        root.getChildren().addAll(header, card);
        return root;
    }

    private static void showCreateVoucherModal(Stage owner, Runnable onCreated) {
        Stage modal = createModal(owner, "Tạo Mã Khuyến Mãi Mới");
        VBox root = new VBox(12);
        root.setPadding(new Insets(22));
        root.setPrefWidth(420);
        root.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16px; -fx-border-color: #DFD7CA; -fx-border-radius: 16px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 24, 0.15, 0, 8);");

        Label lblTitle = new Label("🎁 Phát Hành Mã Voucher Mới");
        lblTitle.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #112823;");

        TextField txtCode = new TextField("BREW25K");
        ComboBox<String> cboType = new ComboBox<>();
        cboType.getItems().addAll("AMOUNT (Giảm tiền mặt VNĐ)", "PERCENT (Giảm theo %)");
        cboType.getSelectionModel().selectFirst();
        cboType.setMaxWidth(Double.MAX_VALUE);

        TextField txtVal = new TextField("25000");
        TextField txtMin = new TextField("90000");
        TextField txtExp = new TextField("31/12/2026");

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button btnCancel = new Button("Hủy");
        btnCancel.setStyle("-fx-background-color: #EAE4DA; -fx-text-fill: #1C1917; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 8 16 8 16; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> modal.close());

        Button btnSave = new Button("Phát Hành Ngay");
        btnSave.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 8 18 8 18; -fx-cursor: hand;");
        btnSave.setOnAction(e -> {
            String code = txtCode.getText().trim().toUpperCase();
            if (!code.isEmpty()) {
                String type = cboType.getValue().startsWith("PERCENT") ? "PERCENT" : "AMOUNT";
                double val = 15000;
                double min = 0;
                try { val = Double.parseDouble(txtVal.getText().trim()); } catch (NumberFormatException ignored) {}
                try { min = Double.parseDouble(txtMin.getText().trim()); } catch (NumberFormatException ignored) {}
                MenuRepository.addOrUpdateVoucher(new Voucher(code, type, val, min, true, txtExp.getText().trim(), 0));
                modal.close();
                if (onCreated != null) onCreated.run();
            }
        });
        actions.getChildren().addAll(btnCancel, btnSave);

        root.getChildren().addAll(
            lblTitle,
            new Label("Mã Voucher (In hoa):"), txtCode,
            new Label("Hình thức giảm:"), cboType,
            new Label("Giá trị giảm (% hoặc VNĐ):"), txtVal,
            new Label("Đơn hàng tối thiểu (VNĐ):"), txtMin,
            new Label("Hạn sử dụng:"), txtExp,
            actions
        );
        showModal(modal, root);
    }

    // =========================================================================
    // TAB 8: NHÂN SỰ & QUẢN TRỊ KPI 2 CHIỀU (KHÁCH HÀNG & QUẢN LÝ)
    // =========================================================================
    public static VBox buildStaffRbacView(Stage parentStage, Consumer<String> toastCallback) {
        VBox root = new VBox(14);
        VBox.setVgrow(root, Priority.ALWAYS);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox titleCol = new VBox(2);
        Label lblTitle = new Label("Quản Lý Nhân Sự, Phân Quyền (RBAC) & Đánh Giá KPI 2 Chiều");
        lblTitle.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 21px; -fx-font-weight: 800; -fx-text-fill: #112823;");
        Label lblSub = new Label("Đánh giá toàn diện hiệu suất qua 2 kênh: Điểm bình chọn trực tiếp từ Khách hàng (CSAT) và Điểm đánh giá nghiệp vụ từ Quản lý");
        lblSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #78716C;");
        titleCol.getChildren().addAll(lblTitle, lblSub);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        VBox rowsBox = new VBox(8);
        final Runnable[] renderStaff = new Runnable[1];

        Button btnAddStaff = new Button("+ Thêm nhân viên mới");
        btnAddStaff.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 36px; -fx-padding: 7 16 7 16; -fx-cursor: hand;");
        btnAddStaff.setOnAction(e -> showAddStaffModal(parentStage, () -> {
            renderStaff[0].run();
            if (toastCallback != null) toastCallback.accept("Đã tạo tài khoản nhân viên mới thành công!");
        }));

        header.getChildren().addAll(titleCol, sp, btnAddStaff);

        // --- 4 THẺ TỔNG QUAN KPI (SCORECARDS) ---
        HBox kpiScorecards = new HBox(12);
        kpiScorecards.setAlignment(Pos.CENTER_LEFT);

        Label lblKpiTotalStaff = new Label("6");
        Label lblKpiCustomerCsat = new Label("4.8 ★");
        Label lblKpiManagerRating = new Label("4.8 ★");
        Label lblKpiTotalReviews = new Label("4");

        kpiScorecards.getChildren().addAll(
            createStaffKpiCard("TỔNG NHÂN SỰ", lblKpiTotalStaff, "Nhân sự hoạt động trong ca", "#112823"),
            createStaffKpiCard("ĐIỂM CSAT KHÁCH HÀNG", lblKpiCustomerCsat, "Bình chọn trực tiếp từ Kiosk/Bàn", "#D97706"),
            createStaffKpiCard("ĐIỂM QUẢN LÝ CHẤM", lblKpiManagerRating, "Đánh giá chuyên cần & nghiệp vụ", "#059669"),
            createStaffKpiCard("PHẢN HỒI TỪ KHÁCH", lblKpiTotalReviews, "Lượt đánh giá & góp ý dịch vụ", "#2563EB")
        );

        // BẢNG DANH SÁCH NHÂN VIÊN & KPI
        VBox card = new VBox(10);
        VBox.setVgrow(card, Priority.ALWAYS);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12px; -fx-border-color: #E6E0D6; -fx-border-radius: 12px;");

        HBox tableHeader = new HBox(8);
        tableHeader.setPadding(new Insets(8, 12, 8, 12));
        tableHeader.setStyle("-fx-background-color: #F4EFE6; -fx-background-radius: 8px;");

        Label hStaffName = createColHeader("HỌ TÊN NHÂN SỰ", 175);
        HBox.setHgrow(hStaffName, Priority.ALWAYS);

        tableHeader.getChildren().addAll(
            createColHeader("MÃ NV", 60),
            hStaffName,
            createColHeader("VAI TRÒ (RBAC)", 135),
            createColHeader("ĐƠN PHỤC VỤ", 100),
            createColHeader("KHÁCH ĐÁNH GIÁ (50%)", 155),
            createColHeader("QUẢN LÝ CHẤM (50%)", 135),
            createColHeader("XẾP LOẠI KPI", 125),
            createColHeader("THAO TÁC", 185)
        );

        ScrollPane scroll = new ScrollPane(rowsBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        card.getChildren().addAll(tableHeader, scroll);

        // KHUNG HIỂN THỊ PHẢN HỒI TỪ KHÁCH HÀNG
        VBox feedbackCard = new VBox(10);
        feedbackCard.setPadding(new Insets(14));
        feedbackCard.setStyle("-fx-background-color: #FAF8F5; -fx-background-radius: 12px; -fx-border-color: #EAE3D6; -fx-border-radius: 12px;");

        HBox fbHeader = new HBox(8);
        fbHeader.setAlignment(Pos.CENTER_LEFT);
        Label lblFbTitle = new Label("💬 Ý Kiến Đánh Giá & Góp Ý Trực Tiếp Từ Khách Hàng (Mới Nhất)");
        lblFbTitle.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 800; -fx-text-fill: #112823;");
        Region spFb = new Region();
        HBox.setHgrow(spFb, Priority.ALWAYS);
        Button btnReloadFb = new Button("🔄 Cập nhật");
        btnReloadFb.setStyle("-fx-background-color: #EFE9DF; -fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #563521; -fx-background-radius: 6px; -fx-cursor: hand; -fx-padding: 3 9;");
        btnReloadFb.setOnAction(e -> renderStaff[0].run());
        fbHeader.getChildren().addAll(lblFbTitle, spFb, btnReloadFb);

        FlowPane feedbacksFlow = new FlowPane(10, 10);
        feedbackCard.getChildren().addAll(fbHeader, feedbacksFlow);

        renderStaff[0] = () -> {
            rowsBox.getChildren().clear();
            feedbacksFlow.getChildren().clear();

            List<StaffModel> allStaff = DatabaseManager.getAllStaff();
            List<CustomerFeedback> allFeedbacks = DatabaseManager.getAllCustomerFeedbacks();

            // Cập nhật Scorecards
            lblKpiTotalStaff.setText(allStaff.size() + " nhân sự");
            lblKpiTotalReviews.setText(allFeedbacks.size() + " phản hồi");

            double totalCustStars = 0;
            int totalCustCount = 0;
            double totalMgrRating = 0;

            for (StaffModel s : allStaff) {
                if (s.getCustomerRatingCount() > 0) {
                    totalCustStars += s.getCustomerRatingSum();
                    totalCustCount += s.getCustomerRatingCount();
                }
                totalMgrRating += s.getManagerRating();
            }

            double avgCust = totalCustCount > 0 ? (totalCustStars / totalCustCount) : 4.8;
            double avgMgr = allStaff.size() > 0 ? (totalMgrRating / allStaff.size()) : 4.8;
            lblKpiCustomerCsat.setText(String.format("%.1f ★", avgCust));
            lblKpiManagerRating.setText(String.format("%.1f ★", avgMgr));

            // Render Danh sách nhân sự
            for (StaffModel s : allStaff) {
                HBox row = new HBox(8);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(8, 12, 8, 12));
                row.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 8px; -fx-border-color: #EFEAE2; -fx-border-radius: 8px;");

                Label lId = new Label("NV-0" + s.getStaffId());
                lId.setPrefWidth(60);
                lId.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 700; -fx-text-fill: #78716C;");

                VBox nameBox = new VBox(1);
                nameBox.setPrefWidth(175);
                HBox.setHgrow(nameBox, Priority.ALWAYS);
                Label lName = new Label(s.getFullName());
                lName.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #112823;");
                Label lUser = new Label("@" + s.getUsername());
                lUser.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #854D0E;");
                nameBox.getChildren().addAll(lName, lUser);

                ComboBox<String> cboRole = new ComboBox<>();
                cboRole.getItems().addAll("OWNER", "MANAGER", "CASHIER", "WAITER", "BARISTA");
                cboRole.setValue(s.getRole());
                cboRole.setPrefWidth(135);
                cboRole.setStyle(COMBO_STYLE);
                cboRole.setOnAction(ev -> {
                    DatabaseManager.updateStaffRole(s.getStaffId(), cboRole.getValue());
                    renderStaff[0].run();
                    if (toastCallback != null) toastCallback.accept("Đã cập nhật vai trò " + s.getFullName() + " -> " + cboRole.getValue());
                });

                Label lOrders = new Label(s.getOrdersServed() + " đơn");
                lOrders.setPrefWidth(100);
                lOrders.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #374151;");

                // Khách đánh giá (Stars)
                HBox custBox = new HBox(4);
                custBox.setAlignment(Pos.CENTER_LEFT);
                custBox.setPrefWidth(155);
                Label lCustStars = new Label(String.format("⭐ %.1f", s.getAverageCustomerRating()));
                lCustStars.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #D97706;");
                Label lCustCount = new Label("(" + s.getCustomerRatingCount() + " lượt)");
                lCustCount.setStyle("-fx-font-size: 11px; -fx-text-fill: #6B7280;");
                custBox.getChildren().addAll(lCustStars, lCustCount);

                // Quản lý chấm
                Label lMgrRating = new Label(String.format("⭐ %.1f / 5.0", s.getManagerRating()));
                lMgrRating.setPrefWidth(135);
                lMgrRating.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #059669;");

                // Xếp loại KPI Rank
                Label lRank = new Label(s.getKpiRank());
                lRank.setPrefWidth(125);
                String rankColor = s.getOverallKpiScore() >= 4.7 ? "#047857" : (s.getOverallKpiScore() >= 4.0 ? "#0284C7" : "#D97706");
                String rankBg = s.getOverallKpiScore() >= 4.7 ? "#ECFDF5" : (s.getOverallKpiScore() >= 4.0 ? "#E0F2FE" : "#FEF3C7");
                lRank.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: " + rankColor + "; -fx-background-color: " + rankBg + "; -fx-background-radius: 4px; -fx-padding: 3 7;");

                // Thao tác (Chấm KPI & Khóa)
                HBox actionsBox = new HBox(6);
                actionsBox.setAlignment(Pos.CENTER_LEFT);
                actionsBox.setPrefWidth(185);

                Button btnGradeKpi = new Button("⭐ Chấm KPI");
                btnGradeKpi.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #92400E; -fx-font-size: 11px; -fx-font-weight: 700; -fx-background-radius: 6px; -fx-cursor: hand; -fx-padding: 4 9;");
                btnGradeKpi.setOnAction(ev -> showStaffKpiModal(parentStage, s, () -> renderStaff[0].run(), toastCallback));

                boolean active = "ACTIVE".equalsIgnoreCase(s.getStatus());
                Button btnStatus = new Button(active ? "● Hoạt động" : "🔒 Khóa");
                btnStatus.setStyle(
                    "-fx-background-color: " + (active ? "#DCFCE7" : "#FEE2E2") + "; " +
                    "-fx-text-fill: " + (active ? "#15803D" : "#B91C1C") + "; " +
                    "-fx-font-family: 'Inter', sans-serif; -fx-font-size: 11px; -fx-font-weight: 800; -fx-background-radius: 6px; -fx-padding: 4 9; -fx-cursor: hand;"
                );
                btnStatus.setOnAction(ev -> {
                    DatabaseManager.toggleStaffStatus(s.getStaffId());
                    StaffModel updated = DatabaseManager.getStaffById(s.getStaffId());
                    if (updated != null) {
                        CafeServerService.getInstance().broadcastStaffStatus(
                            updated.getStaffId(), updated.getUsername(), updated.getStatus(), updated.getRole()
                        );
                    }
                    renderStaff[0].run();
                    String stDesc = (updated != null && "ACTIVE".equalsIgnoreCase(updated.getStatus())) ? "Đang hoạt động" : "Đã khóa";
                    if (toastCallback != null) toastCallback.accept("Đã cập nhật trạng thái " + s.getUsername() + " -> " + stDesc);
                });

                actionsBox.getChildren().addAll(btnGradeKpi, btnStatus);
                row.getChildren().addAll(lId, nameBox, cboRole, lOrders, custBox, lMgrRating, lRank, actionsBox);
                rowsBox.getChildren().add(row);
            }

            // Render Danh sách Feedbacks thực tế từ khách
            if (allFeedbacks.isEmpty()) {
                Label lblEmptyFb = new Label("Chưa có đánh giá nào từ khách hàng.");
                lblEmptyFb.setStyle("-fx-font-size: 12px; -fx-text-fill: #9CA3AF;");
                feedbacksFlow.getChildren().add(lblEmptyFb);
            } else {
                for (CustomerFeedback fb : allFeedbacks) {
                    VBox fbCard = new VBox(4);
                    fbCard.setPadding(new Insets(8, 12, 8, 12));
                    fbCard.setPrefWidth(310);
                    fbCard.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 8px; -fx-border-color: #E5E7EB; -fx-border-radius: 8px;");

                    HBox fbTop = new HBox(6);
                    fbTop.setAlignment(Pos.CENTER_LEFT);
                    Label lTbl = new Label("Bàn " + (fb.getTableNumber() < 10 ? "0" + fb.getTableNumber() : fb.getTableNumber()));
                    lTbl.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 800; -fx-text-fill: #112823;");
                    Label lStars = new Label("★".repeat(fb.getRating()));
                    lStars.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #F59E0B;");
                    Region spFbCard = new Region();
                    HBox.setHgrow(spFbCard, Priority.ALWAYS);
                    String timeStr = new java.text.SimpleDateFormat("HH:mm dd/MM").format(new java.util.Date(fb.getTimestamp()));
                    Label lTime = new Label(timeStr);
                    lTime.setStyle("-fx-font-size: 10.5px; -fx-text-fill: #9CA3AF;");
                    fbTop.getChildren().addAll(lTbl, lStars, spFbCard, lTime);

                    Label lStaffTarget = new Label("Gửi tới: " + fb.getStaffFullName());
                    lStaffTarget.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #92400E;");

                    String tagsText = (fb.getTags() != null && !fb.getTags().isEmpty()) ? String.join(" • ", fb.getTags()) : "Trải nghiệm chung";
                    Label lTags = new Label(tagsText);
                    lTags.setStyle("-fx-font-size: 10.5px; -fx-text-fill: #047857; -fx-font-weight: 600;");

                    String commentText = (fb.getComment() != null && !fb.getComment().trim().isEmpty()) ? "\"" + fb.getComment() + "\"" : "";
                    Label lComment = new Label(commentText);
                    lComment.setWrapText(true);
                    lComment.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #374151; -fx-font-style: italic;");

                    fbCard.getChildren().addAll(fbTop, lStaffTarget, lTags);
                    if (!commentText.isEmpty()) {
                        fbCard.getChildren().add(lComment);
                    }
                    feedbacksFlow.getChildren().add(fbCard);
                }
            }
        };

        renderStaff[0].run();
        root.getChildren().addAll(header, kpiScorecards, card, feedbackCard);
        return root;
    }

    private static VBox createStaffKpiCard(String title, Label valLabel, String subText, String valColor) {
        VBox card = new VBox(3);
        card.setPadding(new Insets(10, 14, 10, 14));
        card.setPrefWidth(215);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 10px; -fx-border-color: #E6E0D6; -fx-border-radius: 10px;");

        Label lblT = new Label(title);
        lblT.setStyle("-fx-font-size: 10.5px; -fx-font-weight: 700; -fx-text-fill: #78716C; -fx-letter-spacing: 0.5px;");

        valLabel.setStyle("-fx-font-size: 19px; -fx-font-weight: 900; -fx-text-fill: " + valColor + ";");

        Label lblSub = new Label(subText);
        lblSub.setStyle("-fx-font-size: 10.5px; -fx-text-fill: #A8A29E;");

        card.getChildren().addAll(lblT, valLabel, lblSub);
        return card;
    }

    private static void showStaffKpiModal(Stage owner, StaffModel staff, Runnable onUpdated, Consumer<String> toastCallback) {
        Stage modal = createModal(owner, "Chấm Điểm KPI Nhân Sự • " + staff.getFullName());
        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        root.setPrefWidth(440);
        root.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16px; -fx-border-color: #DFD7CA; -fx-border-radius: 16px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 24, 0.15, 0, 8);");

        Label lblTitle = new Label("⭐ Chấm Điểm KPI Nhân Sự");
        lblTitle.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 19px; -fx-font-weight: 800; -fx-text-fill: #112823;");

        Label lblStaffInfo = new Label(staff.getFullName() + " (" + staff.getRoleDisplayName() + " • @" + staff.getUsername() + ")");
        lblStaffInfo.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #92400E;");

        // Thống kê hiện tại từ khách
        HBox custSummary = new HBox(8);
        custSummary.setAlignment(Pos.CENTER_LEFT);
        custSummary.setStyle("-fx-background-color: #FFFBEB; -fx-padding: 8 12; -fx-background-radius: 8px; -fx-border-color: #FDE68A; -fx-border-radius: 8px;");
        Label lblCustSummary = new Label("Khách hàng đánh giá: ⭐ " + staff.getAverageCustomerRating() + " / 5.0 (" + staff.getCustomerRatingCount() + " lượt đánh giá)");
        lblCustSummary.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #B45309;");
        custSummary.getChildren().add(lblCustSummary);

        // Quản lý chấm điểm
        Label lblScoreTitle = new Label("Quản lý chấm điểm nghiệp vụ & thái độ (Thang 1.0 - 5.0 sao):");
        lblScoreTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #374151;");

        Spinner<Double> spnRating = new Spinner<>(1.0, 5.0, staff.getManagerRating(), 0.1);
        spnRating.setEditable(true);
        spnRating.setMaxWidth(Double.MAX_VALUE);
        spnRating.setStyle("-fx-font-size: 13px; -fx-font-weight: 800;");

        Label lblNoteTitle = new Label("Ghi chú nhận xét của Quản lý / Chủ quán:");
        lblNoteTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #374151;");

        TextArea txtNote = new TextArea(staff.getManagerNote() != null ? staff.getManagerNote() : "");
        txtNote.setPromptText("Nhập nhận xét về tác phong, chuyên cần, tốc độ phục vụ...");
        txtNote.setPrefRowCount(3);
        txtNote.setWrapText(true);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button btnCancel = new Button("Hủy");
        btnCancel.setStyle("-fx-background-color: #EAE4DA; -fx-text-fill: #1C1917; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 7 14; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> modal.close());

        Button btnSave = new Button("Lưu Điểm KPI");
        btnSave.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 7 18; -fx-cursor: hand;");
        btnSave.setOnAction(e -> {
            double rate = spnRating.getValue();
            String note = txtNote.getText() != null ? txtNote.getText().trim() : "";
            DatabaseManager.updateStaffManagerRating(staff.getStaffId(), rate, note);
            modal.close();
            if (onUpdated != null) onUpdated.run();
            if (toastCallback != null) toastCallback.accept("Đã cập nhật KPI cho nhân sự: " + staff.getFullName() + " (" + rate + "★)");
        });
        actions.getChildren().addAll(btnCancel, btnSave);

        root.getChildren().addAll(
            lblTitle, lblStaffInfo, custSummary,
            lblScoreTitle, spnRating,
            lblNoteTitle, txtNote,
            actions
        );
        showModal(modal, root);
    }

    private static void showAddStaffModal(Stage owner, Runnable onAdded) {
        Stage modal = createModal(owner, "Thêm Nhân Viên Mới");
        VBox root = new VBox(12);
        root.setPadding(new Insets(22));
        root.setPrefWidth(420);
        root.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 16px; -fx-border-color: #DFD7CA; -fx-border-radius: 16px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.22), 24, 0.15, 0, 8);");

        Label lblTitle = new Label("👤 Thêm Tài Khoản Nhân Sự Mới");
        lblTitle.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #112823;");

        TextField txtFullName = new TextField();
        txtFullName.setPromptText("Họ và tên nhân viên...");

        TextField txtUsername = new TextField();
        txtUsername.setPromptText("Tên đăng nhập (VD: cashier2)...");

        PasswordField txtPassword = new PasswordField();
        txtPassword.setText("123456");

        ComboBox<String> cboRole = new ComboBox<>();
        cboRole.getItems().addAll("CASHIER", "WAITER", "BARISTA", "MANAGER");
        cboRole.getSelectionModel().selectFirst();
        cboRole.setMaxWidth(Double.MAX_VALUE);

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button btnCancel = new Button("Hủy");
        btnCancel.setStyle("-fx-background-color: #EAE4DA; -fx-text-fill: #1C1917; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 8 16 8 16; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> modal.close());

        Button btnSave = new Button("Tạo Tài Khoản");
        btnSave.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-padding: 8 18 8 18; -fx-cursor: hand;");
        btnSave.setOnAction(e -> {
            if (!txtFullName.getText().trim().isEmpty() && !txtUsername.getText().trim().isEmpty()) {
                DatabaseManager.addStaff(txtFullName.getText(), txtUsername.getText(), txtPassword.getText(), cboRole.getValue());
                modal.close();
                if (onAdded != null) onAdded.run();
            }
        });
        actions.getChildren().addAll(btnCancel, btnSave);

        root.getChildren().addAll(
            lblTitle,
            new Label("Họ và tên nhân viên:"), txtFullName,
            new Label("Tài khoản đăng nhập:"), txtUsername,
            new Label("Mật khẩu khởi tạo:"), txtPassword,
            new Label("Vai trò (RBAC):"), cboRole,
            actions
        );
        showModal(modal, root);
    }

    // =========================================================================
    // TAB 9: BÁO CÁO TÀI CHÍNH & KẾ TOÁN ĐỘNG (DYNAMIC PERIOD + CSV EXPORT)
    // =========================================================================
    public static VBox buildFinancialReportsView(Consumer<String> toastCallback) {
        return buildFinancialReportsView(null, toastCallback);
    }

    public static VBox buildFinancialReportsView(Stage parentStage, Consumer<String> toastCallback) {
        VBox root = new VBox(14);
        VBox.setVgrow(root, Priority.ALWAYS);

        final String[] currentPeriod = new String[]{"7 ngày qua"};

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox titleCol = new VBox(2);
        Label lblTitle = new Label("Báo Cáo Tài Chính, Giá Vốn (COGS) & Lợi Nhuận Ròng");
        lblTitle.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #112823;");
        Label lblSub = new Label("Chọn kỳ báo cáo để tính toán tự động Doanh thu gộp, Giá vốn nguyên liệu BOM, Chi phí vận hành và Lãi ròng");
        lblSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #78716C;");
        titleCol.getChildren().addAll(lblTitle, lblSub);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        HBox periodBtnsBox = new HBox(6);
        periodBtnsBox.setAlignment(Pos.CENTER_LEFT);
        String[] periods = {"Hôm nay", "7 ngày qua", "Tháng này", "Quý này"};
        List<Button> pBtns = new ArrayList<>();

        Button btnExportCsv = new Button("📥 Xuất Báo Cáo CSV / Excel");
        btnExportCsv.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 36px; -fx-padding: 7 16 7 16; -fx-cursor: hand;");

        header.getChildren().addAll(titleCol, sp, periodBtnsBox, btnExportCsv);

        // 4 Financial Summary Cards
        HBox kpiRow = new HBox(14);
        Label lblGrossRev = new Label();
        Label lblCogs = new Label();
        Label lblOpex = new Label();
        Label lblNetProfit = new Label();
        kpiRow.getChildren().addAll(
            createMiniKpiCard("TỔNG DOANH THU GỘP", lblGrossRev, "#112823"),
            createMiniKpiCard("GIÁ VỐN HÀNG BÁN (COGS 31%)", lblCogs, "#B45309"),
            createMiniKpiCard("CHI PHÍ VẬN HÀNH & KM", lblOpex, "#57534E"),
            createMiniKpiCard("LỢI NHUẬN RÒNG (NET PROFIT)", lblNetProfit, "#15803D")
        );

        // Ledger Table
        VBox ledgerCard = new VBox(8);
        VBox.setVgrow(ledgerCard, Priority.ALWAYS);
        ledgerCard.setPadding(new Insets(16));
        ledgerCard.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12px; -fx-border-color: #E6E0D6; -fx-border-radius: 12px;");

        HBox ledgerHeader = new HBox(10);
        ledgerHeader.setPadding(new Insets(8, 14, 8, 14));
        ledgerHeader.setStyle("-fx-background-color: #F4EFE6; -fx-background-radius: 8px;");
        Label hPeriod = createColHeader("KỲ HẠCH TOÁN", 160);
        HBox.setHgrow(hPeriod, Priority.ALWAYS);
        hPeriod.setMaxWidth(Double.MAX_VALUE);

        Label hGross = createColHeader("DOANH THU GỘP", 165);
        HBox.setHgrow(hGross, Priority.ALWAYS);
        hGross.setMaxWidth(Double.MAX_VALUE);

        ledgerHeader.getChildren().addAll(
            hPeriod,
            createColHeader("SỐ ĐƠN HÀNG", 120),
            hGross,
            createColHeader("GIÁ VỐN (BOM COGS)", 165),
            createColHeader("KHUYẾN MÃI & VẬN HÀNH", 165),
            createColHeader("LỢI NHUẬN RÒNG", 165),
            createColHeader("TỶ SUẤT LÃI RÒNG", 120)
        );

        VBox ledgerRows = new VBox(6);
        ScrollPane scroll = new ScrollPane(ledgerRows);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        ledgerCard.getChildren().addAll(ledgerHeader, scroll);

        final List<String[]> currentCsvRows = new ArrayList<>();
        final double[] financialTotals = new double[4]; // gross, cogs, opex, net
        final int[] totalOrderCount = new int[1];

        Runnable renderFinancials = () -> {
            ledgerRows.getChildren().clear();
            currentCsvRows.clear();

            String[][] data;
            if ("Hôm nay".equals(currentPeriod[0])) {
                data = new String[][]{
                    {"Ca Sáng (07:00 - 11:30)", "54 đơn", "3620000", "1122200", "420000"},
                    {"Ca Trưa (11:30 - 15:30)", "42 đơn", "2890000", "895900", "350000"},
                    {"Ca Chiều (15:30 - 19:00)", "38 đơn", "2540000", "787400", "310000"},
                    {"Ca Tối (19:00 - 22:30)", "49 đơn", "3450000", "1069500", "410000"}
                };
            } else if ("7 ngày qua".equals(currentPeriod[0])) {
                data = new String[][]{
                    {"Thứ Hai (13/10)", "118 đơn", "7850000", "2433500", "1150000"},
                    {"Thứ Ba (14/10)", "124 đơn", "8240000", "2554400", "1180000"},
                    {"Thứ Tư (15/10)", "132 đơn", "8890000", "2755900", "1240000"},
                    {"Thứ Năm (16/10)", "129 đơn", "8620000", "2672200", "1210000"},
                    {"Thứ Sáu (17/10)", "164 đơn", "11250000", "3487500", "1490000"},
                    {"Thứ Bảy (18/10)", "212 đơn", "14800000", "4588000", "1820000"},
                    {"Chủ Nhật (19/10)", "198 đơn", "13650000", "4231500", "1740000"}
                };
            } else if ("Tháng này".equals(currentPeriod[0])) {
                data = new String[][]{
                    {"Tuần 1 Tháng 10", "890 đơn", "59400000", "18414000", "8200000"},
                    {"Tuần 2 Tháng 10", "945 đơn", "63800000", "19778000", "8650000"},
                    {"Tuần 3 Tháng 10", "1.077 đơn", "73300000", "22723000", "9830000"},
                    {"Tuần 4 Tháng 10 (Dự kiến)", "980 đơn", "66200000", "20522000", "8900000"}
                };
            } else {
                data = new String[][]{
                    {"Tháng 07/2026", "3.620 đơn", "242500000", "75175000", "34200000"},
                    {"Tháng 08/2026", "3.810 đơn", "256800000", "79608000", "35800000"},
                    {"Tháng 09/2026", "3.740 đơn", "251200000", "77872000", "35100000"},
                    {"Tháng 10/2026", "3.892 đơn", "262700000", "81437000", "35580000"}
                };
            }

            double sumGross = 0;
            double sumCogs = 0;
            double sumOpex = 0;
            double sumNet = 0;
            int orderCountSum = 0;

            for (String[] r : data) {
                double gross = Double.parseDouble(r[2]);
                double cogs = Double.parseDouble(r[3]);
                double opex = Double.parseDouble(r[4]);
                double net = gross - cogs - opex;
                double pct = gross > 0 ? (net / gross) * 100.0 : 0;

                try {
                    String cleanOrders = r[1].replaceAll("[^0-9]", "");
                    if (!cleanOrders.isEmpty()) orderCountSum += Integer.parseInt(cleanOrders);
                } catch (Exception ignored) {}

                sumGross += gross;
                sumCogs += cogs;
                sumOpex += opex;
                sumNet += net;

                currentCsvRows.add(new String[]{r[0], r[1], String.valueOf((long) gross), String.valueOf((long) cogs), String.valueOf((long) opex), String.valueOf((long) net), String.format("%.1f%%", pct)});

                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(10, 14, 10, 14));
                row.setStyle("-fx-background-color: #FAF8F5; -fx-background-radius: 8px; -fx-border-color: #EFEAE2; -fx-border-radius: 8px;");

                Label lPeriod = createCellLabel(r[0], 160, "#112823", true);
                HBox.setHgrow(lPeriod, Priority.ALWAYS);
                lPeriod.setMaxWidth(Double.MAX_VALUE);

                Label lGross = createCellLabel(CurrencyFormatter.formatVND(gross), 165, "#112823", true);
                HBox.setHgrow(lGross, Priority.ALWAYS);
                lGross.setMaxWidth(Double.MAX_VALUE);

                row.getChildren().addAll(
                    lPeriod,
                    createCellLabel(r[1], 120, "#57534E", false),
                    lGross,
                    createCellLabel(CurrencyFormatter.formatVND(cogs), 165, "#B45309", false),
                    createCellLabel(CurrencyFormatter.formatVND(opex), 165, "#57534E", false),
                    createCellLabel(CurrencyFormatter.formatVND(net), 165, "#15803D", true),
                    createCellLabel(String.format("%.1f%%", pct), 120, "#15803D", true)
                );
                ledgerRows.getChildren().add(row);
            }

            financialTotals[0] = sumGross;
            financialTotals[1] = sumCogs;
            financialTotals[2] = sumOpex;
            financialTotals[3] = sumNet;
            totalOrderCount[0] = orderCountSum;

            lblGrossRev.setText(CurrencyFormatter.formatVND(sumGross));
            lblCogs.setText(CurrencyFormatter.formatVND(sumCogs));
            lblOpex.setText(CurrencyFormatter.formatVND(sumOpex));
            lblNetProfit.setText(CurrencyFormatter.formatVND(sumNet));
        };

        for (String p : periods) {
            Button b = new Button(p);
            b.setOnAction(e -> {
                currentPeriod[0] = p;
                for (Button btn : pBtns) {
                    boolean act = btn.getText().equals(p);
                    btn.setStyle(act
                        ? "-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 34px; -fx-padding: 6 14 6 14; -fx-cursor: hand;"
                        : "-fx-background-color: #F5EFEB; -fx-text-fill: #57534E; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 600; -fx-background-radius: 8px; -fx-pref-height: 34px; -fx-padding: 6 14 6 14; -fx-cursor: hand;");
                }
                renderFinancials.run();
            });
            pBtns.add(b);
            periodBtnsBox.getChildren().add(b);
        }
        pBtns.get(1).fire();

        btnExportCsv.setOnAction(e -> {
            try {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Xuất Báo Cáo Tài Chính & Kế Toán (CSV / Excel)");
                String sanitizedPeriod = currentPeriod[0].replaceAll("[^a-zA-Z0-9]", "_");
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date());
                fileChooser.setInitialFileName("BaoCaoTaiChinh_" + sanitizedPeriod + "_" + timeStamp + ".csv");
                fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Tệp CSV hỗ trợ Excel (*.csv)", "*.csv"),
                    new FileChooser.ExtensionFilter("Tất cả tệp (*.*)", "*.*")
                );

                Stage targetStage = parentStage;
                if (targetStage == null && btnExportCsv.getScene() != null && btnExportCsv.getScene().getWindow() instanceof Stage) {
                    targetStage = (Stage) btnExportCsv.getScene().getWindow();
                }

                File selectedFile = fileChooser.showSaveDialog(targetStage);
                if (selectedFile == null) return; // Người dùng hủy chọn

                try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(selectedFile), StandardCharsets.UTF_8))) {
                    pw.write('\ufeff'); // UTF-8 BOM cho Excel
                    pw.println("\"BÁO CÁO TÀI CHÍNH & KẾ TOÁN HOẠT ĐỘNG - " + DatabaseManager.brandName + "\"");
                    pw.println("\"Kỳ hạch toán: " + currentPeriod[0] + "\"");
                    pw.println("\"Thời điểm xuất báo cáo: " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()) + "\"");
                    pw.println("");
                    pw.println("\"Kỳ hạch toán\",\"Số đơn hàng\",\"Doanh thu gộp (VNĐ)\",\"Giá vốn COGS (VNĐ)\",\"Chi phí vận hành (VNĐ)\",\"Lợi nhuận ròng (VNĐ)\",\"Tỷ suất lãi (%)\"");
                    for (String[] r : currentCsvRows) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < r.length; i++) {
                            if (i > 0) sb.append(",");
                            sb.append("\"").append(r[i].replace("\"", "\"\"")).append("\"");
                        }
                        pw.println(sb.toString());
                    }
                    pw.println("");
                    // Dòng tổng kết Grand Total
                    double gross = financialTotals[0];
                    double cogs = financialTotals[1];
                    double opex = financialTotals[2];
                    double net = financialTotals[3];
                    double pct = gross > 0 ? (net / gross) * 100.0 : 0;
                    pw.println("\"TỔNG CỘNG\",\"" + totalOrderCount[0] + " đơn (Toàn kỳ)\",\"" + (long) gross + "\",\"" + (long) cogs + "\",\"" + (long) opex + "\",\"" + (long) net + "\",\"" + String.format("%.1f%%", pct) + "\"");
                }

                if (toastCallback != null) {
                    toastCallback.accept("✅ Đã xuất báo cáo thành công ra tệp: " + selectedFile.getName());
                }

                if (java.awt.Desktop.isDesktopSupported()) {
                    try {
                        java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                        if (desktop.isSupported(java.awt.Desktop.Action.OPEN)) {
                            desktop.open(selectedFile);
                        }
                    } catch (Exception ignored) {}
                }
            } catch (Exception ex) {
                if (toastCallback != null) toastCallback.accept("Lỗi xuất CSV: " + ex.getMessage());
            }
        });

        root.getChildren().addAll(header, kpiRow, ledgerCard);
        return root;
    }

    // =========================================================================
    // TAB 10: CÀI ĐẶT HỆ THỐNG 2 CỘT (THƯƠNG HIỆU, MÁY IN, RESET SEED & ĐĂNG XUẤT)
    // =========================================================================
    public static VBox buildSystemSettingsView(Stage parentStage, Consumer<String> toastCallback, Runnable onResetSeedData, Runnable onLogout) {
        VBox root = new VBox(14);
        VBox.setVgrow(root, Priority.ALWAYS);

        Label lblTitle = new Label("Cấu Hình Hệ Thống, Máy In Nhiệt & Cơ Sở Dữ Liệu");
        lblTitle.setStyle("-fx-font-family: 'Playfair Display', serif; -fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #112823;");

        HBox twoCols = new HBox(18);
        VBox.setVgrow(twoCols, Priority.ALWAYS);

        // Left Column: Brand & Thermal Receipt Settings
        VBox leftCol = new VBox(12);
        HBox.setHgrow(leftCol, Priority.ALWAYS);
        leftCol.setPadding(new Insets(20));
        leftCol.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12px; -fx-border-color: #E6E0D6; -fx-border-radius: 12px;");

        Label lblLeftTitle = new Label("THÔNG TIN THƯƠNG HIỆU & MẪU PHIẾU THANH TOÁN");
        lblLeftTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #78716C; -fx-letter-spacing: 0.8px;");

        TextField txtBrand = new TextField(DatabaseManager.brandName);
        TextField txtAddress = new TextField(DatabaseManager.storeAddress);
        TextField txtHotline = new TextField(DatabaseManager.storeHotline);
        TextField txtWifi = new TextField(DatabaseManager.wifiInfo);
        TextField txtFooter = new TextField(DatabaseManager.receiptFooter);

        HBox leftBtns = new HBox(10);
        Button btnSaveConfig = new Button("💾 Lưu Cấu Hình Quán");
        btnSaveConfig.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 38px; -fx-padding: 8 18 8 18; -fx-cursor: hand;");
        btnSaveConfig.setOnAction(e -> {
            DatabaseManager.brandName = txtBrand.getText().trim();
            DatabaseManager.storeAddress = txtAddress.getText().trim();
            DatabaseManager.storeHotline = txtHotline.getText().trim();
            DatabaseManager.wifiInfo = txtWifi.getText().trim();
            DatabaseManager.receiptFooter = txtFooter.getText().trim();
            if (toastCallback != null) toastCallback.accept("Đã lưu cấu hình thương hiệu & mẫu phiếu in nhiệt thành công!");
        });

        Button btnTestPrint = new Button("🖨 In Thử Phiếu Mẫu (Test Print)");
        btnTestPrint.setStyle("-fx-background-color: #F5EFEB; -fx-text-fill: #112823; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 38px; -fx-padding: 8 18 8 18; -fx-cursor: hand;");
        btnTestPrint.setOnMouseEntered(e -> btnTestPrint.setStyle("-fx-background-color: #EAE4DA; -fx-text-fill: #112823; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 38px; -fx-padding: 8 18 8 18; -fx-cursor: hand;"));
        btnTestPrint.setOnMouseExited(e -> btnTestPrint.setStyle("-fx-background-color: #F5EFEB; -fx-text-fill: #112823; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-background-radius: 8px; -fx-pref-height: 38px; -fx-padding: 8 18 8 18; -fx-cursor: hand;"));
        btnTestPrint.setOnAction(e -> {
            Order sample = new Order(9999, 1, "DINE_IN",
                Collections.singletonList(new OrderItem("Cà phê Sữa Đá Đặc Biệt (Test Print)", 1, "Size M", 35000)),
                "PAID", 0, 0, null, "Phiếu kiểm tra máy in nhiệt 80mm", System.currentTimeMillis());
            OrderSuccessDialog.showReceiptForOrder(parentStage, sample, "Admin System Test");
        });
        leftBtns.getChildren().addAll(btnSaveConfig, btnTestPrint);

        leftCol.getChildren().addAll(
            lblLeftTitle,
            new Label("Tên thương hiệu quán:"), txtBrand,
            new Label("Địa chỉ in trên phiếu:"), txtAddress,
            new Label("Hotline CSKH:"), txtHotline,
            new Label("Thông tin Wi-Fi dưới đáy phiếu:"), txtWifi,
            new Label("Lời cảm ơn cuối hóa đơn:"), txtFooter,
            leftBtns
        );

        // Right Column: Server Diagnostics, Reset Seed Data & Logout
        VBox rightCol = new VBox(14);
        rightCol.setPrefWidth(420);
        rightCol.setPadding(new Insets(20));
        rightCol.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12px; -fx-border-color: #E6E0D6; -fx-border-radius: 12px;");

        Label lblRightTitle = new Label("TRẠNG THÁI MÁY CHỦ KDS & DỮ LIỆU HỆ THỐNG");
        lblRightTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #78716C; -fx-letter-spacing: 0.8px;");

        VBox statusBox = new VBox(8);
        statusBox.setPadding(new Insets(14));
        statusBox.setStyle("-fx-background-color: #F8F5F0; -fx-background-radius: 10px;");
        statusBox.getChildren().addAll(
            new Label("● Máy chủ Socket KDS: Đang hoạt động (" + cafe.utils.NetworkConfig.getServerHost() + ":" + CafeServerService.getInstance().getActiveServerPort() + ")"),
            new Label("● Kết nối SQL Server Pool: Sẵn sàng (Non-blocking Warmup)"),
            new Label("● Máy in hóa đơn nhiệt: Khổ K80 (80mm Editorial Slip)"),
            new Label("● Phiên bản hệ thống: Brew & Bean Enterprise POS v4.0")
        );
        for (javafx.scene.Node n : statusBox.getChildren()) {
            n.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 600; -fx-text-fill: #112823;");
        }

        Button btnResetAll = new Button("↻ Khôi Phục Dữ Liệu Mẫu Toàn Hệ Thống (Reset Seed Data)");
        btnResetAll.setMaxWidth(Double.MAX_VALUE);
        btnResetAll.setStyle("-fx-background-color: #FFFBEB; -fx-text-fill: #92400E; -fx-border-color: #F59E0B; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-font-size: 12.5px; -fx-font-weight: 800; -fx-padding: 12 16 12 16; -fx-cursor: hand;");
        btnResetAll.setOnAction(e -> {
            CafeServerService.getInstance().resetToSeedState();
            DatabaseManager.resetStaffAndIngredientsSeed();
            if (onResetSeedData != null) onResetSeedData.run();
            if (toastCallback != null) toastCallback.accept("Đã khôi phục toàn bộ Đơn hàng, Bàn, Kho (25 mặt hàng) & Nhân sự về trạng thái mẫu!");
        });

        rightCol.getChildren().addAll(lblRightTitle, statusBox, btnResetAll);

        if (onLogout != null) {
            Button btnLogout = new Button("🚪 Đăng Xuất Khỏi Trung Tâm Quản Trị");
            btnLogout.setMaxWidth(Double.MAX_VALUE);
            btnLogout.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-font-size: 13px; -fx-font-weight: 800; -fx-background-radius: 8px; -fx-padding: 12 16 12 16; -fx-cursor: hand;");
            btnLogout.setOnAction(e -> onLogout.run());
            rightCol.getChildren().add(btnLogout);
        }

        twoCols.getChildren().addAll(leftCol, rightCol);
        styleInputs(leftCol);
        root.getChildren().addAll(lblTitle, twoCols);
        return root;
    }

    // =========================================================================
    // SHARED UI HELPERS
    // =========================================================================
    private static Label createColHeader(String text, double width) {
        Label l = new Label(text);
        l.setPrefWidth(width);
        l.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #57534E;");
        return l;
    }

    private static Label createCellLabel(String text, double width, String colorHex, boolean bold) {
        Label l = new Label(text);
        l.setPrefWidth(width);
        l.setStyle("-fx-font-size: 12.5px; -fx-font-weight: " + (bold ? "800" : "600") + "; -fx-text-fill: " + colorHex + ";");
        return l;
    }

    private static VBox createMiniKpiCard(String title, Label valueLabel, String valueColorHex) {
        VBox card = new VBox(4);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setPadding(new Insets(14, 18, 14, 18));
        card.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12px; -fx-border-color: #E6E0D6; -fx-border-radius: 12px;");
        Label lTitle = new Label(title);
        lTitle.setStyle("-fx-font-size: 10.5px; -fx-font-weight: 800; -fx-text-fill: #78716C; -fx-letter-spacing: 0.6px;");
        valueLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: " + valueColorHex + ";");
        card.getChildren().addAll(lTitle, valueLabel);
        return card;
    }

    private static Stage createModal(Stage owner, String title) {
        Stage modal = new Stage();
        if (owner != null) modal.initOwner(owner);
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initStyle(StageStyle.TRANSPARENT);
        modal.setTitle(title);
        return modal;
    }

    public static final String INPUT_STYLE =
        "-fx-background-color: #FAF8F5; -fx-border-color: #DFD8CE; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-border-width: 1px; " +
        "-fx-padding: 8 12 8 12; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 13px; -fx-text-fill: #112823;";

    public static final String COMBO_STYLE =
        "-fx-background-color: #FAF8F5; -fx-background-radius: 8px; -fx-border-color: #DFD8CE; -fx-border-radius: 8px; -fx-border-width: 1px; " +
        "-fx-padding: 4 8 4 10; -fx-font-family: 'Inter', sans-serif; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-text-fill: #112823; -fx-cursor: hand;";

    /** Áp dụng style phẳng chuẩn cho mọi TextField / PasswordField / ComboBox chưa được style. */
    public static <T extends javafx.scene.Parent> T styleInputs(T parent) {
        for (javafx.scene.Node n : parent.getChildrenUnmodifiable()) {
            if (n instanceof ComboBox) {
                if (n.getStyle() == null || n.getStyle().isEmpty() || !n.getStyle().contains("-fx-background-color")) {
                    n.setStyle(COMBO_STYLE);
                }
            } else if (n instanceof TextInputControl) {
                if (n.getStyle() == null || n.getStyle().isEmpty()) {
                    n.setStyle(INPUT_STYLE);
                }
            } else if (n instanceof Label && ((Label) n).getStyle().isEmpty()) {
                n.setStyle("-fx-font-family: 'Inter', sans-serif; -fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #4A453E;");
            }
            if (n instanceof javafx.scene.Parent && !(n instanceof ComboBox) && !(n instanceof TextInputControl)) {
                styleInputs((javafx.scene.Parent) n);
            }
        }
        return parent;
    }

    private static void showModal(Stage modal, VBox root) {
        styleInputs(root);

        Button btnClose = new Button("✕");
        btnClose.setStyle("-fx-background-color: #F5EFEB; -fx-text-fill: #4A453E; -fx-font-size: 12px; -fx-font-weight: 700; -fx-background-radius: 999px; -fx-min-width: 28px; -fx-min-height: 28px; -fx-max-width: 28px; -fx-max-height: 28px; -fx-padding: 0; -fx-cursor: hand;");
        btnClose.setOnAction(e -> modal.close());

        cafe.utils.WindowChromeHelper.makeDraggable(modal, root);
        StackPane card = new StackPane(root, btnClose);
        StackPane.setAlignment(btnClose, Pos.TOP_RIGHT);
        StackPane.setMargin(btnClose, new Insets(14, 14, 0, 0));
        card.setStyle("-fx-background-color: transparent;");

        StackPane wrap = new StackPane(card);
        wrap.setPadding(new Insets(16));
        wrap.setStyle("-fx-background-color: transparent;");
        Scene scene = new Scene(wrap);
        scene.setFill(Color.TRANSPARENT);
        cafe.utils.WindowChromeHelper.applyTheme(scene);
        modal.setScene(scene);
        modal.show();
        AnimationHelper.playDialogEntrance(card);
    }

    // =========================================================================
    // TAB 11: HẠ TẦNG & MẠNG SOCKET TELEMETRY (LTM & REAL-TIME MONITORING)
    // =========================================================================
    public static VBox buildNetworkTelemetryView(Stage parentStage, Consumer<String> toastCallback) {
        VBox root = new VBox(14);
        VBox.setVgrow(root, Priority.ALWAYS);

        CafeServerService server = CafeServerService.getInstance();
        cafe.services.ClientSocketService clientSock = cafe.services.ClientSocketService.getActiveInstance();
        if (!server.isServerRunning() && clientSock != null) {
            clientSock.requestAdminTelemetry();
        }

        // 1. Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label lblTitle = new Label("Hạ tầng & Giám sát Mạng Socket (LTM)");
        lblTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #112823;");
        Label lblSub = new Label("Giám sát giao thông mạng TCP thời gian thực, độ trễ RTT (Ping), kiểm soát phiên & Phòng vệ Anti-Spam");
        lblSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #6F695F;");
        titleBox.getChildren().addAll(lblTitle, lblSub);

        Region spHeader = new Region();
        HBox.setHgrow(spHeader, Priority.ALWAYS);

        final Runnable[] renderRows = new Runnable[1];
        Label lblKpiPortVal = new Label(String.valueOf(server.getActiveServerPort()));

        // Nút Ping toàn bộ thiết bị (Dùng icon ha-tang-mang.png chuẩn, không dùng emoji)
        Button btnPingAll = new Button();
        HBox pingAllGraphic = new HBox(6);
        pingAllGraphic.setAlignment(Pos.CENTER);
        javafx.scene.Node pingAllIcon = cafe.utils.IconHelper.getIcon("admin", "ha-tang-mang.png", cafe.utils.SvgIcons.SETTINGS, "", 15, Color.WHITE);
        Label lblPingAllText = new Label("Ping toàn bộ thiết bị");
        lblPingAllText.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: 700; -fx-font-size: 12.5px;");
        lblPingAllText.setMinWidth(Region.USE_PREF_SIZE);
        pingAllGraphic.getChildren().addAll(pingAllIcon, lblPingAllText);
        btnPingAll.setGraphic(pingAllGraphic);
        btnPingAll.setMinWidth(Region.USE_PREF_SIZE);
        btnPingAll.setStyle("-fx-background-color: #112823; -fx-background-radius: 8px; -fx-padding: 8 16 8 16; -fx-cursor: hand;");
        AnimationHelper.applyButtonPress(btnPingAll);

        // Nút Đổi Cổng Mạng (Dùng icon cai-dat.png chuẩn)
        Button btnPortConfig = new Button();
        HBox portCfgGraphic = new HBox(6);
        portCfgGraphic.setAlignment(Pos.CENTER);
        javafx.scene.Node portCfgIcon = cafe.utils.IconHelper.getIcon("admin", "cai-dat.png", cafe.utils.SvgIcons.SETTINGS, "", 15, Color.web("#112823"));
        Label lblPortCfgText = new Label("Đổi Cổng Mạng");
        lblPortCfgText.setStyle("-fx-text-fill: #112823; -fx-font-weight: 700; -fx-font-size: 12.5px;");
        lblPortCfgText.setMinWidth(Region.USE_PREF_SIZE);
        portCfgGraphic.getChildren().addAll(portCfgIcon, lblPortCfgText);
        btnPortConfig.setGraphic(portCfgGraphic);
        btnPortConfig.setMinWidth(Region.USE_PREF_SIZE);
        btnPortConfig.setStyle("-fx-background-color: #FFFFFF; -fx-border-color: #D6CEBE; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 8 14 8 14; -fx-cursor: hand;");
        AnimationHelper.applyButtonPress(btnPortConfig);
        btnPortConfig.setOnAction(e -> showNetworkPortModal(parentStage, toastCallback, () -> {
            lblKpiPortVal.setText(String.valueOf(server.getActiveServerPort()));
            if (!server.isServerRunning() && clientSock != null) {
                clientSock.requestAdminTelemetry();
            }
            renderRows[0].run();
        }));

        // Nút Làm mới (Dùng icon staff/lam-moi.png chuẩn)
        Button btnRefresh = new Button();
        HBox refreshGraphic = new HBox(6);
        refreshGraphic.setAlignment(Pos.CENTER);
        javafx.scene.Node refreshIcon = cafe.utils.IconHelper.getIcon("staff", "lam-moi.png", cafe.utils.SvgIcons.SETTINGS, "", 14, Color.web("#4A453E"));
        Label lblRefreshText = new Label("Làm mới");
        lblRefreshText.setStyle("-fx-text-fill: #4A453E; -fx-font-weight: 700; -fx-font-size: 12.5px;");
        lblRefreshText.setMinWidth(Region.USE_PREF_SIZE);
        refreshGraphic.getChildren().addAll(refreshIcon, lblRefreshText);
        btnRefresh.setGraphic(refreshGraphic);
        btnRefresh.setMinWidth(Region.USE_PREF_SIZE);
        btnRefresh.setStyle("-fx-background-color: #FAF6F0; -fx-border-color: #E2DDD5; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-padding: 8 14 8 14; -fx-cursor: hand;");
        AnimationHelper.applyButtonPress(btnRefresh);

        header.getChildren().addAll(titleBox, spHeader, btnPingAll, btnPortConfig, btnRefresh);

        // 2. Metric KPI Cards (4 Cards)
        HBox kpiRow = new HBox(12);

        VBox cardPort = createMiniKpiCard("CỔNG LẮNG NGHE (TCP)", lblKpiPortVal, "#1E3A8A");

        Label lblKpiSessionsVal = new Label("0 phiên");
        VBox cardSessions = createMiniKpiCard("THIẾT BỊ TRỰC TUYẾN", lblKpiSessionsVal, "#047857");

        Label lblKpiRttVal = new Label(server.getAverageRttMs() + " ms");
        VBox cardRtt = createMiniKpiCard("RTT PING TRUNG BÌNH", lblKpiRttVal, "#D97706");

        double totalKb = (server.getTotalBytesReceived() + server.getTotalBytesSent()) / 1024.0;
        Label lblKpiTrafficVal = new Label(String.format("%.1f KB", totalKb));
        VBox cardTraffic = createMiniKpiCard("LƯU LƯỢNG & PHÒNG VỆ", lblKpiTrafficVal, "#7C3AED");

        kpiRow.getChildren().addAll(cardPort, cardSessions, cardRtt, cardTraffic);

        // 2.5. Pending Reconnect Requests Card
        VBox pendingCard = new VBox(8);
        pendingCard.setPadding(new Insets(12, 16, 12, 16));
        pendingCard.setStyle("-fx-background-color: #FFFBEB; -fx-background-radius: 10px; -fx-border-color: #FDE68A; -fx-border-radius: 10px;");
        pendingCard.setVisible(false);
        pendingCard.setManaged(false);

        // 3. Active Sessions Card
        VBox sessionsCard = new VBox(10);
        sessionsCard.setPadding(new Insets(16));
        sessionsCard.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12px; -fx-border-color: #E6E0D6; -fx-border-radius: 12px;");
        VBox.setVgrow(sessionsCard, Priority.ALWAYS);

        HBox sessionsCardHeader = new HBox(10);
        sessionsCardHeader.setAlignment(Pos.CENTER_LEFT);

        Label lblSessionsTitle = new Label("Danh sách Phiên Socket Client kết nối trực tiếp");
        lblSessionsTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #112823;");

        Region spSessions = new Region();
        HBox.setHgrow(spSessions, Priority.ALWAYS);

        HBox secBadge = new HBox(6);
        secBadge.setAlignment(Pos.CENTER_LEFT);
        secBadge.setStyle("-fx-background-color: #ECFDF5; -fx-padding: 4 10; -fx-background-radius: 20px;");
        Circle secDot = new Circle(4, Color.web("#065F46"));
        Label lblSecBadge = new Label("Anti-Spam Rate-Limiting: 15 req/s (BẬT)");
        lblSecBadge.setStyle("-fx-text-fill: #065F46; -fx-font-size: 11px; -fx-font-weight: 700;");
        secBadge.getChildren().addAll(secDot, lblSecBadge);

        sessionsCardHeader.getChildren().addAll(lblSessionsTitle, spSessions, secBadge);

        // Table Header
        HBox tblHeader = new HBox(8);
        tblHeader.setPadding(new Insets(8, 12, 8, 12));
        tblHeader.setStyle("-fx-background-color: #F8F5EE; -fx-background-radius: 6px;");
        tblHeader.setAlignment(Pos.CENTER_LEFT);

        Label thId = createTableHeader("MÃ PHIÊN", 90);
        Label thTable = createTableHeader("BÀN", 80);
        Label thDevice = createTableHeader("LOẠI THIẾT BỊ", 110);
        Label thIp = createTableHeader("IP & CỔNG CLIENT", 160);
        Label thRtt = createTableHeader("RTT LATENCY", 110);
        Label thPkts = createTableHeader("GÓI RX / TX", 120);
        Label thBytes = createTableHeader("LƯU LƯỢNG", 130);
        Label thStatus = createTableHeader("TRẠNG THÁI", 130);
        Region spTh = new Region();
        HBox.setHgrow(spTh, Priority.ALWAYS);
        Label thAction = createTableHeader("THAO TÁC", 140);
        thAction.setAlignment(Pos.CENTER_RIGHT);

        tblHeader.getChildren().addAll(thId, thTable, thDevice, thIp, thRtt, thPkts, thBytes, thStatus, spTh, thAction);

        // Scrollable Rows
        VBox rowsContainer = new VBox(6);
        ScrollPane scrollPane = new ScrollPane(rowsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        final boolean[] isMeasuringPing = new boolean[]{false};
        final java.util.Map<String, Label> sessionRttLabels = new java.util.concurrent.ConcurrentHashMap<>();
        final java.util.Map<String, cafe.models.MessageProtocol.SessionSnapshotDto> prevSessionsMap = new java.util.HashMap<>();
        final java.util.Map<String, Long> disconnectedUntilMap = new java.util.HashMap<>();
        final java.util.Map<String, cafe.models.MessageProtocol.SessionSnapshotDto> disconnectedDtosMap = new java.util.HashMap<>();

        renderRows[0] = () -> {
            // Lấy dữ liệu từ Server trực tiếp hoặc từ Snapshot Socket gửi về
            java.util.List<cafe.models.MessageProtocol.SessionSnapshotDto> sessionDtos = new java.util.ArrayList<>();
            java.util.List<cafe.models.MessageProtocol.ReconnectRequestDto> reqDtos = new java.util.ArrayList<>();

            if (server.isServerRunning()) {
                cafe.models.MessageProtocol.AdminTelemetrySnapshotMessage localSnap = server.buildTelemetrySnapshotMessage();
                if (localSnap.sessions != null) sessionDtos.addAll(localSnap.sessions);
                if (localSnap.pendingRequests != null) reqDtos.addAll(localSnap.pendingRequests);
            } else {
                cafe.models.MessageProtocol.AdminTelemetrySnapshotMessage remoteSnap = server.getRemoteTelemetrySnapshot();
                if (remoteSnap != null) {
                    if (remoteSnap.sessions != null) sessionDtos.addAll(remoteSnap.sessions);
                    if (remoteSnap.pendingRequests != null) reqDtos.addAll(remoteSnap.pendingRequests);
                }
            }

            // Quản lý các phiên vừa ngắt kết nối trong 5 giây (Section 3.5)
            long now = System.currentTimeMillis();
            java.util.Set<String> currentIds = new java.util.HashSet<>();
            for (cafe.models.MessageProtocol.SessionSnapshotDto s : sessionDtos) {
                currentIds.add(s.sessionId);
                disconnectedUntilMap.remove(s.sessionId);
                disconnectedDtosMap.remove(s.sessionId);
            }
            for (java.util.Map.Entry<String, cafe.models.MessageProtocol.SessionSnapshotDto> entry : prevSessionsMap.entrySet()) {
                if (!currentIds.contains(entry.getKey()) && !disconnectedUntilMap.containsKey(entry.getKey())) {
                    disconnectedUntilMap.put(entry.getKey(), now + 5000L);
                    disconnectedDtosMap.put(entry.getKey(), entry.getValue());
                }
            }
            disconnectedUntilMap.entrySet().removeIf(e -> e.getValue() <= now);
            disconnectedDtosMap.keySet().removeIf(id -> !disconnectedUntilMap.containsKey(id));

            prevSessionsMap.clear();
            for (cafe.models.MessageProtocol.SessionSnapshotDto s : sessionDtos) {
                prevSessionsMap.put(s.sessionId, s);
            }

            if (!disconnectedUntilMap.isEmpty()) {
                new Thread(() -> {
                    try { Thread.sleep(5100); } catch (InterruptedException ignored) {}
                    Platform.runLater(renderRows[0]::run);
                }).start();
            }

            // Render Pending Reconnect Requests
            if (reqDtos.isEmpty()) {
                pendingCard.getChildren().clear();
                pendingCard.setVisible(false);
                pendingCard.setManaged(false);
            } else {
                pendingCard.getChildren().clear();
                pendingCard.setVisible(true);
                pendingCard.setManaged(true);

                HBox pendingHeader = new HBox(8);
                pendingHeader.setAlignment(Pos.CENTER_LEFT);
                Circle warnDot = new Circle(4.5, Color.web("#D97706"));
                Label lblPendingTitle = new Label("YÊU CẦU NỐI LẠI PHIÊN ĐANG CHỜ DUYỆT (" + reqDtos.size() + ")");
                lblPendingTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #92400E;");
                Region spPending = new Region();
                HBox.setHgrow(spPending, Priority.ALWAYS);
                Label lblPendingHint = new Label("Thiết bị bị ngắt kết nối yêu cầu khôi phục quyền truy cập");
                lblPendingHint.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #B45309;");
                pendingHeader.getChildren().addAll(warnDot, lblPendingTitle, spPending, lblPendingHint);
                pendingCard.getChildren().add(pendingHeader);

                for (cafe.models.MessageProtocol.ReconnectRequestDto req : reqDtos) {
                    HBox reqRow = new HBox(10);
                    reqRow.setAlignment(Pos.CENTER_LEFT);
                    reqRow.setPadding(new Insets(8, 12, 8, 12));
                    reqRow.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 8px; -fx-border-color: #FCD34D; -fx-border-radius: 8px;");

                    String timeStr = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date(req.requestTime));
                    Label lblTime = new Label(timeStr);
                    lblTime.setStyle("-fx-font-family: monospace; -fx-font-size: 11px; -fx-text-fill: #6B7280;");

                    Label lblDev = new Label(req.deviceId + " (Bàn " + (req.tableNumber < 10 ? "0" + req.tableNumber : req.tableNumber) + ")");
                    lblDev.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #92400E;");

                    Label lblType = new Label(req.deviceType != null ? req.deviceType : "KIOSK");
                    lblType.setStyle("-fx-font-size: 11px; -fx-background-color: #FEF3C7; -fx-text-fill: #78350F; -fx-padding: 2 6 2 6; -fx-background-radius: 4px;");

                    Label lblIp = new Label("IP: " + (req.ip != null ? req.ip : "127.0.0.1"));
                    lblIp.setStyle("-fx-font-family: monospace; -fx-font-size: 11.5px; -fx-text-fill: #4B5563;");

                    Label lblNote = new Label(req.note != null ? req.note : "Xin mở lại phiên làm việc");
                    lblNote.setStyle("-fx-font-size: 12px; -fx-text-fill: #374151;");
                    HBox.setHgrow(lblNote, Priority.ALWAYS);

                    Button btnApprove = new Button("Phê duyệt");
                    btnApprove.setMinWidth(Region.USE_PREF_SIZE);
                    btnApprove.setStyle("-fx-background-color: #DCFCE7; -fx-text-fill: #15803D; -fx-font-size: 11px; -fx-font-weight: 700; -fx-background-radius: 6px; -fx-padding: 4 10 4 10; -fx-cursor: hand;");
                    btnApprove.setOnAction(ev -> {
                        if (server.isServerRunning()) {
                            server.approveReconnectRequest(req.deviceId);
                        } else if (clientSock != null) {
                            clientSock.sendAdminCommand("APPROVE_RECONNECT", req.deviceId, "", false);
                        }
                        if (toastCallback != null) {
                            toastCallback.accept("Đã phê duyệt cấp lại phiên cho thiết bị: " + req.deviceId);
                        }
                        renderRows[0].run();
                    });

                    Button btnReject = new Button("Từ chối");
                    btnReject.setMinWidth(Region.USE_PREF_SIZE);
                    btnReject.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #B91C1C; -fx-font-size: 11px; -fx-font-weight: 700; -fx-background-radius: 6px; -fx-padding: 4 10 4 10; -fx-cursor: hand;");
                    btnReject.setOnAction(ev -> {
                        if (server.isServerRunning()) {
                            server.rejectReconnectRequest(req.deviceId, "Quản trị viên từ chối cấp phiên");
                        } else if (clientSock != null) {
                            clientSock.sendAdminCommand("REJECT_RECONNECT", req.deviceId, "Quản trị viên từ chối cấp phiên", false);
                        }
                        if (toastCallback != null) {
                            toastCallback.accept("Đã từ chối cấp lại phiên cho thiết bị: " + req.deviceId);
                        }
                        renderRows[0].run();
                    });

                    reqRow.getChildren().addAll(lblTime, lblDev, lblType, lblIp, lblNote, btnApprove, btnReject);
                    pendingCard.getChildren().add(reqRow);
                }
            }

            rowsContainer.getChildren().clear();

            lblKpiPortVal.setText(String.valueOf(server.getActiveServerPort()));
            long onlineCount = sessionDtos.stream().filter(s -> !"STALE".equalsIgnoreCase(s.linkState)).count();
            long staleCount = sessionDtos.size() - onlineCount;
            if (staleCount > 0) {
                lblKpiSessionsVal.setText(onlineCount + " phiên (" + staleCount + " mất tín hiệu)");
            } else {
                lblKpiSessionsVal.setText(onlineCount + " phiên");
            }
            lblKpiRttVal.setText(server.getAverageRttMs() + " ms");
            double kb = (server.getTotalBytesReceived() + server.getTotalBytesSent()) / 1024.0;
            lblKpiTrafficVal.setText(String.format("%.1f KB", kb));

            if (sessionDtos.isEmpty() && disconnectedDtosMap.isEmpty()) {
                VBox emptyBox = new VBox(10);
                emptyBox.setAlignment(Pos.CENTER);
                emptyBox.setPadding(new Insets(36, 12, 36, 12));

                javafx.scene.Node emptyNetIcon = cafe.utils.IconHelper.getIcon("admin", "ha-tang-mang.png", cafe.utils.SvgIcons.SETTINGS, "", 32, Color.web("#A8A29E"));
                Label lblEmptyTitle = new Label("Chưa có thiết bị Kiosk hoặc Staff nào kết nối tới Server");
                lblEmptyTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #57534E;");

                Label lblEmptySub = new Label("Mở ứng dụng Client (Giao diện Kiosk) hoặc Tablet nhân viên để bắt đầu giao tiếp qua Socket TCP trên cổng " + server.getActiveServerPort());
                lblEmptySub.setStyle("-fx-font-size: 12px; -fx-text-fill: #A8A29E;");

                emptyBox.getChildren().addAll(emptyNetIcon, lblEmptyTitle, lblEmptySub);
                rowsContainer.getChildren().add(emptyBox);
                return;
            }

            for (cafe.models.MessageProtocol.SessionSnapshotDto s : sessionDtos) {
                boolean isStale = "STALE".equalsIgnoreCase(s.linkState);
                HBox row = new HBox(8);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(8, 12, 8, 12));
                if (isStale) {
                    row.setStyle("-fx-background-color: #FFFDF5; -fx-background-radius: 6px; -fx-border-color: #FDE68A; -fx-border-radius: 6px; -fx-border-width: 1px;");
                } else {
                    row.setStyle("-fx-background-color: #FAF8F5; -fx-background-radius: 6px; -fx-border-color: #EFE9DF; -fx-border-radius: 6px; -fx-border-width: 1px;");
                }

                Label rId = new Label("#" + s.sessionId);
                rId.setPrefWidth(90);
                rId.setStyle("-fx-font-family: monospace; -fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #112823;");

                String tblText = s.currentTable > 0 ? "Bàn " + String.format("%02d", s.currentTable) : "Chưa gán";
                Label rTable = new Label(tblText);
                rTable.setPrefWidth(80);
                rTable.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #78350F;");

                Label rDevice = new Label(s.deviceType != null ? s.deviceType : "KIOSK");
                rDevice.setPrefWidth(110);
                rDevice.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 600; -fx-text-fill: #4B5563;");

                Label rIp = new Label(s.remoteAddress != null ? s.remoteAddress : "127.0.0.1");
                rIp.setPrefWidth(160);
                rIp.setStyle("-fx-font-family: monospace; -fx-font-size: 11.5px; -fx-text-fill: #374151;");

                Label rRtt = new Label();
                rRtt.setPrefWidth(110);
                if (isStale) {
                    Circle rttDot = new Circle(4, Color.web("#D97706"));
                    rRtt.setGraphic(rttDot);
                    rRtt.setGraphicTextGap(5);
                    rRtt.setText("--");
                    rRtt.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #D97706;");
                } else {
                    long rtt = s.rttLatencyMs;
                    String rttColor = rtt <= 20 ? "#047857" : (rtt <= 80 ? "#D97706" : "#DC2626");
                    Circle rttDot = new Circle(4, Color.web(rttColor));
                    rRtt.setGraphic(rttDot);
                    rRtt.setGraphicTextGap(5);
                    rRtt.setText(rtt + " ms");
                    rRtt.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: " + rttColor + ";");
                }
                sessionRttLabels.put(s.sessionId, rRtt);

                Label rPkts = new Label(s.packetsReceived + " / " + s.packetsSent);
                rPkts.setPrefWidth(120);
                rPkts.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #4B5563;");

                double inKb = s.bytesReceived / 1024.0;
                double outKb = s.bytesSent / 1024.0;
                Label rBytes = new Label(String.format("%.1f / %.1f KB", inKb, outKb));
                rBytes.setPrefWidth(130);
                rBytes.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #4B5563;");

                Label rStatus = new Label();
                rStatus.setPrefWidth(130);
                if (s.isThrottled) {
                    Circle stDot = new Circle(4.5, Color.web("#DC2626"));
                    rStatus.setGraphic(stDot);
                    rStatus.setGraphicTextGap(5);
                    rStatus.setText("Throttled (" + s.throttledCount + ")");
                    rStatus.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #DC2626; -fx-background-color: #FEE2E2; -fx-background-radius: 4px; -fx-padding: 2 6;");
                } else if (isStale) {
                    Circle stDot = new Circle(4.5, Color.web("#D97706"));
                    rStatus.setGraphic(stDot);
                    rStatus.setGraphicTextGap(5);
                    String staleLabel = s.silentSeconds > 0 ? "Mất tín hiệu (" + s.silentSeconds + "s)" : "Mất tín hiệu";
                    rStatus.setText(staleLabel);
                    rStatus.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #92400E; -fx-background-color: #FFFBEB; -fx-background-radius: 4px; -fx-padding: 2 6;");
                } else {
                    Circle stDot = new Circle(4.5, Color.web("#16A34A"));
                    rStatus.setGraphic(stDot);
                    rStatus.setGraphicTextGap(5);
                    rStatus.setText("Đang kết nối");
                    rStatus.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #047857; -fx-background-color: #ECFDF5; -fx-background-radius: 4px; -fx-padding: 2 6;");
                }

                Region rSp = new Region();
                HBox.setHgrow(rSp, Priority.ALWAYS);

                HBox actionsBox = new HBox(6);
                actionsBox.setAlignment(Pos.CENTER_RIGHT);
                actionsBox.setPrefWidth(140);

                Button btnPingSingle = new Button("Ping");
                btnPingSingle.setMinWidth(Region.USE_PREF_SIZE);
                btnPingSingle.setStyle("-fx-background-color: #E0E7FF; -fx-text-fill: #3730A3; -fx-font-size: 11px; -fx-font-weight: 700; -fx-background-radius: 4px; -fx-cursor: hand; -fx-padding: 3 9;");
                btnPingSingle.setOnAction(ev -> {
                    isMeasuringPing[0] = true;
                    btnPingSingle.setText("Đang đo...");
                    btnPingSingle.setDisable(true);
                    if (server.isServerRunning()) {
                        server.sendPingWithCallback(s.sessionId, (rttVal) -> Platform.runLater(() -> {
                            btnPingSingle.setText("Ping");
                            btnPingSingle.setDisable(false);
                            isMeasuringPing[0] = false;
                            if (rttVal >= 0) {
                                rRtt.setText(rttVal + " ms");
                                if (toastCallback != null) toastCallback.accept("Phản hồi từ Phiên #" + s.sessionId + " (Bàn " + s.currentTable + "): " + rttVal + " ms");
                            } else {
                                rRtt.setText("Timeout");
                                if (toastCallback != null) toastCallback.accept("Phiên #" + s.sessionId + " không phản hồi gói tin Ping (Timeout)!");
                            }
                        }));
                    } else if (clientSock != null) {
                        clientSock.sendAdminCommand("PING_SINGLE", s.sessionId, "", false);
                        new Thread(() -> {
                            try { Thread.sleep(900); } catch (InterruptedException ignored) {}
                            Platform.runLater(() -> {
                                btnPingSingle.setText("Ping");
                                btnPingSingle.setDisable(false);
                                isMeasuringPing[0] = false;
                            });
                        }).start();
                    }
                });

                Button btnKick = new Button("Kick");
                btnKick.setMinWidth(Region.USE_PREF_SIZE);
                btnKick.setStyle("-fx-background-color: #FEE2E2; -fx-text-fill: #991B1B; -fx-font-size: 11px; -fx-font-weight: 700; -fx-background-radius: 4px; -fx-cursor: hand; -fx-padding: 3 9;");
                btnKick.setOnAction(ev -> {
                    if (server.isServerRunning()) {
                        server.kickSession(s.sessionId);
                        Platform.runLater(renderRows[0]::run);
                    } else if (clientSock != null) {
                        clientSock.sendAdminCommand("KICK_SESSION", s.sessionId, "Ngắt bởi Quản trị viên", false);
                    }
                    if (toastCallback != null) toastCallback.accept("Đã ngắt kết nối phiên #" + s.sessionId);
                });

                actionsBox.getChildren().addAll(btnPingSingle, btnKick);
                row.getChildren().addAll(rId, rTable, rDevice, rIp, rRtt, rPkts, rBytes, rStatus, rSp, actionsBox);
                rowsContainer.getChildren().add(row);
            }

            // Hiển thị các phiên vừa ngắt kết nối trong 5 giây (Section 3.5)
            for (cafe.models.MessageProtocol.SessionSnapshotDto s : disconnectedDtosMap.values()) {
                HBox row = new HBox(8);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(8, 12, 8, 12));
                row.setStyle("-fx-background-color: #F9FAFB; -fx-background-radius: 6px; -fx-border-color: #E5E7EB; -fx-border-radius: 6px; -fx-border-width: 1px; -fx-opacity: 0.75;");

                Label rId = new Label("#" + s.sessionId);
                rId.setPrefWidth(90);
                rId.setStyle("-fx-font-family: monospace; -fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #6B7280;");

                String tblText = s.currentTable > 0 ? "Bàn " + String.format("%02d", s.currentTable) : "Chưa gán";
                Label rTable = new Label(tblText);
                rTable.setPrefWidth(80);
                rTable.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #9CA3AF;");

                Label rDevice = new Label(s.deviceType != null ? s.deviceType : "KIOSK");
                rDevice.setPrefWidth(110);
                rDevice.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 600; -fx-text-fill: #9CA3AF;");

                Label rIp = new Label(s.remoteAddress != null ? s.remoteAddress : "127.0.0.1");
                rIp.setPrefWidth(160);
                rIp.setStyle("-fx-font-family: monospace; -fx-font-size: 11.5px; -fx-text-fill: #9CA3AF;");

                Label rRtt = new Label("--");
                rRtt.setPrefWidth(110);
                Circle rttDot = new Circle(4, Color.web("#9CA3AF"));
                rRtt.setGraphic(rttDot);
                rRtt.setGraphicTextGap(5);
                rRtt.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #9CA3AF;");

                Label rPkts = new Label(s.packetsReceived + " / " + s.packetsSent);
                rPkts.setPrefWidth(120);
                rPkts.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #9CA3AF;");

                double inKb = s.bytesReceived / 1024.0;
                double outKb = s.bytesSent / 1024.0;
                Label rBytes = new Label(String.format("%.1f / %.1f KB", inKb, outKb));
                rBytes.setPrefWidth(130);
                rBytes.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #9CA3AF;");

                Label rStatus = new Label();
                rStatus.setPrefWidth(130);
                Circle stDot = new Circle(4.5, Color.web("#9CA3AF"));
                rStatus.setGraphic(stDot);
                rStatus.setGraphicTextGap(5);
                rStatus.setText("Đã ngắt");
                rStatus.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #4B5563; -fx-background-color: #F3F4F6; -fx-background-radius: 4px; -fx-padding: 2 6;");

                Region rSp = new Region();
                HBox.setHgrow(rSp, Priority.ALWAYS);

                HBox actionsBox = new HBox(6);
                actionsBox.setAlignment(Pos.CENTER_RIGHT);
                actionsBox.setPrefWidth(140);

                Button btnPingSingle = new Button("Ping");
                btnPingSingle.setMinWidth(Region.USE_PREF_SIZE);
                btnPingSingle.setDisable(true);
                btnPingSingle.setStyle("-fx-background-color: #F3F4F6; -fx-text-fill: #9CA3AF; -fx-font-size: 11px; -fx-font-weight: 700; -fx-background-radius: 4px; -fx-padding: 3 9;");

                Button btnKick = new Button("Kick");
                btnKick.setMinWidth(Region.USE_PREF_SIZE);
                btnKick.setDisable(true);
                btnKick.setStyle("-fx-background-color: #F3F4F6; -fx-text-fill: #9CA3AF; -fx-font-size: 11px; -fx-font-weight: 700; -fx-background-radius: 4px; -fx-padding: 3 9;");

                actionsBox.getChildren().addAll(btnPingSingle, btnKick);
                row.getChildren().addAll(rId, rTable, rDevice, rIp, rRtt, rPkts, rBytes, rStatus, rSp, actionsBox);
                rowsContainer.getChildren().add(row);
            }
        };

        if (clientSock != null) {
            clientSock.addTelemetryListener(snap -> Platform.runLater(renderRows[0]::run));
            clientSock.addPingResultListener(pingRes -> Platform.runLater(() -> {
                isMeasuringPing[0] = false;
                lblPingAllText.setText("Ping toàn bộ thiết bị");
                btnPingAll.setDisable(false);
                if ("ALL".equalsIgnoreCase(pingRes.sessionId)) {
                    lblKpiRttVal.setText(pingRes.avgRttMs + " ms");
                    if (toastCallback != null) {
                        toastCallback.accept("Đã đo độ trễ toàn mạng: RTT trung bình " + pingRes.avgRttMs + " ms");
                    }
                } else {
                    Label targetLbl = sessionRttLabels.get(pingRes.sessionId);
                    if (targetLbl != null && pingRes.rttMs >= 0) {
                        targetLbl.setText(pingRes.rttMs + " ms");
                    }
                    if (toastCallback != null) {
                        toastCallback.accept(pingRes.success
                            ? "Phản hồi từ Phiên #" + pingRes.sessionId + ": " + pingRes.rttMs + " ms"
                            : "Phiên #" + pingRes.sessionId + " không phản hồi gói tin Ping!");
                    }
                }
                renderRows[0].run();
            }));
        }

        btnPingAll.setOnAction(e -> {
            if (!server.isServerRunning() && clientSock != null) {
                isMeasuringPing[0] = true;
                lblPingAllText.setText("Đang đo toàn mạng...");
                btnPingAll.setDisable(true);
                clientSock.sendAdminCommand("PING_ALL", "ALL", "", false);
                new Thread(() -> {
                    try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
                    Platform.runLater(() -> {
                        isMeasuringPing[0] = false;
                        lblPingAllText.setText("Ping toàn bộ thiết bị");
                        btnPingAll.setDisable(false);
                    });
                }).start();
                return;
            }
            List<CafeServerService.ClientSession> sessions = server.getActiveSessionsList();
            if (sessions.isEmpty()) {
                if (toastCallback != null) toastCallback.accept("Chưa có thiết bị nào kết nối để đo Ping.");
                return;
            }
            isMeasuringPing[0] = true;
            lblPingAllText.setText("Đang đo toàn mạng...");
            btnPingAll.setDisable(true);

            java.util.concurrent.atomic.AtomicInteger pendingResponses = new java.util.concurrent.atomic.AtomicInteger(sessions.size());
            java.util.List<Long> latencies = new java.util.concurrent.CopyOnWriteArrayList<>();

            for (CafeServerService.ClientSession s : sessions) {
                server.sendPingWithCallback(s.sessionId, (rttVal) -> Platform.runLater(() -> {
                    Label targetLbl = sessionRttLabels.get(s.sessionId);
                    if (targetLbl != null && rttVal >= 0) {
                        latencies.add(rttVal);
                        targetLbl.setText(rttVal + " ms");
                    }
                    if (pendingResponses.decrementAndGet() == 0) {
                        isMeasuringPing[0] = false;
                        lblPingAllText.setText("Ping toàn bộ thiết bị");
                        btnPingAll.setDisable(false);
                        long avg = latencies.isEmpty() ? 0 : Math.round(latencies.stream().mapToLong(Long::longValue).average().orElse(0));
                        lblKpiRttVal.setText(avg + " ms");
                        if (toastCallback != null) {
                            toastCallback.accept("Đã đo độ trễ " + latencies.size() + "/" + sessions.size() + " thiết bị: RTT trung bình " + avg + " ms");
                        }
                    }
                }));
            }
        });

        btnRefresh.setOnAction(e -> {
            if (!server.isServerRunning() && clientSock != null) {
                clientSock.requestAdminTelemetry();
            }
            renderRows[0].run();
            if (toastCallback != null) toastCallback.accept("Đã cập nhật bảng số liệu mạng từ máy chủ!");
        });

        sessionsCard.getChildren().addAll(sessionsCardHeader, tblHeader, scrollPane);

        // 4. Network Protocol & Academic Overview (Thuyết minh đồ án LTM)
        HBox infoBox = new HBox(12);
        infoBox.setStyle("-fx-background-color: #FAF8F5; -fx-background-radius: 10px; -fx-border-color: #E2DDD5; -fx-border-radius: 10px; -fx-padding: 12 16 12 16;");

        VBox c1 = createNetworkDocCard("1. Giao thức TCP Socket Full-Duplex", "Duy trì kết nối 2 chiều liên tục qua java.net.Socket. Cấu hình tcpNoDelay(true) để triệt tiêu thuật toán Nagle, tối ưu độ trễ gói tin < 5ms.");
        VBox c2 = createNetworkDocCard("2. Bảng Định tuyến Decoupled Routing", "Tách biệt Session ID và Mã bàn (Table Number). Một bàn có thể kết nối nhiều thiết bị (Tablet nhân viên + Kiosk khách) đồng bộ trạng thái tức thời.");
        VBox c3 = createNetworkDocCard("3. Cơ chế Rate-Limiting & Chống Spam", "Thuật toán Sliding Window Log chặn khi tần suất > 15 req/s. Ngăn chặn nguy cơ tấn công từ chối dịch vụ (DoS/Flooding) lên KDS máy chủ pha chế.");

        infoBox.getChildren().addAll(c1, c2, c3);

        // Auto-refresh timer: 3 seconds
        Timeline timer = new Timeline(new KeyFrame(Duration.seconds(3), ev -> {
            if (!isMeasuringPing[0]) {
                if (!server.isServerRunning() && clientSock != null) {
                    clientSock.requestAdminTelemetry();
                }
                renderRows[0].run();
            }
        }));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();

        root.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                timer.stop();
            }
        });

        renderRows[0].run();
        server.setOnReconnectRequestsChanged(() -> Platform.runLater(renderRows[0]::run));
        root.getChildren().addAll(header, kpiRow, pendingCard, sessionsCard, infoBox);
        return root;
    }

    private static Label createTableHeader(String text, double width) {
        Label lbl = new Label(text);
        lbl.setPrefWidth(width);
        lbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #78716C; -fx-letter-spacing: 0.5px;");
        return lbl;
    }

    private static VBox createNetworkDocCard(String title, String desc) {
        VBox box = new VBox(4);
        HBox.setHgrow(box, Priority.ALWAYS);
        Label lTitle = new Label(title);
        lTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #112823;");
        Label lDesc = new Label(desc);
        lDesc.setWrapText(true);
        lDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #57534E; -fx-line-spacing: 2px;");
        box.getChildren().addAll(lTitle, lDesc);
        return box;
    }

    public static void showNetworkPortModal(Stage owner, Consumer<String> toastCallback) {
        showNetworkPortModal(owner, toastCallback, null);
    }

    public static void showNetworkPortModal(Stage owner, Consumer<String> toastCallback, Runnable onPortChanged) {
        Stage modal = createModal(owner, "Cấu hình Cổng Mạng Socket");
        VBox root = new VBox(14);
        root.setPrefWidth(420);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 12px; -fx-border-color: #E6E0D6; -fx-border-radius: 12px;");

        Label lblTitle = new Label("Cấu hình Cổng Mạng Socket (TCP)");
        lblTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #112823;");

        Label lblDesc = new Label("Thay đổi cổng mạng cho Máy chủ KDS và các Kiosk/Staff Client kết nối trong mạng LAN:");
        lblDesc.setWrapText(true);
        lblDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #6F695F;");

        VBox form = new VBox(10);
        Label lblPort = new Label("Cổng Socket TCP (Mặc định khuyến nghị: 8888):");
        lblPort.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #112823;");

        int activePort = CafeServerService.getInstance().getActiveServerPort();
        TextField txtPort = new TextField(String.valueOf(activePort));
        txtPort.setStyle(INPUT_STYLE);

        Label lblHost = new Label("Địa chỉ Máy chủ IP (Mặc định: 127.0.0.1 hoặc IP mạng LAN):");
        lblHost.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #112823;");

        TextField txtHost = new TextField(cafe.utils.NetworkConfig.getServerHost());
        txtHost.setStyle(INPUT_STYLE);

        Label lblNotice = new Label("Lưu ý: Thay đổi cổng mạng sẽ lưu vào file config/network.properties và Máy chủ sẽ lập tức chuyển sang lắng nghe trên cổng mới.");
        lblNotice.setWrapText(true);
        lblNotice.setStyle("-fx-font-size: 11px; -fx-text-fill: #065F46; -fx-background-color: #ECFDF5; -fx-padding: 8 10; -fx-background-radius: 6px;");

        form.getChildren().addAll(lblPort, txtPort, lblHost, txtHost, lblNotice);

        HBox btns = new HBox(10);
        btns.setAlignment(Pos.CENTER_RIGHT);

        Button btnCancel = new Button("Hủy");
        btnCancel.setStyle("-fx-background-color: #F5EFEB; -fx-text-fill: #4A453E; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-background-radius: 6px; -fx-padding: 7 14; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> modal.close());

        Button btnSave = new Button("Lưu Cấu Hình Mạng");
        btnSave.setStyle("-fx-background-color: #112823; -fx-text-fill: #FFFFFF; -fx-font-size: 12.5px; -fx-font-weight: 700; -fx-background-radius: 6px; -fx-padding: 7 18; -fx-cursor: hand;");
        btnSave.setOnAction(e -> {
            try {
                int port = Integer.parseInt(txtPort.getText().trim());
                if (port < 1024 || port > 65535) {
                    if (toastCallback != null) toastCallback.accept("Cổng mạng phải nằm trong khoảng 1024 - 65535!");
                    return;
                }
                String host = txtHost.getText().trim();
                if (host.isEmpty()) host = "127.0.0.1";
                cafe.utils.NetworkConfig.saveConfig(host, port);
                boolean ok = CafeServerService.getInstance().restartServer(port);
                if (onPortChanged != null) {
                    onPortChanged.run();
                }
                if (toastCallback != null) {
                    if (ok) {
                        toastCallback.accept("✅ Đã chuyển đổi thành công sang cổng " + port + "! Máy chủ đang lắng nghe.");
                    } else {
                        toastCallback.accept("⚠️ Đã lưu cấu hình cổng " + port + " nhưng chưa mở được ngay (kiểm tra xung đột cổng).");
                    }
                }
                modal.close();
            } catch (NumberFormatException ex) {
                if (toastCallback != null) toastCallback.accept("Cổng mạng phải là số nguyên hợp lệ!");
            }
        });

        btns.getChildren().addAll(btnCancel, btnSave);
        root.getChildren().addAll(lblTitle, lblDesc, form, btns);
        showModal(modal, root);
    }
}
