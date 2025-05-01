package NodeClient.Components;

import NodeClient.RingAPI.RingStorage;
import Utilities.NodeEntity.NodeEntity;
import Utilities.RestMessagesRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

// Global exception handler
@ControllerAdvice
class GlobalExceptionHandler {
    private final RingStorage ringStorage;

    public GlobalExceptionHandler(RingStorage ringStorage) {
        this.ringStorage = ringStorage;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) throws Exception {

        // Notify neighbours that they are now eachother neighbours
        NodeEntity nextNode = this.ringStorage.getNode("NEXT").orElseThrow(() ->
                new IllegalStateException("Existing Node does not have next set")
        );
        NodeEntity previousNode = this.ringStorage.getNode("PREVIOUS").orElseThrow(() ->
                new IllegalStateException("Existing Node does not have previous set")
        );

        // Perform shutdown code
        RestMessagesRepository.removingSelfFromSystem(this.ringStorage.currentName(), this.ringStorage.getNamingServerIP(), previousNode, nextNode);

        // Continue throwing error
        throw e;
    }
}