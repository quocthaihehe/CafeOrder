package cafe.client;

import cafe.models.MenuItem;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class UIHelper {
    // Tạm thời vẫn dùng danh sách này, Phase sau ta sẽ móc Database vào
    private static final MenuItem[] menu = {
        new MenuItem("Cà phê đen", 20000),
        new MenuItem("Cà phê sữa", 25000),
        new MenuItem("Bạc xỉu", 25000),
        new MenuItem("Sinh tố dâu", 35000),
        new MenuItem("Trà đào cam sả", 30000),
        new MenuItem("Matcha đá xay", 40000)
    };

    public static double getPrice(String name) {
        for (MenuItem item : menu) {
            if (item.getName().equals(name)) return item.getPrice();
        }
        return 0;
    }

    public static void loadMenuIntoPanel(JPanel panel, java.util.function.Consumer<MenuItem> onClick) {
        panel.removeAll();
        // Dùng GridLayout 0 hàng, 3 cột để tự co giãn theo cửa sổ (Responsive)
        panel.setLayout(new java.awt.GridLayout(0, 3, 15, 15)); 
        
        for (MenuItem item : menu) {
            JPanel card = new JPanel();
            card.setLayout(new java.awt.BorderLayout());
            
            // Trang trí viền thẻ
            javax.swing.border.Border line = javax.swing.BorderFactory.createLineBorder(new java.awt.Color(200, 200, 200), 1, true);
            card.setBorder(javax.swing.BorderFactory.createTitledBorder(line, item.getName(), javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.TOP, new java.awt.Font("SansSerif", java.awt.Font.BOLD, 14)));
            
            // Giá tiền ở giữa
            JLabel lblPrice = new JLabel(String.format("%,.0f đ", item.getPrice()));
            lblPrice.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            lblPrice.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 16));
            
            // Nút bấm ở đáy
            JButton btnSelect = new JButton("CHỌN MÓN");
            btnSelect.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 12));
            btnSelect.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            btnSelect.addActionListener((java.awt.event.ActionEvent e) -> {
                onClick.accept(item); 
            });
            
            // Thêm padding cho đẹp
            card.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                card.getBorder(), 
                javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));
            
            card.add(lblPrice, java.awt.BorderLayout.CENTER);
            card.add(btnSelect, java.awt.BorderLayout.SOUTH);
            
            panel.add(card); 
        }
        
        panel.revalidate();
        panel.repaint();
    }
}
