package lab6.net;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class TcpProtocol {
    public static final String PING = "PING";
    public static final String PONG = "OK PONG";
    public static final String UNKNOWN_COMMAND = "ERROR Неизвестная команда.";
    public static final int TIMEOUT_MS = 5000;
    private static final int MAX_MESSAGE_BYTES = 1024 * 1024;

    public static String readMessage(DataInputStream input) throws IOException {
        int length = input.readInt();
        checkLength(length);
        byte[] bytes = new byte[length];
        input.readFully(bytes);
        return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
    }

    public static void writeMessage(DataOutputStream output, String message) throws IOException {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        checkLength(bytes.length);
        output.writeInt(bytes.length);
        output.write(bytes);
        output.flush();
    }

    private static void checkLength(int length) throws IOException {
        if (length < 1 || length > MAX_MESSAGE_BYTES) {
            throw new IOException("Недопустимая длина сообщения: " + length);
        }
    }
}
