package Utilities.NodeEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/node/internal")
public class NodeInternalController {

    private NodeEntity nextNode;
    private NodeEntity previousNode;

    // Health check endpoint
    @GetMapping("/status")
    public String getStatus() {
        return "alive";
    }

    // Update next node
    @PostMapping("/updateNext")
    public String updateNext(@RequestBody NodeEntity newNextNode) {
        this.nextNode = newNextNode;
        System.out.println("Updated next node to: " + newNextNode);
        return "Next node updated to " + newNextNode.getNodeName();
    }

    // Update previous node
    @PostMapping("/updatePrevious")
    public String updatePrevious(@RequestBody NodeEntity newPreviousNode) {
        this.previousNode = newPreviousNode;
        System.out.println("Updated previous node to: " + newPreviousNode);
        return "Previous node updated to " + newPreviousNode.getNodeName();
    }

    // Debug route to view current neighbors
    @GetMapping("/neighbors")
    public String getNeighbors() {
        return "Previous: " + (previousNode != null ? previousNode.toString() : "null") +
                ", Next: " + (nextNode != null ? nextNode.toString() : "null");
    }
}
