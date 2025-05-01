package NodeClient.RingAPI;

import Utilities.NodeEntity.NodeEntity;
import Utilities.NodeEntity.NodeEntityIn;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import schnitzel.NamingServer.NamingServerHash;

import java.util.Optional;

@RestController
public class RingAPIController {
    private final RingStorage ringStorage;

    RingAPIController(RingStorage ringStorage) {
        this.ringStorage = ringStorage;
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public static class ResourceNotFoundException extends RuntimeException {
        ResourceNotFoundException(String message) {
            super(message);
        }
    }

    @PostMapping("/ring/{direction}")
    NodeEntity set_neighbour(@PathVariable String direction,
                         @RequestBody NodeEntityIn nodeEntityIn,
                         HttpServletRequest request) {
        NodeEntity newNodeEntity = new NodeEntity(
                request.getRemoteAddr(),
                NamingServerHash.hash(nodeEntityIn.nodeName),
                nodeEntityIn.nodeName
        );
        return ringStorage.setNode(direction, newNodeEntity);
    }

    @GetMapping("/ring/{direction}")
    NodeEntity get_neighbour(@PathVariable String direction) {
        Optional<NodeEntity> nodeOpt = ringStorage.getNode(direction);
        if (nodeOpt.isEmpty()) {
            throw new RingAPIController.ResourceNotFoundException("Direction " + direction + " is not set");
        }
        return nodeOpt.get();
    }
}
