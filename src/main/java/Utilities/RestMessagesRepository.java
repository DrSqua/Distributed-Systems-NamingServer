package Utilities;

import Utilities.NodeEntity.NodeEntity;
import Utilities.NodeEntity.NodeEntityIn;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

public class RestMessagesRepository {
    public static void updateNeighbour(NodeEntity neighbour, String direction, NodeEntityIn data) {
        RestTemplate restTemplate = new RestTemplate();
        try {
            String url = "http://" + neighbour.getIpAddress() + ":8080/node/ring/" + direction;
            HttpEntity<NodeEntityIn> request = new HttpEntity<>(data);
            restTemplate.postForEntity(url, request, Void.class);
        } catch (Exception e) {
            System.err.println("Failed to update " + direction + " on node " + neighbour.getIpAddress());
        }
    }
}
