package schnitzel.NamingServer.Node;
import Utilities.NodeEntity.NodeEntity;
import Utilities.NodeEntity.NodeEntityIn;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import schnitzel.NamingServer.NamingServerHash;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class NodeController {
    private final NodeStorageService nodeStorageService;
    private final ConcurrentHashMap<Long, NodeEntity> fileRegistry = new ConcurrentHashMap<>();
    NodeController(NodeStorageService nodeStorageService) {
        this.nodeStorageService = nodeStorageService;
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public static class ResourceNotFoundException extends RuntimeException {
        ResourceNotFoundException(String message) {
            super(message);
        }
    }

    /**
     *
     * @param nodeIdentifier: Which is either a number (but as String type) or the nodeName to be hashed
     * @return Hash either way
     */
    long parseIdentifier(String nodeIdentifier) {
        try {
            return Integer.parseInt(nodeIdentifier);
        }  catch (NumberFormatException e) {
            return NamingServerHash.hash(nodeIdentifier);
        }
    }

    // Get Unique
    @GetMapping("/node/{nodeIdentifier}")
    NodeEntity get(@PathVariable String nodeIdentifier) {
        Long nodeHash = parseIdentifier(nodeIdentifier);
        Optional<NodeEntity> nodeOpt = nodeStorageService.findById(nodeHash);
        if (nodeOpt.isEmpty()) {
            throw new ResourceNotFoundException("Node with hash " + nodeHash + " does not exist");
        }
        return nodeOpt.get();
    }

    @DeleteMapping("/node/{nodeIdentifier}")
    void delete(@PathVariable String nodeIdentifier) {
        Long nodeHash = parseIdentifier(nodeIdentifier);
        if (!nodeStorageService.existsById(nodeHash)) {
            throw new RuntimeException("Node with hash " + nodeHash + " already exists.");
        }
        this.nodeStorageService.deleteById(nodeHash);
    }

    /**
     * @param nodeEntityIn: The Node which wants to be added to the naming server
     * @return Created Node-hash: Ip address mapping
     */
    @PostMapping("/node")
    long post(@RequestBody NodeEntityIn nodeEntityIn,
                    HttpServletRequest request) {
        long nodeHash = NamingServerHash.hash(nodeEntityIn.nodeName);

        // Ensure uniqueness of nodeHash before saving
        if (nodeStorageService.existsById(nodeHash)) {
            throw new RuntimeException("Node with hash " + nodeHash + " already exists.");
        }

        NodeEntity newNodeEntity = new NodeEntity(
                request.getRemoteAddr(),
                nodeHash,
                nodeEntityIn.nodeName
        );
        this.nodeStorageService.put(nodeHash, newNodeEntity);
        return this.nodeStorageService.count();
    }

    /**
     * @return List of NodeEntities which are Node-hash: Ip address mappings
     */
    @GetMapping("/node")
    Iterable<NodeEntity> getNodes() {
        return nodeStorageService.getAll();
    }


    /**
     *
     * @param fileHash: Hash value of the file
     * @return the owner of the given file
     */
    @GetMapping("/node/owner")
    public NodeEntity getFileOwner(@RequestParam long fileHash) {
        return fileRegistry.get(fileHash);
    }

    @PostMapping("/node/owner")
    public void updateFileOwner(@RequestParam NodeEntity fileOwnerNode, @RequestParam long fileHash) {
        fileRegistry.putIfAbsent(fileHash, fileOwnerNode);
    }

    /**
     * @param fileHash: Hash value of the file
     * @param nodeHash: Hash value of the originating node
     * @return Informs originating node that he needs to replicate or ignore the file
     */
    @GetMapping("/node/replication")
    public String checkReplicationResponsibility(@RequestParam long fileHash, @RequestParam long nodeHash) {
        NodeEntity responsibleNode = findResponsibleNode(fileHash);

        if (responsibleNode.getNodeHash().equals(nodeHash)) {
            fileRegistry.putIfAbsent(fileHash, responsibleNode);
            return "REPLICATE";
        }
        return "IGNORE";
    }

    private NodeEntity findResponsibleNode(long fileHash) {
        ArrayList<Long> sortedHashes = nodeStorageService.keys();
        Collections.sort(sortedHashes);
        for (int i=0; i<sortedHashes.size(); i++) {
            long current = sortedHashes.get(i);
            // +sortedHashes.size() % sortedHashes.size() for negative indices, hashmaps won't wrap around in java
            long previous = sortedHashes.get((i - 1 + sortedHashes.size()) % sortedHashes.size());
            if (fileHash > previous && fileHash <= current) {
                return nodeStorageService.findById(current).orElse(null);
            }
        }
        // Wrap around, fileHash is less than all node hashes -> smallest nodeHash owns it
        return nodeStorageService.findById(sortedHashes.get(0)).orElse(null);
    }
}
