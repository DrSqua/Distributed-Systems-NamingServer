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

    // Get Unique
    @GetMapping("/node/{nodeHash}")
    NodeEntity get(@PathVariable Long nodeHash) {
        Optional<NodeEntity> nodeOpt = nodeStorageService.findById(nodeHash);
        if (nodeOpt.isEmpty()) {
            throw new ResourceNotFoundException("Node with hash " + nodeHash + " does not exist");
        }
        return nodeOpt.get();
    }

    @DeleteMapping("/node/{nodeHash}")
    void delete(@PathVariable Long nodeHash) {
        System.out.println("Deleting node with hash " + nodeHash);
        if (!nodeStorageService.existsById(nodeHash)) {
            throw new RuntimeException("Node with hash " + nodeHash + " doesn't exist. Cannot delete it.");
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
        long nodeHash = NamingServerHash.hashNode(nodeEntityIn.nodeName, request.getRemoteAddr());

        // Ensure uniqueness of nodeHash before saving
        if (nodeStorageService.existsById(nodeHash)) {
            throw new RuntimeException("Node with hash " + nodeHash + " already exists.");
        }

        NodeEntity newNodeEntity = new NodeEntity(
                request.getRemoteAddr(),
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
}
