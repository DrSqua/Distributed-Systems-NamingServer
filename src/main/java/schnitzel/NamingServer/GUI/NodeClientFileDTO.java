package schnitzel.NamingServer.GUI;

// This DTO represents what you expect /api/files/list on NodeClient to return
public class NodeClientFileDTO {
    private String fileName;
    private String type;

    public NodeClientFileDTO() {}

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}