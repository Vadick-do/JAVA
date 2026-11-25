package lab7.client;

import lab7.model.PhotoCamera;
import lab7.model.Smartphone;
import lab7.model.WebCamera;
import lab7.net.NetworkSettings;
import lab7.net.StatisticsPacket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

public class StatisticsReceiver implements AutoCloseable {
    private final DatagramSocket socket;
    private final Thread worker;
    private final Consumer<CameraStatistics> onStatistics;
    private final Consumer<String> onError;
    private final Map<Integer, StatisticsPacket.Reading[]> parts = new TreeMap<>();
    private StatisticsPacket current;
    private SocketAddress sender;

    public StatisticsReceiver(NetworkSettings settings, Consumer<CameraStatistics> onStatistics,
                              Consumer<String> onError) throws IOException {
        socket = openSocket(settings);
        this.onStatistics = onStatistics;
        this.onError = onError;
        worker = new Thread(this::receive, "udp-statistics");
        worker.start();
    }

    private static DatagramSocket openSocket(NetworkSettings settings) throws IOException {
        int port = settings.statisticsAddress().getPort();
        if (settings.multicastInterface() == null) {
            return new DatagramSocket(port);
        }
        MulticastSocket multicast = new MulticastSocket(port);
        try {
            multicast.joinGroup(settings.statisticsAddress(), settings.multicastInterface());
            return multicast;
        } catch (IOException e) {
            multicast.close();
            throw e;
        }
    }

    private void receive() {
        byte[] buffer = new byte[StatisticsPacket.MAX_PACKET_BYTES + 1];
        DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);
        try {
            while (!socket.isClosed()) {
                datagram.setLength(buffer.length);
                socket.receive(datagram);
                StatisticsPacket packet;
                try {
                    packet = StatisticsPacket.read(datagram);
                } catch (IOException ignored) {
                    continue;
                }
                accept(packet, datagram.getSocketAddress());
            }
        } catch (IOException e) {
            if (!socket.isClosed()) {
                onError.accept(e.getMessage());
            }
        }
    }

    private void accept(StatisticsPacket packet, SocketAddress source) {
        if (!valid(packet)) {
            return;
        }
        if (current == null || packet.timestampMillis() > current.timestampMillis()) {
            current = packet;
            sender = source;
            parts.clear();
        }
        if (packet.timestampMillis() != current.timestampMillis() || !source.equals(sender)
                || packet.partCount() != current.partCount() || packet.webCameras() != current.webCameras()
                || packet.photoCameras() != current.photoCameras() || packet.smartphones() != current.smartphones()) {
            return;
        }
        if (parts.putIfAbsent(packet.partIndex(), packet.readings()) == null && parts.size() == packet.partCount()) {
            List<StatisticsPacket.Reading> readings = new ArrayList<>();
            for (StatisticsPacket.Reading[] part : parts.values()) {
                readings.addAll(List.of(part));
            }
            onStatistics.accept(new CameraStatistics(packet.webCameras(), packet.photoCameras(), packet.smartphones(),
                    List.copyOf(readings)));
        }
    }

    private boolean valid(StatisticsPacket packet) {
        if (packet.timestampMillis() <= 0 || packet.webCameras() < 0 || packet.photoCameras() < 0
                || packet.smartphones() < 0 || packet.partIndex() < 0 || packet.partIndex() >= packet.partCount()
                || packet.readings() == null) {
            return false;
        }
        long total = (long) packet.webCameras() + packet.photoCameras() + packet.smartphones();
        int packetSize = StatisticsPacket.READINGS_PER_PACKET;
        long expectedParts = Math.max(1L, (total + packetSize - 1) / packetSize);
        long expectedReadings = Math.min(packetSize, total - (long) packet.partIndex() * packetSize);
        if (packet.partCount() != expectedParts || packet.readings().length != expectedReadings) {
            return false;
        }
        for (StatisticsPacket.Reading reading : packet.readings()) {
            if (reading == null || reading.id() <= 0 || !Double.isFinite(reading.value())
                    || !(WebCamera.TYPE.equals(reading.type()) || PhotoCamera.TYPE.equals(reading.type())
                    || Smartphone.TYPE.equals(reading.type()))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void close() {
        socket.close();
        try {
            worker.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
