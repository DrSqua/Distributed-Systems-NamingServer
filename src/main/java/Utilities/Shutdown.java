package Utilities;

import NodeClient.File.FileMessage;
import NodeClient.File.FileService;
import NodeClient.RingAPI.RingStorage;
import Utilities.NodeEntity.NodeEntity;
import schnitzel.NamingServer.NamingServerHash;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Shutdown {

    public static void transferReplicatedFiles(FileService fileService, RingStorage ringStorage) throws IOException {
            File[] files = Paths.get("replicated_files").toFile().listFiles();
            NodeEntity currentNode = RestMessagesRepository.getNode(ringStorage.currentName(), ringStorage.getNamingServerIP());

            if (files == null) {
                return;
            }
            for (File file : files) {
                if (!file.isFile()) {
                    continue;
                }
                String fileName = file.getName();
                long fileHash = NamingServerHash.hash(fileName);
                NodeEntity ownerNode = RestMessagesRepository.getFileOwner(fileHash, ringStorage.getNamingServerIP());
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
                // first transfer to the target node
                FileMessage transferMessage = new FileMessage(fileName, "TRANSFER_REPLICA", data);
                RestMessagesRepository.handleTransfer(transferMessage, targetNode.getIpAddress());
                // then delete the file
                FileMessage deleteMessage = new FileMessage(fileName, "DELETE_REPLICA", null);
                fileService.handleFileOperations(deleteMessage);
            }
        }
}
