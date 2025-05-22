package NodeClient.Agents;

import NodeClient.File.FileListResponse;
import NodeClient.File.FileMessage;
import NodeClient.File.FileService;
import NodeClient.RingAPI.RingStorage;
import Utilities.NodeEntity.NodeEntity;
import Utilities.RestMessagesRepository;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SyncAgent extends Agent {
    // Can't inject fileService as @Aurowired, we need to inject it manually by using transient to avoid serialization issues
    private transient FileService fileService;
    private transient RingStorage ringStorage;
    private final int tickPeriod = 5000;

    private Map<String, Boolean> agentLockStates;

    @Override
    protected void setup() {
        System.out.println("Starting Sync Agent");
        // fetch spring beans
        ApplicationContext context = (ApplicationContext) getArguments()[0];
        this.fileService = context.getBean(FileService.class);
        this.ringStorage = context.getBean(RingStorage.class);
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

    private void performSync() throws IOException {
        List<String> localFiles = fileService.listLocalFiles();
        Set<String> localFileSet = new HashSet<>(localFiles);

        NodeEntity nextNode = ringStorage.getNode("NEXT").orElse(null);
        NodeEntity previousNode = ringStorage.getNode("PREVIOUS").orElse(null);
        syncWithNeighbor(nextNode, localFileSet);
        syncWithNeighbor(previousNode, localFileSet);
    }

    private void syncWithNeighbor(NodeEntity node, Set<String> localFileSet) throws IOException {
        FileListResponse neighborFiles = RestMessagesRepository.getFileListResponse(node);
        Set<String> replicatedSet = new HashSet<>(neighborFiles.replicatedFiles());

        for (String fileName: localFileSet) {
            if (!replicatedSet.contains(fileName)) {
                byte[] data = fileService.readFile(fileName);
                if (data != null) {
                    FileMessage message = new FileMessage(fileName, "REPLICATE", data);
                    RestMessagesRepository.handleFileOperations(message, node.getIpAddress());
                }
            }
        }

        for (String fileName: replicatedSet) {
            if (!localFileSet.contains(fileName)) {
                FileMessage msg = new FileMessage(fileName, "DELETE_REPLICA", null);
                RestMessagesRepository.handleFileOperations(msg, node.getIpAddress());
            }
        }
    }
}
