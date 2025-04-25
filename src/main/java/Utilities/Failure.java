package Utilities;

import Utilities.NodeEntity.NodeEntity;
import schnitzel.NamingServer.Node.NodeStorageService;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

public class Failure {
    private final NodeStorageService nodeStorageService;
    private final RestTemplate restTemplate = new RestTemplate();

    public Failure(NodeStorageService nodeStorageService) {
        this.nodeStorageService = nodeStorageService;
    }

    public void handleFailure(NodeEntity failedNode) {
        try {
            // Get neighboring nodes
            NodeEntity previous = getNeighbor(failedNode, "previous");
            NodeEntity next = getNeighbor(failedNode, "next");

            if (previous != null && next != null) {
                // Tell previous to update its next
                updateNeighbor(previous.getIpAddress(), "updateNext", next);

                // Tell next to update its previous
                updateNeighbor(next.getIpAddress(), "updatePrevious", previous);
            }

            // Remove from NamingServer
            nodeStorageService.deleteById(failedNode.getNodeHash());

            System.out.println("Removed node " + failedNode.getNodeName() + " from ring.");

        } catch (Exception e) {
            System.err.println("Failure handling failed for node " + failedNode.getNodeName());
            e.printStackTrace();
        }
    }

    private NodeEntity getNeighbor(NodeEntity node, String direction) {
        try {
            String url = "http://" + node.getIpAddress() + ":8080/ring/" + direction;
            ResponseEntity<NodeEntity> response = restTemplate.getForEntity(url, NodeEntity.class);
            return response.getBody();
        } catch (Exception e) {
            return null;
        }
    }

    private void updateNeighbor(String ip, String endpoint, NodeEntity payload) {
        try {
            String url = "http://" + ip + ":8080/node/internal/" + endpoint;
            HttpEntity<NodeEntity> request = new HttpEntity<>(payload);
            restTemplate.postForEntity(url, request, Void.class);
        } catch (Exception e) {
            System.err.println("Failed to update " + endpoint + " on node " + ip);
        }
    }
}
