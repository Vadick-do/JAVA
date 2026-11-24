package lab7.server;

import lab7.model.Camera;
import lab7.model.FileReadResult;
import lab7.model.PhotoCamera;
import lab7.model.Smartphone;
import lab7.model.WebCamera;
import lab7.net.CameraRequest;
import lab7.net.CameraResponse;
import lab7.net.LogResult;
import lab7.net.StatisticsPacket;
import lab7.net.TcpProtocol;
import lab7.server.io.CamerasCsvWriter;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class CameraRequests {
    private static final int MAX_FILE_NAME_LENGTH = 255;
    private static final int READINGS_PER_PACKET = 24;
    private final CameraServer server = new CameraServer();
    private final Path dataDirectory;
    private final Map<Path, CamerasCsvWriter> journals = new HashMap<>();

    CameraRequests(Path dataDirectory) {
        this.dataDirectory = dataDirectory.toAbsolutePath().normalize();
    }

    void loadDefaultFile() throws IOException {
        server.loadCameras(sourceFile(TcpProtocol.DEFAULT_FILE).toString());
    }

    synchronized void updateCharacteristics() {
        server.updateCharacteristics();
    }

    synchronized List<StatisticsPacket> statistics() {
        List<StatisticsPacket.Reading> readings = new ArrayList<>();
        int webCameras = 0;
        int photoCameras = 0;
        int smartphones = 0;
        for (Camera camera : server.getCameras()) {
            if (camera instanceof WebCamera web) {
                webCameras++;
                readings.add(new StatisticsPacket.Reading(web.getId(), WebCamera.TYPE, web.getFPS()));
            } else if (camera instanceof PhotoCamera photo) {
                photoCameras++;
                readings.add(new StatisticsPacket.Reading(photo.getId(), PhotoCamera.TYPE, photo.getIso()));
            } else if (camera instanceof Smartphone phone) {
                smartphones++;
                readings.add(new StatisticsPacket.Reading(phone.getId(), Smartphone.TYPE, phone.getStorage()));
            }
        }

        long timestamp = System.currentTimeMillis();
        int partCount = Math.max(1, (readings.size() + READINGS_PER_PACKET - 1) / READINGS_PER_PACKET);
        List<StatisticsPacket> packets = new ArrayList<>(partCount);
        for (int part = 0; part < partCount; part++) {
            int start = part * READINGS_PER_PACKET;
            int end = Math.min(start + READINGS_PER_PACKET, readings.size());
            StatisticsPacket.Reading[] values = readings.subList(start, end).toArray(StatisticsPacket.Reading[]::new);
            packets.add(new StatisticsPacket(timestamp, part, partCount, webCameras, photoCameras, smartphones, values));
        }
        return packets;
    }

    synchronized byte[] handle(CameraRequest request) throws IOException {
        String command = request.command();
        String[] args = request.arguments();
        try {
            switch (command) {
                case TcpProtocol.PING:
                    requireLength(args, 0);
                    return response(TcpProtocol.PONG);
                case TcpProtocol.LIST:
                    requireLength(args, 0);
                    return response(server.getCameras().toArray(Camera[]::new));
                case TcpProtocol.GET:
                    requireLength(args, 1);
                    List<Camera> cameras = server.getCameras();
                    return response(cameras.get(cameraIndex(cameras, args[0])));
                case TcpProtocol.READ:
                case TcpProtocol.LOAD:
                    return readFile(args, TcpProtocol.LOAD.equals(command));
                case TcpProtocol.REPLACE:
                    requireLength(args, 2);
                    replaceFile(args[0], args[1]);
                    return response(TcpProtocol.OK);
                case TcpProtocol.LOG:
                    requireLength(args, 2);
                    return response(logFile(args[0], args[1]));
                case TcpProtocol.ADD:
                    requireLength(args, 4);
                    addCamera(args[0], args[1], args[2], args[3]);
                    return response(TcpProtocol.OK);
                case TcpProtocol.EDIT:
                    requireLength(args, 4);
                    editCamera(args[0], args[1], args[2], args[3]);
                    return response(TcpProtocol.OK);
                case TcpProtocol.VIDEO:
                case TcpProtocol.ZOOM:
                    requireLength(args, 2);
                    changeCamera(command, args[0], args[1]);
                    return response(TcpProtocol.OK);
                case TcpProtocol.DELETE:
                case TcpProtocol.PHOTO:
                case TcpProtocol.MODE:
                case TcpProtocol.RANDOMIZE:
                case TcpProtocol.TOGGLE:
                case TcpProtocol.RESET:
                    requireLength(args, 1);
                    changeCamera(command, args[0], "");
                    return response(TcpProtocol.OK);
                default:
                    return error("Неизвестная команда.");
            }
        } catch (NumberFormatException e) {
            return error("Неверный формат числа.");
        } catch (IllegalArgumentException | IllegalStateException | IOException e) {
            return error(e.getMessage());
        }
    }

    private byte[] response(Serializable value) throws IOException {
        return TcpProtocol.serialize(new CameraResponse(value));
    }

    private byte[] error(String message) throws IOException {
        return TcpProtocol.serialize(CameraResponse.failure(message));
    }

    private byte[] readFile(String[] args, boolean load) throws IOException {
        if (args.length > 1) {
            throw new IllegalArgumentException("Неверное число аргументов.");
        }
        String name = args.length == 0 ? TcpProtocol.DEFAULT_FILE : args[0];
        FileReadResult result = server.readCameras(sourceFile(name).toString());
        byte[] response = response(result);
        if (load) {
            server.addCameras(result.getCams());
        }
        return response;
    }

    private Path dataFile(String name, String extension) {
        if (name.length() > MAX_FILE_NAME_LENGTH || !name.endsWith(extension)
                || name.contains("/") || name.contains("\\") || name.indexOf(0) >= 0) {
            throw new IllegalArgumentException("Укажите имя файла " + extension + " в папке данных сервера.");
        }
        Path file = dataDirectory.resolve(name).normalize();
        if (!dataDirectory.equals(file.getParent()) || Files.isSymbolicLink(file)) {
            throw new IllegalArgumentException("Этот файл нельзя использовать: " + name);
        }
        return file;
    }

    private Path sourceFile(String name) throws IOException {
        Path file = dataFile(name, ".txt");
        if (!Files.isRegularFile(file)) {
            throw new IOException("Файл данных не найден или не является файлом: " + name);
        }
        return file;
    }

    private void replaceFile(String sourceName, String targetName) throws IOException {
        Path source = sourceFile(sourceName);
        Path target = dataFile(targetName, ".txt");
        if (Files.exists(target)) {
            if (!Files.isRegularFile(target)) {
                throw new IOException("Путь не является файлом: " + targetName);
            }
            if (Files.isSameFile(source, target)) {
                throw new IOException("Для замены нужно указать два разных файла.");
            }
        }
        Path temp = Files.createTempFile(dataDirectory, "cameras-", ".tmp");
        try {
            Files.copy(source, temp, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private LogResult logFile(String sourceName, String csvName) throws IOException {
        Path source = sourceFile(sourceName);
        Path csv = dataFile(csvName, ".csv");
        if (Files.exists(csv) && Files.isSameFile(source, csv)) {
            throw new IOException("Файл данных и CSV-журнал должны быть разными файлами.");
        }
        FileReadResult result = server.readCameras(source.toString());
        CamerasCsvWriter journal = journals.get(csv);
        if (journal == null) {
            journal = new CamerasCsvWriter(csv.toString());
            journals.put(csv, journal);
        }
        int changes = journal.writeChanges(result.getCams());
        return new LogResult(changes, result.getObjectCount(), result.getPropertiesRead(),
                result.getPropertiesMissing(), result.getWarnings().toArray(String[]::new));
    }

    private void addCamera(String type, String model, String first, String second) {
        checkModel(model);
        switch (type) {
            case WebCamera.TYPE:
                server.addWebCamera(model, decimal(first, WebCamera.MIN_WIDTH, WebCamera.MAX_WIDTH),
                        decimal(second, WebCamera.MIN_FPS, WebCamera.MAX_FPS));
                break;
            case PhotoCamera.TYPE:
                server.addPhotoCamera(model, integer(first, PhotoCamera.MIN_ISO, PhotoCamera.MAX_ISO),
                        integer(second, PhotoCamera.MIN_BATTERY, PhotoCamera.MAX_BATTERY));
                break;
            case Smartphone.TYPE:
                server.addSmartphone(model, decimal(first, Smartphone.MIN_MEGAPIXELS, Smartphone.MAX_MEGAPIXELS),
                        decimal(second, 0, Double.MAX_VALUE));
                break;
            default:
                throw new IllegalArgumentException("Неизвестный тип устройства.");
        }
    }

    private void editCamera(String id, String model, String first, String second) {
        checkModel(model);
        List<Camera> cameras = server.getCameras();
        int index = cameraIndex(cameras, id);
        Camera camera = cameras.get(index);
        if (camera instanceof WebCamera) {
            server.editWebCamera(index, model, decimal(first, WebCamera.MIN_WIDTH, WebCamera.MAX_WIDTH),
                    decimal(second, WebCamera.MIN_FPS, WebCamera.MAX_FPS));
        } else if (camera instanceof PhotoCamera) {
            server.editPhotoCamera(index, model, integer(first, PhotoCamera.MIN_ISO, PhotoCamera.MAX_ISO),
                    integer(second, PhotoCamera.MIN_BATTERY, PhotoCamera.MAX_BATTERY));
        } else if (camera instanceof Smartphone) {
            server.editSmartphone(index, model, decimal(first, Smartphone.MIN_MEGAPIXELS, Smartphone.MAX_MEGAPIXELS),
                    decimal(second, 0, Double.MAX_VALUE));
        }
    }

    private void changeCamera(String command, String id, String value) {
        int index = cameraIndex(server.getCameras(), id);
        switch (command) {
            case TcpProtocol.DELETE -> server.deleteCamera(index);
            case TcpProtocol.PHOTO -> server.takePhoto(index);
            case TcpProtocol.VIDEO -> server.recordVideo(index, Integer.parseInt(value));
            case TcpProtocol.ZOOM -> server.zoom(index, Double.parseDouble(value));
            case TcpProtocol.MODE -> server.switchMode(index);
            case TcpProtocol.RANDOMIZE -> server.randomizeCharacteristic(index);
            case TcpProtocol.TOGGLE -> server.toggleSpecialAction(index);
            case TcpProtocol.RESET -> server.resetCamera(index);
        }
    }

    private int cameraIndex(List<Camera> cameras, String value) {
        int id = Integer.parseInt(value);
        for (int i = 0; i < cameras.size(); i++) {
            if (cameras.get(i).getId() == id) {
                return i;
            }
        }
        throw new IllegalArgumentException("Нет устройства с ID " + id + ".");
    }

    private void requireLength(String[] args, int length) {
        if (args.length != length) {
            throw new IllegalArgumentException("Неверное число аргументов.");
        }
    }

    private void checkModel(String model) {
        if (model.trim().isEmpty()) {
            throw new IllegalArgumentException("Название модели не может быть пустым.");
        }
    }

    private int integer(String value, int min, int max) {
        int number = Integer.parseInt(value);
        if (number < min || number > max) {
            throw new IllegalArgumentException("Значение должно быть от " + min + " до " + max + ".");
        }
        return number;
    }

    private double decimal(String value, double min, double max) {
        double number = Double.parseDouble(value);
        if (!Double.isFinite(number) || number < min || number > max) {
            throw new IllegalArgumentException("Значение должно быть от " + min + " до " + max + ".");
        }
        return number;
    }
}
