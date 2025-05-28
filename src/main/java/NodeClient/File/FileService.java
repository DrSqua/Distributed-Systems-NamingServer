package NodeClient.File;

import NodeClient.RingAPI.RingStorage;

import Utilities.NodeEntity.NodeEntity;
import Utilities.RestMessagesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import schnitzel.NamingServer.NamingServerHash;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
            String thisNodesIP = InetAddress.getLocalHost().toString();
            String lastID = thisNodesIP.substring(thisNodesIP.lastIndexOf('.') + 1);
            Files.createDirectories(localPath);
            Path targetPath = localPath.resolve("node"+lastID+".txt" );
            Files.writeString(
                    targetPath,
                    "Node, with hash: "+ringStorage.currentHash()+ ", IP: "+thisNodesIP+"\n",
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
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

    public RingStorage getRingStorage() {
        return ringStorage;
    }

    public Set<String> getLockedFiles() {
        return fileLockingStates.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
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
                // register that this is the original file owner
                String namingServerIp = ringStorage.getNamingServerIP();
                long originalOwnerNodeHash = NamingServerHash.hash(ringStorage.getSelf().getNodeName());
                RestMessagesRepository.registerFileOriginalOwner(fileHash, originalOwnerNodeHash, namingServerIp);
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
            // targetPath doesn't really matter here
            case "LOCKED":
                lockFile(fileName);
                break;
            case "UNLOCKED":
                unlockFile(fileName);
                break;
        }
        fileLoggerService.logOperation(fileName, fileHash, operation, ringStorage.currentName(), String.valueOf(targetPath));
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
        fileLoggerService.logOperation(fileName, fileHash, operation, ringStorage.currentName(), String.valueOf(targetPath));
    }

    // handle "DOWNLOAD" operation
    public byte[] downloadFile(String fileName) throws IOException {
        long fileHash = NamingServerHash.hash(fileName);
        // we only need to check the localPath as the naming server already checked for ownership
        Path filePath = localPath.resolve(fileName);
        if (Files.exists(filePath)) {
            fileLoggerService.logOperation(fileName, fileHash, "DOWNLOAD", ringStorage.currentName(), String.valueOf(filePath));
            return Files.readAllBytes(filePath);
        }
        return null;
    }

    public void replicateToNeighbors(String fileName, String operation, byte[] data) throws InterruptedException {
        // Getting next node
        NodeEntity nextNode = this.ringStorage.getNode("NEXT").orElse(null);
        System.out.println("ReplicateToNeighbors (next): "+nextNode);
        // Getting previous node
        NodeEntity previousNode = this.ringStorage.getNode("PREVIOUS").orElse(null);
        System.out.println("ReplicateToNeighbors (previous): "+previousNode);
        // Getting current node
        NodeEntity currentNode;
        try {
            currentNode = this.ringStorage.getSelf();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return;
        }
        if (nextNode == null || previousNode == null) {
            System.out.println("No next or previous node found");
            return;
        }
        String nextIpAddress = nextNode.getIpAddress();
        String previousIpAddress = previousNode.getIpAddress();
        // if 1 neighbor
        if (ringStorage.getCurrentNodeCount() == 2) {
            replicateToOneNeighbor(fileName, operation, data);
        }
        // if more then 1 neighbor (so Prev and next are not himself)
        else if (ringStorage.getCurrentNodeCount() >= 3){
            FileMessage message = new FileMessage(fileName, operation, data);
            try {
                // Send a Post request to next node for file replication
                RestMessagesRepository.handleFileOperations(message, nextIpAddress);
                // Send a Post request to previous node for file replication
                RestMessagesRepository.handleFileOperations(message, previousIpAddress);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void replicateToOneNeighbor(String fileName, String operation, byte[] data) throws InterruptedException {
        FileMessage message = new FileMessage(fileName, operation, data);
        // Getting next node and its IP
        NodeEntity nextNode = ringStorage.getNode("NEXT").orElseThrow(() ->
                new IllegalStateException("Existing Node does not have next set")
        );
        String nextIpAddress = nextNode.getIpAddress();
        // Send a Post request to next node for file replication
        RestMessagesRepository.handleFileOperations(message, nextIpAddress);
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

    public void editFile(String fileName) throws IOException, InterruptedException {
        if (isFileLocked(fileName)) {
            System.out.println("File " + fileName + " is locked");
            return;
        }
        // lock file and replicate to neighbors that they need to lock as well
        handleFileOperations(new FileMessage(fileName, "LOCKED", null));
        replicateToNeighbors(fileName, "LOCKED", null);
        System.out.println("Editing file: " + fileName);
        //
        // EDITING FILE...
        // Simulate with sleeping 10 seconds
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // after editing unlock the files again
        handleFileOperations(new FileMessage(fileName, "UNLOCKED", null));
        replicateToNeighbors(fileName, "UNLOCKED", null);
        System.out.println("Finished editing file: " + fileName);
    }

    // send a replicated file to the local_files as we're now the owner of this file
    public void promoteReplicaToLocal(String fileName, byte[] fileData) throws IOException {
        Path replicatedFilePath = replicatedPath.resolve(fileName);
        Path localFilePath = localPath.resolve(fileName);
        // Copy file to localFilePath
        Files.write(localFilePath, fileData, StandardOpenOption.CREATE);
        // Delete the replicated file
        Files.deleteIfExists(replicatedFilePath);
    }

    private void lockFile(String fileName) {
        fileLockingStates.put(fileName, true);
    }

    private void unlockFile(String fileName) {
        fileLockingStates.put(fileName, false);
    }

    private boolean isFileLocked(String fileName) {
        return fileLockingStates.getOrDefault(fileName, false);
    }
}