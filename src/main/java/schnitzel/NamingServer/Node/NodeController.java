package schnitzel.NamingServer.Node;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import schnitzel.NamingServer.NamingServerHash;

import java.util.Optional;

@RestController
public class NodeController {
    private final NodeRepository repository;
    NodeController(NodeRepository repository) {
        this.repository = repository;
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
        System.out.println(nodeIdentifier);
        Long nodeHash = parseIdentifier(nodeIdentifier);
        Optional<NodeEntity> nodeOpt = repository.findById(nodeHash);
        if (nodeOpt.isEmpty()) {
            throw new ResourceNotFoundException("Node with hash " + nodeHash + " does not exist");
        }
        return nodeOpt.get();
    }

    @DeleteMapping("/node/{nodeIdentifier}")
    void delete(@PathVariable String nodeIdentifier) {
        Long nodeHash = parseIdentifier(nodeIdentifier);
        if (!repository.existsById(nodeHash)) {
            throw new RuntimeException("Node with hash " + nodeHash + " already exists.");
        }
        this.repository.deleteById(nodeHash);
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
        if (repository.existsById(nodeHash)) {
            throw new RuntimeException("Node with hash " + nodeHash + " already exists.");
        }

        NodeEntity newNodeEntity = new NodeEntity(
                request.getRemoteAddr(),
                nodeHash,
                nodeEntityIn.nodeName
        );
        this.repository.save(newNodeEntity);
        return this.repository.count();
    }

    /**
     * @return List of NodeEntities which are Node-hash: Ip address mappings
     */
    @GetMapping("/node")
    Iterable<NodeEntity> getNodes() {
        return repository.findAll();
    }
}
