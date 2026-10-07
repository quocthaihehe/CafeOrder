package cafe.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class CurrencyFormatter {
    private static final DecimalFormat formatter;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.of("vi", "VN"));
        symbols.setGroupingSeparator('.');
        formatter = new DecimalFormat("#,###", symbols);
    }

    public static String format(double amount) {
        return formatter.format(amount) + " đ";
    }
}
