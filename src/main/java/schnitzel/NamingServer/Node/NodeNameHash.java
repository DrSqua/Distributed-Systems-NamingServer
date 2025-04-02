package schnitzel.NamingServer.Node;

public class NodeNameHash {

    public static Long hash(String nodeIdentifier) {
        long max = 5;

        double hashCode = nodeIdentifier.hashCode();
        return (long) ((hashCode + max) * (32768/max + max));
    }
}
