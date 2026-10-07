package cafe.models;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class CartModel {
    private final ObservableList<OrderItem> items = FXCollections.observableArrayList();
    private final DoubleProperty totalAmount = new SimpleDoubleProperty(0.0);
    private final IntegerProperty totalQuantity = new SimpleIntegerProperty(0);

    public CartModel() {
        recalculate();
    }

    public ObservableList<OrderItem> getItems() {
        return items;
    }

    public DoubleProperty totalAmountProperty() {
        return totalAmount;
    }

    public double getTotalAmount() {
        return totalAmount.get();
    }

    public IntegerProperty totalQuantityProperty() {
        return totalQuantity;
    }

    public int getTotalQuantity() {
        return totalQuantity.get();
    }

    public void addItem(MenuItem menuItem, int qty, String note) {
        if (qty <= 0) return;
        String cleanNote = note != null ? note.trim() : "";
        boolean merged = false;

        for (OrderItem existing : items) {
            boolean sameName = existing.getName().equalsIgnoreCase(menuItem.getName());
            boolean sameNote = (existing.getNote() == null && cleanNote.isEmpty()) ||
                               (existing.getNote() != null && existing.getNote().equalsIgnoreCase(cleanNote));
            if (sameName && sameNote) {
                existing.setQty(existing.getQty() + qty);
                merged = true;
                break;
            }
        }

        if (!merged) {
            items.add(new OrderItem(menuItem.getName(), qty, cleanNote, menuItem.getPrice()));
        }
        recalculate();
    }

    public void updateQuantity(OrderItem item, int delta) {
        int newQty = item.getQty() + delta;
        if (newQty <= 0) {
            items.remove(item);
        } else {
            item.setQty(newQty);
            // Trigger list update for listeners
            int idx = items.indexOf(item);
            if (idx >= 0) {
                items.set(idx, item);
            }
        }
        recalculate();
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
        recalculate();
    }

    public void clear() {
        items.clear();
        recalculate();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public void recalculate() {
        double sum = 0.0;
        int count = 0;
        for (OrderItem item : items) {
            sum += item.getSubtotal();
            count += item.getQty();
        }
        totalAmount.set(sum);
        totalQuantity.set(count);
    }
}
