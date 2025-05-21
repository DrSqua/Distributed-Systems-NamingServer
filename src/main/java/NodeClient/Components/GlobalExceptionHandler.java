package NodeClient.Components;

import NodeClient.RingAPI.RingStorage;
import Utilities.NodeEntity.NodeEntity;
import Utilities.RestMessagesRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Optional;

// Global exception handler
@ControllerAdvice
class GlobalExceptionHandler {
    private final RingStorage ringStorage;

    public GlobalExceptionHandler(RingStorage ringStorage) {
        this.ringStorage = ringStorage;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) throws Exception {
        // Check if the node has any neighbours
        Optional<NodeEntity> nextNode = this.ringStorage.getNode("NEXT");
        Optional<NodeEntity> previousNode = this.ringStorage.getNode("PREVIOUS");

        if (nextNode.isPresent() && previousNode.isPresent()) {
            // Tell neighbours they are now each other's neighbour
            RestMessagesRepository.updateNeighbour(nextNode.get(), "PREVIOUS", previousNode.get().asEntityIn());
            RestMessagesRepository.updateNeighbour(previousNode.get(), "NEXT", nextNode.get().asEntityIn());
        }

        // Notify server we are leaving system
        RestMessagesRepository.removeFromNamingServer(this.ringStorage.currentName(), this.ringStorage.getNamingServerIP());

        // Continue throwing error
        throw e;
    }
}