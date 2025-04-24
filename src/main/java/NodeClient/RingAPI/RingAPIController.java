package NodeClient.RingAPI;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/ring/{direction}/{nodeIdentifier}")
    String set_neighbour(@PathVariable String direction, @PathVariable String nodeIdentifier) {
        return ringStorage.setNodeIp(direction, nodeIdentifier);
    }

    @GetMapping("/ring/{direction}")
    String get_neighbour(@PathVariable String direction) {
        Optional<String> nodeOpt = ringStorage.getNodeIP(direction);
        if (nodeOpt.isEmpty()) {
            throw new RingAPIController.ResourceNotFoundException("Direction " + direction + " is not set");
        }
        return nodeOpt.get();
    }
}
