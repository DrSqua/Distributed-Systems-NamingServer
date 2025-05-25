package NodeClient.Components;

import NodeClient.File.FileLoggerService;
import NodeClient.File.FileMessage;
import NodeClient.File.FileService;
import NodeClient.RingAPI.RingStorage;
import Utilities.NodeEntity.NodeEntity;
import Utilities.RestMessagesRepository;
import Utilities.Shutdown;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import schnitzel.NamingServer.NamingServerHash;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Component
public class OnShutdownBean {
    private final RingStorage ringStorage;
    private final FileService fileService;
    private final FileLoggerService fileLoggerService;

    @Autowired
    public OnShutdownBean(RingStorage ringStorage, FileService fileService) {
        this.ringStorage = ringStorage;
        this.fileService = fileService;
        this.fileLoggerService = fileService.getFileLoggerService();
    }

    @PreDestroy
    public void destroy() throws IOException, InterruptedException {
        NodeEntity nextNode = this.ringStorage.getNode("NEXT").orElseThrow(() ->
                new IllegalStateException("Existing Node does not have next set")
        );
        NodeEntity previousNode = this.ringStorage.getNode("PREVIOUS").orElseThrow(() ->
                new IllegalStateException("Existing Node does not have previous set")
        );
        System.out.println("Shutting down self " + this.ringStorage.currentName());

        RestMessagesRepository.removingSelfFromSystem(this.ringStorage.getSelf(), this.ringStorage.getNamingServerIP(), previousNode, nextNode);
        try {
            handleLocalFiles();
            Shutdown.transferReplicatedFiles(this.fileService, this.ringStorage);
        }
        catch (IOException e) {
            System.out.println("Could not handle local files or transfer replicated files, continuing shutdown process...");
        }
    }

    private void handleLocalFiles() throws IOException {
        File[] files = Paths.get("local_files").toFile().listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (!file.isFile()) {
                continue;
            }
            String fileName = file.getName();
            long fileHash = NamingServerHash.hash(fileName);
            // log a shutdown operation
            fileLoggerService.logOperation(fileName, fileHash, "SHUTDOWN", ringStorage.currentName(), file.getPath());
            boolean wasDownloaded = fileLoggerService.wasFileDownloaded(fileName);
            // if the file has been downloaded we transfer the file to our neighbor and then delete the file here
            if (wasDownloaded) {
                NodeEntity previousNode = this.ringStorage.getNode("PREVIOUS").orElseThrow(() ->
                        new IllegalStateException("Existing Node does not have previous set")
                );
                FileMessage message = new FileMessage(fileName, "TRANSFER_LOCAL", Files.readAllBytes(file.toPath()));
                RestMessagesRepository.handleTransfer(message, previousNode.getIpAddress());
            }
            // now we can safely delete the file
            FileMessage message = new FileMessage(fileName, "DELETE_LOCAL", Files.readAllBytes(file.toPath()));
            fileService.handleFileOperations(message);
        }
    }
}
