package lab7.net;

import lab7.model.Camera;
import lab7.model.FileReadResult;
import lab7.model.Image;
import lab7.model.PhotoCamera;
import lab7.model.Smartphone;
import lab7.model.WebCamera;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Set;

public class TcpProtocol {
    public static final String PING = "PING";
    public static final String LIST = "LIST";
    public static final String GET = "GET";
    public static final String READ = "READ";
    public static final String LOAD = "LOAD";
    public static final String LOG = "LOG";
    public static final String REPLACE = "REPLACE";
    public static final String ADD = "ADD";
    public static final String EDIT = "EDIT";
    public static final String DELETE = "DELETE";
    public static final String PHOTO = "PHOTO";
    public static final String VIDEO = "VIDEO";
    public static final String ZOOM = "ZOOM";
    public static final String MODE = "MODE";
    public static final String RANDOMIZE = "RANDOMIZE";
    public static final String TOGGLE = "TOGGLE";
    public static final String RESET = "RESET";
    public static final String DEFAULT_FILE = "devices.txt";
    public static final String OK = "OK";
    public static final String PONG = OK + " PONG";
    public static final int TIMEOUT_MS = 5000;
    private static final int MAX_MESSAGE_BYTES = 1024 * 1024;
    private static final Set<Class<?>> REQUEST_TYPES = Set.of(CameraRequest.class, String[].class);
    private static final Set<Class<?>> RESPONSE_TYPES = Set.of(
            CameraResponse.class, Camera[].class, Camera.class, WebCamera.class, PhotoCamera.class,
            Smartphone.class, Image.class, FileReadResult.class, ArrayList.class, Object[].class,
            LogResult.class, String[].class);

    public static <T> T readMessage(DataInputStream input, Class<T> type) throws IOException {
        int length = input.readInt();
        if (length < 1 || length > MAX_MESSAGE_BYTES) {
            throw new IOException("Недопустимая длина сообщения: " + length);
        }
        byte[] bytes = new byte[length];
        input.readFully(bytes);
        Set<Class<?>> allowedTypes = type == CameraRequest.class ? REQUEST_TYPES : RESPONSE_TYPES;
        return MessageSerialization.decode(new ByteArrayInputStream(bytes), type, allowedTypes,
                MAX_MESSAGE_BYTES / Integer.BYTES);
    }

    public static byte[] serialize(Serializable message) throws IOException {
        return MessageSerialization.encode(message, MAX_MESSAGE_BYTES);
    }

    public static void writeMessage(DataOutputStream output, byte[] bytes) throws IOException {
        output.writeInt(bytes.length);
        output.write(bytes);
        output.flush();
    }

}
