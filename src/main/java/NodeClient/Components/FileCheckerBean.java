package NodeClient.Components;

import Utilities.FileRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

@Component
public class FileCheckerBean {
    Path path = Paths.get("").toAbsolutePath().normalize();
    private ArrayList<String> localFileList = new ArrayList<>();
    private ArrayList<String> prevFileList = new ArrayList<>();
    private Path filePathLocal;
    private Path filePathReplica;
    public FileCheckerBean() {
        this.filePathLocal = path.resolve("local_files");
        this.filePathReplica = path.resolve("replica_files");
    }

    @PostConstruct
    public void start() {
        new Thread(this::checkFiles).start();
    }
    private void checkFiles() {
//        for (File localFile : filePathLocal.toFile().listFiles()) {
//            if (localFile.isFile()) {
//                localFileList.add(localFile.getName());
//            }
//        }

    }
}
