package cafe.client.ui;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class ClientUI extends JFrame {
    public ClientUI() {
        setTitle("Màn hình Đặt Món Tại Bàn");
        setSize(400, 700); // Khung dọc giống điện thoại/iPad
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Giữa màn hình
    }

    public static void main(String[] args) {
        // BƯỚC QUAN TRỌNG: Kích hoạt bộ lọc giao diện hiện đại FlatLaf
        try {
            FlatLightLaf.setup();
        } catch (Exception ex) {
            System.err.println("Không thể khởi tạo FlatLaf");
        }
        
        // Mở cửa sổ
        SwingUtilities.invokeLater(() -> {
            new ClientUI().setVisible(true);
        });
    }
}
