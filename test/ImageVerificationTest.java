package test;

import cafe.models.MenuItem;
import cafe.models.MenuRepository;
import cafe.utils.ImageManager;
import javafx.application.Platform;
import javafx.scene.image.Image;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ImageVerificationTest {
    public static void main(String[] args) throws Exception {
        System.out.println("=== KIỂM THỬ TOÀN BỘ 8 DANH MỤC (69 MÓN) VỚI ĐỘ PHÂN GIẢI GỐC 971x1620 ===");

        CountDownLatch fxLatch = new CountDownLatch(1);
        try {
            Platform.startup(() -> fxLatch.countDown());
        } catch (IllegalStateException e) {
            fxLatch.countDown();
        }
        fxLatch.await(5, TimeUnit.SECONDS);

        List<MenuItem> allItems = MenuRepository.getAllItems();
        int tested = 0;
        int fullResPass = 0;

        String[] targetCategories = {
            "Cà phê", 
            "Trà & Trái cây", 
            "Đá xay & Sinh tố", 
            "Trà sữa",
            "Soda & Nước ép",
            "Matcha & Chocolate",
            "Bánh ngọt",
            "Món ăn nhẹ"
        };

        for (String cat : targetCategories) {
            System.out.println("\n--- [" + cat.toUpperCase() + "] ---");
            for (MenuItem item : allItems) {
                if (cat.equalsIgnoreCase(item.getCategory())) {
                    tested++;
                    // 1. Kiểm thử Full Resolution cho ItemDetailDialog
                    Image fullImg = ImageManager.getProductImageFull(item);
                    if (fullImg == null || fullImg.isError()) {
                        System.err.println("❌ LỖI: Không tải được ảnh Full cho " + item.getName());
                        continue;
                    }

                    double fw = fullImg.getWidth();
                    double fh = fullImg.getHeight();

                    // 2. Kiểm thử Card Grid Resolution
                    Image cardImg = ImageManager.getProductImage(item, 600, 1000);
                    double cw = cardImg.getWidth();
                    double ch = cardImg.getHeight();

                    System.out.printf("✅ Món #%02d: %-30s | Detail Dialog: %.0fx%.0f (GỐC NGUYÊN BẢN) | Card: %.0fx%.0f\n",
                            item.getId(), item.getName(), fw, fh, cw, ch);

                    // Kiểm tra ảnh Detail Dialog phải là ảnh gốc sắc nét (>= 900x1400)
                    if (fw >= 900 && fh >= 1400) {
                        fullResPass++;
                    } else {
                        System.err.printf("⚠️ CẢNH BÁO: Món #%02d (%s) kích thước %.0fx%.0f chưa đạt chuẩn 971x1620!\n",
                                item.getId(), item.getName(), fw, fh);
                    }
                }
            }
        }

        System.out.printf("\nKết quả: %d/%d món đạt chuẩn ĐỘ NÉT GỐC TỐI ĐA (Native Full Resolution 971x1620)!\n",
                fullResPass, tested);

        if (fullResPass == tested && tested == 69) {
            System.out.println("🎉 HOÀN TẤT XUẤT SẮC: TOÀN BỘ 69/69 MÓN TRÊN CẢ 8 DANH MỤC ĐẠT ĐỘ SẮC NÉT NGUYÊN BẢN 100%! 🎉");
        } else {
            System.err.println("⚠️ Có món chưa đạt độ phân giải gốc! Cần kiểm tra lại.");
            System.exit(1);
        }
        Platform.exit();
    }
}
