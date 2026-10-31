package lab5;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ConsoleView {
    private final Scanner scanner;

    public ConsoleView(Scanner scanner) {
        this.scanner = scanner;
    }

    public void showMessage(String message) {
        System.out.println(message);
    }

    public void showPrompt(String message) {
        System.out.print(message);
    }

    public String readLine() {
        return scanner.nextLine().trim();
    }

    public void showHelp(String dataFileName) {
        System.out.println("Использование: java -jar build/lab5/lab5.jar <режим> [параметры]");
        System.out.println("Режимы работы:");
        System.out.println("  --help, -h                   Показать справку");
        System.out.println("  --interactive                Работа через меню, как в ЛР4");
        System.out.println("  --show                       Один раз вывести состояние устройств из файла");
        System.out.println("  --watch                      Периодически читать файл и выводить состояние устройств");
        System.out.println("  --log <файл.csv>             Периодически читать файл и записывать изменения в CSV");
        System.out.println("  --replace <источник> <цель>   Заменить содержимое цели содержимым источника");
        System.out.println("Параметры:");
        System.out.println("  --file <имя>                 Файл данных для --show, --watch, --log или --interactive");
        System.out.println("  --interval <секунды>         Период для --watch и --log, по умолчанию 5");
        System.out.println("Файл по умолчанию: " + dataFileName);
        System.out.println("При --replace исходный файл сохраняется. Если цели нет, она создается.");
        System.out.println("В CSV записывается начальное состояние, затем только изменившиеся значения.");
        System.out.println("Столбцы CSV: time,number,property,value. Номер устройства соответствует порядку в файле.");
        System.out.println("Время в CSV — время обнаружения изменения. Существующий журнал дополняется.");
        System.out.println("Без ключей выводится справка.");
    }

    public void showLogStart(String fileName, String csvFileName, int interval) {
        System.out.println("\nCSV-журнал: " + csvFileName);
        showWatchStart(fileName, interval);
    }

    public void showWatchStart(String fileName, int interval) {
        System.out.println("\nПериодическое чтение файла: " + fileName);
        System.out.println("Интервал: " + interval + " сек.");
        System.out.println("При ошибке чтения попытка повторится через указанный интервал.");
        System.out.println("Для завершения нажмите Ctrl+C.");
    }

    public void showMenu(String dataFileName) {
        System.out.println("\n--- МЕНЮ ---");
        System.out.println("0. Выход");
        System.out.println("1. Просмотр всех устройств");
        System.out.println("2. Добавить устройство");
        System.out.println("3. Изменить свойства устройства");
        System.out.println("4. Удалить устройство");
        System.out.println("5. Функциональная работа с устройством");
        System.out.println("6. Сделать фото на все устройства сразу");
        System.out.println("7. Прочитать данные из файла (текущий: " + dataFileName + ")");
        System.out.println("8. Изменить название файла с данными");

        System.out.print("Выберите действие: ");
    }

    public void showDeviceMenu(Camera cam) {
        String randomCharText = "Изменить случайную характеристику";
        String specialActionText = "Специальное действие устройства";

        if (cam instanceof WebCamera wc) {
            randomCharText = "Изменить случайную характеристику (FPS)";
            specialActionText = "Включить/выключить микрофон (сейчас: " + (wc.isMicrophoneEnabled() ? "вкл" : "выкл") + ")";
        } else if (cam instanceof PhotoCamera pc) {
            randomCharText = "Изменить случайную характеристику (ISO)";
            specialActionText = "Включить/выключить вспышку (сейчас: " + (pc.isFlashActive() ? "вкл" : "выкл") + ")";
        } else if (cam instanceof Smartphone sp) {
            randomCharText = "Изменить случайную характеристику (свободная память)";
            specialActionText = "Включить/выключить ночной режим (сейчас: " + (sp.isNightMode() ? "вкл" : "выкл") + ")";
        }

        System.out.println("\n--- Работа с устройством [" + cam.getModel() + "] ---");
        System.out.println("0. Назад в главное меню");
        System.out.println("1. Сделать фото");
        System.out.println("2. Записать видео");
        System.out.println("3. Изменить зум (текущий: " + cam.getCurrentZoom() + "x)");
        System.out.println("4. Переключить режим (фото <-> видео) [текущий: " + cam.getMode() + "]");
        System.out.println("5. " + randomCharText);
        System.out.println("6. " + specialActionText);
        System.out.println("7. Посмотреть сделанные снимки");
        if (cam instanceof PhotoCamera) {
            System.out.println("8. Зарядить батарею (до 100%)");
        } else if (cam instanceof Smartphone) {
            System.out.println("8. Очистить память (до 8192 МБ)");
        } else if (cam instanceof WebCamera) {
            System.out.println("8. Рассчитать мегапиксели веб-камеры");
        }
        System.out.print("Ваш выбор: ");
    }

    public void printCameras(List<Camera> cams) {
        if (cams.isEmpty()) {
            System.out.println("Список устройств пуст.");
            return;
        }

        System.out.println("\nСписок зарегистрированных устройств:");
        for (int i = 0; i < cams.size(); i++) {
            Camera cam = cams.get(i);
            System.out.print((i + 1) + ". [ID: " + cam.getId() + "] ");

            if (cam instanceof WebCamera wc) {
                System.out.println("[Веб-камера] " + wc.getModel()
                        + " | Разрешение: " + wc.getWidth() + " px | FPS: " + wc.getFPS()
                        + " | Зум: " + wc.getCurrentZoom() + "x"
                        + " | Микрофон: " + (wc.isMicrophoneEnabled() ? "вкл" : "выкл")
                        + " | Режим: " + wc.getMode() + " | Снимков: " + wc.getImages().size());
            } else if (cam instanceof PhotoCamera pc) {
                System.out.println("[Зеркалка] " + pc.getModel()
                        + " | ISO: " + pc.getIso() + " | Батарея: " + pc.getBattery() + "%"
                        + " | Зум: " + pc.getCurrentZoom() + "x"
                        + " | Вспышка: " + (pc.isFlashActive() ? "вкл" : "выкл")
                        + " | Режим: " + pc.getMode() + " | Снимков: " + pc.getImages().size());
            } else if (cam instanceof Smartphone sp) {
                System.out.println("[Смартфон] " + sp.getModel()
                        + " | Матрица: " + sp.getMegapixels() + " Мп | Память: " + sp.getStorage() + " МБ"
                        + " | Зум: " + sp.getCurrentZoom() + "x"
                        + " | Ночной режим: " + (sp.isNightMode() ? "вкл" : "выкл")
                        + " | Режим: " + sp.getMode() + " | Снимков: " + sp.getImages().size());
            }

            if (!cam.getImages().isEmpty()) {
                ArrayList<String> fileNames = new ArrayList<>();
                for (Image img : cam.getImages()) {
                    fileNames.add(img.getFileName());
                }
                System.out.println("   Файлы: " + String.join(", ", fileNames));
            }
        }
    }

    public void printImages(Camera cam) {
        List<Image> images = cam.getImages();
        if (images.isEmpty()) {
            System.out.println("Список снимков пуст.");
            return;
        }
        System.out.println("Список сохраненных снимков (" + images.size() + " шт.):");
        for (int i = 0; i < images.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + images.get(i));
        }
    }

    public void printReport(FileReadResult result) {
        for (String warning : result.getWarnings()) {
            showMessage(warning);
        }
        System.out.println("\n========== ОТЧЕТ О ЧТЕНИИ ФАЙЛА ==========");
        System.out.println("Найдено и воссоздано объектов: " + result.getObjectCount());
        System.out.println("Успешно прочитано свойств:      " + result.getPropertiesRead());
        System.out.println("Не найдено (пропущено) свойств: " + result.getPropertiesMissing());
        System.out.println("==========================================");
    }

    public String readString(String msg) {
        while (true) {
            System.out.print(msg);
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) {
                return input;
            }
            System.out.println("Ошибка! Поле не может быть пустым.");
        }
    }

    public String readString(String msg, String defaultValue) {
        System.out.print(msg);
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            return defaultValue;
        }
        return input;
    }

    public int readInt(String msg, int min, int max) {
        while (true) {
            System.out.print(msg);
            try {
                int val = Integer.parseInt(scanner.nextLine().trim());
                if (val >= min && val <= max) {
                    return val;
                }
                System.out.println("Значение должно быть от " + min + " до " + max + "!");
            } catch (NumberFormatException e) {
                System.out.println("Ошибка! Введите целое число.");
            }
        }
    }

    public int readInt(String msg, int min, int max, int defaultValue) {
        while (true) {
            System.out.print(msg);
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return defaultValue;
            }
            try {
                int val = Integer.parseInt(input);
                if (val >= min && val <= max) {
                    return val;
                }
                System.out.println("Значение должно быть от " + min + " до " + max + "!");
            } catch (NumberFormatException e) {
                System.out.println("Ошибка! Введите целое число.");
            }
        }
    }

    public double readDouble(String msg, double min, double max) {
        while (true) {
            System.out.print(msg);
            try {
                double val = Double.parseDouble(scanner.nextLine().trim().replace(',', '.'));
                if (Double.isFinite(val) && val >= min && val <= max) {
                    return val;
                }
                System.out.println("Значение должно быть от " + min + " до " + max + "!");
            } catch (NumberFormatException e) {
                System.out.println("Ошибка! Введите число.");
            }
        }
    }

    public double readDouble(String msg, double min, double max, double defaultValue) {
        while (true) {
            System.out.print(msg);
            String input = scanner.nextLine().trim().replace(',', '.');
            if (input.isEmpty()) {
                return defaultValue;
            }
            try {
                double val = Double.parseDouble(input);
                if (Double.isFinite(val) && val >= min && val <= max) {
                    return val;
                }
                System.out.println("Значение должно быть от " + min + " до " + max + "!");
            } catch (NumberFormatException e) {
                System.out.println("Ошибка! Введите число.");
            }
        }
    }

    public void showPhotoTaken(Camera cam) {
        Image image = cam.getImages().get(cam.getImages().size() - 1);
        if (cam instanceof WebCamera wc) {
            System.out.println("Веб-камера [" + wc.getModel() + "] сделала снимок "
                    + image.getResolution() + " (зум: " + wc.getCurrentZoom()
                    + "x). Сохранено в: " + image.getFileName());
        } else if (cam instanceof PhotoCamera pc) {
            System.out.println("Зеркалка [" + pc.getModel() + "] сделала фото. ISO: " + pc.getIso()
                    + ", Вспышка: " + (pc.isFlashActive() ? "сработала" : "выкл")
                    + ", Батарея: " + pc.getBattery() + "%. Сохранено: " + image.getFileName());
        } else if (cam instanceof Smartphone sp) {
            System.out.println("Смартфон [" + sp.getModel() + "] сохранил фото " + sp.getMegapixels() + " Мп."
                    + " Ночной режим: " + (sp.isNightMode() ? "вкл" : "выкл")
                    + ", Остаток памяти: " + sp.getStorage() + " МБ. Файл: " + image.getFileName());
        }
    }

    public void showVideoRecorded(Camera cam, int seconds) {
        if (cam instanceof WebCamera wc) {
            System.out.println("Веб-камера ведет запись: " + seconds + " сек. при " + wc.getFPS() + " FPS."
                    + " Микрофон: " + (wc.isMicrophoneEnabled() ? "включен" : "выключен"));
        } else if (cam instanceof PhotoCamera pc) {
            System.out.println("Зеркалка записала видео " + seconds + " сек. Батарея: " + pc.getBattery() + "%");
        } else if (cam instanceof Smartphone sp) {
            System.out.println("Смартфон записал видео (" + seconds + " сек). Свободно памяти: " + sp.getStorage() + " МБ");
        }
    }

    public void showZoomChanged(Camera cam) {
        if (cam instanceof WebCamera) {
            System.out.println("Цифровой зум веб-камеры установлен на " + cam.getCurrentZoom() + "x");
        } else if (cam instanceof PhotoCamera) {
            System.out.println("Кольцо объектива повернуто. Оптический зум: " + cam.getCurrentZoom() + "x");
        } else if (cam instanceof Smartphone) {
            System.out.println("Камера смартфона переключена на зум " + cam.getCurrentZoom() + "x");
        }
    }

    public void showModeChanged(Camera cam) {
        System.out.println("Режим изменен на: " + cam.getMode());
    }

    public void showRandomCharacteristic(Camera cam) {
        if (cam instanceof WebCamera wc) {
            System.out.println("FPS веб-камеры случайно изменен: " + wc.getFPS());
        } else if (cam instanceof PhotoCamera pc) {
            System.out.println("Новое случайное ISO: " + pc.getIso());
        } else if (cam instanceof Smartphone sp) {
            System.out.println("Память смартфона случайно изменена: " + sp.getStorage() + " МБ");
        }
    }

    public void showSpecialAction(Camera cam) {
        if (cam instanceof WebCamera wc) {
            System.out.println("Микрофон веб-камеры теперь: " + (wc.isMicrophoneEnabled() ? "включен" : "выключен"));
        } else if (cam instanceof PhotoCamera pc) {
            System.out.println("Вспышка зеркалки теперь: " + (pc.isFlashActive() ? "вкл" : "выкл"));
        } else if (cam instanceof Smartphone sp) {
            System.out.println("Ночной режим смартфона: " + (sp.isNightMode() ? "вкл" : "выкл"));
        }
    }

    public void showDeviceReset(Camera cam) {
        if (cam instanceof PhotoCamera pc) {
            System.out.println("Батарея зеркалки [" + pc.getModel() + "] заряжена на 100%.");
        } else if (cam instanceof Smartphone sp) {
            System.out.println("Память смартфона [" + sp.getModel() + "] очищена (свободно 8192.0 МБ).");
        } else if (cam instanceof WebCamera wc) {
            System.out.printf("Разрешение веб-камеры: %.2f Мп (при ширине %.0f px)\n", wc.calculateMegapixels(), wc.getWidth());
        }
    }
}
