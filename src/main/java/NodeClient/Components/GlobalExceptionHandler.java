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
        System.out.println("Global exception caught: " + e.getMessage());
        // Check if the node has any neighbours
        Optional<NodeEntity> nextNode = this.ringStorage.getNode("NEXT");
        Optional<NodeEntity> previousNode = this.ringStorage.getNode("PREVIOUS");

        try {
            if (nextNode.isPresent() && previousNode.isPresent()) {
                // Tell neighbours they are now each other's neighbour
                RestMessagesRepository.updateNeighbour(nextNode.get(), "PREVIOUS", previousNode.get().asEntityIn());
                RestMessagesRepository.updateNeighbour(previousNode.get(), "NEXT", nextNode.get().asEntityIn());
            }
        } catch (Exception ex) {
            System.err.println("Notifying neighbours when in GlobalException failed: " + ex.getMessage());
        }

        // Notify server we are leaving system
        RestMessagesRepository.removeFromNamingServerNoExcept(this.ringStorage.getSelf(), this.ringStorage.getNamingServerIP());
        e.printStackTrace();
        System.err.println("GlobalExceptionHandler: Initiating IMMEDIATE FORCEFUL SHUTDOWN of the NodeClient (System.exit(1)).");
        new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            System.exit(1);
        }, "GlobalExceptionHandler-ForceShutdownThread").start();

        // Continue throwing error
        throw e;
    }
}