package lab4;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CamerasFileReader {
    private final BufferedReader reader;
    private int propertiesRead = 0;
    private int propertiesMissing = 0;

    public CamerasFileReader(BufferedReader reader) {
        this.reader = reader;
    }

    public CamerasFileReader(Reader reader) {
        this.reader = (reader instanceof BufferedReader br) ? br : new BufferedReader(reader);
    }

    public CamerasFileReader(InputStream inputStream) {
        this.reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    }

    public FileReadResult readAll() throws IOException {
        List<Camera> cams = new ArrayList<>();
        propertiesRead = 0;
        propertiesMissing = 0;

        String currentType = null;
        Map<String, String> currentFields = new HashMap<>();

        String line;
        while ((line = reader.readLine()) != null) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            boolean isEndOfObject = trimmed.endsWith(";");
            if (isEndOfObject) {
                trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
            } else if (trimmed.endsWith(",")) {
                trimmed = trimmed.substring(0, trimmed.length() - 1).trim();
            }

            int colonIndex = trimmed.indexOf(":");
            if (colonIndex != -1) {
                String key = trimmed.substring(0, colonIndex).trim().toLowerCase();
                String value = trimmed.substring(colonIndex + 1).trim();

                if (key.equals("type")) {
                    if (currentType != null) {
                        Camera cam = createCamera(currentType, currentFields);
                        if (cam != null) {
                            cams.add(cam);
                        }
                        currentFields.clear();
                    }
                    currentType = value;
                } else if (currentType != null) {
                    currentFields.put(key, value);
                }
            }
            if (isEndOfObject && currentType != null) {
                Camera cam = createCamera(currentType, currentFields);
                if (cam != null) {
                    cams.add(cam);
                }
                currentType = null;
                currentFields.clear();
            }
        }
        if (currentType != null) {
            Camera cam = createCamera(currentType, currentFields);
            if (cam != null) {
                cams.add(cam);
            }
        }

        return new FileReadResult(cams, cams.size(), propertiesRead, propertiesMissing);
    }

    private Camera createCamera(String type, Map<String, String> fields) {
        String normalizedType = type.trim().toLowerCase();

        switch (normalizedType) {
            case "webcamera":
            case "веб-камера":
            case "вебкамера":
                return parseWebCamera(fields);
            
            case "photocamera":
            case "зеркалка":
            case "фотокамера":
                return parsePhotoCamera(fields);
            
            case "smartphone":
            case "смартфон":
            case "телефон":
                return parseSmartphone(fields);
            
            default:
                System.out.println("Предупреждение: неизвестный тип устройства '" + type + "', объект пропущен.");
                return null;
        }
    }

    private WebCamera parseWebCamera(Map<String, String> fields) {
        String model = "Стандартная камера";
        double width = 1280.0;
        double fps = 30.0;
        boolean micro = true;
        double zoom = 1.0;

        if (fields.containsKey("model") && !fields.get("model").trim().isEmpty()) {
            model = fields.get("model").trim();
            propertiesRead++;
        } else {
            propertiesMissing++;
        }

        if (fields.containsKey("width")) {
            try {
                double val = Double.parseDouble(fields.get("width").trim().replace(',', '.'));
                if (val >= 320.0 && val <= 3840.0) {
                    width = val;
                    propertiesRead++;
                } else {
                    System.out.println("Ошибка валидации: ширина должна быть от 320 до 3840 (" + val + "). Взято: 1280.0");
                    propertiesMissing++;
                }
            } catch (NumberFormatException e) {
                System.out.println("Ошибка валидации: неверный формат ширины. Взято: 1280.0");
                propertiesMissing++;
            }
        } else {
            propertiesMissing++;
        }

        if (fields.containsKey("fps")) {
            try {
                double val = Double.parseDouble(fields.get("fps").trim().replace(',', '.'));
                if (val >= 1.0 && val <= 120.0) {
                    fps = val;
                    propertiesRead++;
                } else {
                    System.out.println("Ошибка валидации: FPS должен быть от 1 до 120 (" + val + "). Взято: 30.0");
                    propertiesMissing++;
                }
            } catch (NumberFormatException e) {
                System.out.println("Ошибка валидации: неверный формат FPS. Взято: 30.0");
                propertiesMissing++;
            }
        } else {
            propertiesMissing++;
        }

        String microKey = fields.containsKey("microphone") ? "microphone" : (fields.containsKey("micro") ? "micro" : null);
        if (microKey != null) {
            Boolean val = parseBooleanSafe(fields.get(microKey));
            if (val != null) {
                micro = val;
                propertiesRead++;
            } else {
                System.out.println("Ошибка валидации: неверный формат микрофона. Взято: true");
                propertiesMissing++;
            }
        } else {
            propertiesMissing++;
        }

        if (fields.containsKey("zoom")) {
            try {
                double val = Double.parseDouble(fields.get("zoom").trim().replace(',', '.'));
                if (val >= 1.0 && val <= 4.0) {
                    zoom = val;
                    propertiesRead++;
                } else {
                    System.out.println("Ошибка валидации: зум веб-камеры должен быть от 1.0 до 4.0 (" + val + "). Взято: 1.0");
                    propertiesMissing++;
                }
            } catch (NumberFormatException e) {
                System.out.println("Ошибка валидации: неверный формат зума. Взято: 1.0");
                propertiesMissing++;
            }
        } else {
            propertiesMissing++;
        }
        return new WebCamera(model, width, fps, micro, zoom);
    }
    private PhotoCamera parsePhotoCamera(Map<String, String> fields) {
        String model = "Sony ZV-1";
        int iso = 100;
        int battery = 100;
        boolean flash = false;
        double zoom = 1.0;

        if (fields.containsKey("model") && !fields.get("model").trim().isEmpty()) {
            model = fields.get("model").trim();
            propertiesRead++;
        } else {
            propertiesMissing++;
        }

        if (fields.containsKey("iso")) {
            try {
                int val = Integer.parseInt(fields.get("iso").trim());
                if (val >= 100 && val <= 6400) {
                    iso = val;
                    propertiesRead++;
                } else {
                    System.out.println("Ошибка валидации: ISO должно быть от 100 до 6400 (" + val + "). Взято: 100");
                    propertiesMissing++;
                }
            } catch (NumberFormatException e) {
                System.out.println("Ошибка валидации: неверный формат ISO. Взято: 100");
                propertiesMissing++;
            }
        } else {
            propertiesMissing++;
        }
   
        if (fields.containsKey("battery")) {
            try {
                int val = Integer.parseInt(fields.get("battery").trim());
                if (val >= 0 && val <= 100) {
                    battery = val;
                    propertiesRead++;
                } else {
                    System.out.println("Ошибка валидации: заряд батареи должен быть от 0 до 100 (" + val + "). Взято: 100");
                    propertiesMissing++;
                }
            } catch (NumberFormatException e) {
                System.out.println("Ошибка валидации: неверный формат батареи. Взято: 100");
                propertiesMissing++;
            }
        } else {
            propertiesMissing++;
        }

        if (fields.containsKey("flash")) {
            Boolean val = parseBooleanSafe(fields.get("flash"));
            if (val != null) {
                flash = val;
                propertiesRead++;
            } else {
                System.out.println("Ошибка валидации: неверный формат вспышки. Взято: false");
                propertiesMissing++;
            }
        } else {
            propertiesMissing++;
        }

        if (fields.containsKey("zoom")) {
            try {
                double val = Double.parseDouble(fields.get("zoom").trim().replace(',', '.'));
                if (val >= 1.0 && val <= 10.0) {
                    zoom = val;
                    propertiesRead++;
                } else {
                    System.out.println("Ошибка валидации: зум зеркалки должен быть от 1.0 до 10.0 (" + val + "). Взято: 1.0");
                    propertiesMissing++;
                }
            } catch (NumberFormatException e) {
                System.out.println("Ошибка валидации: неверный формат зума. Взято: 1.0");
                propertiesMissing++;
            }
        } else {
            propertiesMissing++;
        }
        return new PhotoCamera(model, iso, battery, flash, zoom);
    }
    private Smartphone parseSmartphone(Map<String, String> fields) {
        String model = "Iphone 11";
        double megapixels = 48.0;
        double storage = 8192.0;
        boolean nightMode = false;
        double zoom = 1.0;

        if (fields.containsKey("model") && !fields.get("model").trim().isEmpty()) {
            model = fields.get("model").trim();
            propertiesRead++;
        } else {
            propertiesMissing++;
        }

        if (fields.containsKey("megapixels")) {
            try {
                double val = Double.parseDouble(fields.get("megapixels").trim().replace(',', '.'));
                if (val >= 2.0 && val <= 200.0) {
                    megapixels = val;
                    propertiesRead++;
                } else {
                    System.out.println("Ошибка валидации: мегапиксели должны быть от 2 до 200 (" + val + "). Взято: 48.0");
                    propertiesMissing++;
                }
            } catch (NumberFormatException e) {
                System.out.println("Ошибка валидации: неверный формат мегапикселей. Взято: 48.0");
                propertiesMissing++;
            }
        } else {
            propertiesMissing++;
        }

        if (fields.containsKey("storage")) {
            try {
                double val = Double.parseDouble(fields.get("storage").trim().replace(',', '.'));
                if (val >= 0.0) {
                    storage = val;
                    propertiesRead++;
                } else {
                    System.out.println("Ошибка валидации: свободная память не может быть меньше 0 (" + val + "). Взято: 8192.0");
                    propertiesMissing++;
                }
            } catch (NumberFormatException e) {
                System.out.println("Ошибка валидации: неверный формат памяти. Взято: 8192.0");
                propertiesMissing++;
            }
        } else {
            propertiesMissing++;
        }

        String nightKey = fields.containsKey("nightmode") ? "nightmode" : (fields.containsKey("night_mode") ? "night_mode" : null);
        if (nightKey != null) {
            Boolean val = parseBooleanSafe(fields.get(nightKey));
            if (val != null) {
                nightMode = val;
                propertiesRead++;
            } else {
                System.out.println("Ошибка валидации: неверный формат ночного режима. Взято: false");
                propertiesMissing++;
            }
        } else {
            propertiesMissing++;
        }

        if (fields.containsKey("zoom")) {
            try {
                double val = Double.parseDouble(fields.get("zoom").trim().replace(',', '.'));
                if (val >= 0.5 && val <= 50.0) {
                    zoom = val;
                    propertiesRead++;
                } else {
                    System.out.println("Ошибка валидации: зум смартфона должен быть от 0.5 до 50.0 (" + val + "). Взято: 1.0");
                    propertiesMissing++;
                }
            } catch (NumberFormatException e) {
                System.out.println("Ошибка валидации: неверный формат зума. Взято: 1.0");
                propertiesMissing++;
            }
        } else {
            propertiesMissing++;
        }
        return new Smartphone(model, megapixels, storage, nightMode, zoom);
    }
    
    private Boolean parseBooleanSafe(String val) {
        if (val == null) {
            return null;
        }
        String s = val.trim().toLowerCase();
        if (s.equals("true") || s.equals("да") || s.equals("1") || s.equals("вкл")) {
            return true;
        }
        if (s.equals("false") || s.equals("нет") || s.equals("0") || s.equals("выкл")) {
            return false;
        }
        return null;
    }
}
