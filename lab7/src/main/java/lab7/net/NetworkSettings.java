package lab7.net;

import java.io.IOException;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public record NetworkSettings(InetSocketAddress serverAddress, InetSocketAddress statisticsAddress,
                              NetworkInterface multicastInterface, int updateIntervalSeconds,
                              int statisticsIntervalSeconds, Path dataDirectory) {

    public static InetSocketAddress loadAddress() throws IOException {
        return serverAddress(loadProperties(propertiesFile()));
    }

    public static NetworkSettings load() throws IOException {
        Path file = propertiesFile();
        Properties properties = loadProperties(file);
        InetSocketAddress server = serverAddress(properties);
        String host = properties.getProperty("statistics.host", "127.0.0.1").trim();
        if (host.isEmpty()) {
            throw new IllegalArgumentException("Укажите statistics.host в application.properties.");
        }
        int port = positiveInt(properties, "statistics.port", "5051", 65535);
        InetSocketAddress statistics = new InetSocketAddress(host, port);
        if (statistics.isUnresolved()) {
            throw new IllegalArgumentException("Не удалось найти адрес рассылки: " + host);
        }

        NetworkInterface multicastInterface = null;
        if (statistics.getAddress().isMulticastAddress()) {
            String name = properties.getProperty("statistics.interface", "").trim();
            multicastInterface = NetworkInterface.getByName(name);
            if (multicastInterface == null) {
                throw new IllegalArgumentException("Укажите существующий сетевой интерфейс в statistics.interface.");
            }
        }

        int updateInterval = positiveInt(properties, "devices.updateIntervalSeconds", "5", Integer.MAX_VALUE);
        int statisticsInterval = positiveInt(properties, "statistics.intervalSeconds", "5", Integer.MAX_VALUE);
        return new NetworkSettings(server, statistics, multicastInterface, updateInterval, statisticsInterval,
                file.getParent());
    }

    private static Path propertiesFile() {
        Path file = Path.of("application.properties");
        if (Files.notExists(file)) {
            file = Path.of("lab7", "application.properties");
        }
        return file.toAbsolutePath().normalize();
    }

    private static Properties loadProperties(Path file) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file)) {
            properties.load(reader);
        }
        return properties;
    }

    private static InetSocketAddress serverAddress(Properties properties) {
        String host = properties.getProperty("server.host", "").trim();
        if (host.isEmpty()) {
            throw new IllegalArgumentException("Укажите server.host в application.properties.");
        }

        int port = positiveInt(properties, "server.port", "", 65535);
        InetSocketAddress address = new InetSocketAddress(host, port);
        if (address.isUnresolved()) {
            throw new IllegalArgumentException("Не удалось найти адрес сервера: " + host);
        }
        return address;
    }

    private static int positiveInt(Properties properties, String key, String defaultValue, int maximum) {
        int number;
        try {
            number = Integer.parseInt(properties.getProperty(key, defaultValue).trim());
        } catch (NumberFormatException e) {
            number = 0;
        }
        if (number < 1 || number > maximum) {
            throw new IllegalArgumentException(key + " должен быть целым числом от 1 до " + maximum + ".");
        }
        return number;
    }
}
