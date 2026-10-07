package cafe.models;

import cafe.utils.CurrencyFormatter;
import java.util.Objects;

public class ToppingItem {
    private int toppingId;
    private String name;
    private double price;

    public ToppingItem(int toppingId, String name, double price) {
        this.toppingId = toppingId;
        this.name = name;
        this.price = price;
    }

    public int getToppingId() { return toppingId; }
    public String getName() { return name; }
    public double getPrice() { return price; }

    public String getFormattedPrice() {
        return "+" + CurrencyFormatter.format(price);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ToppingItem that = (ToppingItem) o;
        return toppingId == that.toppingId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(toppingId);
    }

    @Override
    public String toString() {
        return name + " (" + getFormattedPrice() + ")";
    }
}
