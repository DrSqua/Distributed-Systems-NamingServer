package NodeClient.Components;

import NodeClient.RingAPI.RingStorage;
import Utilities.NodeEntity.NodeEntity;
import Utilities.RestMessagesRepository;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

@Component
public class OnShutdownBean {
    private final RingStorage ringStorage;

    public OnShutdownBean(RingStorage ringStorage) {
        this.ringStorage = ringStorage;
    }

    @PreDestroy
    public void destroy() {
        NodeEntity nextNode = this.ringStorage.getNode("NEXT").orElseThrow(() ->
                new IllegalStateException("Existing Node does not have next set")
        );
        NodeEntity previousNode = this.ringStorage.getNode("PREVIOUS").orElseThrow(() ->
                new IllegalStateException("Existing Node does not have previous set")
        );

        RestMessagesRepository.updateNeighbour(nextNode, "PREVIOUS", previousNode.asEntityIn());
        RestMessagesRepository.updateNeighbour(previousNode, "NEXT", nextNode.asEntityIn());
    }
}
