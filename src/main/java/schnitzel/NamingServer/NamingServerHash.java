package schnitzel.NamingServer;

public class NamingServerHash {

    public static Long hash(String nodeIdentifier) {
        final long max = 2147483647;
        long hashCode = nodeIdentifier.hashCode();
        return ((hashCode + max) * 32768) / (2*max);
    }
}
