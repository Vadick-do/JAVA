package lab4;

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
            System.out.println("Ошибка: имя файла не может быть пустым. Установлено 'unnamed.jpg'.");
            this.fileName = "unnamed.jpg";
        } else {
            this.fileName = fileName.trim();
        }

        if (resolution == null || resolution.trim().isEmpty()) {
            System.out.println("Ошибка: разрешение не может быть пустым. Установлено '1920x1080'.");
            this.resolution = "1920x1080";
        } else {
            this.resolution = resolution.trim();
        }

        if (sizeMb <= 0) {
            System.out.println("Ошибка: размер изображения должен быть больше 0 МБ. Установлено 2.0 МБ.");
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
            System.out.println("Ошибка: имя файла не может быть пустым!");
            return false;
        }
        this.fileName = fileName.trim();
        return true;
    }

    public String getResolution() {
        return resolution;
    }

    public boolean setResolution(String resolution) {
        if (resolution == null || resolution.trim().isEmpty()) {
            System.out.println("Ошибка: разрешение не может быть пустым!");
            return false;
        }
        this.resolution = resolution.trim();
        return true;
    }

    public double getSizeMb() {
        return sizeMb;
    }

    public boolean setSizeMb(double sizeMb) {
        if (sizeMb <= 0) {
            System.out.println("Ошибка: размер изображения должен быть больше 0 МБ!");
            return false;
        }
        this.sizeMb = Math.round(sizeMb * 10.0) / 10.0;
        return true;
    }

    public String getFormat() {
        return format;
    }

    public boolean setFormat(String format) {
        if (format == null || format.trim().isEmpty()) {
            System.out.println("Ошибка: формат файла не может быть пустым!");
            return false;
        }
        this.format = format.trim().toUpperCase();
        return true;
    }

    public double getSizeKb() {
        return Math.round((this.sizeMb * 1024.0) * 10.0) / 10.0;
    }

    public void printInfo() {
        System.out.println("Снимок: " + fileName + " | Формат: " + format 
                + " | Разрешение: " + resolution + " | Размер: " + sizeMb + " МБ");
    }

    @Override
    public String toString() {
        return fileName + " (" + format + ", " + resolution + ", " + sizeMb + " МБ)";
    }
}
