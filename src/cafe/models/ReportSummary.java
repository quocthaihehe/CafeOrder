package cafe.models;

public class ReportSummary {
    private double totalRevenue;
    private int totalOrders;
    private int completedOrders;
    private int cancelledOrders;
    private int pendingOrders;
    private double averageOrderValue;

    public ReportSummary(double totalRevenue, int totalOrders, int completedOrders, int cancelledOrders, int pendingOrders) {
        this.totalRevenue = totalRevenue;
        this.totalOrders = totalOrders;
        this.completedOrders = completedOrders;
        this.cancelledOrders = cancelledOrders;
        this.pendingOrders = pendingOrders;
        this.averageOrderValue = completedOrders > 0 ? totalRevenue / completedOrders : 0.0;
    }

    public double getTotalRevenue() { return totalRevenue; }
    public int getTotalOrders() { return totalOrders; }
    public int getCompletedOrders() { return completedOrders; }
    public int getCancelledOrders() { return cancelledOrders; }
    public int getPendingOrders() { return pendingOrders; }
    public double getAverageOrderValue() { return averageOrderValue; }

    public String getFormattedTotalRevenue() {
        return String.format("%,.0f đ", totalRevenue);
    }

    public String getFormattedAvgOrderValue() {
        return String.format("%,.0f đ", averageOrderValue);
    }
}
