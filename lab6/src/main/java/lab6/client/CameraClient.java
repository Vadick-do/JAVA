package lab6.client;

import lab6.model.Camera;
import lab6.model.FileReadResult;
import lab6.net.NetworkSettings;
import lab6.net.TcpProtocol;

import java.io.IOException;
import java.util.List;

public class CameraClient {
    private TcpClient client;

    public List<Camera> getCameras() throws IOException {
        return CameraReply.decode(request(TcpProtocol.LIST), false).getCams();
    }

    public Camera getCamera(int id) throws IOException {
        List<Camera> cameras = CameraReply.decode(request(TcpProtocol.GET, id), false).getCams();
        if (cameras.size() != 1 || cameras.get(0).getId() != id) {
            throw new IOException("Сервер вернул другое устройство.");
        }
        return cameras.get(0);
    }

    public FileReadResult readCameras(String fileName) throws IOException {
        return CameraReply.decode(request(TcpProtocol.READ, fileName), true);
    }

    public FileReadResult loadCameras(String fileName) throws IOException {
        return CameraReply.decode(request(TcpProtocol.LOAD, fileName), true);
    }

    public void execute(String command, Object... arguments) throws IOException {
        if (!TcpProtocol.OK.equals(request(command, arguments))) {
            throw new IOException("Некорректный ответ сервера.");
        }
    }

    private String request(String command, Object... arguments) throws IOException {
        StringBuilder message = new StringBuilder(command);
        for (Object argument : arguments) {
            String value = argument.toString();
            if (value.indexOf('\n') >= 0) {
                throw new IOException("Аргумент должен занимать одну строку.");
            }
            message.append('\n').append(value);
        }
        if (client == null) {
            try {
                client = new TcpClient(NetworkSettings.loadAddress());
            } catch (IllegalArgumentException e) {
                throw new IOException(e.getMessage(), e);
            }
        }
        String response = client.request(message.toString());
        if (response.startsWith(TcpProtocol.ERROR)) {
            throw new IOException(response.substring(TcpProtocol.ERROR.length()));
        }
        return response;
    }
}
