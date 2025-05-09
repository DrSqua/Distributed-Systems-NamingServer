package schnitzel.NamingServer.Node;
import Utilities.NodeEntity.NodeEntity;
import Utilities.NodeEntity.NodeEntityIn;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import schnitzel.NamingServer.NamingServerHash;

import java.util.List;
import java.util.Optional;

@RestController
public class NodeController {
    private final NodeStorageService nodeStorageService;
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
     * @param fileHash: All the hash values of the files that needs to be replicated
     * @return Informs originating node
     */
    /*@GetMapping("/node/replication")
    public String startupReplication(@RequestParam long fileHash, @RequestParam long nodeHash) {
        // TODO check if fileHash > nodeHash
        return "REPLICATE";
    }*/
}
