package NodeClient.File;

import NodeClient.RingAPI.RingStorage;
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
        Path filePath = localPath.resolve(message.getFileName());
        switch (message.getOperation()) {
            case "CREATE":
                Files.write(filePath, message.getFileData(), StandardOpenOption.CREATE);
                break;
            case "DELETE":
                Files.deleteIfExists(filePath);
                // TODO delete the replicated files as well
                break;
            case "SHUTDOWN":
                // TODO handle transfer files if needed
                break;
        }
    }

    public void replicateToNeighbors(String fileName, byte[] data, String operation) {
        ReplicationMessage message = new ReplicationMessage(fileName, operation, data);
        try {
            if (ringStorage.getNode("NEXT").isPresent()) {
                String nextIpAddress = ringStorage.getNode("NEXT").get().getIpAddress();
                // This will send a Post request to the next node, so it can replicate the file
                new RestTemplate().postForObject("http://" + nextIpAddress + ":" + serverPort + "/node/file/replication", message, Void.class);
            }
            if (ringStorage.getNode("PREVIOUS").isPresent()) {
                String previousIpAddress = ringStorage.getNode("PREVIOUS").get().getIpAddress();
                // This will send a Post request to the previous node, so it can replicate the file
                new RestTemplate().postForObject("http://" + previousIpAddress + ":" + serverPort + "/node/file/replication", message, Void.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
