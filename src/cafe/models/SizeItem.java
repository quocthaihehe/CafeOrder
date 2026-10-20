package cafe.models;

public class SizeItem {
    private String sizeCode;
    private String displayName;
    private int volumeMl;
    private double priceDelta;

    public SizeItem(String sizeCode, String displayName, int volumeMl, double priceDelta) {
        this.sizeCode = sizeCode;
        this.displayName = displayName;
        this.volumeMl = volumeMl;
        this.priceDelta = priceDelta;
    }

    public String getSizeCode() { return sizeCode; }
    public String getDisplayName() { return displayName; }
    public int getVolumeMl() { return volumeMl; }
    public double getPriceDelta() { return priceDelta; }

    public String getLabel() {
        if (priceDelta > 0) {
            return String.format("%s (+%,.0f đ)", displayName, priceDelta);
        } else if (priceDelta < 0) {
            return String.format("%s (%,.0f đ)", displayName, priceDelta);
        }
        return displayName + " (Chuẩn)";
    }

    @Override
    public String toString() {
        return getLabel();
    }
}
