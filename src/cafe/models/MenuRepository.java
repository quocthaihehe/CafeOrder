package cafe.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MenuRepository {
    private static final List<MenuItem> menuItems = new ArrayList<>();

    static {
        // Cà phê thủ công & Espresso
        menuItems.add(new MenuItem(1, "Cà phê Đen Phin", 25000, "Cà phê", "Robusta Đắk Lắk rang mộc đậm đà, hậu vị đắng thanh dịu nhẹ.", "☕"));
        menuItems.add(new MenuItem(2, "Cà phê Sữa Đá", 29000, "Cà phê", "Cà phê phin truyền thống phối hợp sữa đặc béo ngậy hảo hạng.", "☕"));
        menuItems.add(new MenuItem(3, "Bạc Xỉu Kem Béo", 32000, "Cà phê", "Ba tầng nghệ thuật với sữa tươi béo thơm và chút nhấn cà phê.", "🥛"));
        menuItems.add(new MenuItem(4, "Cà phê Muối Cố Đô", 35000, "Cà phê", "Lớp kem muối mặn mòi sánh mịn phủ trên nền cà phê đậm đà.", "🧂"));
        menuItems.add(new MenuItem(5, "Cold Brew Cam Vàng", 42000, "Cà phê", "Cà phê ủ lạnh 18h thơm nồng hoa quả kết hợp tép cam mọng nước.", "🍊"));
        menuItems.add(new MenuItem(6, "Caramel Macchiato", 45000, "Cà phê", "Espresso thơm lừng hòa quyện sữa nóng và sốt caramel óng ánh.", "🍮"));

        // Trà thượng hạng & Trái cây
        menuItems.add(new MenuItem(7, "Trà Đào Cam Sả", 39000, "Trà & Trái cây", "Hương sả thanh khiết, cam tươi mọng nước cùng miếng đào giòn ngọt.", "🍑"));
        menuItems.add(new MenuItem(8, "Trà Vải Hoa Hồng", 42000, "Trà & Trái cây", "Hương hoa hồng dịu mát phối hợp trái vải giòn tan thơm mát mùa hè.", "🌹"));
        menuItems.add(new MenuItem(9, "Trà Sen Vàng Kem Cheese", 45000, "Trà & Trái cây", "Hạt sen bùi thơm, trân châu ngọc trai và lớp kem cheese béo mặn.", "🪷"));
        menuItems.add(new MenuItem(10, "Trà Ô Long Mãng Cầu", 42000, "Trà & Trái cây", "Vị chua ngọt bùng nổ từ mãng cầu xiêm tươi cùng nền trà ô long thanh vị.", "🍹"));

        // Đá xay & Sinh tố thư giãn
        menuItems.add(new MenuItem(11, "Matcha Đá Xay Uji", 49000, "Đá xay & Sinh tố", "Bột trà xanh Uji Kyoto chuẩn Nhật Bản xay cùng sữa và kem tươi.", "🍵"));
        menuItems.add(new MenuItem(12, "Cookie & Cream Đá Xay", 48000, "Đá xay & Sinh tố", "Oreo giòn rụm kết hợp sốt sôcôla Bỉ ngọt ngào và kem whipping bông tuyết.", "🍪"));
        menuItems.add(new MenuItem(13, "Sinh Tố Bơ Dừa Non", 45000, "Đá xay & Sinh tố", "Bơ sáp Đắk Lắk dẻo thơm hòa quyện nước cốt dừa béo bùi thanh mát.", "🥑"));

        // Bánh ngọt & Ăn kèm
        menuItems.add(new MenuItem(14, "Croissant Bơ Pháp", 32000, "Bánh ngọt", "Bánh sừng bò ngàn lớp nướng vàng giòn rụm, nồng nàn hương bơ Pháp.", "🥐"));
        menuItems.add(new MenuItem(15, "Tiramisu Cacao Ý", 38000, "Bánh ngọt", "Bánh bông lan cà phê phủ phô mai mascarpone mềm tan cùng bột cacao.", "🍰"));
        menuItems.add(new MenuItem(16, "Cheesecake Nướng Basque", 45000, "Bánh ngọt", "Lớp mặt cháy caramel đặc trưng, lõi phô mai tan chảy béo ngậy.", "🧀"));
    }

    public static List<MenuItem> getAllItems() {
        return Collections.unmodifiableList(menuItems);
    }

    public static List<String> getCategories() {
        List<String> categories = new ArrayList<>();
        categories.add("Tất cả");
        for (MenuItem item : menuItems) {
            if (!categories.contains(item.getCategory())) {
                categories.add(item.getCategory());
            }
        }
        return categories;
    }

    public static MenuItem findByName(String name) {
        for (MenuItem item : menuItems) {
            if (item.getName().equalsIgnoreCase(name)) {
                return item;
            }
        }
        return null;
    }

    public static double getPrice(String name) {
        MenuItem item = findByName(name);
        return item != null ? item.getPrice() : 0.0;
    }
}
