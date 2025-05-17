package NodeClient.Components;

import NodeClient.File.FileLoggerService;
import NodeClient.File.FileMessage;
import NodeClient.File.FileService;
import NodeClient.RingAPI.RingStorage;
import Utilities.NodeEntity.NodeEntity;
import Utilities.RestMessagesRepository;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import schnitzel.NamingServer.NamingServerHash;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Component
public class OnShutdownBean {
    private final RingStorage ringStorage;
    private final FileService fileService;
    private final FileLoggerService fileLoggerService;

    @Value("${server.port}")
    private int serverPort;

    @Autowired
    public OnShutdownBean(RingStorage ringStorage, FileService fileService) {
        this.ringStorage = ringStorage;
        this.fileService = fileService;
        this.fileLoggerService = fileService.getFileLoggerService();
    }

    @PreDestroy
    public void destroy() throws IOException {
        NodeEntity nextNode = this.ringStorage.getNode("NEXT").orElseThrow(() ->
                new IllegalStateException("Existing Node does not have next set")
        );
        NodeEntity previousNode = this.ringStorage.getNode("PREVIOUS").orElseThrow(() ->
                new IllegalStateException("Existing Node does not have previous set")
        );

        handleLocalFiles();
        transferReplicatedFiles();

        RestMessagesRepository.removingSelfFromSystem(this.ringStorage.currentName(), this.ringStorage.getNamingServerIP(), previousNode, nextNode);
    }

    private void handleLocalFiles() throws IOException {
        File[] files = Paths.get("local_files").toFile().listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }
            String fileName = file.getName();
            long fileHash = NamingServerHash.hash(fileName);
            // log a shutdown operation
            fileLoggerService.logOperation(fileName, fileHash, "SHUTDOWN", file.getPath());
            boolean wasDownloaded = fileLoggerService.wasFileDownloaded(fileName);
            // if the file has been downloaded we transfer the file to our neighbor and then delete the file here
            if (wasDownloaded) {
                NodeEntity previousNode = this.ringStorage.getNode("PREVIOUS").orElseThrow(() ->
                        new IllegalStateException("Existing Node does not have previous set")
                );
                FileMessage message = new FileMessage(fileName, "TRANSFER", Files.readAllBytes(file.toPath()));
                RestMessagesRepository.handleTransfer(message, previousNode.getIpAddress(), serverPort);

            }
            // now we can safely delete the file
            FileMessage message = new FileMessage(fileName, "DELETE_LOCAL", Files.readAllBytes(file.toPath()));
            fileService.handleFileOperations(message);
        }
    }

    private void transferReplicatedFiles() throws IOException {
        File[] files = Paths.get("replicated_files").toFile().listFiles();
        NodeEntity currentNode = RestMessagesRepository.getNode(this.ringStorage.currentName(), this.ringStorage.getNamingServerIP());

        if (files == null) {
            return;
        }
        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }
            String fileName = file.getName();
            long fileHash = NamingServerHash.hash(fileName);
            NodeEntity ownerNode = RestMessagesRepository.getFileOwner(fileHash, ringStorage.getNamingServerIP(), serverPort);
            NodeEntity ownerPrev = RestMessagesRepository.getNeighbor(ownerNode, "PREVIOUS");
            NodeEntity ownerNext = RestMessagesRepository.getNeighbor(ownerNode, "NEXT");
            // Check here if we need to replicate the file to next or previous node
            NodeEntity targetNode;
            // if current node is the owner's previous node, we have to replicate the file to current node's previous node
            if (currentNode.getNodeHash().equals(ownerPrev.getNodeHash())) {
                targetNode = RestMessagesRepository.getNeighbor(ownerPrev, "PREVIOUS");
            }
            // if current node is the owner's next node, we have to replicate the file to current node's next node
            else if (currentNode.getNodeHash().equals(ownerNext.getNodeHash())) {
                targetNode = RestMessagesRepository.getNeighbor(ownerNext, "NEXT");
            }
            else {
                continue;
            }
            byte[] data = Files.readAllBytes(file.toPath());
            // first transfer to the right node
            FileMessage transferMessage = new FileMessage(fileName, "TRANSFER", data);
            RestMessagesRepository.handleTransfer(transferMessage, targetNode.getIpAddress(), serverPort);
            // then delete the file
            FileMessage deleteMessage = new FileMessage(fileName, "DELETE_REPLICA", null);
            fileService.handleFileOperations(deleteMessage);
        }
    }
}
