package cafe.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * NetworkConfig - Quản lý cấu hình mạng tập trung cho hệ thống CafeOrder:
 * - Tránh hardcode cổng mạng (Zero Hardcoded Port).
 * - Tự động lưu và đồng bộ cấu hình qua tệp config/network.properties.
 * - Ưu tiên nạp cổng động từ biến môi trường (SERVER_PORT / PORT) hoặc tham số dòng lệnh (-Dserver.port).
 * - Mặc định sử dụng cổng 8888 (nằm trong dải Registered Ports 1024-49151), tránh cổng 5000 vốn hay bị đụng độ với hệ thống.
 */
public class NetworkConfig {
    private static final String CONFIG_DIR = "config";
    private static final String CONFIG_FILE = "config/network.properties";
    private static final String FALLBACK_CONFIG_FILE = "network.properties";

    private static String serverHost = "127.0.0.1";
    private static int serverPort = 8888; // Mặc định 8888

    static {
        loadConfigFromFile();
        loadConfigFromEnvAndProps();
    }

    private static void loadConfigFromFile() {
        File f = new File(CONFIG_FILE);
        if (!f.exists()) {
            f = new File(FALLBACK_CONFIG_FILE);
        }
        if (f.exists() && f.isFile()) {
            try (FileInputStream fis = new FileInputStream(f);
                 InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
                Properties props = new Properties();
                props.load(isr);
                String pHost = props.getProperty("server.host");
                if (pHost != null && !pHost.trim().isEmpty()) {
                    serverHost = pHost.trim();
                }
                String pPort = props.getProperty("server.port");
                if (pPort != null && !pPort.trim().isEmpty()) {
                    int p = Integer.parseInt(pPort.trim());
                    if (isValidPort(p)) {
                        serverPort = p;
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    private static void loadConfigFromEnvAndProps() {
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

    public static synchronized void saveConfig(String host, int port) {
        if (host != null && !host.trim().isEmpty()) {
            serverHost = host.trim();
        }
        if (isValidPort(port)) {
            serverPort = port;
        }

        // Ghi ra file config/network.properties
        try {
            File dir = new File(CONFIG_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File f = new File(CONFIG_FILE);
            Properties props = new Properties();
            props.setProperty("server.host", serverHost);
            props.setProperty("server.port", String.valueOf(serverPort));
            props.setProperty("last_updated", String.valueOf(System.currentTimeMillis()));

            try (FileOutputStream fos = new FileOutputStream(f);
                 OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                props.store(osw, "Cấu hình mạng hệ thống Brew & Bean CoffeePOS");
            }
        } catch (IOException e) {
            System.err.println("[NetworkConfig] Không thể lưu tệp cấu hình mạng: " + e.getMessage());
        }
    }

    public static String getServerHost() {
        return serverHost;
    }

    public static synchronized void setServerHost(String host) {
        if (host != null && !host.trim().isEmpty()) {
            serverHost = host.trim();
            saveConfig(serverHost, serverPort);
        }
    }

    public static int getServerPort() {
        return serverPort;
    }

    public static synchronized void setServerPort(int port) {
        if (isValidPort(port)) {
            serverPort = port;
            saveConfig(serverHost, serverPort);
        }
    }

    public static boolean isValidPort(int port) {
        return port >= 1024 && port <= 65535;
    }
}
