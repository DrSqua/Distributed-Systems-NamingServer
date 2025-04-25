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
    // Used for health checks (ping endpoint)
    @GetMapping("/node/internal/status")
    public String getStatus() {
        return "OK";
    }

    // Endpoint to update the next node from outside
    @PostMapping("/node/internal/updateNext")
    public void updateNext(@RequestBody NodeEntity newNext) {
        ringStorage.setNode("next", newNext);
        System.out.println("Updated next node to: " + newNext);
    }

    // Endpoint to update the previous node from outside
    @PostMapping("/node/internal/updatePrevious")
    public void updatePrevious(@RequestBody NodeEntity newPrevious) {
        ringStorage.setNode("previous", newPrevious);
        System.out.println("Updated previous node to: " + newPrevious);
    }

}
