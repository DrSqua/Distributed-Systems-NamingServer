package NodeClient.RingAPI;

import Utilities.NodeEntity.NodeEntity;
import Utilities.NodeEntity.NodeEntityIn;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import schnitzel.NamingServer.NamingServerHash;

import java.net.UnknownHostException;
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
                         HttpServletRequest request) throws UnknownHostException {
        NodeEntity newNodeEntity = new NodeEntity(
                request.getRemoteAddr(),
                nodeEntityIn.nodeName
        );
        System.out.println(ringStorage.getOwnIp() + " is setting " + direction + " to " + newNodeEntity);
        return ringStorage.setNode(direction, newNodeEntity);
    }

    @GetMapping("/ring/{direction}")
    Optional<NodeEntity> get_neighbour(@PathVariable String direction) {
        Optional<NodeEntity> nodeOpt = ringStorage.getNode(direction);
        if (nodeOpt.isEmpty()) {
            // throw new RingAPIController.ResourceNotFoundException("Direction " + direction + " is not set");
        }
        return nodeOpt;
    }
}
