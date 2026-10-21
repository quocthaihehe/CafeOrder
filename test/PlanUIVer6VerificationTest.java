package test;

import cafe.models.MenuItem;
import cafe.models.MessageProtocol;
import cafe.models.MessageType;
import cafe.services.CafeServerService;
import cafe.services.DatabaseManager;

import java.util.ArrayList;
import java.util.List;

public class PlanUIVer6VerificationTest {

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("  KIỂM THỬ TÍNH NĂNG MỚI PLAN UI VER 6.0");
        System.out.println("===============================================================");

        int passed = 0;
        int failed = 0;

        // Test 1: MessageProtocol SessionKicked
        try {
            MessageProtocol.SessionKickedMessage kickedMsg = new MessageProtocol.SessionKickedMessage("CLIENT-009", "Kicked by Admin", 60000L);
            if ("CLIENT-009".equals(kickedMsg.sessionId) && MessageType.SESSION_KICKED.name().equals(kickedMsg.type) && kickedMsg.banMs == 60000L) {
                System.out.println("[PASS] 1. MessageProtocol.SessionKickedMessage cấu hình chuẩn.");
                passed++;
            } else {
                System.err.println("[FAIL] 1. MessageProtocol.SessionKickedMessage sai dữ liệu.");
                failed++;
            }
        } catch (Exception e) {
            System.err.println("[FAIL] 1. Exception: " + e.getMessage());
            failed++;
        }

        // Test 2: CafeServerService kickSession and blacklist
        try {
            CafeServerService server = CafeServerService.getInstance();
            server.kickSession("CLIENT-TEST-15");
            boolean notBlacklisted = server.isSessionBlacklisted("CLIENT-RANDOM-999");
            if (!notBlacklisted) {
                System.out.println("[PASS] 2. CafeServerService blacklist kiểm tra chính xác.");
                passed++;
            } else {
                System.err.println("[FAIL] 2. Blacklist verification failed.");
                failed++;
            }
        } catch (Exception e) {
            System.err.println("[FAIL] 2. Exception: " + e.getMessage());
            failed++;
        }

        // Test 3: DatabaseManager saveRecipeForProduct
        try {
            List<DatabaseManager.RecipeEntry> recipe = new ArrayList<>();
            recipe.add(new DatabaseManager.RecipeEntry("Cà phê hạt Robusta", 20.0, "gram", 6000.0));
            recipe.add(new DatabaseManager.RecipeEntry("Sữa đặc Ông Thọ", 30.0, "ml", 3600.0));
            DatabaseManager.saveRecipeForProduct(999, recipe);
            MenuItem dummyItem = new MenuItem(999, "Test Item", 40000, "Cà phê", "desc", "☕");
            List<DatabaseManager.RecipeEntry> loaded = DatabaseManager.getRecipesForProduct(dummyItem);
            if (loaded != null && loaded.size() == 2 && "Cà phê hạt Robusta".equals(loaded.get(0).ingredientName)) {
                System.out.println("[PASS] 3. DatabaseManager saveRecipeForProduct & getRecipesForProduct khớp 100%.");
                passed++;
            } else {
                System.err.println("[FAIL] 3. DatabaseManager recipe items mismatch.");
                failed++;
            }
        } catch (Exception e) {
            System.err.println("[FAIL] 3. Exception: " + e.getMessage());
            failed++;
        }

        // Test 4: DatabaseManager updateStaffPassword
        try {
            boolean updated = DatabaseManager.updateStaffPassword(1, "9999");
            if (updated) {
                System.out.println("[PASS] 4. DatabaseManager updateStaffPassword hoạt động ổn định.");
                passed++;
            } else {
                System.err.println("[FAIL] 4. updateStaffPassword returned false.");
                failed++;
            }
        } catch (Exception e) {
            System.err.println("[FAIL] 4. Exception: " + e.getMessage());
            failed++;
        }

        System.out.println("---------------------------------------------------------------");
        System.out.println("TỔNG KẾT: " + passed + " PASSED | " + failed + " FAILED");
        System.out.println("===============================================================");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
