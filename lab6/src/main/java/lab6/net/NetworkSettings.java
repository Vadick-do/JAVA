package lab6.net;

import java.io.IOException;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class NetworkSettings {
    private static final String PORT_ERROR = "server.port должен быть целым числом от 1 до 65535.";

    public static InetSocketAddress loadAddress() throws IOException {
        Path file = Path.of("application.properties");
        if (Files.notExists(file)) {
            file = Path.of("lab6", "application.properties");
        }

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file)) {
            properties.load(reader);
        }

        String host = properties.getProperty("server.host", "").trim();
        if (host.isEmpty()) {
            throw new IllegalArgumentException("Укажите server.host в application.properties.");
        }

        int port;
        try {
            port = Integer.parseInt(properties.getProperty("server.port", "").trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(PORT_ERROR);
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException(PORT_ERROR);
        }

        InetSocketAddress address = new InetSocketAddress(host, port);
        if (address.isUnresolved()) {
            throw new IllegalArgumentException("Не удалось найти адрес сервера: " + host);
        }
        return address;
    }
}
