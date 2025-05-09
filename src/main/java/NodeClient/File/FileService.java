package NodeClient.File;

import NodeClient.RingAPI.RingStorage;

import Utilities.NodeEntity.NodeEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;

@Service
public class FileService {
    private final Path localPath = Paths.get("local_files").toAbsolutePath();
    // Inject RingStorage so we know the neighbors
    private final RingStorage ringStorage;
    // Get the Server port from application.properties
    @Value("${server.port}")
    private int serverPort;

    @Autowired
    public FileService(RingStorage ringStorage) throws IOException {
        Files.createDirectories(localPath);
        this.ringStorage = ringStorage;
    }

    public void storeFile(MultipartFile file) throws IOException {
        Path targetPath = localPath.resolve(file.getOriginalFilename());
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
    }

    public void deleteFile(String name) throws IOException {
        Path targetPath = localPath.resolve(name);
        Files.deleteIfExists(targetPath);
    }

    public void handleReplication(ReplicationMessage message) throws IOException {
        Path filePath = localPath.resolve(message.fileName());
        switch (message.operation()) {
            case "CREATE":
                Files.write(filePath, message.fileData(), StandardOpenOption.CREATE);
                break;
            case "DELETE":
                Files.deleteIfExists(filePath);
                break;
            case "SHUTDOWN":
                // TODO handle transfer files if needed
                break;
        }
    }

    public void replicateToNeighbors(String fileName, String operation, byte[] data) {
        ReplicationMessage message = new ReplicationMessage(fileName, operation, data);
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
