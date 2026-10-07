package cafe.models;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class CartModel {
    private final ObservableList<OrderItem> items = FXCollections.observableArrayList();
    private final DoubleProperty subtotalAmount = new SimpleDoubleProperty(0.0);
    private final DoubleProperty discountAmount = new SimpleDoubleProperty(0.0);
    private final DoubleProperty totalAmount = new SimpleDoubleProperty(0.0);
    private final IntegerProperty totalQuantity = new SimpleIntegerProperty(0);

    private String orderType = "DINE_IN"; // "DINE_IN" or "TAKEAWAY"
    private Voucher appliedVoucher = null;

    public CartModel() {
        recalculate();
    }

    public ObservableList<OrderItem> getItems() {
        return items;
    }

    public DoubleProperty subtotalAmountProperty() { return subtotalAmount; }
    public double getSubtotalAmount() { return subtotalAmount.get(); }

    public DoubleProperty discountAmountProperty() { return discountAmount; }
    public double getDiscountAmount() { return discountAmount.get(); }

    public DoubleProperty totalAmountProperty() { return totalAmount; }
    public double getTotalAmount() { return totalAmount.get(); }

    public IntegerProperty totalQuantityProperty() { return totalQuantity; }
    public int getTotalQuantity() { return totalQuantity.get(); }

    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }

    public Voucher getAppliedVoucher() { return appliedVoucher; }

    public boolean applyVoucher(Voucher voucher) {
        if (voucher == null) {
            this.appliedVoucher = null;
            recalculate();
            return false;
        }
        if (getSubtotalAmount() < voucher.getMinOrderAmount()) {
            return false;
        }
        this.appliedVoucher = voucher;
        recalculate();
        return true;
    }

    public void removeVoucher() {
        this.appliedVoucher = null;
        recalculate();
    }

    public void addItem(MenuItem menuItem, int qty, String note) {
        if (qty <= 0) return;
        OrderItem oi = new OrderItem(menuItem.getId(), menuItem.getName(), qty, menuItem.getPrice(),
                "M", 0.0, 100, 100, "COLD", null, note);
        addItem(oi);
    }

    public void addItem(OrderItem newItem) {
        if (newItem == null || newItem.getQty() <= 0) return;
        boolean merged = false;

        for (OrderItem existing : items) {
            boolean sameName = existing.getName().equalsIgnoreCase(newItem.getName());
            boolean sameCustom = existing.getCustomizationSummary().equals(newItem.getCustomizationSummary());
            if (sameName && sameCustom) {
                existing.setQty(existing.getQty() + newItem.getQty());
                merged = true;
                break;
            }
        }

        if (!merged) {
            items.add(newItem);
        }
        recalculate();
    }

    public void updateQuantity(OrderItem item, int delta) {
        int newQty = item.getQty() + delta;
        if (newQty <= 0) {
            items.remove(item);
        } else {
            item.setQty(newQty);
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
        this.appliedVoucher = null;
        recalculate();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public void recalculate() {
        double subtotal = 0.0;
        int count = 0;
        for (OrderItem item : items) {
            subtotal += item.getSubtotal();
            count += item.getQty();
        }
        subtotalAmount.set(subtotal);
        totalQuantity.set(count);

        double discount = 0.0;
        if (appliedVoucher != null) {
            if (subtotal >= appliedVoucher.getMinOrderAmount()) {
                discount = appliedVoucher.calculateDiscount(subtotal);
            } else {
                appliedVoucher = null; // Huỷ voucher nếu đơn bị hạ dưới mức tối thiểu
            }
        }
        discountAmount.set(discount);
        totalAmount.set(Math.max(0, subtotal - discount));
    }
}
