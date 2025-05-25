package Utilities;

import NodeClient.Agents.FailureAgent;
import NodeClient.File.FileListResponse;
import NodeClient.File.FileMessage;
import Utilities.NodeEntity.NodeEntity;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.util.List;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class RestMessagesRepository {
    private static final int nodeClientPort = 8081;
    private static final int namingServerPort = 8080;

    public static void updateNeighbour(NodeEntity neighbour,
                                       String direction,
                                       NodeEntity data) throws InterruptedException, UnknownHostException {
        Thread.sleep(1000);
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://" + neighbour.getIpAddress() + ":"+ nodeClientPort +"/ring/" + direction;

        System.out.println(InetAddress.getLocalHost().getHostAddress() + " sending " + data + " " + url);

        HttpEntity<NodeEntity> request = new HttpEntity<>(data);
        restTemplate.postForEntity(url, request, Void.class);
    }

    public static NodeEntity getNeighbor(NodeEntity node, String direction) {
        String url = "http://" + node.getIpAddress() + ":" + nodeClientPort + "/ring/" + direction;
        return new RestTemplate().getForObject(url, NodeEntity.class);
    }

    public static NodeEntity getNode(String nodeName, String namingServerIp) {
        String url = "http://" + namingServerIp + ":" + namingServerPort + "/node/" + nodeName;
        return new RestTemplate().getForObject(url, NodeEntity.class);
    }

    public static void removeFromNamingServer(NodeEntity node, String namingServerIP) {
        RestTemplate restTemplate = new RestTemplate();
        String url = "http://" + namingServerIP + ":"+ namingServerPort +"/node/" + node.getNodeHash();
        HttpEntity<String> request = new HttpEntity<>(null);
        restTemplate.delete(url, request, Void.class);
        System.out.println("Removing self (" + node.getNodeName() + ") from naming server ");
    }

    public static void removeFromNamingServerNoExcept(NodeEntity node, String namingServerIP) {
        try {
            RestMessagesRepository.removeFromNamingServer(node, namingServerIP);
        } catch (Exception e) {
            System.err.println("RemoveFromNamingServer NoExcept failed: " + e.getMessage());
        }
    }

    public static String checkReplicationResponsibility(long fileHash, long nodeHash, String namingServerIp) {
        String parameters = "fileHash=" + fileHash + "&nodeHash=" + nodeHash;
        String url = "http://" + namingServerIp + ":" + namingServerPort + "/file/replication?" + parameters;
        return new RestTemplate().getForObject(url, String.class);
    }

    public static void removingSelfFromSystem(NodeEntity node, String namingServerIP, NodeEntity previousNeighbour, NodeEntity nextNeighbour) throws InterruptedException, UnknownHostException {
        // Tell neighbours they are now each other's neighbour
        // Only if neighbour is not self
        if (!node.equals(nextNeighbour)) {
            RestMessagesRepository.updateNeighbour(nextNeighbour, "PREVIOUS", previousNeighbour);
        }
        if (!node.equals(previousNeighbour)) {
            RestMessagesRepository.updateNeighbour(previousNeighbour, "NEXT", nextNeighbour);
        }
        // Notify server we are leaving system
        RestMessagesRepository.removeFromNamingServer(node, namingServerIP);
    }

    public static void handleFileOperations(FileMessage message, String targetNodeIp) {
        new RestTemplate().postForObject("http://" + targetNodeIp + ":" + nodeClientPort + "/node/file/replication", message, Void.class);
    }

    public static void handleTransfer(FileMessage message, String targetNodeIp) {
        new RestTemplate().postForObject("http://" + targetNodeIp + ":" + nodeClientPort + "/node/file/transfer", message, Void.class);
    }

    public static NodeEntity getFileOwner(long FileHash, String namingServerIp) {
        String url = "http://" + namingServerIp + ":" + namingServerPort + "/file/owner?fileHash=" + FileHash;
        return new RestTemplate().getForObject(url, NodeEntity.class);
    }

    public static NodeEntity getFileOriginalOwner(long fileHash, String namingServerIp) {
        String url = "http://" + namingServerIp + ":" + namingServerPort + "/file/original-owner?fileHash=" + fileHash;
        return new RestTemplate().getForObject(url, NodeEntity.class);
    }

    public static void registerFileOriginalOwner(long fileHash, long originalNodeHash, String namingServerIp) {
        String url = "http://" + namingServerIp + ":" + namingServerPort + "/file/register-original-owner";
        String parameters = "fileHash=" + fileHash + "&originalNodeHash=" + originalNodeHash;
        new RestTemplate().postForObject(url + "?" + parameters, null, Void.class);
    }

    public static FileListResponse getFileListResponse(NodeEntity node) {
        String url = "http://" + node.getIpAddress() + ":" + nodeClientPort + "/node/file/list";
        return new RestTemplate().getForObject(url, FileListResponse.class);
    }

    public static void sendAgentToNode(FailureAgent failureAgent, String targetNodeIp) throws IOException {
        String url = "http://" + targetNodeIp + ":" + nodeClientPort + "/node/agent/receive";
        // serialize agent to byte array
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(failureAgent);
        oos.flush();
        byte[] agentBytes = bos.toByteArray();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        HttpEntity<byte[]> requestEntity = new HttpEntity<>(agentBytes, headers);
        new RestTemplate().postForObject(url, requestEntity, Void.class);
    }
}
