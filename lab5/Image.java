package lab5;

public class Image {
    private String fileName;
    private String resolution;
    private double sizeMb;
    private String format;

    public Image() {
        this("image.jpg", "1920x1080", 2.0, "JPG");
    }

    public Image(String fileName) {
        this(fileName, "1920x1080", 2.0, extractFormatFromFileName(fileName));
    }

    public Image(String fileName, String resolution, double sizeMb, String format) {
        if (fileName == null || fileName.trim().isEmpty()) {
            this.fileName = "unnamed.jpg";
        } else {
            this.fileName = fileName.trim();
        }

        if (resolution == null || resolution.trim().isEmpty()) {
            this.resolution = "1920x1080";
        } else {
            this.resolution = resolution.trim();
        }

        if (!Double.isFinite(sizeMb) || sizeMb <= 0) {
            this.sizeMb = 2.0;
        } else {
            this.sizeMb = Math.round(sizeMb * 10.0) / 10.0;
        }

        if (format == null || format.trim().isEmpty()) {
            this.format = extractFormatFromFileName(this.fileName);
        } else {
            this.format = format.trim().toUpperCase();
        }
    }

    public Image(String fileName, int width, int height, double sizeMb, String format) {
        this(fileName, (width > 0 && height > 0) ? (width + "x" + height) : "1920x1080", sizeMb, format);
    }

    private static String extractFormatFromFileName(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex < fileName.length() - 1) {
                return fileName.substring(dotIndex + 1).toUpperCase();
            }
        }
        return "JPG";
    }

    public String getFileName() {
        return fileName;
    }

    public String getName() {
        return fileName;
    }

    public boolean setFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("Ошибка: имя файла не может быть пустым!");
        }
        this.fileName = fileName.trim();
        return true;
    }

    public String getResolution() {
        return resolution;
    }

    public boolean setResolution(String resolution) {
        if (resolution == null || resolution.trim().isEmpty()) {
            throw new IllegalArgumentException("Ошибка: разрешение не может быть пустым!");
        }
        this.resolution = resolution.trim();
        return true;
    }

    public double getSizeMb() {
        return sizeMb;
    }

    public boolean setSizeMb(double sizeMb) {
        if (!Double.isFinite(sizeMb) || sizeMb <= 0) {
            throw new IllegalArgumentException("Ошибка: размер изображения должен быть больше 0 МБ!");
        }
        this.sizeMb = Math.round(sizeMb * 10.0) / 10.0;
        return true;
    }

    public String getFormat() {
        return format;
    }

    public boolean setFormat(String format) {
        if (format == null || format.trim().isEmpty()) {
            throw new IllegalArgumentException("Ошибка: формат файла не может быть пустым!");
        }
        this.format = format.trim().toUpperCase();
        return true;
    }

    public double getSizeKb() {
        return Math.round((this.sizeMb * 1024.0) * 10.0) / 10.0;
    }

    @Override
    public String toString() {
        return fileName + " (" + format + ", " + resolution + ", " + sizeMb + " МБ)";
    }
}
