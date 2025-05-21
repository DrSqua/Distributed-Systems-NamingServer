package Utilities;

import NodeClient.File.FileListResponse;
import NodeClient.File.FileMessage;
import Utilities.NodeEntity.NodeEntity;
import Utilities.NodeEntity.NodeEntityIn;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;

public class RestMessagesRepository {
    private static final int nodeClientPort = 8081;
    private static final int namingServerPort = 8080;

    public static void updateNeighbour(NodeEntity neighbour, String direction, NodeEntityIn data) {
        RestTemplate restTemplate = new RestTemplate();
        try {
            String url = "http://" + neighbour.getIpAddress() + ":"+ nodeClientPort +"/ring/" + direction;
            HttpEntity<NodeEntityIn> request = new HttpEntity<>(data);
            restTemplate.postForEntity(url, request, Void.class);
        } catch (Exception e) {
            System.err.println("Failed to update " + direction + " on node " + neighbour.getIpAddress());
        }
    }

    public static NodeEntity getNeighbor(NodeEntity node, String direction) {
        String url = "http://" + node.getIpAddress() + ":" + nodeClientPort + "/ring/" + direction;
        return new RestTemplate().getForObject(url, NodeEntity.class);
    }

    public static NodeEntity getNode(String nodeName, String namingServerIp) {
        String url = "http://" + namingServerIp + ":" + namingServerPort + "/node/" + nodeName;
        return new RestTemplate().getForObject(url, NodeEntity.class);
    }

    public static void removeFromNamingServer(String nodeName, String namingServerIP) {
        RestTemplate restTemplate = new RestTemplate();
        try {
            String url = "http://" + namingServerIP + ":"+ namingServerPort +"/node/" + nodeName;
            HttpEntity<String> request = new HttpEntity<>(null);
            restTemplate.delete(url, request, Void.class);
            System.out.println("Removing self (" + nodeName + ") from naming server ");
        } catch (Exception e) {
            // TODO - what if we are already in the exception throwing?
        }
    }

    public static String checkReplicationResponsibility(long fileHash, long nodeHash, String namingServerIp) {
        String parameters = "fileHash=" + fileHash + "&nodeHash=" + nodeHash;
        String url = "http://" + namingServerIp + ":" + namingServerPort + "/node/replication?" + parameters;
        return new RestTemplate().getForObject(url, String.class);
    }

    public static void removingSelfFromSystem(String nodeName, String namingServerIP, NodeEntity previousNeighbour, NodeEntity nextNeighbour) {
        // Tell neighbours they are now each other's neighbour
        RestMessagesRepository.updateNeighbour(nextNeighbour, "PREVIOUS", previousNeighbour.asEntityIn());
        RestMessagesRepository.updateNeighbour(previousNeighbour, "NEXT", nextNeighbour.asEntityIn());

        // Notify server we are leaving system
        RestMessagesRepository.removeFromNamingServer(nodeName, namingServerIP);
    }

    public static void handleFileOperations(FileMessage message, String targetNodeIp) {
        new RestTemplate().postForObject("http://" + targetNodeIp + ":" + namingServerPort + "/node/file/replication", message, Void.class);
    }

    public static void handleTransfer(FileMessage message, String targetNodeIp) {
        new RestTemplate().postForObject("http://" + targetNodeIp + ":" + namingServerPort + "/node/file/transfer", message, Void.class);
    }

    public static NodeEntity getFileOwner(long FileHash, String namingServerIp) {
        String url = "http://" + namingServerIp + ":" + namingServerPort + "/node/owner?fileHash=" + FileHash;
        return new RestTemplate().getForObject(url, NodeEntity.class);
    }

    public static FileListResponse getFileListResponse(NodeEntity node) {
        String url = "http://" + node.getIpAddress() + ":" + nodeClientPort + "/node/file/list";
        return new RestTemplate().getForObject(url, FileListResponse.class);
    }
}
