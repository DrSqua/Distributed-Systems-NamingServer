package NodeClient.Components;

import NodeClient.File.FileService;
import NodeClient.File.ReadyForReplication;
import NodeClient.RingAPI.RingStorage;
import Utilities.NodeEntity.NodeEntity;
import Utilities.RestMessagesRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import schnitzel.NamingServer.NamingServerHash;

import javax.sound.midi.SysexMessage;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.UnknownHostException;
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
        new Thread(() -> {
            try {
                startup();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    private void startup() throws InterruptedException {
        // Tomcat is often still not initialised when asking to set the neighbours so give it some extra time
        // this will make sure we can send the message when everything is set
        Thread.sleep(750);

        // Check if there are more than 1 node on our system
        // ,so we have a node to replicate to
        while (ringStorage.getCurrentNodeCount() <= 1) {
            try {
                System.out.println("Waiting for more nodes, now only 1 => wait");
                // Wait 0.5 seconds before checking again
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        while (true){
            try {
                while (!ReadyForReplication.getIsReadyForReplication()){
                    try {
                        System.out.println("waiting for the ready (after NEXT/PREV is set) ");
                        NodeEntity nextNode = this.ringStorage.getNode("NEXT").orElse(null);
                        // Getting previous node
                        NodeEntity previousNode = this.ringStorage.getNode("PREVIOUS").orElse(null);
                        NodeEntity currentNode;
                        if (nextNode == null || previousNode == null) {
                            System.out.println("No next or previous node found");
                        }else{
                            String currenIpAddress = InetAddress.getLocalHost().toString();
                            String nextIpAddress = nextNode.getIpAddress();
                            String previousIpAddress = previousNode.getIpAddress();
                            try {
                                currentNode = this.ringStorage.getSelf();
                            } catch (UnknownHostException e) {
                                e.printStackTrace();
                                return;
                            }
                            if ((currenIpAddress != nextIpAddress) && (currenIpAddress != previousIpAddress)) {
                                ReadyForReplication.setIsReadyForReplication(true);

                            }
                        }
                        // Wait 0.5 seconds before checking again
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                System.out.println("Starting file checker + verify&report files");
                verifyAndReportFiles();
                System.out.println("check files:");
                checkFiles();
                ReadyForReplication.setIsReadyForReplication(false);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
    }

    public void setReadyForReplication(boolean readyForReplication) {

    }

    private void verifyAndReportFiles() throws IOException, InterruptedException {
        File[] files = filePathLocal.toFile().listFiles();
        // Guard clause
        if (files == null) {
            System.out.println("No files found in " + filePathLocal.toAbsolutePath());
            return;
        }
        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }
            String fileName = file.getName();
            long fileHash = NamingServerHash.hash(fileName);
            System.out.println("Checking file: " + fileName+", with hash: " + fileHash);
            String response = RestMessagesRepository.checkReplicationResponsibility(fileHash, ringStorage.currentHash(), ringStorage.getNamingServerIP());
            if ("REPLICATE".equalsIgnoreCase(response)) {
                byte[] data = Files.readAllBytes(file.toPath());
                System.out.println("Replicating to neighbours: " + fileName+", "+data);
                fileService.replicateToNeighbors(fileName, "REPLICATE", data);
            }
        }
    }

    // Check every 5 seconds for changes on localFolder for replication
    private void checkFiles() throws InterruptedException, IOException {
        while (true) {
            System.out.println("We zitten heel de tijd de files te checken na de eerste replication");
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
                    fileService.replicateToNeighbors(fileName, "REPLICATE", data);
                }
            }
            Thread.sleep(5000);
        }
    }
}
