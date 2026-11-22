package lab7.net;

import lab7.model.Camera;
import lab7.model.FileReadResult;
import lab7.model.Image;
import lab7.model.PhotoCamera;
import lab7.model.Smartphone;
import lab7.model.WebCamera;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
            Integer.class, Number.class);

    public static <T> T readMessage(DataInputStream input, Class<T> type) throws IOException {
        int length = input.readInt();
        if (length < 1 || length > MAX_MESSAGE_BYTES) {
            throw new IOException("Недопустимая длина сообщения: " + length);
        }
        byte[] bytes = new byte[length];
        input.readFully(bytes);
        Set<Class<?>> allowedTypes = type == CameraRequest.class ? REQUEST_TYPES : RESPONSE_TYPES;
        try (ObjectInputStream objects = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            objects.setObjectInputFilter(info -> filter(info, allowedTypes));
            Object message = objects.readObject();
            if (!type.isInstance(message)) {
                throw new IOException("Неожиданный тип сообщения.");
            }
            return type.cast(message);
        } catch (ClassNotFoundException e) {
            throw new IOException("Неизвестный класс сообщения.", e);
        }
    }

    public static byte[] serialize(Serializable message) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream objects = new ObjectOutputStream(bytes)) {
            objects.writeObject(message);
        }
        if (bytes.size() > MAX_MESSAGE_BYTES) {
            throw new IOException("Сообщение слишком большое.");
        }
        return bytes.toByteArray();
    }

    public static void writeMessage(DataOutputStream output, byte[] bytes) throws IOException {
        output.writeInt(bytes.length);
        output.write(bytes);
        output.flush();
    }

    private static ObjectInputFilter.Status filter(ObjectInputFilter.FilterInfo info, Set<Class<?>> allowedTypes) {
        if (info.depth() > 12 || info.arrayLength() > MAX_MESSAGE_BYTES / Integer.BYTES) {
            return ObjectInputFilter.Status.REJECTED;
        }
        Class<?> type = info.serialClass();
        if (type == null) {
            return ObjectInputFilter.Status.UNDECIDED;
        }
        return allowedTypes.contains(type) ? ObjectInputFilter.Status.ALLOWED : ObjectInputFilter.Status.REJECTED;
    }
}
