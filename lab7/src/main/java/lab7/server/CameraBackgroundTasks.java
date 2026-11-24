package lab7.server;

import lab7.net.NetworkSettings;
import lab7.net.StatisticsPacket;
import lab7.net.TcpProtocol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class CameraBackgroundTasks implements AutoCloseable {
    private final NetworkSettings settings;
    private final CameraRequests requests;
    private final DatagramSocket socket;
    private final ScheduledExecutorService scheduler;

    CameraBackgroundTasks(NetworkSettings settings, CameraRequests requests) throws IOException {
        this.settings = settings;
        this.requests = requests;
        socket = openSocket(settings);
        scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    void start() {
        int updateInterval = settings.updateIntervalSeconds();
        scheduler.scheduleAtFixedRate(requests::updateCharacteristics, updateInterval, updateInterval, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::sendStatistics, 0, settings.statisticsIntervalSeconds(), TimeUnit.SECONDS);
    }

    private void sendStatistics() {
        try {
            for (StatisticsPacket statistics : requests.statistics()) {
                byte[] bytes = TcpProtocol.serialize(statistics);
                if (bytes.length > StatisticsPacket.MAX_PACKET_BYTES) {
                    throw new IOException("Пакет статистики слишком большой.");
                }
                DatagramPacket packet = new DatagramPacket(bytes, bytes.length, settings.statisticsAddress());
                socket.send(packet);
            }
        } catch (IOException e) {
            if (!socket.isClosed()) {
                System.err.println("Ошибка отправки статистики: " + e.getMessage());
            }
        }
    }

    private static DatagramSocket openSocket(NetworkSettings settings) throws IOException {
        if (settings.multicastInterface() != null) {
            MulticastSocket socket = new MulticastSocket();
            try {
                socket.setNetworkInterface(settings.multicastInterface());
                socket.setTimeToLive(1);
                return socket;
            } catch (IOException e) {
                socket.close();
                throw e;
            }
        }

        DatagramSocket socket = new DatagramSocket();
        try {
            socket.setBroadcast(true);
            return socket;
        } catch (IOException e) {
            socket.close();
            throw e;
        }
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
        socket.close();
    }
}
