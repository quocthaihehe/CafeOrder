package cafe.models;

import java.util.ArrayList;
import java.util.List;

public class OrderItem {
    private int productId;
    private String name;
    private int qty;
    private double basePrice;
    private double unitPrice;
    private String status = "CONFIRMED"; // CONFIRMED, OUT_OF_STOCK, CANCELLED

    private String sizeCode; // S, M, L
    private double sizeDelta;
    private int sugarPercent; // 0, 30, 50, 70, 100
    private int icePercent;   // 0, 30, 50, 70, 100
    private String temperature; // COLD, HOT
    private List<ToppingItem> toppings = new ArrayList<>();
    private String note;

    public OrderItem(String name, int qty, String note) {
        this(0, name, qty, 0.0, "M", 0.0, 100, 100, "COLD", new ArrayList<>(), note, "CONFIRMED");
        parseLegacyCustomizationsFromNote();
    }

    public OrderItem(String name, int qty, String note, double unitPrice) {
        this(0, name, qty, unitPrice, "M", 0.0, 100, 100, "COLD", new ArrayList<>(), note, "CONFIRMED");
        parseLegacyCustomizationsFromNote();
    }

    public OrderItem(String name, int qty, String note, double unitPrice, String status) {
        this(0, name, qty, unitPrice, "M", 0.0, 100, 100, "COLD", new ArrayList<>(), note, status);
        parseLegacyCustomizationsFromNote();
    }

    public OrderItem(int productId, String name, int qty, double basePrice, String sizeCode, double sizeDelta,
                     int sugarPercent, int icePercent, String temperature, List<ToppingItem> toppings, String note) {
        this(productId, name, qty, basePrice, sizeCode, sizeDelta, sugarPercent, icePercent, temperature, toppings, note, "CONFIRMED");
    }

    public OrderItem(int productId, String name, int qty, double basePrice, String sizeCode, double sizeDelta,
                     int sugarPercent, int icePercent, String temperature, List<ToppingItem> toppings, String note, String status) {
        this.productId = productId;
        this.name = name;
        this.qty = qty;
        this.basePrice = basePrice;
        this.sizeCode = sizeCode != null ? sizeCode : "M";
        this.sizeDelta = sizeDelta;
        this.sugarPercent = sugarPercent;
        this.icePercent = icePercent;
        this.temperature = temperature != null ? temperature : "COLD";
        this.toppings = toppings != null ? toppings : new ArrayList<>();
        this.note = note != null ? note : "";
        this.status = status != null ? status : "CONFIRMED";

        recalculateUnitPrice();
    }

    private void parseLegacyCustomizationsFromNote() {
        if (this.note == null || this.note.trim().isEmpty()) return;
        String raw = this.note.trim();
        List<String> remainingNotes = new ArrayList<>();

        String[] tokens = raw.split("•");
        for (String t : tokens) {
            String token = t.trim();
            if (token.isEmpty()) continue;

            java.util.regex.Matcher mSize = java.util.regex.Pattern.compile("(?i)^[«\"']?\\s*size\\s*([SML])\\s*[»\"']?$").matcher(token);
            if (mSize.find()) {
                this.sizeCode = mSize.group(1).toUpperCase();
                continue;
            }

            if (token.startsWith("+")) {
                String topName = token.substring(1).trim();
                if (!topName.isEmpty()) {
                    boolean exists = false;
                    for (ToppingItem item : this.toppings) {
                        if (topName.equalsIgnoreCase(item.getName())) {
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        this.toppings.add(new ToppingItem(topName, 0.0));
                    }
                }
                continue;
            }

            java.util.regex.Matcher mSugar = java.util.regex.Pattern.compile("(?i)(\\d+)%\\s*đường").matcher(token);
            if (mSugar.find()) {
                try {
                    this.sugarPercent = Integer.parseInt(mSugar.group(1));
                    continue;
                } catch (NumberFormatException ignored) {}
            }
            if (token.matches("(?i).*không đường.*")) {
                this.sugarPercent = 0;
                continue;
            }

            java.util.regex.Matcher mIce = java.util.regex.Pattern.compile("(?i)(?:đá\\s*)?(\\d+)%\\s*(?:đá)?").matcher(token);
            if (mIce.find() && token.toLowerCase().contains("đá")) {
                try {
                    this.icePercent = Integer.parseInt(mIce.group(1));
                    continue;
                } catch (NumberFormatException ignored) {}
            }
            if (token.matches("(?i).*không đá.*")) {
                this.icePercent = 0;
                continue;
            }

            if (token.matches("(?i).*nóng.*")) {
                this.temperature = "HOT";
                this.icePercent = 0;
                continue;
            }

            remainingNotes.add(token);
        }
        this.note = String.join(" • ", remainingNotes).trim();
    }

    public void recalculateUnitPrice() {
        double total = basePrice + sizeDelta;
        if (toppings != null) {
            for (ToppingItem t : toppings) {
                total += t.getPrice();
            }
        }
        if (total > 0 || unitPrice == 0) {
            this.unitPrice = Math.max(0, total);
        }
    }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getQty() { return qty; }
    public void setQty(int qty) { this.qty = qty; }

    public double getBasePrice() { return basePrice; }
    public void setBasePrice(double basePrice) {
        this.basePrice = basePrice;
        recalculateUnitPrice();
    }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isBillable() {
        return qty > 0
                && Double.isFinite(unitPrice)
                && unitPrice >= 0
                && (status == null || "CONFIRMED".equalsIgnoreCase(status.trim()));
    }

    public String getSizeCode() { return sizeCode; }
    public void setSizeCode(String sizeCode) { this.sizeCode = sizeCode; }

    public double getSizeDelta() { return sizeDelta; }
    public void setSizeDelta(double sizeDelta) {
        this.sizeDelta = sizeDelta;
        recalculateUnitPrice();
    }

    public int getSugarPercent() { return sugarPercent; }
    public void setSugarPercent(int sugarPercent) { this.sugarPercent = sugarPercent; }

    public int getIcePercent() { return icePercent; }
    public void setIcePercent(int icePercent) { this.icePercent = icePercent; }

    public String getTemperature() { return temperature; }
    public void setTemperature(String temperature) { this.temperature = temperature; }

    public List<ToppingItem> getToppings() { return toppings; }
    public void setToppings(List<ToppingItem> toppings) {
        this.toppings = toppings != null ? toppings : new ArrayList<>();
        recalculateUnitPrice();
    }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public double getSubtotal() {
        if (qty <= 0 || !Double.isFinite(unitPrice) || unitPrice < 0) {
            return 0.0;
        }
        return unitPrice * qty;
    }

    public String getFormattedSubtotal() {
        return String.format("%,.0f đ", getSubtotal());
    }

    public String getCustomizationSummary() {
        List<String> parts = new ArrayList<>();
        if (sizeCode != null && !sizeCode.isEmpty()) {
            parts.add("Size " + sizeCode);
        }
        if ("HOT".equalsIgnoreCase(temperature)) {
            parts.add("Nóng");
        } else if (icePercent != 100) {
            parts.add("Đá " + icePercent + "%");
        }
        if (sugarPercent != 100) {
            parts.add(sugarPercent + "% đường");
        }
        if (toppings != null && !toppings.isEmpty()) {
            List<String> topNames = new ArrayList<>();
            for (ToppingItem t : toppings) {
                topNames.add(t.getName());
            }
            parts.add("+" + String.join(", ", topNames));
        }
        if (note != null && !note.trim().isEmpty()) {
            parts.add("«" + note.trim() + "»");
        }
        return String.join(" • ", parts);
    }

    @Override
    public String toString() {
        String custom = getCustomizationSummary();
        return qty + "x " + name + (custom.isEmpty() ? "" : " (" + custom + ")");
    }
}
