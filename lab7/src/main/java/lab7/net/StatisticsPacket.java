package lab7.net;

import java.io.Serializable;

public record StatisticsPacket(long timestampMillis, int partIndex, int partCount, int webCameras,
                               int photoCameras, int smartphones, Reading[] readings) implements Serializable {
    public static final int MAX_PACKET_BYTES = 1232;

    public record Reading(int id, String type, double value) implements Serializable {
    }
}
