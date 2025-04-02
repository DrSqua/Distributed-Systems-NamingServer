package schnitzel.NamingServer;

public class NamingServerHash {

    public static Long hash(String nodeIdentifier) {
        long max = 2147483647;
        long hashCode = nodeIdentifier.hashCode();
        double helper = (double) 32768/max + max;
        return (long) ((hashCode + max) * helper);
    }
}
