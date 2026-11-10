package lab6.server;

import lab6.model.Camera;
import lab6.model.FileReadResult;
import lab6.model.PhotoCamera;
import lab6.model.Smartphone;
import lab6.model.WebCamera;
import lab6.net.TcpProtocol;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

class CameraRequests {
    private static final String DEFAULT_FILE = "devices.txt";
    private static final int MAX_FILE_NAME_LENGTH = 255;
    private final CameraServer server = new CameraServer();
    private final Path dataDirectory;

    CameraRequests(Path dataDirectory) {
        this.dataDirectory = dataDirectory.toAbsolutePath().normalize();
    }

    void loadDefaultFile() throws IOException {
        server.loadCameras(sourceFile(DEFAULT_FILE).toString());
    }

    String handle(String request) {
        String[] args = request.split("\n", 6);
        try {
            switch (args[0]) {
                case TcpProtocol.PING:
                    requireLength(args, 1);
                    return TcpProtocol.PONG;
                case "LIST":
                    requireLength(args, 1);
                    return CameraResponse.cameras(server.getCameras());
                case "GET":
                    requireLength(args, 2);
                    List<Camera> cameras = server.getCameras();
                    return CameraResponse.cameras(List.of(cameras.get(cameraIndex(cameras, args[1]))));
                case "READ":
                case "LOAD":
                    return readFile(args, args[0].equals("LOAD"));
                case "REPLACE":
                    requireLength(args, 3);
                    replaceFile(args[1], args[2]);
                    return TcpProtocol.OK;
                case "ADD":
                    requireLength(args, 5);
                    addCamera(args[1], args[2], args[3], args[4]);
                    return TcpProtocol.OK;
                case "EDIT":
                    requireLength(args, 5);
                    editCamera(args[1], args[2], args[3], args[4]);
                    return TcpProtocol.OK;
                case "VIDEO":
                case "ZOOM":
                    requireLength(args, 3);
                    changeCamera(args[0], args[1], args[2]);
                    return TcpProtocol.OK;
                case "DELETE":
                case "PHOTO":
                case "MODE":
                case "RANDOMIZE":
                case "TOGGLE":
                case "RESET":
                    requireLength(args, 2);
                    changeCamera(args[0], args[1], "");
                    return TcpProtocol.OK;
                default:
                    return TcpProtocol.UNKNOWN_COMMAND;
            }
        } catch (NumberFormatException e) {
            return TcpProtocol.ERROR + "Неверный формат числа.";
        } catch (IllegalArgumentException | IllegalStateException | IOException e) {
            return TcpProtocol.ERROR + e.getMessage();
        }
    }

    private String readFile(String[] args, boolean load) throws IOException {
        if (args.length > 2) {
            throw new IllegalArgumentException("Неверное число аргументов.");
        }
        String name = args.length == 1 ? DEFAULT_FILE : args[1];
        FileReadResult result = server.readCameras(sourceFile(name).toString());
        String response = CameraResponse.report(result);
        if (load) {
            server.addCameras(result.getCams());
        }
        return response;
    }

    private Path dataFile(String name) {
        if (name.length() > MAX_FILE_NAME_LENGTH || !name.endsWith(".txt")
                || name.contains("/") || name.contains("\\") || name.indexOf(0) >= 0) {
            throw new IllegalArgumentException("Укажите имя файла .txt в папке данных сервера.");
        }
        Path file = dataDirectory.resolve(name).normalize();
        if (!dataDirectory.equals(file.getParent()) || Files.isSymbolicLink(file)) {
            throw new IllegalArgumentException("Этот файл нельзя использовать: " + name);
        }
        return file;
    }

    private Path sourceFile(String name) throws IOException {
        Path file = dataFile(name);
        if (!Files.isRegularFile(file)) {
            throw new IOException("Файл данных не найден или не является файлом: " + name);
        }
        return file;
    }

    private void replaceFile(String sourceName, String targetName) throws IOException {
        Path source = sourceFile(sourceName);
        Path target = dataFile(targetName);
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
            case "DELETE" -> server.deleteCamera(index);
            case "PHOTO" -> server.takePhoto(index);
            case "VIDEO" -> server.recordVideo(index, Integer.parseInt(value));
            case "ZOOM" -> server.zoom(index, Double.parseDouble(value));
            case "MODE" -> server.switchMode(index);
            case "RANDOMIZE" -> server.randomizeCharacteristic(index);
            case "TOGGLE" -> server.toggleSpecialAction(index);
            case "RESET" -> server.resetCamera(index);
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
