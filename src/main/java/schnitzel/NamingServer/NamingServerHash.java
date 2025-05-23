package schnitzel.NamingServer;

public class NamingServerHash {

    public static Long hash(String nodeIdentifier) {
        return (long) nodeIdentifier.hashCode();
    }

    public static Long hashNode(String nodeName, String nodeIpAddress) {
        System.out.println("Hashing node: " + nodeName + " with IP: " + nodeIpAddress);
        Long Hashed =  hash(nodeName + nodeIpAddress);
        System.out.println("Hash: " + Hashed);
        return Hashed;
    }
}
