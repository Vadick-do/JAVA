package lab7.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Set;

final class MessageSerialization {
    private MessageSerialization() {
    }

    static byte[] encode(Serializable message, int maxBytes) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream objects = new ObjectOutputStream(bytes)) {
            objects.writeObject(message);
        }
        if (bytes.size() > maxBytes) {
            throw new IOException("Сообщение слишком большое.");
        }
        return bytes.toByteArray();
    }

    static <T> T decode(ByteArrayInputStream bytes, Class<T> type, Set<Class<?>> allowedTypes,
                        int maxArrayLength) throws IOException {
        try (ObjectInputStream objects = new ObjectInputStream(bytes)) {
            objects.setObjectInputFilter(info -> filter(info, allowedTypes, maxArrayLength));
            Object message = objects.readObject();
            if (!type.isInstance(message)) {
                throw new IOException("Неожиданный тип сообщения.");
            }
            return type.cast(message);
        } catch (ClassNotFoundException e) {
            throw new IOException("Неизвестный класс сообщения.", e);
        }
    }

    private static ObjectInputFilter.Status filter(ObjectInputFilter.FilterInfo info, Set<Class<?>> allowedTypes,
                                                   int maxArrayLength) {
        if (info.depth() > 12 || info.arrayLength() > maxArrayLength) {
            return ObjectInputFilter.Status.REJECTED;
        }
        Class<?> type = info.serialClass();
        if (type == null) {
            return ObjectInputFilter.Status.UNDECIDED;
        }
        return allowedTypes.contains(type) ? ObjectInputFilter.Status.ALLOWED : ObjectInputFilter.Status.REJECTED;
    }
}
