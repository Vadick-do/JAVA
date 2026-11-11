package lab6.server.io;

import lab6.model.FileReadResult;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class CamerasFileManager {

    public FileReadResult readFile(String fileName) throws IOException {
        try (FileInputStream fis = new FileInputStream(fileName);
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(isr)) {
            CamerasFileReader fileReader = new CamerasFileReader(br);
            return fileReader.readAll();
        }
    }
}
