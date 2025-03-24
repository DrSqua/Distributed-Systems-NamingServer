package schnitzel.NamingServer;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FileMapping {
    private static final String FILE_PATH = "map.json";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void saveMapToJson(Map<Integer, Integer> map) {
        try {
            objectMapper.writeValue(new File(FILE_PATH), map);
            System.out.println("Map saved to " + FILE_PATH);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Map<Integer, Integer> loadMapFromJson() {
        try {
            return objectMapper.readValue(new File(FILE_PATH), Map.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
