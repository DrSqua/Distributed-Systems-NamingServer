package NodeClient.RingAPI;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import schnitzel.NamingServer.Node.NodeController;

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

    @PostMapping("/{direction}/{nodeIdentifier}")
    String set_neighbour(@PathVariable String direction, @PathVariable String nodeIdentifier) {
        return ringStorage.setNodeIp(direction, nodeIdentifier);
    }

    @PostMapping("/{direction}")
    String get_neighbour(@PathVariable String direction) {
        Optional<String> nodeOpt = ringStorage.getNodeIP(direction);
        if (nodeOpt.isEmpty()) {
            throw new RingAPIController.ResourceNotFoundException("Direction " + direction + " is not set");
        }
        return nodeOpt.get();
    }
}
