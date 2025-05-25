package schnitzel.NamingServer;

import static java.lang.Math.abs;

public class NamingServerHash {
    public static Long hash(String nodeIdentifier) {
        return (long) nodeIdentifier.hashCode();
    }
    public static Long hashNode(String nodeName, String nodeIpAddress) {
        return abs(hash(nodeName + nodeIpAddress)%32769);
    }
}
