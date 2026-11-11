package lab6.client;

import lab6.model.Camera;
import lab6.model.FileReadResult;
import lab6.model.Image;
import lab6.model.PhotoCamera;
import lab6.model.Smartphone;
import lab6.model.WebCamera;
import lab6.net.TcpProtocol;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class CameraReply {
    private static final String BAD_REPLY = "Некорректный ответ сервера.";
    private final String[] fields;
    private int index;

    private CameraReply(String response) throws IOException {
        fields = response.split("\n", -1);
        if (!TcpProtocol.OK.equals(read())) {
            throw new IOException(BAD_REPLY);
        }
    }

    static FileReadResult decode(String response, boolean report) throws IOException {
        CameraReply reply = new CameraReply(response);
        try {
            List<Camera> cameras = new ArrayList<>();
            int count = reply.count();
            for (int i = 0; i < count; i++) {
                cameras.add(reply.readCamera());
            }
            int propertiesRead = 0;
            int propertiesMissing = 0;
            List<String> warnings = new ArrayList<>();
            if (report) {
                propertiesRead = reply.count();
                propertiesMissing = reply.count();
                int warningCount = reply.count();
                for (int i = 0; i < warningCount; i++) {
                    warnings.add(reply.read());
                }
            }
            if (reply.index != reply.fields.length) {
                throw new IOException(BAD_REPLY);
            }
            return new FileReadResult(cameras, count, propertiesRead, propertiesMissing, warnings);
        } catch (IllegalArgumentException e) {
            throw new IOException(BAD_REPLY, e);
        }
    }

    private Camera readCamera() throws IOException {
        int id = count();
        if (id == 0) {
            throw new IOException(BAD_REPLY);
        }
        String model = read();
        String mode = read();
        double zoom = decimal(read());
        String type = read();
        String first = read();
        String second = read();
        boolean flag = switch (read()) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new IOException(BAD_REPLY);
        };
        Camera camera = switch (type) {
            case WebCamera.TYPE -> new WebCamera(model, decimal(first), decimal(second), flag, zoom, id);
            case PhotoCamera.TYPE -> new PhotoCamera(model, Integer.parseInt(first), Integer.parseInt(second), flag, zoom, id);
            case Smartphone.TYPE -> new Smartphone(model, decimal(first), decimal(second), flag, zoom, id);
            default -> throw new IOException(BAD_REPLY);
        };
        camera.setMode(mode);
        int imageCount = count();
        for (int i = 0; i < imageCount; i++) {
            camera.getImages().add(new Image(read(), read(), decimal(read()), read()));
        }
        return camera;
    }

    private String read() throws IOException {
        if (index >= fields.length) {
            throw new IOException(BAD_REPLY);
        }
        return fields[index++];
    }

    private int count() throws IOException {
        int number = Integer.parseInt(read());
        if (number < 0) {
            throw new IOException(BAD_REPLY);
        }
        return number;
    }

    private double decimal(String value) throws IOException {
        double number = Double.parseDouble(value);
        if (!Double.isFinite(number)) {
            throw new IOException(BAD_REPLY);
        }
        return number;
    }
}
