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

    public static void removeFromNamingServer(String nodeName, String namingServerIP) {
        RestTemplate restTemplate = new RestTemplate();
        try {
            String url = "http://" + namingServerIP + ":8080/node/" + nodeName;
            HttpEntity<String> request = new HttpEntity<>(null);
            restTemplate.delete(url, request, Void.class);
        } catch (Exception e) {
            // TODO - what if we are already in the exception throwing?
        }
    }

    public static void removingSelfFromSystem(String nodeName, String namingServerIP, NodeEntity previousNeighbour, NodeEntity nextNeighbour) {
        // Tell neighbours they are now eachother's neighbour
        RestMessagesRepository.updateNeighbour(nextNeighbour, "PREVIOUS", previousNeighbour.asEntityIn());
        RestMessagesRepository.updateNeighbour(previousNeighbour, "NEXT", nextNeighbour.asEntityIn());

        // Notify server we are leaving system
        RestMessagesRepository.removeFromNamingServer(nodeName, namingServerIP);
    }
}
