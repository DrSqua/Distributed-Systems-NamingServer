package schnitzel.NamingServer;

import static java.lang.Math.abs;

public class NamingServerHash {
    public static Long hash(String nodeIdentifier) {
        return (long) abs(nodeIdentifier.hashCode()%32769);
    }
    public static Long hashNode(String nodeName, String nodeIpAddress) {
        return hash(nodeName + nodeIpAddress);
    }
}
