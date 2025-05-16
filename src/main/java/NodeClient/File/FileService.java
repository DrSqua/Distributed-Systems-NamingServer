package NodeClient.File;

import NodeClient.RingAPI.RingStorage;

import Utilities.NodeEntity.NodeEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import schnitzel.NamingServer.NamingServerHash;

import java.io.IOException;
import java.nio.file.*;

@Service
public class FileService {
    private final Path localPath = Paths.get("local_files").toAbsolutePath();
    private final Path replicatedPath = Paths.get("replicated_files").toAbsolutePath();
    // Inject RingStorage so we know the neighbors
    private final RingStorage ringStorage;
    // Get the Server port from application.properties
    @Value("${server.port}")
    private int serverPort;

    @Autowired
    private FileLoggerService fileLoggerService;

    @Autowired
    public FileService(RingStorage ringStorage) throws IOException {
        Files.createDirectories(localPath);
        Files.createDirectories(replicatedPath);
        this.ringStorage = ringStorage;
    }

    public void handleFileOperations(FileMessage message) throws IOException {
        String fileName = message.fileName();
        long fileHash = NamingServerHash.hash(fileName);
        Path replicatedFilePath = replicatedPath.resolve(fileName);
        Path localFilePath = localPath.resolve(fileName);
        Path targetPath = null;
        switch (message.operation()) {
            case "CREATE":
                targetPath = localFilePath;
                Files.write(targetPath, message.fileData(), StandardOpenOption.CREATE);
            case "REPLICATE":
                targetPath = replicatedFilePath;
                Files.write(targetPath, message.fileData(), StandardOpenOption.CREATE);
            case "DELETE_LOCAL":
                targetPath = localFilePath;
                Files.deleteIfExists(targetPath);
            case "DELETE_REPLICA":
                targetPath = replicatedFilePath;
                Files.deleteIfExists(targetPath);
        }
        fileLoggerService.logReplication(fileName, fileHash, message.operation(), String.valueOf(targetPath));
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
            new RestTemplate().postForObject("http://" + nextIpAddress + ":" + serverPort + "/node/file/replication", message, Void.class);

            // Getting previous node and its IP
            NodeEntity previousNode = this.ringStorage.getNode("PREVIOUS").orElseThrow(() ->
                    new IllegalStateException("Existing Node does not have previous set")
            );
            String previousIpAddress = previousNode.getIpAddress();
            // Send a Post request to previous node for file replication
            new RestTemplate().postForObject("http://" + previousIpAddress + ":" + serverPort + "/node/file/replication", message, Void.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
