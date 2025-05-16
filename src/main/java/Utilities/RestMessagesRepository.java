package Utilities;

import NodeClient.File.FileMessage;
import Utilities.NodeEntity.NodeEntity;
import Utilities.NodeEntity.NodeEntityIn;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

public class RestMessagesRepository {
    @Value("${multicast.port}")
    private static int PORT;

    public static void updateNeighbour(NodeEntity neighbour, String direction, NodeEntityIn data) {
        RestTemplate restTemplate = new RestTemplate();
        try {
            String url = "http://" + neighbour.getIpAddress() + ":"+PORT+"/ring/" + direction;
            HttpEntity<NodeEntityIn> request = new HttpEntity<>(data);
            restTemplate.postForEntity(url, request, Void.class);
        } catch (Exception e) {
            System.err.println("Failed to update " + direction + " on node " + neighbour.getIpAddress());
        }
    }

    public static NodeEntity getNeighbor(NodeEntity node, String direction) {
        String url = "http://" + node.getIpAddress() + ":" + PORT + "/ring/" + direction;
        return new RestTemplate().getForObject(url, NodeEntity.class);
    }

    public static NodeEntity getNode(String nodeIdentifier, String namingServerIp) {
        String url = "http://" + namingServerIp + ":" + PORT + "/node/" + nodeIdentifier;
        return new RestTemplate().getForObject(url, NodeEntity.class);
    }

    public static void removeFromNamingServer(String nodeName, String namingServerIP) {
        RestTemplate restTemplate = new RestTemplate();
        try {
            String url = "http://" + namingServerIP + ":"+PORT+"/node/" + nodeName;
            HttpEntity<String> request = new HttpEntity<>(null);
            restTemplate.delete(url, request, Void.class);
        } catch (Exception e) {
            // TODO - what if we are already in the exception throwing?
        }
    }

    public static String checkReplicationResponsibility(long fileHash, long nodeHash, String namingServerIp) {
        String parameters = "fileHash=" + fileHash + "&nodeHash=" + nodeHash;
        String url = "http://" + namingServerIp + ":" + PORT + "/node/replication?" + parameters;
        return new RestTemplate().getForObject(url, String.class);
    }

    public static void removingSelfFromSystem(String nodeName, String namingServerIP, NodeEntity previousNeighbour, NodeEntity nextNeighbour) {
        // Tell neighbours they are now eachother's neighbour
        RestMessagesRepository.updateNeighbour(nextNeighbour, "PREVIOUS", previousNeighbour.asEntityIn());
        RestMessagesRepository.updateNeighbour(previousNeighbour, "NEXT", nextNeighbour.asEntityIn());

        // Notify server we are leaving system
        RestMessagesRepository.removeFromNamingServer(nodeName, namingServerIP);
    }

    public static void transferFile(FileMessage message, String targetNodeIp, int serverPort) {
        new RestTemplate().postForObject("http://" + targetNodeIp + ":" + serverPort + "/node/file/replication", message, Void.class);
    }

    public static void updateNamingServerFileRegistry(NodeEntity node, int fileHash, String namingServerIp, int port) {
        String url = "http://" + namingServerIp + ":" + port +"/node/owner?fileOwnerNode=" + node + "&fileHash=" + fileHash;
        new RestTemplate().getForObject(url, NodeEntity.class);
    }

    public static NodeEntity getFileOwner(long FileHash, String namingServerIp, int serverPort) {
        String url = "http://" + namingServerIp + ":" + serverPort + "/node/owner?fileHash=" + FileHash;
        return new RestTemplate().getForObject(url, NodeEntity.class);
    }
}
