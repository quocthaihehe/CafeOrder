package cafe.server.ui;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class ServerUI extends JFrame {
    public ServerUI() {
        setTitle("Màn hình Quầy Pha Chế (KDS)");
        setSize(800, 600);
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
            new ServerUI().setVisible(true);
        });
    }
}
