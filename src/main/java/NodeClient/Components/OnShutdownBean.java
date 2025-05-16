package NodeClient.Components;

import NodeClient.File.FileMessage;
import NodeClient.RingAPI.RingStorage;
import Utilities.NodeEntity.NodeEntity;
import Utilities.RestMessagesRepository;
import jakarta.annotation.PreDestroy;
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

    @Value("${server.port}")
    private int serverPort;

    public OnShutdownBean(RingStorage ringStorage) {
        this.ringStorage = ringStorage;
    }

    @PreDestroy
    public void destroy() throws IOException {
        NodeEntity nextNode = this.ringStorage.getNode("NEXT").orElseThrow(() ->
                new IllegalStateException("Existing Node does not have next set")
        );
        NodeEntity previousNode = this.ringStorage.getNode("PREVIOUS").orElseThrow(() ->
                new IllegalStateException("Existing Node does not have previous set")
        );

        NodeEntity currentNode = RestMessagesRepository.getNode(this.ringStorage.currentName(), this.ringStorage.getNamingServerIP());

        transferReplicatedFiles(currentNode);

        RestMessagesRepository.removingSelfFromSystem(this.ringStorage.currentName(), this.ringStorage.getNamingServerIP(), previousNode, nextNode);
    }

    private void transferReplicatedFiles(NodeEntity currentNode) throws IOException {
        File[] files = Paths.get("replicated_files").toFile().listFiles();

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
            FileMessage message = new FileMessage(fileName, "TRANSFER", data);
            RestMessagesRepository.transferFile(message, targetNode.getIpAddress(), serverPort);
        }
    }

    /*
    private void notifyFileOwners() throws IOException {
        File[] files = Paths.get("replicated_files").toFile().listFiles();

        if (files == null) {
            return;
        }
        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }
            String fileName = file.getName();
            long fileHash = NamingServerHash.hash(fileName);
            String namingServerIp = ringStorage.getNamingServerIP();
            String url = "http://" + namingServerIp + ":" + serverPort + "/node/owner?fileHash=" + fileHash;
            NodeEntity owner = new RestTemplate().getForObject(url, NodeEntity.class);
            byte[] fileData = Files.readAllBytes(file.toPath());
            FileMessage shutdownMessage = new FileMessage(fileName, "DELETE", fileData);
            url = "http://" + owner.getIpAddress() + ":" + serverPort + "/node/file/replication";
            new RestTemplate().postForObject(url, shutdownMessage, Void.class);
        }
    }
    */
}
