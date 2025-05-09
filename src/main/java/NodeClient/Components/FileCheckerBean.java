package NodeClient.Components;

import NodeClient.File.FileService;
import NodeClient.RingAPI.RingStorage;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import schnitzel.NamingServer.NamingServerHash;

import java.io.File;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

@Component
public class FileCheckerBean {
    private final RingStorage ringStorage;
    private final FileService fileService;
    private final Map<String, Long> knownFiles = new HashMap<>();
    private final Path filePathLocal;

    @Value("${server.port}")
    private int serverPort;

    @Autowired
    public FileCheckerBean(RingStorage ringStorage, FileService fileService) {
        this.ringStorage = ringStorage;
        this.fileService = fileService;
        Path path = Paths.get("").toAbsolutePath().normalize();
        this.filePathLocal = path.resolve("local_files");
    }

    @PostConstruct
    public void start() {
        new Thread(this::startup).start();
    }

    private void startup() {
        // Check if discovery is done
        while (ringStorage.getNode("PREVIOUS").isEmpty() || ringStorage.getNode("NEXT").isEmpty()) {
            try {
                // Wait 0.5 seconds before checking again
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        verifyAndReportFiles();
        checkFiles();
    }

    private void verifyAndReportFiles() {
        File[] files = filePathLocal.toFile().listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String fileName = file.getName();
                    long fileHash = NamingServerHash.hash(fileName);
                    try {
                        String parameters = "fileHash=" + fileHash + "&nodeName=" + ringStorage.currentHash();
                        String url = "http://" + ringStorage.getNamingServerIP() + ":" + serverPort + "/node/replication?" + parameters;
                        String response = new RestTemplate().getForObject(url, String.class);
                        if ("REPLICATE".equalsIgnoreCase(response)) {
                            byte[] data = Files.readAllBytes(file.toPath());
                            fileService.replicateToNeighbors(fileName, "CREATE", data);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    // Check every 5 seconds for changes on localFolder for replication
    private void checkFiles() {
        while (true) {
            try {
                File[] files = filePathLocal.toFile().listFiles();
                if (files != null) {
                    for (File localFile : files) {
                        if (localFile.isFile()) {
                            String fileName = localFile.getName();
                            long fileHash = NamingServerHash.hash(fileName);
                            if (!knownFiles.containsKey(fileName) || knownFiles.get(fileName) != fileHash) {
                                knownFiles.put(fileName, fileHash);
                                fileService.replicateToNeighbors(fileName, "CREATE", Files.readAllBytes(localFile.toPath()));
                            }
                        }
                    }
                }
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
