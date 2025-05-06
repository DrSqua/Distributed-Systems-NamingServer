package NodeClient.Components;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;

@Component
public class FileCheckerBean {
    private ArrayList<String> localFileList = new ArrayList<>();
    private ArrayList<String> prevFileList = new ArrayList<>();
    private Path filePathLocal;
    private Path filePathReplica;

    public FileCheckerBean() {
        Path path = Paths.get("").toAbsolutePath().normalize();
        this.filePathLocal = path.resolve("local_files");
        this.filePathReplica = path.resolve("replica_files");
    }

    @PostConstruct
    public void start() {
        new Thread(this::checkFiles).start();
    }

    private void checkFiles() {
        while (true) {
            try {
                File[] files = filePathLocal.toFile().listFiles();
                if (files != null) {
                    for (File localFile : files) {
                        if (localFile.isFile()) {
                            String fileName = localFile.getName();
                            if (!prevFileList.contains(fileName)) {
                                prevFileList.add(fileName);
                                replicateFile(localFile.toPath());
                            }
                        }
                    }
                }
                Thread.sleep(5000); // Check every 5 seconds for changes in the localFile folder
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void replicateFile(Path localFilePath) {
        Path targetPath = filePathReplica.resolve(localFilePath.getFileName());
        try {
            Files.copy(localFilePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
