package Utilities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import Utilities.NodeEntity.NodeEntity;
import schnitzel.NamingServer.Node.NodeStorageService;
import schnitzel.NamingServer.NamingServerHash;
import schnitzel.NamingServer.NamingServer;

@Component
public class Failure {

    private final NodeStorageService nodeStorageService;
    private final RestTemplate restTemplate;

    @Autowired
    public Failure(NodeStorageService nodeStorageService, RestTemplate restTemplate) {
        this.nodeStorageService = nodeStorageService;
        this.restTemplate = restTemplate;
    }

    // Periodically monitor node failures by pinging the previous and next node over HTTP REST
    public void monitorNodeFailures() {
        while (true) {
            for (NodeEntity node : nodeStorageService.getAll()) {
                // Check if the node is alive by pinging both its neighbors
                if (!isNodeReachable(node.getIpAddress())) {
                    System.out.println("Node " + node.getNodeName() + " failed! Removing it from the ring...");
                    handleFailure(node.getNodeHash());
                }
            }

            try {
                Thread.sleep(5000);  // Periodic check every 5 seconds
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // Handle the failure of a node and update its neighbors (previous and next) via HTTP REST
    public void handleFailure(Long failedNodeHash) {
        try {
            // Get the previous and next nodes of the failed node
            NodeEntity previousNode = getPreviousNode(failedNodeHash);
            NodeEntity nextNode = getNextNode(failedNodeHash);

            // Update the neighbors to bypass the failed node via HTTP requests
            if (previousNode != null && nextNode != null) {
                updateNodeNeighbors(previousNode, nextNode);
            }

            // Remove the failed node from the Naming Server registry via HTTP
            removeNodeFromNamingServer(failedNodeHash);
            System.out.println("Node with hash " + failedNodeHash + " successfully removed from the ring.");
        } catch (Exception e) {
            System.err.println("Error handling failure of node with hash " + failedNodeHash);
            e.printStackTrace();
        }
    }

    // Check if a node is reachable by making an HTTP request (ping the node's REST API)
    public boolean isNodeReachable(String ipAddress) {
        try {
            // Making a GET request to the node's REST API endpoint (assuming node is up and has a /status endpoint)
            String url = "http://" + ipAddress + ":8080/status";  // Assuming node's status endpoint is /status
            restTemplate.getForObject(url, String.class);
            return true;  // Node is reachable
        } catch (Exception e) {
            return false;  // Node is not reachable
        }
    }

    // Update the neighbors after a node failure
    public void updateNodeNeighbors(NodeEntity previousNode, NodeEntity nextNode) {
        try {
            String previousNodeUrl = "http://" + previousNode.getIpAddress() + ":8080/updateNextNode";
            String nextNodeUrl = "http://" + nextNode.getIpAddress() + ":8080/updatePreviousNode";

            // Notify the previous node to update its next node
            restTemplate.postForObject(previousNodeUrl, nextNode, NodeEntity.class);
            // Notify the next node to update its previous node
            restTemplate.postForObject(nextNodeUrl, previousNode, NodeEntity.class);

            System.out.println("Successfully updated the neighbors' references.");
        } catch (Exception e) {
            System.err.println("Error updating node neighbors.");
            e.printStackTrace();
        }
    }

    // Remove the failed node from the Naming Server
    public void removeNodeFromNamingServer(Long failedNodeHash) {
        String url = "http://localhost:8080/nodes/" + failedNodeHash;  // Assuming the naming server exposes this endpoint
        try {
            restTemplate.delete(url);
            System.out.println("Node with hash " + failedNodeHash + " removed from Naming Server.");
        } catch (Exception e) {
            System.err.println("Error removing node from Naming Server.");
            e.printStackTrace();
        }
    }

    // Fetch the previous node in the ring from the Naming Server
    public NodeEntity getPreviousNode(Long nodeHash) {
        // Assuming there's a REST API to get the previous node in the ring
        String url = "http://localhost:8080/nodes/previous/" + nodeHash;
        return restTemplate.getForObject(url, NodeEntity.class);
    }

    // Fetch the next node in the ring from the Naming Server
    public NodeEntity getNextNode(Long nodeHash) {
        // Assuming there's a REST API to get the next node in the ring
        String url = "http://localhost:8080/nodes/next/" + nodeHash;
        return restTemplate.getForObject(url, NodeEntity.class);
    }

    // This method could be used to manually trigger failure handling (e.g., when a node crashes)
    public void simulateFailure(Long nodeHash) {
        handleFailure(nodeHash);
    }
}
