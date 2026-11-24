package lab7.client;

import lab7.model.Camera;
import lab7.model.FileReadResult;
import lab7.net.CameraRequest;
import lab7.net.LogResult;
import lab7.net.NetworkSettings;
import lab7.net.TcpProtocol;

import java.io.IOException;
import java.util.List;

public class CameraClient {
    private TcpClient client;

    public List<Camera> getCameras() throws IOException {
        return List.of(request(TcpProtocol.LIST, Camera[].class));
    }

    public Camera getCamera(int id) throws IOException {
        Camera camera = request(TcpProtocol.GET, Camera.class, id);
        if (camera.getId() != id) {
            throw new IOException("Сервер вернул другое устройство.");
        }
        return camera;
    }

    public FileReadResult readCameras(String fileName) throws IOException {
        return request(TcpProtocol.READ, FileReadResult.class, fileName);
    }

    public FileReadResult loadCameras(String fileName) throws IOException {
        return request(TcpProtocol.LOAD, FileReadResult.class, fileName);
    }

    public LogResult logCameras(String fileName, String csvFileName) throws IOException {
        LogResult result = request(TcpProtocol.LOG, LogResult.class, fileName, csvFileName);
        if (result.warnings() == null || result.changes() < 0) {
            throw new IOException("Некорректный ответ сервера.");
        }
        return result;
    }

    public void execute(String command, Object... arguments) throws IOException {
        if (!TcpProtocol.OK.equals(request(command, String.class, arguments))) {
            throw new IOException("Некорректный ответ сервера.");
        }
    }

    private <T> T request(String command, Class<T> type, Object... arguments) throws IOException {
        String[] values = new String[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            values[i] = arguments[i].toString();
            if (values[i].indexOf('\n') >= 0) {
                throw new IOException("Аргумент должен занимать одну строку.");
            }
        }
        if (client == null) {
            try {
                client = new TcpClient(NetworkSettings.loadAddress());
            } catch (IllegalArgumentException e) {
                throw new IOException(e.getMessage(), e);
            }
        }
        return client.request(new CameraRequest(command, values)).valueAs(type);
    }
}
