package lab5;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CamerasCsvWriter implements Closeable {
    private final BufferedWriter writer;
    private boolean headerWritten;
    private List<Map<String, String>> previous = new ArrayList<>();

    public CamerasCsvWriter(String fileName) throws IOException {
        File file = new File(fileName);
        headerWritten = file.length() > 0;
        writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8));
    }

    public int writeChanges(List<Camera> cams) throws IOException {
        if (!headerWritten) {
            writer.write("time,number,property,value\r\n");
            headerWritten = true;
        }

        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        List<Map<String, String>> current = new ArrayList<>();
        int changes = 0;
        for (int i = 0; i < cams.size(); i++) {
            Map<String, String> fields = getProperties(cams.get(i));
            current.add(fields);
            Map<String, String> old = i < previous.size() ? previous.get(i) : new LinkedHashMap<>();
            boolean newCamera = !fields.get("type").equals(old.get("type"));

            for (Map.Entry<String, String> entry : fields.entrySet()) {
                if (newCamera || !entry.getValue().equals(old.get(entry.getKey()))) {
                    writer.write(time + "," + (i + 1) + "," + entry.getKey() + ",\""
                            + entry.getValue().replace("\"", "\"\"") + "\"\r\n");
                    changes++;
                }
            }
        }
        writer.flush();
        previous = current;
        return changes;
    }

    private Map<String, String> getProperties(Camera cam) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("model", cam.getModel());
        fields.put("mode", cam.getMode());
        fields.put("zoom", Double.toString(cam.getCurrentZoom()));

        if (cam instanceof WebCamera wc) {
            fields.put("type", "WebCamera");
            fields.put("width", Double.toString(wc.getWidth()));
            fields.put("fps", Double.toString(wc.getFPS()));
            fields.put("microphone", Boolean.toString(wc.isMicrophoneEnabled()));
        } else if (cam instanceof PhotoCamera pc) {
            fields.put("type", "PhotoCamera");
            fields.put("iso", Integer.toString(pc.getIso()));
            fields.put("battery", Integer.toString(pc.getBattery()));
            fields.put("flash", Boolean.toString(pc.isFlashActive()));
        } else if (cam instanceof Smartphone sp) {
            fields.put("type", "Smartphone");
            fields.put("megapixels", Double.toString(sp.getMegapixels()));
            fields.put("storage", Double.toString(sp.getStorage()));
            fields.put("nightmode", Boolean.toString(sp.isNightMode()));
        }
        return fields;
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
