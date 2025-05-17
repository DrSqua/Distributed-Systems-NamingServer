package NodeClient.File;

import NodeClient.RingAPI.RingStorage;

import Utilities.NodeEntity.NodeEntity;
import Utilities.RestMessagesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import schnitzel.NamingServer.NamingServerHash;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

@Service
public class FileService {
    private final Path localPath = Paths.get("local_files").toAbsolutePath();
    private final Path replicatedPath = Paths.get("replicated_files").toAbsolutePath();
    // Inject RingStorage so we can get the neighbors
    private final RingStorage ringStorage;
    private final FileLoggerService fileLoggerService;
    // Get the Server port from application.properties
    @Value("${server.port}")
    private int serverPort;

    @Autowired
    public FileService(RingStorage ringStorage, FileLoggerService fileLoggerService) throws IOException {
        Files.createDirectories(localPath);
        Files.createDirectories(replicatedPath);
        this.ringStorage = ringStorage;
        this.fileLoggerService = fileLoggerService;
    }

    public FileLoggerService getFileLoggerService() {
        return fileLoggerService;
    }

    // handle all file operations except transfer (because transfer also needs to transfer logs)
    public void handleFileOperations(FileMessage message) throws IOException {
        String fileName = message.fileName();
        long fileHash = NamingServerHash.hash(fileName);
        Path replicatedFilePath = replicatedPath.resolve(fileName);
        Path localFilePath = localPath.resolve(fileName);
        Path targetPath = null;
        String operation = message.operation();
        switch (operation) {
            case "CREATE":
                targetPath = localFilePath;
                Files.write(targetPath, message.fileData(), StandardOpenOption.CREATE);
                break;
            case "REPLICATE":
                targetPath = replicatedFilePath;
                Files.write(targetPath, message.fileData(), StandardOpenOption.CREATE);
                break;
            case "DELETE_LOCAL":
                targetPath = localFilePath;
                Files.deleteIfExists(targetPath);
                break;
            case "DELETE_REPLICA":
                targetPath = replicatedFilePath;
                Files.deleteIfExists(targetPath);
                break;
        }
        fileLoggerService.logOperation(fileName, fileHash, operation, String.valueOf(targetPath));
    }

    // handle "TRANSFER" operation
    public void handleTransfer(FileMessage message) throws IOException {
        String fileName = message.fileName();
        long fileHash = NamingServerHash.hash(fileName);
        Path targetPath = replicatedPath.resolve(fileName);
        Files.write(targetPath, message.fileData(), StandardOpenOption.CREATE);
        // write the previous logs before transfer
        List<FileLogEntry> logs = fileLoggerService.getLogsForFile(fileName);
        fileLoggerService.writeLogs(logs);
        // write the transfer log
        fileLoggerService.logOperation(fileName, fileHash, message.operation(), String.valueOf(targetPath));
    }

    // handle "DOWNLOAD" operation
    public byte[] readFile(String fileName) throws IOException {
        long fileHash = NamingServerHash.hash(fileName);

        // we only need to check the localPath as the naming server already checked for ownership
        Path filePath = localPath.resolve(fileName);
        if (Files.exists(filePath)) {
            fileLoggerService.logOperation(fileName, fileHash, "DOWNLOAD", String.valueOf(filePath));
            return Files.readAllBytes(filePath);
        }
        return null;
    }

    public void replicateToNeighbors(String fileName, String operation, byte[] data) {
        FileMessage message = new FileMessage(fileName, operation, data);
        try {
            // Getting next node and its IP
            NodeEntity nextNode = ringStorage.getNode("NEXT").orElseThrow(() ->
                    new IllegalStateException("Existing Node does not have next set")
            );
            String nextIpAddress = nextNode.getIpAddress();
            // Send a Post request to next node for file replication
            RestMessagesRepository.handleFileOperations(message, nextIpAddress, serverPort);
            // Getting previous node and its IP
            NodeEntity previousNode = this.ringStorage.getNode("PREVIOUS").orElseThrow(() ->
                    new IllegalStateException("Existing Node does not have previous set")
            );
            String previousIpAddress = previousNode.getIpAddress();
            // Send a Post request to previous node for file replication
            RestMessagesRepository.handleFileOperations(message, previousIpAddress, serverPort);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
