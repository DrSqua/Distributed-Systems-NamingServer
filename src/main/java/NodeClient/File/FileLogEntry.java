package NodeClient.File;

public class FileLogEntry {
    private String fileName;
    private long fileHash;
    private String operation;
    private String replicationTime;
    private String localPath;

    // TODO better logs
    public FileLogEntry(String fileName, long fileHash, String operation, String replicationTime, String localPath) {
        this.fileName = fileName;
        this.fileHash = fileHash;
        this.operation = operation;
        this.replicationTime = replicationTime;
        this.localPath = localPath;
    }
}
