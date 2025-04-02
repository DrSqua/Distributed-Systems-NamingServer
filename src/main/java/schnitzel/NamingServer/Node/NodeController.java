package schnitzel.NamingServer.Node;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
public class NodeController {
    private final static double max = 2147483647;

    private final NodeRepository repository;
    NodeController(NodeRepository repository) {
        this.repository = repository;
    }

    /**
     *
     * @return List of NodeEntities which are Node-hash: Ip address mappings
     */
    @GetMapping("/node")
    Iterable<NodeEntity> getNodes() {
        return repository.findAll();
    }

    // Get Unique
    @GetMapping("/node/{nodeIdentifier}")
    NodeEntity get(@PathVariable String nodeIdentifier) {
        try {
            Long nodeHash = (long) Integer.parseInt(nodeIdentifier);
            Optional<NodeEntity> nodeEntity = this.repository.findById(nodeHash);
            if (nodeEntity.isPresent()) {
                return nodeEntity.get();
            } // Else throw Not Found?
        } catch (NumberFormatException e) {
            return null;
        }
        return null;
    }

    /**
     * @param nodeEntityIn: The Node which wants to be added to the naming server
     * @return Created Node-hash: Ip address mapping
     */
    @PostMapping("/node")
    NodeEntity post(@RequestBody NodeEntityIn nodeEntityIn,
                    HttpServletRequest request) {
        long nodeHash = NodeNameHash.hash(nodeEntityIn.nodeName);

        // Ensure uniqueness of nodeHash before saving
        if (repository.existsById(nodeHash)) {
            throw new RuntimeException("Node with hash " + nodeHash + " already exists.");
        }

        NodeEntity newNodeEntity = new NodeEntity(
                request.getRemoteAddr(),
                nodeHash,
                nodeEntityIn.nodeName
        );

        return this.repository.save(newNodeEntity);
    }
}
