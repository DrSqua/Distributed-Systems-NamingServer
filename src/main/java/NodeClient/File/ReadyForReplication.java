package NodeClient.File;

public class ReadyForReplication {
    private static volatile Boolean isReadyForReplication = false;

    public static void setIsReadyForReplication(Boolean ready) {
        isReadyForReplication = ready;
    }
    public static Boolean getIsReadyForReplication() {
        return isReadyForReplication;
    }
}
