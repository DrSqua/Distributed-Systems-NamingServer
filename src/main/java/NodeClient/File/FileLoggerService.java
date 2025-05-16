package NodeClient.File;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// TODO better log service
@Service
public class FileLoggerService {
    private final File logFile = Paths.get("replication_log.json").toFile();
    private final ObjectMapper mapper = new ObjectMapper();

    public synchronized void logReplication(String fileName, long hash, String operation, String localPath) {
        List<FileLogEntry> entries = readLog();

        FileLogEntry newEntry = new FileLogEntry(fileName, hash, operation, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()), localPath);

        entries.add(newEntry);
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(logFile, entries);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<FileLogEntry> readLog() {
        if (!logFile.exists()) {
            return new ArrayList<>();
        }
        try {
            return mapper.readValue(logFile, new TypeReference<>() {});
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
