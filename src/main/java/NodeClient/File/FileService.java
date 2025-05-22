package NodeClient.File;

import NodeClient.RingAPI.RingStorage;

import Utilities.NodeEntity.NodeEntity;
import Utilities.RestMessagesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import schnitzel.NamingServer.NamingServerHash;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FileService {
    private final Path localPath = Paths.get("local_files").toAbsolutePath();
    private final Path replicatedPath = Paths.get("replicated_files").toAbsolutePath();
    // Inject RingStorage so we can get the neighbors
    private final RingStorage ringStorage;
    private final FileLoggerService fileLoggerService;
    // File Locking Map
    private final Map<String, Boolean> fileLockingStates = new ConcurrentHashMap<>();

    @Autowired
    public FileService(RingStorage ringStorage, FileLoggerService fileLoggerService) {
        try {
            Files.createDirectories(localPath);
            Path filePath = localPath.resolve("file.txt");
            byte[] data = "Pooepieeeeeees".getBytes();
            Files.write(filePath, data, StandardOpenOption.CREATE);
            Files.createDirectories(replicatedPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.ringStorage = ringStorage;
        this.fileLoggerService = fileLoggerService;
    }

    public FileLoggerService getFileLoggerService() {
        return fileLoggerService;
    }

    // handle all file operations except "TRANSFER"s and "DOWNLOAD"s (because they work different)
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
        String operation = message.operation();
        Path targetPath = null;
        if (operation.equals("TRANSFER_LOCAL")) {
            targetPath = localPath.resolve(fileName);
        }
        else if (operation.equals("TRANSFER_REPLICA")) {
            targetPath = replicatedPath.resolve(fileName);
        }
        assert targetPath != null;
        Files.write(targetPath, message.fileData(), StandardOpenOption.CREATE);
        // write the previous logs before transfer
        List<FileLogEntry> logs = fileLoggerService.getLogsForFile(fileName);
        fileLoggerService.writeLogs(logs);
        // write the transfer log
        fileLoggerService.logOperation(fileName, fileHash, operation, String.valueOf(targetPath));
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
            RestMessagesRepository.handleFileOperations(message, nextIpAddress);
            // Getting previous node and its IP
            NodeEntity previousNode = this.ringStorage.getNode("PREVIOUS").orElseThrow(() ->
                    new IllegalStateException("Existing Node does not have previous set")
            );
            String previousIpAddress = previousNode.getIpAddress();
            // Send a Post request to previous node for file replication
            RestMessagesRepository.handleFileOperations(message, previousIpAddress);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns a list of names of files stored locally that are owned by this node.
     * @return List of local file names, or empty list if none or error.
     */
    public List<String> listLocalFiles() throws IOException {
        return Files.list(localPath)
                .filter(Files::isRegularFile)
                .map(path -> path.getFileName().toString())
                .toList();
    }

    /**
     * Returns a list of names of files stored on this node as replicas from other nodes.
     * @return List of replicated file names, or empty list if none or error.
     */
    public List<String> listReplicatedFiles() throws IOException {
        return Files.list(replicatedPath)
                .filter(Files::isRegularFile)
                .map(path -> path.getFileName().toString())
                .toList();
    }

    public void lockFile(String fileName) {
        fileLockingStates.put(fileName, true);
    }

    public void unlockFile(String fileName) {
        fileLockingStates.put(fileName, false);
    }

    public boolean isFileLocked(String fileName) {
        return fileLockingStates.getOrDefault(fileName, false);
    }

    public Map<String, Boolean> getCurrentLockingStates() {
        return fileLockingStates;
    }
}