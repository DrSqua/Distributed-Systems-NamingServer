package NodeClient.Components;

import NodeClient.File.FileService;
import NodeClient.RingAPI.RingStorage;
import Utilities.NodeEntity.NodeEntity;
import Utilities.RestMessagesRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import schnitzel.NamingServer.NamingServerHash;

import java.io.File;
import java.io.IOException;
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
        // Check if there are more than 1 node on our system
        // ,so we have a node to replicate to
        while (ringStorage.getCurrentNodeCount() <= 1) {
            try {
                // Wait 0.5 seconds before checking again
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        try {
            verifyAndReportFiles();
            checkFiles();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    private void verifyAndReportFiles() throws IOException {
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
            String response = RestMessagesRepository.checkReplicationResponsibility(fileHash, ringStorage.currentHash(), ringStorage.getNamingServerIP());
            if ("REPLICATE".equalsIgnoreCase(response)) {
                byte[] data = Files.readAllBytes(file.toPath());
                // only replicate to 1 neighbor if only 1 other node exist
                if (ringStorage.getCurrentNodeCount() == 2) {
                    fileService.replicateToOneNeighbor(fileName, "REPLICATE", data);
                }
                // if more than 1 neighbor we can replicate to previous and next neighbor
                else if (ringStorage.getCurrentNodeCount() >= 3) {
                    fileService.replicateToNeighbors(fileName, "REPLICATE", data);
                }
            }
        }
    }

    // Check every 5 seconds for changes on localFolder for replication
    private void checkFiles() throws InterruptedException, IOException {
        while (true) {
            // check if we have a node to replicate to
            if (ringStorage.getCurrentNodeCount() <= 1) {
                Thread.sleep(500);
                continue;
            }
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
                    byte[] data = Files.readAllBytes(localFile.toPath());
                    // only replicate to 1 neighbor if only 1 other node exist
                    if (ringStorage.getCurrentNodeCount() == 2) {
                        fileService.replicateToOneNeighbor(fileName, "REPLICATE", data);
                    }
                    // if more than 1 neighbor we can replicate to previous and next neighbor
                    else if (ringStorage.getCurrentNodeCount() >= 3) {
                        fileService.replicateToNeighbors(fileName, "REPLICATE", data);
                    }
                    /*
                    NodeEntity nextNode = this.ringStorage.getNode("NEXT").orElseThrow(() ->
                            new IllegalStateException("Existing Node does not have next set")
                    );
                    NodeEntity previousNode = this.ringStorage.getNode("PREVIOUS").orElseThrow(() ->
                            new IllegalStateException("Existing Node does not have previous set")
                    );
                    if (nextNode.getNodeHash() == this.ringStorage.currentHash() ||
                            previousNode.getNodeHash() == this.ringStorage.currentHash()) {
                        // :)
                    } else {
                        // fileService.replicateToNeighbors(fileName,"REPLICATE", Files.readAllBytes(localFile.toPath()));
                    }
                    */
                }
            }
            Thread.sleep(5000);
        }
    }
}
