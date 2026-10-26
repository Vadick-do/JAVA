package lab5;

import java.util.ArrayList;
import java.util.List;

public class FileReadResult {
    private final List<Camera> cams;
    private final int objectCount;
    private final int propertiesRead;
    private final int propertiesMissing;

    public FileReadResult(List<Camera> cams, int objectCount, int propertiesRead, int propertiesMissing) {
        this.cams = cams != null ? cams : new ArrayList<>();
        this.objectCount = objectCount;
        this.propertiesRead = propertiesRead;
        this.propertiesMissing = propertiesMissing;
    }

    public List<Camera> getCams() {
        return cams;
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

    public void printReport() {
        System.out.println("\n========== ОТЧЕТ О ЧТЕНИИ ФАЙЛА ==========");
        System.out.println("Найдено и воссоздано объектов: " + objectCount);
        System.out.println("Успешно прочитано свойств:      " + propertiesRead);
        System.out.println("Не найдено (пропущено) свойств: " + propertiesMissing);
        System.out.println("==========================================");
    }
}