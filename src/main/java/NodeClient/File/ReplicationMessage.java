package NodeClient.File;

public class ReplicationMessage {
    private String fileName;
    private String operation;
    private byte[] fileData;

    public ReplicationMessage(String fileName, String operation, byte[] fileData) {
        this.fileName = fileName;
        this.operation = operation;
        this.fileData = fileData;
    }

    public String getFileName() {
        return fileName;
    }

    public String getOperation() {
        return operation;
    }

    public byte[] getFileData() {
        return fileData;
    }
}

