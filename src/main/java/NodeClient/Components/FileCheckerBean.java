package NodeClient.Components;

import NodeClient.File.FileService;
import NodeClient.RingAPI.RingStorage;
import Utilities.RestMessagesRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
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
        // Guard clause
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }
            String fileName = file.getName();
            long fileHash = NamingServerHash.hash(fileName);
            try {
                String response = RestMessagesRepository.checkReplicationResponsibility(fileHash, ringStorage.currentHash(), ringStorage.getNamingServerIP());
                if ("REPLICATE".equalsIgnoreCase(response)) {
                    byte[] data = Files.readAllBytes(file.toPath());
                    fileService.replicateToNeighbors(fileName, "REPLICATE", data);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    // Check every 5 seconds for changes on localFolder for replication
    // TODO why not just replicate it in REST API at uploadFile()
    private void checkFiles() {
        while (true) {
            try {
                File[] files = filePathLocal.toFile().listFiles();
                // Guard clause
                if (files == null) {
                    return;
                }
                for (File localFile : files) {
                    if (!localFile.isFile()) {
                        continue;
                    }
                    String fileName = localFile.getName();
                    long fileHash = NamingServerHash.hash(fileName);
                    if (!knownFiles.containsKey(fileName) || knownFiles.get(fileName) != fileHash) {
                        knownFiles.put(fileName, fileHash);
                        fileService.replicateToNeighbors(fileName,"REPLICATE", Files.readAllBytes(localFile.toPath()));
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
