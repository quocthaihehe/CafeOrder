package cafe.models;

import java.text.SimpleDateFormat;
import java.util.Date;

public class AuditLog {
    private int id;
    private String timestamp;
    private String user;
    private String action;
    private String target;
    private String result; // "SUCCESS", "FAILED"

    public AuditLog(int id, String timestamp, String user, String action, String target, String result) {
        this.id = id;
        this.timestamp = timestamp != null ? timestamp : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        this.user = user;
        this.action = action;
        this.target = target;
        this.result = result != null ? result : "SUCCESS";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
}
