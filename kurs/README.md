# Бронирование ресурсов

Для сборки и запуска нужны Docker с Compose и Make. На macOS сначала запустите Docker Desktop и дождитесь готовности Docker Engine. Java и Maven на хосте не нужны. Первая сборка скачивает базовые образы и зависимости Maven.

Из папки `kurs` соберите приложение и запустите unit-тесты:

```sh
make build
make test
```

- `make build` проверяет проект Maven на JDK 21 и собирает образ приложения с JRE 21.
- `make test` запускает unit-тесты в отдельном контейнере без PostgreSQL. Maven берёт зависимости из образа сборки.
- `make test-integration` проверяет миграции и ограничения в отдельном проекте Compose с временной PostgreSQL. После прогона тестовый том удаляется.

## Запуск с PostgreSQL

Один раз создайте локальные файлы настроек:

```sh
cp -n .env.example .env
cp -n application.properties.example application.properties
```

Впишите свой пароль после `DB_PASSWORD=` в `.env`. PostgreSQL применяет имя базы, пользователя и пароль при инициализации пустого тома. При повторном запуске используйте параметры уже созданной базы.

```sh
make up
make run
make logs
make down
```

- `make up` запускает PostgreSQL и контейнер `socat`. Java обращается к `postgres:5432` внутри Compose. `socat` пробрасывает порт для локального pgAdmin 4.
- `make run` собирает образ при изменении исходников, запускает Java с внешним файлом и применяет миграции Flyway. При повторном запуске Flyway проверяет историю и применяет только новые миграции. Приложение выводит число применённых миграций и завершается.
- `make logs` показывает журналы PostgreSQL и `socat`. `make down` останавливает контейнеры с сохранением тома `booking-coursework_postgres-data`.

## Подключение из pgAdmin 4

После `make up` в локальном pgAdmin 4 выберите **Register → Server**. На вкладке **General** задайте произвольное имя, на вкладке **Connection** заполните поля.

| Поле | Значение по умолчанию |
| --- | --- |
| Host name/address | `127.0.0.1` |
| Port | `15432` |
| Maintenance database | `booking_coursework` |
| Username | `booking` |
| Password | значение `DB_PASSWORD` из `.env` |

Если меняли `DB_NAME` или `DB_USER`, укажите параметры созданной базы. Если порт `15432` занят, задайте свободный `POSTGRES_HOST_PORT` в `.env`, снова выполните `make up` и укажите этот порт в pgAdmin 4. Публикация ограничена локальным интерфейсом. После `make run` таблицы `resources`, `slots`, `bookings` и `flyway_schema_history` появятся в `Schemas → public → Tables`.

## Настройки Java

Compose подключает `application.properties` для чтения и передаёт Java путь к нему через `--config`. Другой файл можно выбрать командой `make run CONFIG_FILE=./другой.properties`.

Переменная окружения имеет приоритет над соответствующим свойством файла. Если порт не задан ни там, ни там, Java использует `5432`. Остальные значения обязательны.

| Свойство | Переменная |
| --- | --- |
| `db.host` | `DB_HOST` |
| `db.port` | `DB_PORT` |
| `db.name` | `DB_NAME` |
| `db.user` | `DB_USER` |
| `db.password` | `DB_PASSWORD` |

## Модели

Модели `Resource`, `ResourceType` и `Slot` находятся в `core/domain`. Ресурс имеет непустое название до 100 символов после удаления пробелов по краям и положительную вместимость. Слот начинается на границе часа UTC и длится ровно 60 минут.

В том же пакете `Booking` и `BookingStatus` описывают десятиминутный холд, подтверждение, отмену и истечение. Модель получает текущее время явно. Переходы проверяются unit-тестами.
