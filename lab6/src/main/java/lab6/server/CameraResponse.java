package lab6.server;

import lab6.model.Camera;
import lab6.model.FileReadResult;
import lab6.model.Image;
import lab6.model.PhotoCamera;
import lab6.model.Smartphone;
import lab6.model.WebCamera;
import lab6.net.TcpProtocol;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

class CameraResponse {
    static String cameras(List<Camera> cameras) throws IOException {
        return finish(cameraFields(cameras));
    }

    static String report(FileReadResult report) throws IOException {
        StringBuilder response = cameraFields(report.getCams());
        fields(response, report.getPropertiesRead(), report.getPropertiesMissing(), report.getWarnings().size());
        for (String warning : report.getWarnings()) {
            fields(response, warning);
        }
        return finish(response);
    }

    private static StringBuilder cameraFields(List<Camera> cameras) {
        StringBuilder response = new StringBuilder(TcpProtocol.OK);
        fields(response, cameras.size());
        for (Camera camera : cameras) {
            fields(response, camera.getId(), camera.getModel(), camera.getMode(), camera.getCurrentZoom());
            if (camera instanceof WebCamera web) {
                fields(response, WebCamera.TYPE, web.getWidth(), web.getFPS(), web.isMicrophoneEnabled());
            } else if (camera instanceof PhotoCamera photo) {
                fields(response, PhotoCamera.TYPE, photo.getIso(), photo.getBattery(), photo.isFlashActive());
            } else if (camera instanceof Smartphone phone) {
                fields(response, Smartphone.TYPE, phone.getMegapixels(), phone.getStorage(), phone.isNightMode());
            }
            fields(response, camera.getImages().size());
            for (Image image : camera.getImages()) {
                fields(response, image.getFileName(), image.getResolution(), image.getSizeMb(), image.getFormat());
            }
        }
        return response;
    }

    private static void fields(StringBuilder response, Object... values) {
        for (Object value : values) {
            response.append('\n').append(value);
        }
    }

    private static String finish(StringBuilder response) throws IOException {
        String text = response.toString();
        if (text.getBytes(StandardCharsets.UTF_8).length > TcpProtocol.MAX_MESSAGE_BYTES) {
            throw new IOException("Ответ слишком большой.");
        }
        return text;
    }
}
