package NodeClient.MulticastListening;

import NodeClient.RingAPI.RingStorage;
import Utilities.NodeEntity.NodeEntity;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class NodeMulticastListener {
    private final RingStorage ringStorage;

    public NodeMulticastListener(RingStorage ringStorage) {
        this.ringStorage = ringStorage;
    }

    @PostConstruct
    public void start() {
        new Thread(this::listen).start();
    }

    private void listen() {
        NodeEntity receivedNode;
        Long hashedNodeName = 1L; // TODO


        NodeEntity nextNode = this.ringStorage.getNode("NEXT").orElseThrow(() ->
                new IllegalStateException("Existing Node does not have next set")
        );
        NodeEntity previousNode = this.ringStorage.getNode("PREVIOUS").orElseThrow(() ->
                new IllegalStateException("Existing Node does not have previous set")
        );
        /*
         * If currentID< hash < nextID, nexID= hash, current node updates its own parameter nextID,
         * and sends response to node giving the information on currentIDand nextID
         *
         * If previousID< hash < currentID, previousID= hash, current node updates its own parameter previousID,
         * and sends response to node giving the information on currentIDand previousID
         */
        if (this.ringStorage.currentHash() < hashedNodeName && hashedNodeName < nextNode.hashCode()) {
            this.ringStorage.setNode("NEXT", receivedNode);

            // TODO
            // Send which neighbours the other node has

        } else if (previousNode.hashCode() < hashedNodeName && hashedNodeName < this.ringStorage.currentHash()) {
            this.ringStorage.setNode("PREVIOUS", receivedNode);

            // TODO
            // Send which neighbours the other node has

        } else {
            throw new IllegalStateException("Unexpected state");
        }

    }
}
