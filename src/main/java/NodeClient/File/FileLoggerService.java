package NodeClient.File;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileLoggerService {
    private final File logFile = Paths.get("file_logs.json").toFile();
    private final ObjectMapper mapper = new ObjectMapper();

    public FileLoggerService() {
        if (!logFile.exists()) {
            try {
                // Create empty JSON array file to start with
                mapper.writerWithDefaultPrettyPrinter().writeValue(logFile, new ArrayList<FileLogEntry>());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void logOperation(String fileName, long hash, String operation, String currentNodeName, String localPath) {
        FileLogEntry newEntry = new FileLogEntry(fileName, hash, operation,currentNodeName, Instant.now().toString(), localPath);
        try (FileWriter fw = new FileWriter(logFile, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
                String json = mapper.writeValueAsString(newEntry);
                out.println(json);
             } catch (IOException e) {
                e.printStackTrace();
        }
    }

    public void writeLogs(List<FileLogEntry> logs) {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(logFile, logs);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<FileLogEntry> getAllLogs() {
        List<FileLogEntry> entries = new ArrayList<>();
        if (!logFile.exists()) {
            return entries;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                FileLogEntry entry = mapper.readValue(line, FileLogEntry.class);
                entries.add(entry);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return entries;
    }

    public List<FileLogEntry> getLogsForFile(String fileName) {
        List<FileLogEntry> entries = getAllLogs();
        List<FileLogEntry> matchingEntries = new ArrayList<>();
        for (FileLogEntry entry : entries) {
            if (entry.fileName().equals(fileName)) {
                matchingEntries.add(entry);
            }
        }
        return matchingEntries;
    }

    public boolean wasFileDownloaded(String fileName) {
        return getAllLogs().stream().anyMatch(
                entry -> entry.fileName().equals(fileName) && entry.operation().equals("DOWNLOAD")
        );
    }
}
