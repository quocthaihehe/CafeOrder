package cafe.models;

import java.util.*;

public class MenuRepository {
    private static final List<MenuItem> menuItems = new ArrayList<>();
    private static final List<SizeItem> sizeItems = new ArrayList<>();
    private static final List<ToppingItem> toppingItems = new ArrayList<>();
    private static final Map<String, List<ToppingItem>> categoryToppingsMap = new HashMap<>();
    private static final Map<String, Voucher> vouchersMap = new HashMap<>();

    static {
        initSizes();
        initToppings();
        initProducts();
        initVouchers();
    }

    private static void initSizes() {
        sizeItems.add(new SizeItem("S", "Nhỏ (300ml)", 300, -5000));
        sizeItems.add(new SizeItem("M", "Vừa (400ml)", 400, 0));
        sizeItems.add(new SizeItem("L", "Lớn (500ml)", 500, 7000));
    }

    private static void initToppings() {
        ToppingItem tcDen = new ToppingItem(1, "Trân châu đen", 7000);
        ToppingItem tcTrang = new ToppingItem(2, "Trân châu trắng", 7000);
        ToppingItem tcDuongDen = new ToppingItem(3, "Trân châu đường đen", 8000);
        ToppingItem tcHoangKim = new ToppingItem(4, "Trân châu hoàng kim", 9000);
        ToppingItem thachTraiCay = new ToppingItem(5, "Thạch trái cây", 7000);
        ToppingItem thachDua = new ToppingItem(6, "Thạch dừa", 7000);
        ToppingItem thachCaPhe = new ToppingItem(7, "Thạch cà phê", 7000);
        ToppingItem puddingTrung = new ToppingItem(8, "Pudding trứng", 8000);
        ToppingItem suongSao = new ToppingItem(9, "Sương sáo", 7000);
        ToppingItem nhaDam = new ToppingItem(10, "Nha đam", 7000);
        ToppingItem hatSen = new ToppingItem(11, "Hạt sen", 8000);
        ToppingItem daoMieng = new ToppingItem(12, "Đào miếng", 10000);
        ToppingItem traiVai = new ToppingItem(13, "Vải", 10000);
        ToppingItem kemCheese = new ToppingItem(14, "Kem cheese macchiato", 12000);
        ToppingItem foamMuoi = new ToppingItem(15, "Foam muối", 10000);
        ToppingItem kemTuoi = new ToppingItem(16, "Kem tươi whipping", 8000);
        ToppingItem oreo = new ToppingItem(17, "Oreo vụn", 8000);
        ToppingItem shotEspresso = new ToppingItem(18, "Shot espresso thêm", 10000);
        ToppingItem suaDacThem = new ToppingItem(19, "Sữa đặc thêm", 5000);

        toppingItems.addAll(Arrays.asList(
            tcDen, tcTrang, tcDuongDen, tcHoangKim, thachTraiCay, thachDua, thachCaPhe,
            puddingTrung, suongSao, nhaDam, hatSen, daoMieng, traiVai, kemCheese,
            foamMuoi, kemTuoi, oreo, shotEspresso, suaDacThem
        ));

        // Category toppings mapping
        categoryToppingsMap.put("Cà phê", Arrays.asList(shotEspresso, thachCaPhe, foamMuoi, kemTuoi, puddingTrung, suaDacThem));
        categoryToppingsMap.put("Trà & Trái cây", Arrays.asList(tcTrang, thachTraiCay, daoMieng, traiVai, nhaDam, hatSen, thachDua));
        categoryToppingsMap.put("Trà sữa", Arrays.asList(tcDen, tcDuongDen, tcHoangKim, puddingTrung, suongSao, kemCheese, thachDua, thachTraiCay));
        categoryToppingsMap.put("Đá xay & Sinh tố", Arrays.asList(kemTuoi, oreo, tcDen, thachCaPhe));
        categoryToppingsMap.put("Soda & Nước ép", Arrays.asList(thachTraiCay, nhaDam, suongSao));
        categoryToppingsMap.put("Matcha & Chocolate", Arrays.asList(kemTuoi, tcDen, kemCheese, oreo));
    }

    private static void initProducts() {
        // 1. CÀ PHÊ (16 món ban đầu + món mới)
        menuItems.add(new MenuItem(1, "Cà phê Đen Phin", 25000, "Cà phê", "Robusta Đắk Lắk rang mộc đậm đà, hậu vị đắng thanh dịu nhẹ.", "☕", true, true, false, false, 4));
        menuItems.add(new MenuItem(2, "Cà phê Sữa Đá", 29000, "Cà phê", "Cà phê phin truyền thống phối hợp sữa đặc béo ngậy hảo hạng.", "☕", true, true, true, false, 4));
        menuItems.add(new MenuItem(3, "Bạc Xỉu Kem Béo", 32000, "Cà phê", "Ba tầng nghệ thuật với sữa tươi béo thơm và chút nhấn cà phê.", "🥛", true, true, true, false, 5));
        menuItems.add(new MenuItem(4, "Cà phê Muối Cố Đô", 35000, "Cà phê", "Lớp kem muối mặn mòi sánh mịn phủ trên nền cà phê đậm đà.", "🧂", true, false, true, false, 5));
        menuItems.add(new MenuItem(5, "Cold Brew Cam Vàng", 42000, "Cà phê", "Cà phê ủ lạnh 18h thơm nồng hoa quả kết hợp tép cam mọng nước.", "🍊", true, false, false, false, 3));
        menuItems.add(new MenuItem(6, "Caramel Macchiato", 45000, "Cà phê", "Espresso thơm lừng hòa quyện sữa nóng và sốt caramel óng ánh.", "🍮", true, true, false, false, 5));
        menuItems.add(new MenuItem(17, "Espresso", 30000, "Cà phê", "Một shot espresso nguyên chất, crema dày, vị đậm.", "☕", false, true, false, false, 3));
        menuItems.add(new MenuItem(18, "Americano", 35000, "Cà phê", "Espresso pha loãng với nước, êm và sạch vị.", "☕", true, true, false, false, 3));
        menuItems.add(new MenuItem(19, "Latte", 42000, "Cà phê", "Espresso hòa sữa tươi đánh nóng, bề mặt foam mịn.", "☕", true, true, true, false, 5));
        menuItems.add(new MenuItem(20, "Cappuccino", 42000, "Cà phê", "Tỉ lệ espresso, sữa, foam cân bằng, rắc bột cacao.", "☕", true, true, false, false, 5));
        menuItems.add(new MenuItem(21, "Mocha", 48000, "Cà phê", "Espresso, sốt chocolate và sữa tươi, phủ kem tươi.", "🍫", true, true, false, false, 5));
        menuItems.add(new MenuItem(22, "Flat White", 45000, "Cà phê", "Hai shot espresso với sữa microfoam, vị cà phê rõ.", "☕", true, true, false, false, 5));
        menuItems.add(new MenuItem(23, "Cà phê Dừa", 45000, "Cà phê", "Cà phê đá xay cùng nước cốt dừa béo mát.", "🥥", true, false, true, false, 5));
        menuItems.add(new MenuItem(24, "Cà phê Trứng", 48000, "Cà phê", "Lớp kem trứng đánh bông trên nền cà phê phin.", "🥚", true, true, false, false, 6));
        menuItems.add(new MenuItem(25, "Cold Brew Truyền Thống", 40000, "Cà phê", "Cà phê ủ lạnh 18 giờ, ít chua, hậu vị ngọt.", "🧊", true, false, false, false, 2));

        // 2. TRÀ & TRÁI CÂY
        menuItems.add(new MenuItem(7, "Trà Đào Cam Sả", 39000, "Trà & Trái cây", "Hương sả thanh khiết, cam tươi mọng nước cùng miếng đào giòn ngọt.", "🍑", true, false, true, false, 4));
        menuItems.add(new MenuItem(8, "Trà Vải Hoa Hồng", 42000, "Trà & Trái cây", "Hương hoa hồng dịu mát phối hợp trái vải giòn tan thơm mát mùa hè.", "🌹", true, false, false, false, 4));
        menuItems.add(new MenuItem(9, "Trà Sen Vàng Kem Cheese", 45000, "Trà & Trái cây", "Hạt sen bùi thơm, trân châu ngọc trai và lớp kem cheese béo mặn.", "🪷", true, false, false, false, 5));
        menuItems.add(new MenuItem(10, "Trà Ô Long Mãng Cầu", 42000, "Trà & Trái cây", "Vị chua ngọt bùng nổ từ mãng cầu xiêm tươi cùng nền trà ô long thanh vị.", "🍹", true, false, false, false, 4));
        menuItems.add(new MenuItem(26, "Trà Chanh Giã Tay", 29000, "Trà & Trái cây", "Trà đen, chanh tươi giã tay, thêm chút mật ong.", "🍋", true, false, true, false, 4));
        menuItems.add(new MenuItem(27, "Trà Tắc Xí Muội", 29000, "Trà & Trái cây", "Trà thanh, quất tươi và xí muội chua mặn nhẹ.", "🍊", true, false, false, false, 4));
        menuItems.add(new MenuItem(28, "Trà Gừng Mật Ong", 35000, "Trà & Trái cây", "Trà nóng gừng tươi và mật ong, ấm bụng.", "🍯", false, true, false, false, 4));
        menuItems.add(new MenuItem(29, "Trà Hibiscus Dâu", 42000, "Trà & Trái cây", "Trà hoa atiso đỏ với dâu tươi, chua dịu, màu đẹp.", "🍓", true, false, false, true, 4));
        menuItems.add(new MenuItem(30, "Trà Xoài Nhiệt Đới", 42000, "Trà & Trái cây", "Trà xanh, xoài chín và hạt chia.", "🥭", true, false, false, true, 4));

        // 3. ĐÁ XAY & SINH TỐ
        menuItems.add(new MenuItem(11, "Matcha Đá Xay Uji", 49000, "Đá xay & Sinh tố", "Bột trà xanh Uji Kyoto chuẩn Nhật Bản xay cùng sữa và kem tươi.", "🍵", true, false, false, false, 5));
        menuItems.add(new MenuItem(12, "Cookie & Cream Đá Xay", 48000, "Đá xay & Sinh tố", "Oreo giòn rụm kết hợp sốt sôcôla Bỉ ngọt ngào và kem whipping bông tuyết.", "🍪", true, false, false, false, 5));
        menuItems.add(new MenuItem(13, "Sinh Tố Bơ Dừa Non", 45000, "Đá xay & Sinh tố", "Bơ sáp Đắk Lắk dẻo thơm hòa quyện nước cốt dừa béo bùi thanh mát.", "🥑", true, false, false, false, 5));
        menuItems.add(new MenuItem(31, "Chocolate Chip Đá Xay", 49000, "Đá xay & Sinh tố", "Chocolate đậm xay cùng chip giòn, phủ kem tươi.", "🍫", true, false, false, false, 5));
        menuItems.add(new MenuItem(32, "Dâu Sữa Chua Đá Xay", 46000, "Đá xay & Sinh tố", "Dâu tươi và sữa chua xay lạnh, chua ngọt.", "🍓", true, false, false, false, 5));
        menuItems.add(new MenuItem(33, "Xoài Đá Xay", 45000, "Đá xay & Sinh tố", "Xoài cát chín xay mịn, thơm ngọt.", "🥭", true, false, false, false, 5));
        menuItems.add(new MenuItem(34, "Cà phê Đá Xay", 45000, "Đá xay & Sinh tố", "Cà phê espresso xay cùng sữa, phủ kem.", "☕", true, false, false, false, 5));
        menuItems.add(new MenuItem(35, "Sinh Tố Dâu", 45000, "Đá xay & Sinh tố", "Dâu tươi xay cùng sữa chua và mật ong.", "🍓", true, false, false, false, 5));

        // 4. TRÀ SỮA
        menuItems.add(new MenuItem(36, "Trà Sữa Truyền Thống", 35000, "Trà sữa", "Trà đen đậm vị, sữa béo, vị quen thuộc.", "🧋", true, true, true, false, 4));
        menuItems.add(new MenuItem(37, "Trà Sữa Thái Xanh", 38000, "Trà sữa", "Trà Thái xanh thơm, ngọt béo.", "🧋", true, false, false, false, 4));
        menuItems.add(new MenuItem(38, "Hồng Trà Sữa Trân Châu", 40000, "Trà sữa", "Hồng trà, sữa và trân châu đen dai.", "🧋", true, true, true, false, 4));
        menuItems.add(new MenuItem(39, "Trà Sữa Ô Long Nướng", 42000, "Trà sữa", "Ô long rang thơm khói, sữa béo mịn.", "🧋", true, true, false, false, 4));
        menuItems.add(new MenuItem(40, "Sữa Tươi Trân Châu Đường Đen", 45000, "Trà sữa", "Sữa tươi, trân châu nấu đường đen, vân hổ.", "🐯", true, false, true, false, 5));
        menuItems.add(new MenuItem(41, "Trà Sữa Matcha", 45000, "Trà sữa", "Matcha, sữa tươi và chút kem sữa.", "🍵", true, true, false, true, 4));
        menuItems.add(new MenuItem(42, "Trà Sữa Khoai Môn", 42000, "Trà sữa", "Khoai môn nghiền thơm bùi, trà sữa béo.", "🍠", true, false, false, true, 4));

        // 5. SODA & NƯỚC ÉP
        menuItems.add(new MenuItem(43, "Soda Chanh Dây", 35000, "Soda & Nước ép", "Chanh dây chua thơm, soda sủi bọt.", "🥤", true, false, false, false, 3));
        menuItems.add(new MenuItem(44, "Soda Việt Quất", 38000, "Soda & Nước ép", "Siro việt quất, chanh, soda mát lạnh.", "🫐", true, false, false, false, 3));
        menuItems.add(new MenuItem(45, "Soda Dâu Bạc Hà", 38000, "Soda & Nước ép", "Dâu tươi, bạc hà và soda.", "🍓", true, false, false, false, 3));
        menuItems.add(new MenuItem(46, "Nước Cam Ép", 40000, "Soda & Nước ép", "Cam vàng vắt tươi, không đường thêm.", "🍊", true, false, true, false, 3));
        menuItems.add(new MenuItem(47, "Nước Ép Dưa Hấu", 35000, "Soda & Nước ép", "Dưa hấu ép tươi mát.", "🍉", true, false, false, false, 3));
        menuItems.add(new MenuItem(48, "Nước Ép Ổi Hồng", 40000, "Soda & Nước ép", "Ổi hồng ép nguyên chất, thơm dịu.", "🍐", true, false, false, false, 3));
        menuItems.add(new MenuItem(49, "Chanh Dây Mật Ong", 35000, "Soda & Nước ép", "Chanh dây, mật ong và đá, chua ngọt cân bằng.", "🍯", true, false, false, false, 3));
        menuItems.add(new MenuItem(50, "Sữa Chua Đánh Đá", 32000, "Soda & Nước ép", "Sữa chua lên men mịn, đánh cùng đá.", "🥛", true, false, false, false, 3));

        // 6. MATCHA & CHOCOLATE
        menuItems.add(new MenuItem(51, "Matcha Latte", 48000, "Matcha & Chocolate", "Matcha Nhật, sữa tươi, vị trà rõ.", "🍵", true, true, true, false, 5));
        menuItems.add(new MenuItem(52, "Matcha Dâu", 52000, "Matcha & Chocolate", "Matcha phối dâu tươi, hai tầng màu.", "🍵", true, false, false, true, 5));
        menuItems.add(new MenuItem(53, "Chocolate Nóng", 45000, "Matcha & Chocolate", "Chocolate đậm, sữa nóng, phủ kem tươi.", "🍫", true, true, false, false, 5));
        menuItems.add(new MenuItem(54, "Chocolate Đá", 45000, "Matcha & Chocolate", "Chocolate sữa lạnh, béo ngọt.", "🍫", true, false, false, false, 4));
        menuItems.add(new MenuItem(55, "Hojicha Latte", 48000, "Matcha & Chocolate", "Trà xanh rang Hojicha, thơm khói, ít đắng.", "🍵", true, true, false, true, 5));

        // 7. BÁNH NGỌT
        menuItems.add(new MenuItem(14, "Croissant Bơ Pháp", 32000, "Bánh ngọt", "Bánh sừng bò ngàn lớp nướng vàng giòn rụm, nồng nàn hương bơ Pháp.", "🥐", false, false, false, false, 2));
        menuItems.add(new MenuItem(15, "Tiramisu Cacao Ý", 38000, "Bánh ngọt", "Bánh bông lan cà phê phủ phô mai mascarpone mềm tan cùng bột cacao.", "🍰", false, false, false, false, 2));
        menuItems.add(new MenuItem(16, "Cheesecake Nướng Basque", 45000, "Bánh ngọt", "Lớp mặt cháy caramel đặc trưng, lõi phô mai tan chảy béo ngậy.", "🧀", false, false, false, false, 2));
        menuItems.add(new MenuItem(56, "Muffin Chocolate", 28000, "Bánh ngọt", "Muffin mềm, nhân chocolate chip.", "🧁", false, false, false, false, 2));
        menuItems.add(new MenuItem(57, "Bông Lan Trứng Muối", 35000, "Bánh ngọt", "Bông lan mềm, sốt trứng muối và ruốc.", "🍰", false, false, true, false, 2));
        menuItems.add(new MenuItem(58, "Bánh Flan Caramel", 25000, "Bánh ngọt", "Flan trứng mịn, caramel đắng nhẹ.", "🍮", false, false, false, false, 2));
        menuItems.add(new MenuItem(59, "Mochi Kem", 30000, "Bánh ngọt", "Vỏ mochi dẻo, nhân kem lạnh, chọn vị.", "🍡", false, false, false, true, 2));
        menuItems.add(new MenuItem(60, "Cookie Chocolate", 22000, "Bánh ngọt", "Cookie bơ giòn, chocolate chip.", "🍪", false, false, false, false, 2));
        menuItems.add(new MenuItem(61, "Red Velvet Slice", 42000, "Bánh ngọt", "Bánh nhung đỏ, phủ kem cheese.", "🍰", false, false, false, false, 2));

        // 8. MÓN ĂN NHẸ
        menuItems.add(new MenuItem(62, "Bánh Mì Que Pate", 20000, "Món ăn nhẹ", "Bánh mì que giòn, pate và bơ.", "🥖", false, false, true, false, 4));
        menuItems.add(new MenuItem(63, "Sandwich Gà Phô Mai", 45000, "Món ăn nhẹ", "Gà xé, phô mai, rau tươi, nướng nóng.", "🥪", false, false, false, false, 7));
        menuItems.add(new MenuItem(64, "Khoai Tây Chiên", 35000, "Món ăn nhẹ", "Khoai chiên giòn, kèm sốt tương cà.", "🍟", false, false, true, false, 7));
        menuItems.add(new MenuItem(65, "Gà Viên Chiên", 40000, "Món ăn nhẹ", "Gà viên chiên vàng, sốt mayonnaise.", "🍗", false, false, false, false, 7));
        menuItems.add(new MenuItem(66, "Mì Ý Bò Bằm", 55000, "Món ăn nhẹ", "Mì Ý sốt bò bằm cà chua, phô mai.", "🍝", false, false, false, true, 10));
        menuItems.add(new MenuItem(67, "Bánh Tráng Nướng", 30000, "Món ăn nhẹ", "Bánh tráng nướng giòn, trứng, hành, phô mai.", "🫓", false, false, false, false, 6));
    }

    private static void initVouchers() {
        vouchersMap.put("WELCOME10", new Voucher("WELCOME10", "PERCENT", 10, 50000));
        vouchersMap.put("GIAM15K", new Voucher("GIAM15K", "AMOUNT", 15000, 100000));
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

    public static List<SizeItem> getSizes() {
        return Collections.unmodifiableList(sizeItems);
    }

    public static List<ToppingItem> getAllToppings() {
        return Collections.unmodifiableList(toppingItems);
    }

    public static List<ToppingItem> getToppingsForCategory(String category) {
        if (category == null) return Collections.emptyList();
        List<ToppingItem> list = categoryToppingsMap.get(category);
        return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
    }

    public static Voucher getVoucher(String code) {
        if (code == null) return null;
        return vouchersMap.get(code.trim().toUpperCase());
    }

    public static MenuItem findByName(String name) {
        for (MenuItem item : menuItems) {
            if (item.getName().equalsIgnoreCase(name)) {
                return item;
            }
        }
        return null;
    }

    public static MenuItem findById(int id) {
        for (MenuItem item : menuItems) {
            if (item.getId() == id) {
                return item;
            }
        }
        return null;
    }
}
