package NodeClient.Agents;

import NodeClient.File.FileService;
import NodeClient.RingAPI.RingStorage;
import Utilities.NodeEntity.NodeEntity;
import Utilities.RestMessagesRepository;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import schnitzel.NamingServer.NamingServerHash;

import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class FailureAgent extends Agent {
    private transient FileService fileService;
    private transient RingStorage ringStorage;
    // The failing node
    private String failingNodeName;
    // The node that started the Failure Agent, so the node that detected failure
    private String startedNodeName;

    // JADE requires empty constructor
    public FailureAgent() {}

    public FailureAgent(String failingNodeId, String startedNodeId) {
        this.failingNodeName = failingNodeId;
        this.startedNodeName = startedNodeId;
    }

    public String getFailingNodeName() {
        return failingNodeName;
    }

    public String getStartedNodeName() {
        return startedNodeName;
    }

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length <= 4) {
            this.fileService = (FileService) args[0];
            this.ringStorage = (RingStorage) args[1];
            this.failingNodeName = (String) args[2];
            this.startedNodeName = (String) args[3];
        } else {
            System.err.println("SyncAgent requires 4 arguments");
            doDelete();
        }
        System.out.println("Starting Failure Agent. Failing node id: " + failingNodeName);
        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                try {
                    performFailureRecovery();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        // Failure agent is done and can end itself
                        if (startedNodeName.equals(ringStorage.getSelf().getNodeName())) {
                            System.out.println("Successfully performed failure recovery");
                            doDelete();
                        } else {
                            // Send agent to next node
                            passAgentToNextNode();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void performFailureRecovery() throws IOException {
        List<String> replicatedFiles = fileService.listReplicatedFiles();
        // Look at replicatedFiles of current node, see if they are originally of failing node
        for (String fileName : replicatedFiles) {
            long fileHash = NamingServerHash.hash(fileName);
            String namingServerIp = ringStorage.getNamingServerIP();
            String originalOwnerName = RestMessagesRepository.getFileOriginalOwner(fileHash, namingServerIp).getNodeName();
            if (originalOwnerName == null || !originalOwnerName.equals(failingNodeName)) {
                return;
            }
            // find new owner of the file based on hash
            NodeEntity newFileOwner = RestMessagesRepository.getFileOwner(fileHash, namingServerIp);
            String newFileOwnerName = newFileOwner.getNodeName();
            String currentNode = ringStorage.currentName();
            // if new owner is the current node, we will transfer the replicated file to our local_files and become the new owner
            if (newFileOwnerName == null || !newFileOwnerName.equals(currentNode)) {
                return;
            }
            Path replicatedFilePath = Paths.get("replicated_files").toAbsolutePath().resolve(fileName);
            byte[] fileData = Files.readAllBytes(replicatedFilePath);
            fileService.promoteReplicaToLocal(fileName, fileData);
            // register as new originalFileOwner
            long originalOwnerHash = NamingServerHash.hashNode(newFileOwnerName, newFileOwner.getIpAddress());
            RestMessagesRepository.registerFileOriginalOwner(fileHash, originalOwnerHash, ringStorage.getNamingServerIP());
        }
    }

    private void passAgentToNextNode() throws IOException {
        NodeEntity nextNode = ringStorage.getNode("NEXT").orElse(null);
        if (nextNode == null) {
            doDelete();
            return;
        }
        String currentNodeName = ringStorage.currentName();
        // Don't pass to self or failing node
        if (!nextNode.getNodeName().equals(currentNodeName) && !nextNode.getNodeName().equals(failingNodeName)) {
            RestMessagesRepository.sendAgentToNode(this, nextNode.getIpAddress());
        } else {
            doDelete();
        }
    }
}
