package schnitzel.NamingServer.Node;

import com.fasterxml.jackson.databind.ser.std.InetAddressSerializer;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.net.InetAddress;
import java.util.Optional;

@RestController
public class NodeController {
    private final static double max = 2147483647;

    private final NodeRepository repository;
    NodeController(NodeRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/node")
    Iterable<NodeEntity> getNodes() {
        return repository.findAll();
    }

    // Get Unique
    @GetMapping("/node/{nodeIdentifier}")
    NodeEntity get(@PathVariable String nodeIdentifier) {
        try {
            Long nodeHash = (long) Integer.parseInt(nodeIdentifier);
            Optional<NodeEntity> nodeEntity = this.repository.findByNodeHash(nodeHash);
            if (nodeEntity.isPresent()) {
                return nodeEntity.get();
            } // Else throw Not Found?
        } catch (NumberFormatException e) {
            return null;
        }
        return null;
    }

    @PostMapping("/node")
    NodeEntity post(@RequestBody NodeEntityIn nodeEntityIn,
                    HttpServletRequest request) {
        NodeEntity newNodeEntity = new NodeEntity(
                request.getRemoteAddr(), hash(nodeEntityIn.nodeName)
        );
        NodeEntity savedNodeEntity = this.repository.save(newNodeEntity);
        System.out.println(this.repository.findAll());
        return savedNodeEntity;
    }

    private Long hash(String nodeIdentifier) {
        double hashCode = nodeIdentifier.hashCode();
        return (long) ((hashCode + max) * (32768/max + max));
    }
}
