package lab5;

import java.util.ArrayList;
import java.util.List;

public class FileReadResult {
    private final List<Camera> cams;
    private final List<String> warnings;
    private final int objectCount;
    private final int propertiesRead;
    private final int propertiesMissing;

    public FileReadResult(List<Camera> cams, int objectCount, int propertiesRead, int propertiesMissing, List<String> warnings) {
        this.cams = cams != null ? cams : new ArrayList<>();
        this.warnings = warnings != null ? new ArrayList<>(warnings) : new ArrayList<>();
        this.objectCount = objectCount;
        this.propertiesRead = propertiesRead;
        this.propertiesMissing = propertiesMissing;
    }

    public List<Camera> getCams() {
        return cams;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public int getObjectCount() {
        return objectCount;
    }

    public int getPropertiesRead() {
        return propertiesRead;
    }

    public int getPropertiesMissing() {
        return propertiesMissing;
    }

}
