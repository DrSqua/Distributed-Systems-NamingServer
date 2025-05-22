package schnitzel.NamingServer;

public class NamingServerHash {

    public static Long hash(String nodeIdentifier) {
        return (long) nodeIdentifier.hashCode();
    }

    public static Long hashNode(String nodeName, String nodeIpAddress) {
        return hash(nodeName + nodeIpAddress);
    }
}
