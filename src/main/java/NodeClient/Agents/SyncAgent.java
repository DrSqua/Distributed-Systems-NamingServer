package NodeClient.Agents;

import NodeClient.File.FileListResponse;
import NodeClient.File.FileMessage;
import NodeClient.File.FileService;
import NodeClient.RingAPI.RingStorage;
import Utilities.NodeEntity.NodeEntity;
import Utilities.RestMessagesRepository;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import schnitzel.NamingServer.NamingServerHash;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class SyncAgent extends Agent {
    // Can't inject fileService as @Aurowired, we need to inject it manually by using transient to avoid serialization issues
    private transient FileService fileService;
    private transient RingStorage ringStorage;
    private final int tickPeriod = 5000;

    private final Map<String, Boolean> agentLockStates = new HashMap<>();

    // JADE requires no argument constructor
    public SyncAgent() {}

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length >= 2) {
            this.fileService = (FileService) args[0];
            this.ringStorage = (RingStorage) args[1];
        } else {
            System.err.println("SyncAgent requires 2 arguments");
            doDelete();
            return;
        }
        System.out.println("Starting Sync Agent");
        addBehaviour(new TickerBehaviour(this, tickPeriod) {
            @Override
            protected void onTick() {
                try {
                    performSync();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void performSync() throws IOException, InterruptedException {
        List<String> localFiles = fileService.listLocalFiles();
        Set<String> localFileSet = new HashSet<>(localFiles);

        NodeEntity nextNode = ringStorage.getNode("NEXT").orElse(null);
        NodeEntity previousNode = ringStorage.getNode("PREVIOUS").orElse(null);
        syncWithNeighbor(nextNode, localFileSet);
        syncWithNeighbor(previousNode, localFileSet);
        syncLockStates();
    }

    private void syncWithNeighbor(NodeEntity node, Set<String> localFileSet) throws IOException, InterruptedException {
        FileListResponse neighborFiles = RestMessagesRepository.getFileListResponse(node);
        Set<String> replicatedSet = new HashSet<>(neighborFiles.replicatedFiles());

        // if there is a file that isn't yet on the neighbor's replicated_files we replicate that file to the neighbor
        for (String fileName: localFileSet) {
            if (!replicatedSet.contains(fileName)) {
                Path localPath = Paths.get("local_files").toAbsolutePath();
                Path filePath = localPath.resolve(fileName);
                byte[] data = Files.readAllBytes(filePath);
                    FileMessage message = new FileMessage(fileName, "REPLICATE", data);
                    RestMessagesRepository.handleFileOperations(message, node.getIpAddress());
            }
        }
        // if there is a file on the neighbor's replicated_files but not on our owned files we delete them on the neighbor's node
        for (String fileName: replicatedSet) {
            // first check if we owned this file
            long fileHash = NamingServerHash.hash(fileName);
            String ownerName = RestMessagesRepository.getFileOwner(fileHash, ringStorage.getNamingServerIP()).getNodeName();
            String currentNodeName = ringStorage.currentName();
            if (ownerName != null && ownerName.equals(currentNodeName)) {
                // now check if the replicated file is still in our local_files, otherwise delete replica
                if (!localFileSet.contains(fileName)) {
                    FileMessage msg = new FileMessage(fileName, "DELETE_REPLICA", null);
                    RestMessagesRepository.handleFileOperations(msg, node.getIpAddress());
                }
            }
        }
    }

    private void syncLockStates() {
        Set<String> currentLockedFiles = fileService.getLockedFiles();
        // Sync locked files
        for (String fileName: currentLockedFiles) {
            if (!agentLockStates.getOrDefault(fileName, false)) {
                agentLockStates.put(fileName, true);
            }
        }
        Set<String> previouslyLocked = new HashSet<>(agentLockStates.keySet());
        for (String fileName: previouslyLocked) {
            if (!currentLockedFiles.contains(fileName) && agentLockStates.get(fileName)) {
                agentLockStates.put(fileName, false);
            }
        }
    }
}
