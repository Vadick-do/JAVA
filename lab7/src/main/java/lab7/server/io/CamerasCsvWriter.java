package lab7.server.io;

import lab7.model.Camera;
import lab7.model.PhotoCamera;
import lab7.model.Smartphone;
import lab7.model.WebCamera;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CamerasCsvWriter {
    private final Path file;
    private Path previousSource;
    private List<Map<String, String>> previous = List.of();

    public CamerasCsvWriter(Path file) {
        this.file = file;
    }

    public int writeChanges(Path source, List<Camera> cams) throws IOException {
        boolean emptyFile = Files.notExists(file) || Files.size(file) == 0;
        List<Map<String, String>> current = new ArrayList<>();
        List<Map<String, String>> saved = !emptyFile && source.equals(previousSource) ? previous : List.of();
        int changes = 0;
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            if (emptyFile) {
                writer.write("time,number,property,value\r\n");
            }

            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            for (int i = 0; i < cams.size(); i++) {
                Map<String, String> fields = getProperties(cams.get(i));
                current.add(fields);
                Map<String, String> old = i < saved.size() ? saved.get(i) : Map.of();
                boolean newCamera = !fields.get("type").equals(old.get("type"));

                for (Map.Entry<String, String> entry : fields.entrySet()) {
                    if (newCamera || !entry.getValue().equals(old.get(entry.getKey()))) {
                        writer.write(time + "," + (i + 1) + "," + entry.getKey() + ",\""
                                + entry.getValue().replace("\"", "\"\"") + "\"\r\n");
                        changes++;
                    }
                }
            }
        }
        previous = current;
        previousSource = source;
        return changes;
    }

    private Map<String, String> getProperties(Camera cam) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("model", cam.getModel());
        fields.put("mode", cam.getMode());
        fields.put("zoom", Double.toString(cam.getCurrentZoom()));

        if (cam instanceof WebCamera wc) {
            fields.put("type", WebCamera.TYPE);
            fields.put("width", Double.toString(wc.getWidth()));
            fields.put("fps", Double.toString(wc.getFPS()));
            fields.put("microphone", Boolean.toString(wc.isMicrophoneEnabled()));
        } else if (cam instanceof PhotoCamera pc) {
            fields.put("type", PhotoCamera.TYPE);
            fields.put("iso", Integer.toString(pc.getIso()));
            fields.put("battery", Integer.toString(pc.getBattery()));
            fields.put("flash", Boolean.toString(pc.isFlashActive()));
        } else if (cam instanceof Smartphone sp) {
            fields.put("type", Smartphone.TYPE);
            fields.put("megapixels", Double.toString(sp.getMegapixels()));
            fields.put("storage", Double.toString(sp.getStorage()));
            fields.put("nightmode", Boolean.toString(sp.isNightMode()));
        }
        return fields;
    }

}
