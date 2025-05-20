package schnitzel.NamingServer.GUI.DTO;

public class FileInfoDisplay {
    private String fileName;
    private String type; // "local" or "replicated"

    public FileInfoDisplay() {}
    public FileInfoDisplay(String fileName, String type) {
        this.fileName = fileName;
        this.type = type;
    }

    // Getters and Setters
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}