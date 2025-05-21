package NodeClient.Agents;

public record FileLockingInfo(
        String fileName,
        boolean isLocked
) {}