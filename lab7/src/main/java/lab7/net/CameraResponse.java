package lab7.net;

import java.io.IOException;
import java.io.Serializable;

public record CameraResponse(Serializable value, String error) implements Serializable {
    public CameraResponse(Serializable value) {
        this(value, null);
    }

    public static CameraResponse failure(String message) {
        return new CameraResponse(null, message);
    }

    public <T> T valueAs(Class<T> type) throws IOException {
        if (error != null) {
            throw new IOException(error);
        }
        if (!type.isInstance(value)) {
            throw new IOException("Некорректный ответ сервера.");
        }
        return type.cast(value);
    }
}
