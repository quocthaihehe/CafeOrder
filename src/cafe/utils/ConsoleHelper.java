package cafe.utils;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Scanner;

public class ConsoleHelper {
    /**
     * Ép luồng xuất (Out và Err) sang UTF-8 để hiển thị đúng Tiếng Việt.
     */
    public static void setupUTF8() {
        try {
            System.setOut(new PrintStream(System.out, true, "UTF-8"));
            System.setErr(new PrintStream(System.err, true, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            System.err.println("Không hỗ trợ UTF-8: " + e.getMessage());
        }
    }

    /**
     * Khởi tạo Scanner đọc Tiếng Việt chuẩn UTF-8.
     */
    public static Scanner getScanner() {
        return new Scanner(System.in, "UTF-8");
    }
}
