package lab5;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class CamerasFileManager {

    public FileReadResult readFile(String fileName) throws IOException {
        File file = new File(fileName);
        if (!file.exists() && !file.isAbsolute()) {
            File fallback = new File("lab5", fileName);
            if (fallback.exists()) {
                file = fallback;
            }
        }

        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(isr)) {
            CamerasFileReader fileReader = new CamerasFileReader(br);
            return fileReader.readAll();
        }
    }

    public void checkLogFile(String fileName, String csvFileName) throws IOException {
        File source = new File(fileName);
        File target = new File(csvFileName);
        if (isSameFile(source, target) || (!source.isAbsolute() && isSameFile(new File("lab5", fileName), target))) {
            throw new IOException("Файл данных и CSV-журнал должны быть разными файлами.");
        }
    }

    private boolean isSameFile(File source, File target) throws IOException {
        return source.getCanonicalFile().equals(target.getCanonicalFile())
                || (source.exists() && target.exists() && Files.isSameFile(source.toPath(), target.toPath()));
    }

    public void replaceFile(String sourceName, String targetName) throws IOException {
        File source = new File(sourceName);
        File target = new File(targetName);

        if (!source.isFile()) {
            throw new IOException("Исходный файл '" + sourceName + "' не найден или не является файлом.");
        }
        if (target.exists() && !target.isFile()) {
            throw new IOException("Путь '" + targetName + "' не является файлом.");
        }
        if (target.exists() && Files.isSameFile(source.toPath(), target.toPath())) {
            throw new IOException("Для замены нужно указать два разных файла.");
        }

        try (FileInputStream input = new FileInputStream(source);
             FileOutputStream output = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
        }
    }
}
