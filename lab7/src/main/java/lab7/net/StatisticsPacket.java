package lab7.net;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.util.Set;

public record StatisticsPacket(long timestampMillis, int partIndex, int partCount, int webCameras,
                               int photoCameras, int smartphones, Reading[] readings) implements Serializable {
    public static final int MAX_PACKET_BYTES = 1232;
    public static final int READINGS_PER_PACKET = 24;
    private static final Set<Class<?>> PACKET_TYPES = Set.of(StatisticsPacket.class, Reading.class, Reading[].class);

    public static StatisticsPacket read(DatagramPacket packet) throws IOException {
        if (packet.getLength() < 1 || packet.getLength() > MAX_PACKET_BYTES) {
            throw new IOException("Недопустимый размер пакета статистики.");
        }
        ByteArrayInputStream bytes = new ByteArrayInputStream(packet.getData(), packet.getOffset(), packet.getLength());
        return MessageSerialization.decode(bytes, StatisticsPacket.class, PACKET_TYPES, READINGS_PER_PACKET);
    }

    public byte[] toBytes() throws IOException {
        return MessageSerialization.encode(this, MAX_PACKET_BYTES);
    }

    public record Reading(int id, String type, double value) implements Serializable {
    }
}
