package cafe.server;

import cafe.models.MessageProtocol;
import cafe.models.MessageType;
import cafe.models.Order;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

public class ClientHandler implements Runnable {
    private Socket socket;
    private int currentTable = -1; 

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true, java.nio.charset.StandardCharsets.UTF_8)
        ) {
            String line;
            while ((line = in.readLine()) != null) {
                MessageType type = MessageProtocol.getMessageType(line);
                if (type == null) {
                    continue;
                }

                switch (type) {
                    case HELLO:
                        MessageProtocol.HelloMessage hello = MessageProtocol.fromJson(line, MessageProtocol.HelloMessage.class);
                        this.currentTable = hello.table;
                        CafeServer.clientMap.put(this.currentTable, out); // Ghi danh vào sổ
                        System.out.println("\n[THÔNG BÁO] Bàn số " + currentTable + " đã kết nối.");
                        break;
                        
                    case NEW_ORDER:
                        MessageProtocol.NewOrderMessage orderMsg = MessageProtocol.fromJson(line, MessageProtocol.NewOrderMessage.class);
                        
                        int newOrderId = CafeServer.orderIdCounter.incrementAndGet();
                        int currentQueuePos = CafeServer.orderQueue.getQueueSize() + 1;
                        
                        Order newOrder = new Order(newOrderId, orderMsg.table, orderMsg.items, "QUEUED", System.currentTimeMillis());
                        CafeServer.orderQueue.addOrder(newOrder);
                        
                        MessageProtocol.OrderAckMessage ack = new MessageProtocol.OrderAckMessage(newOrderId, "QUEUED", currentQueuePos);
                        out.print(MessageProtocol.toJson(ack));
                        out.flush();
                        break;
                        
                    default:
                        break;
                }
            }
        } catch (SocketException e) {
            // Ngắt mạng ngầm
        } catch (IOException e) {
            System.err.println("[LỖI I/O] Bàn " + currentTable + ": " + e.getMessage());
        } finally {
            if (currentTable != -1) {
                CafeServer.clientMap.remove(currentTable); // Xóa khỏi sổ khi ngắt kết nối
            }
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {}
            System.out.println("\n[THÔNG BÁO] Bàn " + (currentTable != -1 ? currentTable : "chưa rõ") + " đã ngắt kết nối.");
        }
    }
}
