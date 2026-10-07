-- =====================================================================
-- CAFEORDER - HỆ THỐNG CƠ SỞ DỮ LIỆU ĐẦY ĐỦ (MICROSOFT SQL SERVER)
-- TỔNG HỢP TOÀN BỘ CẤU TRÚC (16 BẢNG) + MENU 67 MÓN + TOPPING + SIZE + KHO
-- =====================================================================

IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = N'CafeOrderDB')
BEGIN
    CREATE DATABASE CafeOrderDB COLLATE Vietnamese_CI_AS;
END
GO

USE CafeOrderDB;
GO

-- ---------------------------------------------------------------------
-- 1. DỌN DẸP BẢNG CŨ THEO THỨ TỰ RÀNG BUỘC KHÓA NGOẠI
-- ---------------------------------------------------------------------
IF OBJECT_ID('dbo.StockMovements', 'U') IS NOT NULL DROP TABLE dbo.StockMovements;
IF OBJECT_ID('dbo.OrderDetailToppings', 'U') IS NOT NULL DROP TABLE dbo.OrderDetailToppings;
IF OBJECT_ID('dbo.CategoryToppings', 'U') IS NOT NULL DROP TABLE dbo.CategoryToppings;
IF OBJECT_ID('dbo.Payments', 'U') IS NOT NULL DROP TABLE dbo.Payments;
IF OBJECT_ID('dbo.OrderDetails', 'U') IS NOT NULL DROP TABLE dbo.OrderDetails;
IF OBJECT_ID('dbo.Orders', 'U') IS NOT NULL DROP TABLE dbo.Orders;
IF OBJECT_ID('dbo.Recipes', 'U') IS NOT NULL DROP TABLE dbo.Recipes;
IF OBJECT_ID('dbo.Products', 'U') IS NOT NULL DROP TABLE dbo.Products;
IF OBJECT_ID('dbo.Categories', 'U') IS NOT NULL DROP TABLE dbo.Categories;
IF OBJECT_ID('dbo.Toppings', 'U') IS NOT NULL DROP TABLE dbo.Toppings;
IF OBJECT_ID('dbo.Sizes', 'U') IS NOT NULL DROP TABLE dbo.Sizes;
IF OBJECT_ID('dbo.Staff', 'U') IS NOT NULL DROP TABLE dbo.Staff;
IF OBJECT_ID('dbo.Vouchers', 'U') IS NOT NULL DROP TABLE dbo.Vouchers;
IF OBJECT_ID('dbo.Ingredients', 'U') IS NOT NULL DROP TABLE dbo.Ingredients;
IF OBJECT_ID('dbo.Suppliers', 'U') IS NOT NULL DROP TABLE dbo.Suppliers;
IF OBJECT_ID('dbo.Tables', 'U') IS NOT NULL DROP TABLE dbo.Tables;
GO

-- ---------------------------------------------------------------------
-- 2. TẠO TOÀN BỘ CẤU TRÚC 16 BẢNG (DDL SCHEMA)
-- ---------------------------------------------------------------------

-- Bảng 1: Danh mục
CREATE TABLE Categories (
    category_id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL UNIQUE,
    display_order INT DEFAULT 0
);
GO

-- Bảng 2: Kích cỡ (Size S/M/L)
CREATE TABLE Sizes (
    size_code NVARCHAR(5) PRIMARY KEY,
    display_name NVARCHAR(30) NOT NULL,
    volume_ml INT NOT NULL,
    price_delta DECIMAL(18,2) NOT NULL DEFAULT 0
);
GO

-- Bảng 3: Sản phẩm
CREATE TABLE Products (
    product_id INT IDENTITY(1,1) PRIMARY KEY,
    category_id INT NOT NULL,
    name NVARCHAR(150) NOT NULL,
    base_price DECIMAL(18, 2) NOT NULL CHECK(base_price >= 0),
    description NVARCHAR(500) NULL,
    icon_emoji NVARCHAR(20) DEFAULT N'☕',
    image_path NVARCHAR(255) NULL,
    allow_size BIT NOT NULL DEFAULT 1,
    is_hot_available BIT NOT NULL DEFAULT 0,
    is_bestseller BIT NOT NULL DEFAULT 0,
    is_new BIT NOT NULL DEFAULT 0,
    prep_minutes INT NOT NULL DEFAULT 5,
    is_active BIT NOT NULL DEFAULT 1,
    CONSTRAINT FK_Products_Categories FOREIGN KEY (category_id) REFERENCES Categories(category_id)
);
GO

-- Bảng 4: Toppings
CREATE TABLE Toppings (
    topping_id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL UNIQUE,
    price DECIMAL(18,2) NOT NULL CHECK(price >= 0),
    is_active BIT NOT NULL DEFAULT 1
);
GO

-- Bảng 5: Danh mục được phép dùng Topping nào
CREATE TABLE CategoryToppings (
    category_id INT NOT NULL,
    topping_id INT NOT NULL,
    PRIMARY KEY (category_id, topping_id),
    CONSTRAINT FK_CatTop_Categories FOREIGN KEY (category_id) REFERENCES Categories(category_id),
    CONSTRAINT FK_CatTop_Toppings FOREIGN KEY (topping_id) REFERENCES Toppings(topping_id)
);
GO

-- Bảng 6: Bàn ăn / Vị trí Kiosk
CREATE TABLE Tables (
    table_id INT IDENTITY(1,1) PRIMARY KEY,
    table_number INT NOT NULL UNIQUE,
    area_zone NVARCHAR(50) DEFAULT N'Tầng 1',
    capacity INT NOT NULL DEFAULT 4,
    status NVARCHAR(30) DEFAULT 'AVAILABLE',
    CONSTRAINT CK_Tables_Status CHECK (status IN ('AVAILABLE','OCCUPIED','RESERVED','CLEANING'))
);
GO

-- Bảng 7: Nhân viên & Phân quyền
CREATE TABLE Staff (
    staff_id INT IDENTITY(1,1) PRIMARY KEY,
    full_name NVARCHAR(100) NOT NULL,
    role NVARCHAR(20) NOT NULL CHECK(role IN ('OWNER','MANAGER','CASHIER','BARISTA','WAITER')),
    username NVARCHAR(50) NOT NULL UNIQUE,
    password_hash NVARCHAR(255) NOT NULL,
    is_active BIT NOT NULL DEFAULT 1
);
GO

-- Bảng 8: Khuyến mãi / Voucher
CREATE TABLE Vouchers (
    voucher_id INT IDENTITY(1,1) PRIMARY KEY,
    code NVARCHAR(30) NOT NULL UNIQUE,
    discount_type NVARCHAR(10) NOT NULL CHECK(discount_type IN ('PERCENT','AMOUNT')),
    discount_value DECIMAL(18,2) NOT NULL CHECK(discount_value > 0),
    min_order_amount DECIMAL(18,2) NOT NULL DEFAULT 0,
    valid_from DATE NOT NULL,
    valid_to DATE NOT NULL,
    usage_limit INT NULL,
    used_count INT NOT NULL DEFAULT 0
);
GO

-- Bảng 9: Đơn hàng (Orders)
CREATE TABLE Orders (
    order_id INT IDENTITY(1000,1) PRIMARY KEY,
    table_id INT NOT NULL,
    order_type NVARCHAR(20) NOT NULL DEFAULT 'DINE_IN',
    status NVARCHAR(30) NOT NULL DEFAULT 'QUEUED',
    total_amount DECIMAL(18, 2) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(18, 2) NOT NULL DEFAULT 0,
    vat_amount DECIMAL(18, 2) NOT NULL DEFAULT 0,
    voucher_id INT NULL,
    staff_id INT NULL,
    note NVARCHAR(255) NULL,
    idempotency_key NVARCHAR(64) NULL UNIQUE,
    created_at DATETIME2(0) DEFAULT SYSDATETIME(),
    completed_at DATETIME2(0) NULL,
    CONSTRAINT CK_Orders_Status CHECK (status IN ('QUEUED','PREPARING','DONE','PAID','CANCELLED')),
    CONSTRAINT CK_Orders_Type CHECK (order_type IN ('DINE_IN','TAKEAWAY','DELIVERY')),
    CONSTRAINT FK_Orders_Tables FOREIGN KEY (table_id) REFERENCES Tables(table_id),
    CONSTRAINT FK_Orders_Vouchers FOREIGN KEY (voucher_id) REFERENCES Vouchers(voucher_id),
    CONSTRAINT FK_Orders_Staff FOREIGN KEY (staff_id) REFERENCES Staff(staff_id)
);
GO

-- Bảng 10: Chi tiết đơn hàng (OrderDetails)
CREATE TABLE OrderDetails (
    detail_id INT IDENTITY(1,1) PRIMARY KEY,
    order_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL CHECK(quantity > 0),
    unit_price DECIMAL(18, 2) NOT NULL,
    size_code NVARCHAR(5) NULL,
    sugar_percent INT NULL,
    ice_percent INT NULL,
    temperature NVARCHAR(10) NULL,
    customer_note NVARCHAR(255) NULL,
    CONSTRAINT FK_OrderDetails_Orders FOREIGN KEY (order_id) REFERENCES Orders(order_id) ON DELETE CASCADE,
    CONSTRAINT FK_OrderDetails_Products FOREIGN KEY (product_id) REFERENCES Products(product_id)
);
GO

-- Bảng 11: Topping gắn theo từng dòng món đã gọi
CREATE TABLE OrderDetailToppings (
    detail_id INT NOT NULL,
    topping_id INT NOT NULL,
    quantity INT NOT NULL DEFAULT 1 CHECK(quantity > 0),
    unit_price DECIMAL(18,2) NOT NULL,
    PRIMARY KEY (detail_id, topping_id),
    CONSTRAINT FK_ODT_OrderDetails FOREIGN KEY (detail_id) REFERENCES OrderDetails(detail_id) ON DELETE CASCADE,
    CONSTRAINT FK_ODT_Toppings FOREIGN KEY (topping_id) REFERENCES Toppings(topping_id)
);
GO

-- Bảng 12: Thanh toán (Payments)
CREATE TABLE Payments (
    payment_id INT IDENTITY(1,1) PRIMARY KEY,
    order_id INT NOT NULL UNIQUE,
    payment_method NVARCHAR(50) NOT NULL,
    transaction_code NVARCHAR(100) NULL,
    amount_paid DECIMAL(18, 2) NOT NULL,
    paid_at DATETIME2(0) DEFAULT SYSDATETIME(),
    CONSTRAINT FK_Payments_Orders FOREIGN KEY (order_id) REFERENCES Orders(order_id)
);
GO

-- Bảng 13: Nhà cung cấp nguyên vật liệu
CREATE TABLE Suppliers (
    supplier_id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(150) NOT NULL,
    phone NVARCHAR(20) NULL,
    address NVARCHAR(255) NULL
);
GO

-- Bảng 14: Nguyên vật liệu kho
CREATE TABLE Ingredients (
    ingredient_id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(150) NOT NULL,
    unit NVARCHAR(30) NOT NULL,
    current_stock DECIMAL(18, 2) NOT NULL DEFAULT 0,
    min_alert_threshold DECIMAL(18, 2) NOT NULL DEFAULT 10
);
GO

-- Bảng 15: Công thức định lượng pha chế (Recipes)
CREATE TABLE Recipes (
    product_id INT NOT NULL,
    ingredient_id INT NOT NULL,
    quantity_needed DECIMAL(18, 2) NOT NULL CHECK(quantity_needed > 0),
    PRIMARY KEY (product_id, ingredient_id),
    CONSTRAINT FK_Recipes_Products FOREIGN KEY (product_id) REFERENCES Products(product_id),
    CONSTRAINT FK_Recipes_Ingredients FOREIGN KEY (ingredient_id) REFERENCES Ingredients(ingredient_id)
);
GO

-- Bảng 16: Lịch sử biến động kho (StockMovements)
CREATE TABLE StockMovements (
    movement_id INT IDENTITY(1,1) PRIMARY KEY,
    ingredient_id INT NOT NULL,
    movement_type NVARCHAR(15) NOT NULL CHECK(movement_type IN ('IMPORT','SALE','WASTE','ADJUST')),
    quantity DECIMAL(18,2) NOT NULL,
    unit_cost DECIMAL(18,2) NULL,
    supplier_id INT NULL,
    order_id INT NULL,
    created_at DATETIME2(0) NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_SM_Ingredients FOREIGN KEY (ingredient_id) REFERENCES Ingredients(ingredient_id),
    CONSTRAINT FK_SM_Suppliers FOREIGN KEY (supplier_id) REFERENCES Suppliers(supplier_id),
    CONSTRAINT FK_SM_Orders FOREIGN KEY (order_id) REFERENCES Orders(order_id)
);
GO

-- Indexes tối ưu truy vấn
CREATE INDEX IX_Orders_Status_Created ON Orders(status, created_at);
CREATE INDEX IX_Orders_Table ON Orders(table_id);
CREATE INDEX IX_OrderDetails_Order ON OrderDetails(order_id);
CREATE INDEX IX_Products_Category ON Products(category_id, is_active);
GO

-- ---------------------------------------------------------------------
-- 3. NẠP DỮ LIỆU KHỞI TẠO MẪU (SEED DATA)
-- ---------------------------------------------------------------------

-- Sizes
INSERT INTO Sizes VALUES 
('S', N'Nhỏ', 300, -5000), 
('M', N'Vừa', 400, 0), 
('L', N'Lớn', 500, 7000);
GO

-- 8 Danh mục
INSERT INTO Categories (name, display_order) VALUES 
(N'Cà phê', 1),
(N'Trà & Trái cây', 2),
(N'Đá xay & Sinh tố', 3),
(N'Bánh ngọt', 4),
(N'Trà sữa', 5),
(N'Soda & Nước ép', 6),
(N'Matcha & Chocolate', 7),
(N'Món ăn nhẹ', 8);
GO

-- 16 Món ban đầu (Category 1-4)
INSERT INTO Products (category_id, name, base_price, description, icon_emoji, allow_size, is_hot_available, is_bestseller, is_new, prep_minutes) VALUES
(1, N'Cà phê Đen Phin', 25000, N'Robusta Đắk Lắk rang mộc đậm đà, hậu vị đắng thanh dịu nhẹ.', N'☕', 1, 1, 0, 0, 4),
(1, N'Cà phê Sữa Đá', 29000, N'Cà phê phin truyền thống phối hợp sữa đặc béo ngậy hảo hạng.', N'☕', 1, 1, 1, 0, 4),
(1, N'Bạc Xỉu Kem Béo', 32000, N'Ba tầng nghệ thuật với sữa tươi béo thơm và chút nhấn cà phê.', N'🥛', 1, 1, 1, 0, 5),
(1, N'Cà phê Muối Cố Đô', 35000, N'Lớp kem muối mặn mòi sánh mịn phủ trên nền cà phê đậm đà.', N'🧂', 1, 0, 1, 0, 5),
(1, N'Cold Brew Cam Vàng', 42000, N'Cà phê ủ lạnh 18h thơm nồng hoa quả kết hợp tép cam mọng nước.', N'🍊', 1, 0, 0, 0, 3),
(1, N'Caramel Macchiato', 45000, N'Espresso thơm lừng hòa quyện sữa nóng và sốt caramel óng ánh.', N'🍮', 1, 1, 0, 0, 5),

(2, N'Trà Đào Cam Sả', 39000, N'Hương sả thanh khiết, cam tươi mọng nước cùng miếng đào giòn ngọt.', N'🍑', 1, 0, 1, 0, 4),
(2, N'Trà Vải Hoa Hồng', 42000, N'Hương hoa hồng dịu mát phối hợp trái vải giòn tan thơm mát mùa hè.', N'🌹', 1, 0, 0, 0, 4),
(2, N'Trà Sen Vàng Kem Cheese', 45000, N'Hạt sen bùi thơm, trân châu ngọc trai và lớp kem cheese béo mặn.', N'🪷', 1, 0, 0, 0, 5),
(2, N'Trà Ô Long Mãng Cầu', 42000, N'Vị chua ngọt bùng nổ từ mãng cầu xiêm tươi cùng nền trà ô long thanh vị.', N'🍹', 1, 0, 0, 0, 4),

(3, N'Matcha Đá Xay Uji', 49000, N'Bột trà xanh Uji Kyoto chuẩn Nhật Bản xay cùng sữa và kem tươi.', N'🍵', 1, 0, 0, 0, 5),
(3, N'Cookie & Cream Đá Xay', 48000, N'Oreo giòn rụm kết hợp sốt sôcôla Bỉ ngọt ngào và kem whipping bông tuyết.', N'🍪', 1, 0, 0, 0, 5),
(3, N'Sinh Tố Bơ Dừa Non', 45000, N'Bơ sáp Đắk Lắk dẻo thơm hòa quyện nước cốt dừa béo bùi thanh mát.', N'🥑', 1, 0, 0, 0, 5),

(4, N'Croissant Bơ Pháp', 32000, N'Bánh sừng bò ngàn lớp nướng vàng giòn rụm, nồng nàn hương bơ Pháp.', N'🥐', 0, 0, 0, 0, 2),
(4, N'Tiramisu Cacao Ý', 38000, N'Bánh bông lan cà phê phủ phô mai mascarpone mềm tan cùng bột cacao.', N'🍰', 0, 0, 0, 0, 2),
(4, N'Cheesecake Nướng Basque', 45000, N'Lớp mặt cháy caramel đặc trưng, lõi phô mai tan chảy béo ngậy.', N'🧀', 0, 0, 0, 0, 2);
GO

-- 51 Món bổ sung
INSERT INTO Products (category_id, name, base_price, description, icon_emoji,
                      allow_size, is_hot_available, is_bestseller, is_new, prep_minutes)
SELECT c.category_id, v.name, v.price, v.descr, v.emoji, v.sz, v.hot, v.best, v.nw, v.prep
FROM (VALUES
 -- CÀ PHÊ
 (N'Cà phê', N'Espresso', 30000, N'Một shot espresso nguyên chất, crema dày, vị đậm.', N'☕', 0,1,0,0,3),
 (N'Cà phê', N'Americano', 35000, N'Espresso pha loãng với nước, êm và sạch vị.', N'☕', 1,1,0,0,3),
 (N'Cà phê', N'Latte', 42000, N'Espresso hòa sữa tươi đánh nóng, bề mặt foam mịn.', N'☕', 1,1,1,0,5),
 (N'Cà phê', N'Cappuccino', 42000, N'Tỉ lệ espresso, sữa, foam cân bằng, rắc bột cacao.', N'☕', 1,1,0,0,5),
 (N'Cà phê', N'Mocha', 48000, N'Espresso, sốt chocolate và sữa tươi, phủ kem tươi.', N'🍫', 1,1,0,0,5),
 (N'Cà phê', N'Flat White', 45000, N'Hai shot espresso với sữa microfoam, vị cà phê rõ.', N'☕', 1,1,0,0,5),
 (N'Cà phê', N'Cà phê Dừa', 45000, N'Cà phê đá xay cùng nước cốt dừa béo mát.', N'🥥', 1,0,1,0,5),
 (N'Cà phê', N'Cà phê Trứng', 48000, N'Lớp kem trứng đánh bông trên nền cà phê phin.', N'🥚', 1,1,0,0,6),
 (N'Cà phê', N'Cold Brew Truyền Thống', 40000, N'Cà phê ủ lạnh 18 giờ, ít chua, hậu vị ngọt.', N'🧊', 1,0,0,0,2),
 -- TRÀ & TRÁI CÂY
 (N'Trà & Trái cây', N'Trà Chanh Giã Tay', 29000, N'Trà đen, chanh tươi giã tay, thêm chút mật ong.', N'🍋', 1,0,1,0,4),
 (N'Trà & Trái cây', N'Trà Tắc Xí Muội', 29000, N'Trà thanh, quất tươi và xí muội chua mặn nhẹ.', N'🍊', 1,0,0,0,4),
 (N'Trà & Trái cây', N'Trà Gừng Mật Ong', 35000, N'Trà nóng gừng tươi và mật ong, ấm bụng.', N'🍯', 0,1,0,0,4),
 (N'Trà & Trái cây', N'Trà Hibiscus Dâu', 42000, N'Trà hoa atiso đỏ với dâu tươi, chua dịu, màu đẹp.', N'🍓', 1,0,0,1,4),
 (N'Trà & Trái cây', N'Trà Xoài Nhiệt Đới', 42000, N'Trà xanh, xoài chín và hạt chia.', N'🥭', 1,0,0,1,4),
 -- ĐÁ XAY & SINH TỐ
 (N'Đá xay & Sinh tố', N'Chocolate Chip Đá Xay', 49000, N'Chocolate đậm xay cùng chip giòn, phủ kem tươi.', N'🍫', 1,0,0,0,5),
 (N'Đá xay & Sinh tố', N'Dâu Sữa Chua Đá Xay', 46000, N'Dâu tươi và sữa chua xay lạnh, chua ngọt.', N'🍓', 1,0,0,0,5),
 (N'Đá xay & Sinh tố', N'Xoài Đá Xay', 45000, N'Xoài cát chín xay mịn, thơm ngọt.', N'🥭', 1,0,0,0,5),
 (N'Đá xay & Sinh tố', N'Cà phê Đá Xay', 45000, N'Cà phê espresso xay cùng sữa, phủ kem.', N'☕', 1,0,0,0,5),
 (N'Đá xay & Sinh tố', N'Sinh Tố Dâu', 45000, N'Dâu tươi xay cùng sữa chua và mật ong.', N'🍓', 1,0,0,0,5),
 -- TRÀ SỮA
 (N'Trà sữa', N'Trà Sữa Truyền Thống', 35000, N'Trà đen đậm vị, sữa béo, vị quen thuộc.', N'🧋', 1,1,1,0,4),
 (N'Trà sữa', N'Trà Sữa Thái Xanh', 38000, N'Trà Thái xanh thơm, ngọt béo.', N'🧋', 1,0,0,0,4),
 (N'Trà sữa', N'Hồng Trà Sữa Trân Châu', 40000, N'Hồng trà, sữa và trân châu đen dai.', N'🧋', 1,1,1,0,4),
 (N'Trà sữa', N'Trà Sữa Ô Long Nướng', 42000, N'Ô long rang thơm khói, sữa béo mịn.', N'🧋', 1,1,0,0,4),
 (N'Trà sữa', N'Sữa Tươi Trân Châu Đường Đen', 45000, N'Sữa tươi, trân châu nấu đường đen, vân hổ.', N'🐯', 1,0,1,0,5),
 (N'Trà sữa', N'Trà Sữa Matcha', 45000, N'Matcha, sữa tươi và chút kem sữa.', N'🍵', 1,1,0,1,4),
 (N'Trà sữa', N'Trà Sữa Khoai Môn', 42000, N'Khoai môn nghiền thơm bùi, trà sữa béo.', N'🍠', 1,0,0,1,4),
 -- SODA & NƯỚC ÉP
 (N'Soda & Nước ép', N'Soda Chanh Dây', 35000, N'Chanh dây chua thơm, soda sủi bọt.', N'🥤', 1,0,0,0,3),
 (N'Soda & Nước ép', N'Soda Việt Quất', 38000, N'Siro việt quất, chanh, soda mát lạnh.', N'🫐', 1,0,0,0,3),
 (N'Soda & Nước ép', N'Soda Dâu Bạc Hà', 38000, N'Dâu tươi, bạc hà và soda.', N'🍓', 1,0,0,0,3),
 (N'Soda & Nước ép', N'Nước Cam Ép', 40000, N'Cam vàng vắt tươi, không đường thêm.', N'🍊', 1,0,1,0,3),
 (N'Soda & Nước ép', N'Nước Ép Dưa Hấu', 35000, N'Dưa hấu ép tươi mát.', N'🍉', 1,0,0,0,3),
 (N'Soda & Nước ép', N'Nước Ép Ổi Hồng', 40000, N'Ổi hồng ép nguyên chất, thơm dịu.', N'🍐', 1,0,0,0,3),
 (N'Soda & Nước ép', N'Chanh Dây Mật Ong', 35000, N'Chanh dây, mật ong và đá, chua ngọt cân bằng.', N'🍯', 1,0,0,0,3),
 (N'Soda & Nước ép', N'Sữa Chua Đánh Đá', 32000, N'Sữa chua lên men mịn, đánh cùng đá.', N'🥛', 1,0,0,0,3),
 -- MATCHA & CHOCOLATE
 (N'Matcha & Chocolate', N'Matcha Latte', 48000, N'Matcha Nhật, sữa tươi, vị trà rõ.', N'🍵', 1,1,1,0,5),
 (N'Matcha & Chocolate', N'Matcha Dâu', 52000, N'Matcha phối dâu tươi, hai tầng màu.', N'🍵', 1,0,0,1,5),
 (N'Matcha & Chocolate', N'Chocolate Nóng', 45000, N'Chocolate đậm, sữa nóng, phủ kem tươi.', N'🍫', 1,1,0,0,5),
 (N'Matcha & Chocolate', N'Chocolate Đá', 45000, N'Chocolate sữa lạnh, béo ngọt.', N'🍫', 1,0,0,0,4),
 (N'Matcha & Chocolate', N'Hojicha Latte', 48000, N'Trà xanh rang Hojicha, thơm khói, ít đắng.', N'🍵', 1,1,0,1,5),
 -- BÁNH NGỌT
 (N'Bánh ngọt', N'Muffin Chocolate', 28000, N'Muffin mềm, nhân chocolate chip.', N'🧁', 0,0,0,0,2),
 (N'Bánh ngọt', N'Bông Lan Trứng Muối', 35000, N'Bông lan mềm, sốt trứng muối và ruốc.', N'🍰', 0,0,1,0,2),
 (N'Bánh ngọt', N'Bánh Flan Caramel', 25000, N'Flan trứng mịn, caramel đắng nhẹ.', N'🍮', 0,0,0,0,2),
 (N'Bánh ngọt', N'Mochi Kem', 30000, N'Vỏ mochi dẻo, nhân kem lạnh, chọn vị.', N'🍡', 0,0,0,1,2),
 (N'Bánh ngọt', N'Cookie Chocolate', 22000, N'Cookie bơ giòn, chocolate chip.', N'🍪', 0,0,0,0,2),
 (N'Bánh ngọt', N'Red Velvet Slice', 42000, N'Bánh nhung đỏ, phủ kem cheese.', N'🍰', 0,0,0,0,2),
 -- MÓN ĂN NHẸ
 (N'Món ăn nhẹ', N'Bánh Mì Que Pate', 20000, N'Bánh mì que giòn, pate và bơ.', N'🥖', 0,0,1,0,4),
 (N'Món ăn nhẹ', N'Sandwich Gà Phô Mai', 45000, N'Gà xé, phô mai, rau tươi, nướng nóng.', N'🥪', 0,0,0,0,7),
 (N'Món ăn nhẹ', N'Khoai Tây Chiên', 35000, N'Khoai chiên giòn, kèm sốt tương cà.', N'🍟', 0,0,1,0,7),
 (N'Món ăn nhẹ', N'Gà Viên Chiên', 40000, N'Gà viên chiên vàng, sốt mayonnaise.', N'🍗', 0,0,0,0,7),
 (N'Món ăn nhẹ', N'Mì Ý Bò Bằm', 55000, N'Mì Ý sốt bò bằm cà chua, phô mai.', N'🍝', 0,0,0,1,10),
 (N'Món ăn nhẹ', N'Bánh Tráng Nướng', 30000, N'Bánh tráng nướng giòn, trứng, hành, phô mai.', N'🫓', 0,0,0,0,6)
) v(cat, name, price, descr, emoji, sz, hot, best, nw, prep)
JOIN Categories c ON c.name = v.cat;
GO

-- Quy ước đường dẫn ảnh mặc định
UPDATE Products SET image_path = 'images/products/' + CAST(product_id AS VARCHAR(10)) + '.jpg' WHERE image_path IS NULL;
GO

-- Toppings
INSERT INTO Toppings (name, price) VALUES
(N'Trân châu đen', 7000), (N'Trân châu trắng', 7000), (N'Trân châu đường đen', 8000),
(N'Trân châu hoàng kim', 9000), (N'Thạch trái cây', 7000), (N'Thạch dừa', 7000),
(N'Thạch cà phê', 7000), (N'Pudding trứng', 8000), (N'Sương sáo', 7000),
(N'Nha đam', 7000), (N'Hạt sen', 8000), (N'Đào miếng', 10000), (N'Vải', 10000),
(N'Kem cheese macchiato', 12000), (N'Foam muối', 10000), (N'Kem tươi whipping', 8000),
(N'Oreo vụn', 8000), (N'Shot espresso thêm', 10000), (N'Sữa đặc thêm', 5000);
GO

-- CategoryToppings mapping
INSERT INTO CategoryToppings (category_id, topping_id)
SELECT c.category_id, t.topping_id
FROM (VALUES
 (N'Cà phê', N'Shot espresso thêm'), (N'Cà phê', N'Thạch cà phê'), (N'Cà phê', N'Foam muối'),
 (N'Cà phê', N'Kem tươi whipping'), (N'Cà phê', N'Pudding trứng'), (N'Cà phê', N'Sữa đặc thêm'),
 (N'Trà & Trái cây', N'Trân châu trắng'), (N'Trà & Trái cây', N'Thạch trái cây'), (N'Trà & Trái cây', N'Đào miếng'),
 (N'Trà & Trái cây', N'Vải'), (N'Trà & Trái cây', N'Nha đam'), (N'Trà & Trái cây', N'Hạt sen'), (N'Trà & Trái cây', N'Thạch dừa'),
 (N'Trà sữa', N'Trân châu đen'), (N'Trà sữa', N'Trân châu đường đen'), (N'Trà sữa', N'Trân châu hoàng kim'),
 (N'Trà sữa', N'Pudding trứng'), (N'Trà sữa', N'Sương sáo'), (N'Trà sữa', N'Kem cheese macchiato'),
 (N'Trà sữa', N'Thạch dừa'), (N'Trà sữa', N'Thạch trái cây'),
 (N'Đá xay & Sinh tố', N'Kem tươi whipping'), (N'Đá xay & Sinh tố', N'Oreo vụn'),
 (N'Đá xay & Sinh tố', N'Trân châu đen'), (N'Đá xay & Sinh tố', N'Thạch cà phê'),
 (N'Soda & Nước ép', N'Thạch trái cây'), (N'Soda & Nước ép', N'Nha đam'), (N'Soda & Nước ép', N'Sương sáo'),
 (N'Matcha & Chocolate', N'Kem tươi whipping'), (N'Matcha & Chocolate', N'Trân châu đen'),
 (N'Matcha & Chocolate', N'Kem cheese macchiato'), (N'Matcha & Chocolate', N'Oreo vụn')
) v(cat, topping)
JOIN Categories c ON c.name = v.cat
JOIN Toppings t ON t.name = v.topping;
GO

-- Tables
INSERT INTO Tables (table_number, area_zone, capacity) VALUES 
(1, N'Tầng 1', 2), (2, N'Tầng 1', 2), (3, N'Tầng 1', 4), (4, N'Tầng 1', 4),
(5, N'Sân Vườn', 4), (6, N'Sân Vườn', 4), (7, N'Phòng Lạnh', 6), (8, N'Phòng Lạnh', 6);
GO

-- Staff
INSERT INTO Staff (full_name, role, username, password_hash) VALUES
(N'Chủ quán', 'OWNER', 'admin', 'cafe123'),
(N'Quản lý ca', 'MANAGER', 'manager', 'cafe123'),
(N'Thu ngân 1', 'CASHIER', 'cashier1', 'cafe123'),
(N'Pha chế 1', 'BARISTA', 'barista1', 'cafe123');
GO

-- Vouchers
INSERT INTO Vouchers (code, discount_type, discount_value, min_order_amount, valid_from, valid_to, usage_limit) VALUES
('WELCOME10', 'PERCENT', 10, 50000, '2026-10-01', '2026-12-31', 500),
('GIAM15K', 'AMOUNT', 15000, 100000, '2026-10-01', '2026-12-31', 200);
GO

-- Ingredients
INSERT INTO Ingredients (name, unit, current_stock, min_alert_threshold) VALUES
(N'Cà phê Robusta rang', N'gram', 5000, 500), (N'Cà phê Arabica rang', N'gram', 3000, 400),
(N'Sữa tươi', N'ml', 20000, 3000), (N'Sữa đặc', N'gram', 6000, 800), (N'Kem béo (whipping)', N'ml', 4000, 500),
(N'Bột kem béo', N'gram', 3000, 400), (N'Trà đen', N'gram', 2000, 300), (N'Trà ô long', N'gram', 1500, 200),
(N'Bột matcha', N'gram', 1000, 150), (N'Bột cacao', N'gram', 1500, 200), (N'Sốt chocolate', N'ml', 3000, 400),
(N'Sốt caramel', N'ml', 2500, 300), (N'Đường nước', N'ml', 15000, 2000), (N'Đá viên', N'gram', 50000, 5000),
(N'Trân châu đen', N'gram', 4000, 500), (N'Thạch trái cây', N'gram', 3000, 400), (N'Cam tươi', N'cái', 80, 15),
(N'Chanh tươi', N'cái', 100, 20), (N'Đào ngâm', N'gram', 3000, 400), (N'Dâu tươi', N'gram', 2000, 300),
(N'Xoài chín', N'gram', 3000, 400), (N'Ly nhựa size M', N'cái', 800, 100), (N'Ly nhựa size L', N'cái', 600, 100),
(N'Ống hút', N'cái', 2000, 300), (N'Túi giấy mang đi', N'cái', 500, 80);
GO
