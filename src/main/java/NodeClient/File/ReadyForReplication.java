package NodeClient.File;

public class ReadyForReplication {
    private static volatile Boolean isReadyForReplication = false;
    private static volatile Boolean hasNewNodeEntered = false;


    public static void setIsReadyForReplication(Boolean ready) {
        isReadyForReplication = ready;
    }
    public static Boolean getIsReadyForReplication() {
        return isReadyForReplication;
    }
    public static void setHasNewNodeEntered(Boolean hasNewNodeEnter) {
        hasNewNodeEntered = hasNewNodeEnter;
    }
    public static Boolean getHasNewNodeEntered() {
        return hasNewNodeEntered;
    }
}
