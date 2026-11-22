package lab7.net;

import java.io.Serializable;
import java.util.Objects;

public record CameraRequest(String command, String... arguments) implements Serializable {
    public CameraRequest {
        Objects.requireNonNull(command);
        arguments = arguments.clone();
        for (String argument : arguments) {
            Objects.requireNonNull(argument);
        }
    }
}
