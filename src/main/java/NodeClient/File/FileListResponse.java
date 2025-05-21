package NodeClient.File;

import java.util.List;

public record FileListResponse(
        List<String> localFiles,
        List<String> replicatedFiles
) {}
