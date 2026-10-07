package cafe.views;

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
import java.util.function.BiConsumer;

public class ItemDetailDialog extends Stage {
    private int quantity = 1;
    private final MenuItem item;
    private final BiConsumer<Integer, String> onConfirmed;

    public ItemDetailDialog(Stage parentStage, MenuItem item, BiConsumer<Integer, String> onConfirmed) {
        this.item = item;
        this.onConfirmed = onConfirmed;

        initOwner(parentStage);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.UTILITY);
        setTitle("Tùy Chọn Món • " + item.getName());

        initUI();
    }

    private void initUI() {
        VBox root = new VBox(16);
        root.getStyleClass().add("modal-canvas");
        root.setPrefWidth(420);

        // Header: Glyph + Info
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane glyphBox = new StackPane();
        glyphBox.getStyleClass().add("item-glyph-box");
        glyphBox.setPrefSize(68, 68);
        Label glyph = new Label(item.getIconEmoji());
        glyph.setStyle("-fx-font-size: 32px;");
        glyphBox.getChildren().add(glyph);

        VBox infoBox = new VBox(4);
        Label lblName = new Label(item.getName());
        lblName.getStyleClass().add("modal-title");

        Label lblCategory = new Label("Danh mục: " + item.getCategory());
        lblCategory.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #9C8980;");

        Label lblPrice = new Label(CurrencyFormatter.format(item.getPrice()));
        lblPrice.getStyleClass().add("modal-price");

        infoBox.getChildren().addAll(lblName, lblCategory, lblPrice);
        header.getChildren().addAll(glyphBox, infoBox);

        // Description
        Label lblDesc = new Label(item.getDescription());
        lblDesc.getStyleClass().add("modal-desc");
        lblDesc.setWrapText(true);

        Separator sep1 = new Separator();

        // Quantity Stepper
        HBox qtyRow = new HBox(14);
        qtyRow.setAlignment(Pos.CENTER_LEFT);

        Label lblQtyTitle = new Label("Số lượng:");
        lblQtyTitle.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 700; -fx-text-fill: #2C1810;");

        Region spacerQty = new Region();
        HBox.setHgrow(spacerQty, Priority.ALWAYS);

        Button btnMinus = new Button("-");
        btnMinus.getStyleClass().add("stepper-btn");
        btnMinus.setPrefSize(32, 32);

        Label lblQtyValue = new Label("1");
        lblQtyValue.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #2C1810; -fx-min-width: 32px; -fx-alignment: center;");

        Button btnPlus = new Button("+");
        btnPlus.getStyleClass().add("stepper-btn");
        btnPlus.setPrefSize(32, 32);

        Label lblSubtotal = new Label(CurrencyFormatter.format(item.getPrice()));
        lblSubtotal.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: #C97A44; -fx-padding: 0 0 0 10;");

        btnMinus.setOnAction(e -> {
            if (quantity > 1) {
                quantity--;
                lblQtyValue.setText(String.valueOf(quantity));
                lblSubtotal.setText(CurrencyFormatter.format(item.getPrice() * quantity));
            }
        });

        btnPlus.setOnAction(e -> {
            quantity++;
            lblQtyValue.setText(String.valueOf(quantity));
            lblSubtotal.setText(CurrencyFormatter.format(item.getPrice() * quantity));
        });

        qtyRow.getChildren().addAll(lblQtyTitle, spacerQty, btnMinus, lblQtyValue, btnPlus, lblSubtotal);

        // Quick Suggestion Chips for Notes
        Label lblNoteTitle = new Label("Ghi chú pha chế:");
        lblNoteTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #2C1810;");

        FlowPane chipsPane = new FlowPane(8, 8);
        String[] suggestions = {"Ít ngọt", "Không đường", "Ít đá", "Không đá", "Nhiều sữa", "Mang về"};
        TextArea txtNote = new TextArea();
        txtNote.getStyleClass().add("custom-textarea");
        txtNote.setPromptText("Ví dụ: Nhiều béo, ít ngọt, mang về...");
        txtNote.setPrefRowCount(2);
        txtNote.setWrapText(true);

        for (String s : suggestions) {
            Button chip = new Button(s);
            chip.getStyleClass().add("suggestion-chip");
            chip.setOnAction(e -> {
                String cur = txtNote.getText().trim();
                if (cur.isEmpty()) {
                    txtNote.setText(s);
                } else if (!cur.contains(s)) {
                    txtNote.setText(cur + ", " + s);
                }
            });
            chipsPane.getChildren().add(chip);
        }

        // Action Buttons
        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(8, 0, 0, 0));

        Button btnCancel = new Button("Đóng");
        btnCancel.getStyleClass().add("btn-secondary");
        btnCancel.setOnAction(e -> close());

        Button btnConfirm = new Button("✓ Thêm vào giỏ");
        btnConfirm.getStyleClass().add("btn-primary");
        btnConfirm.setOnAction(e -> {
            if (onConfirmed != null) {
                onConfirmed.accept(quantity, txtNote.getText().trim());
            }
            close();
        });

        actions.getChildren().addAll(btnCancel, btnConfirm);

        root.getChildren().addAll(
            header,
            lblDesc,
            sep1,
            qtyRow,
            lblNoteTitle,
            chipsPane,
            txtNote,
            actions
        );

        Scene scene = new Scene(root);
        try {
            scene.getStylesheets().add(getClass().getResource("/cafe/views/client.css").toExternalForm());
        } catch (Exception ignored) {}
        setScene(scene);
    }
}
