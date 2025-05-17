package NodeClient.File;

public record FileLogEntry(
        String fileName,
        long fileHash,
        String operation,
        String timeStamp,
        String path
) {}
