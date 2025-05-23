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
                         @RequestBody NodeEntity nodeEntityIn,
                         HttpServletRequest request) throws UnknownHostException {
        System.out.println(ringStorage.getOwnIp() + " is setting " + direction + " to " + nodeEntityIn);
        return ringStorage.setNode(direction, nodeEntityIn);
    }

    @GetMapping("/ring/{direction}")
    Optional<NodeEntity> get_neighbour(@PathVariable String direction) {
        Optional<NodeEntity> nodeOpt = ringStorage.getNode(direction);
        if (nodeOpt.isEmpty()) {
            System.out.println("No node for " + direction);
            // throw new RingAPIController.ResourceNotFoundException("Direction " + direction + " is not set");
        }
        return nodeOpt;
    }
}
