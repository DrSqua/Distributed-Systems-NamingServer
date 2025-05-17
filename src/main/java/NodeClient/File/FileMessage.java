package NodeClient.File;

// record class to store data for the file and which operation we want to perform on it
public record FileMessage(String fileName, String operation, byte[] fileData) {
}
