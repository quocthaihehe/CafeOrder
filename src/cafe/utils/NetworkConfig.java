package cafe.utils;

/**
 * NetworkConfig - Quản lý cấu hình mạng tập trung cho hệ thống CafeOrder:
 * - Tránh hardcode cổng mạng (Zero Hardcoded Port).
 * - Ưu tiên nạp cổng động từ biến môi trường (SERVER_PORT / PORT) hoặc tham số dòng lệnh (-Dserver.port).
 * - Mặc định sử dụng cổng 8888 (nằm trong dải Registered Ports 1024-49151), tránh cổng 5000 vốn hay bị đụng độ với hệ thống.
 */
public class NetworkConfig {
    private static String serverHost = "127.0.0.1";
    private static int serverPort = 8888; // Đổi từ 5000 sang 8888 để tránh xung đột với AirPlay / SSDP / Flask

    static {
        // 1. Kiểm tra tham số dòng lệnh JVM: -Dserver.port=... hoặc -Dserver.host=...
        String propPort = System.getProperty("server.port");
        if (propPort != null && !propPort.trim().isEmpty()) {
            try {
                int p = Integer.parseInt(propPort.trim());
                if (isValidPort(p)) serverPort = p;
            } catch (NumberFormatException ignored) {}
        }

        String propHost = System.getProperty("server.host");
        if (propHost != null && !propHost.trim().isEmpty()) {
            serverHost = propHost.trim();
        }

        // 2. Kiểm tra biến môi trường hệ điều hành: SERVER_PORT hoặc PORT
        String envPort = System.getenv("SERVER_PORT");
        if (envPort == null || envPort.trim().isEmpty()) {
            envPort = System.getenv("PORT");
        }
        if (envPort != null && !envPort.trim().isEmpty()) {
            try {
                int p = Integer.parseInt(envPort.trim());
                if (isValidPort(p)) serverPort = p;
            } catch (NumberFormatException ignored) {}
        }

        String envHost = System.getenv("SERVER_HOST");
        if (envHost != null && !envHost.trim().isEmpty()) {
            serverHost = envHost.trim();
        }
    }

    public static String getServerHost() {
        return serverHost;
    }

    public static void setServerHost(String host) {
        if (host != null && !host.trim().isEmpty()) {
            serverHost = host.trim();
        }
    }

    public static int getServerPort() {
        return serverPort;
    }

    public static void setServerPort(int port) {
        if (isValidPort(port)) {
            serverPort = port;
        }
    }

    public static boolean isValidPort(int port) {
        return port >= 1024 && port <= 65535;
    }
}
