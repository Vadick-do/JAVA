# Бронирование ресурсов

Для сборки и запуска нужны Docker с Compose и Make. На macOS сначала запустите Docker Desktop и дождитесь готовности Docker Engine. Java и Maven на хосте не нужны. Первая сборка скачивает базовые образы и зависимости Maven.

Из папки `kurs` соберите приложение и запустите unit-тесты:

```sh
make build
make test
```

- `make build` проверяет проект Maven на JDK 21 и собирает образ приложения с JRE 21.
- `make test` запускает unit-тесты в отдельном контейнере без PostgreSQL. Maven берёт зависимости из образа сборки.
- `make test-integration` проверяет миграции, ограничения, сервисы и HTTP API на настоящей PostgreSQL. Тесты холдов проверяют серии из 2 и 16 попыток на одном слоте, успешную бронь другого ресурса при удерживаемой блокировке и работу после конфликта. Для каждого класса `*IT.java` создаётся временная PostgreSQL в отдельном проекте Compose. После проверки класса тестовый том удаляется, поэтому наборы не зависят от данных друг друга.

## Запуск с PostgreSQL

Один раз создайте локальные файлы настроек:

```sh
cp -n .env.example .env
cp -n application.properties.example application.properties
```

Впишите свой пароль после `DB_PASSWORD=` в `.env`. PostgreSQL применяет имя базы, пользователя и пароль при инициализации пустого тома. При повторном запуске используйте параметры уже созданной базы.

```sh
make up
curl -i http://127.0.0.1:8080/health
curl -i http://127.0.0.1:8080/api/v1/resources
make logs
make down
```

- `make up` собирает образ при изменении исходников, запускает PostgreSQL, `socat` и HTTP-сервер в фоне. До приёма запросов сервер читает внешний файл и применяет миграции Flyway. При повторном запуске Flyway проверяет историю и применяет только новые миграции. Java обращается к `postgres:5432` внутри Compose. После команды дождитесь в `make logs` сообщения «HTTP-сервер запущен» и проверьте `/health`.
- `make run` запускает сервер на переднем плане с публикацией HTTP-порта. Используйте вместо `make up`, когда сервер `app` уже остановлен: `docker compose stop app`, затем `make run`. Нажатие Ctrl+C завершает сервер.
- `make logs` показывает журналы приложения, PostgreSQL и `socat`. Базовые события старта, остановки и ошибок HTTP пока выводятся в консоль. `make down` останавливает контейнеры с сохранением тома `booking-coursework_postgres-data`.

## Подключение из pgAdmin 4

После `make up` в локальном pgAdmin 4 выберите **Register → Server**. На вкладке **General** задайте произвольное имя, на вкладке **Connection** заполните поля.

| Поле | Значение по умолчанию |
| --- | --- |
| Host name/address | `127.0.0.1` |
| Port | `15432` |
| Maintenance database | `booking_coursework` |
| Username | `booking` |
| Password | значение `DB_PASSWORD` из `.env` |

Если меняли `DB_NAME` или `DB_USER`, укажите параметры созданной базы. Если порт `15432` занят, задайте свободный `POSTGRES_HOST_PORT` в `.env`, снова выполните `make up` и укажите этот порт в pgAdmin 4. Публикация ограничена локальным интерфейсом. После старта сервера таблицы `resources`, `slots`, `bookings` и `flyway_schema_history` появятся в `Schemas → public → Tables`.

## Настройки Java

Compose подключает `application.properties` для чтения и передаёт Java путь к нему через `--config`. Другой файл можно выбрать командой `make up CONFIG_FILE=./другой.properties`.

Переменная окружения имеет приоритет над соответствующим свойством файла. Значения по умолчанию: порт БД `5432`, HTTP-адрес `0.0.0.0`, HTTP-порт `8080`. Остальные параметры БД обязательны. Адреса принимаются как DNS-имя или IPv4; порты — от 1 до 65535.

| Свойство | Переменная |
| --- | --- |
| `db.host` | `DB_HOST` |
| `db.port` | `DB_PORT` |
| `db.name` | `DB_NAME` |
| `db.user` | `DB_USER` |
| `db.password` | `DB_PASSWORD` |
| `http.host` | `HTTP_HOST` |
| `http.port` | `HTTP_PORT` |

Для смены HTTP-порта без пересборки создайте второй файл и задайте в нём `http.port=9090`. Уберите `HTTP_PORT` из окружения и `.env`, если хотите использовать значение файла. Запустите уже собранный образ:

```sh
CONFIG_FILE=./другой.properties HTTP_CONTAINER_PORT=9090 HTTP_HOST_PORT=18080 docker compose up -d --no-build --force-recreate app
curl -i http://127.0.0.1:18080/health
```

`HTTP_CONTAINER_PORT` задаёт целевой порт публикации Compose и должен совпадать с HTTP-портом Java. `HTTP_HOST_PORT` задаёт порт на компьютере. Эти две переменные не меняют настройки Java. При стандартном `http.port=8080` менять их не требуется. В контейнере используйте `http.host=0.0.0.0`, чтобы сервер был доступен через публикацию порта. HTTP публикуется только на `127.0.0.1` компьютера.

## HTTP API

| Запрос | Результат |
| --- | --- |
| `GET /health` | 200, `{"status":"UP"}`; проверяет ответ HTTP-приложения |
| `GET /api/v1/resources` | 200, массив активных ресурсов по возрастанию ID; на пустой БД `[]` |
| `GET /api/v1/resources/7` | 200, карточка активного ресурса с ID 7 |
| `GET /api/v1/resources/abc` | 400, неверный ID; также для нуля, отрицательного числа и переполнения `long` |
| `GET /api/v1/resources/9223372036854775807` | 404, если такой записи нет |
| `GET /api/v1/resources/7/slots?from=2030-01-01T00:00:00Z&to=2030-01-02T00:00:00Z` | 200, массив свободных слотов ресурса за указанный диапазон |
| `POST /api/v1/holds` с JSON `{"slotId":42}` | 201, новый десятиминутный холд для свободного будущего слота |
| `GET /api/v1/bookings/{id}` с UUID брони | 200, текущее состояние брони. Истёкший холд возвращается со статусом `EXPIRED` |
| `POST /api/v1/bookings/{id}/confirm` с JSON `{"customerName":"Анна","customerContact":"anna@example.test"}` | 200, подтверждённая бронь. Повтор с теми же данными возвращает ту же бронь |
| `POST /api/v1/bookings/{id}/cancel` без тела запроса | 200, бронь в конечном статусе `CANCELLED` или `EXPIRED` |

При подтверждении пробелы по краям имени и контакта удаляются. Имя содержит от 1 до 100 символов, контакт — от 1 до 200. Истёкший или отменённый холд, начавшийся слот с неподтверждённым холдом и повтор с другими данными дают 409 `BOOKING_CONFLICT`.

Отмена действующего холда или подтверждённой брони освобождает слот и сохраняет историю. Истёкший холд сохраняется как `EXPIRED`. Повторный запрос возвращает сохранённое состояние без изменения дат. Некорректный UUID даёт 400, неизвестный UUID — 404.

Карточка неактивного ресурса и неизвестный маршрут возвращают 404. Поля ресурса: `id`, `name`, `type` (`TABLE` или `MEETING_ROOM`), `capacity`, `active`. HTTP-ответы имеют Content-Type `application/json`. Ошибка имеет поля `code` и `message`: `INVALID_REQUEST` для 400, `NOT_FOUND` для 404, `INTERNAL_ERROR` для внутреннего сбоя 500. Ответ 500 не содержит SQL, параметров подключения или stack trace. `/health` не обращается к БД и не доказывает её доступность. Демо-данные автоматически не добавляются.

В серверном MVC роль Controller выполняет `ResourceHandler`: читает ID и вызывает сервис. Model — `Resource`, `ResourceService` и репозиторий PostgreSQL. View — `ResourceResponse` и его сериализация Jackson в JSON. `Application` вручную связывает зависимости через конструкторы. При SIGTERM или Ctrl+C shutdown hook останавливает HTTP-сервер.

## Модели

Модели `Resource`, `ResourceType` и `Slot` находятся в `core/domain`. Ресурс имеет непустое название до 100 символов после удаления пробелов по краям и положительную вместимость. Слот начинается на границе часа UTC и длится ровно 60 минут.

В том же пакете `Booking` и `BookingStatus` описывают десятиминутный холд, подтверждение, отмену и истечение. Модель получает текущее время явно. Переходы проверяются unit-тестами.

## Чтение ресурсов

В `features/resources` сервис `ResourceService` получает список через `findAll()` и карточку через `findById(long id)`. Список отсортирован по ID и включает неактивные ресурсы. Пустая таблица даёт пустой список, отсутствующий положительный ID — `Optional.empty()`, неположительный ID — `IllegalArgumentException`.

Сервис открывает отдельное соединение для каждого вызова и закрывает его после чтения, включая случай ошибки. `ResourceRepository` задаёт контракт, `PostgresResourceRepository` выполняет SQL с параметрами и преобразует строки в `Resource`. Репозиторий закрывает свои команды и результаты, а соединение оставляет вызывающему коду. Ошибки JDBC передаются как `SQLException`. HTTP-обработчик фильтрует неактивные ресурсы и преобразует модели в DTO ответа; сервис сохраняет возможность чтения всех записей для административных операций.

## Чтение свободных слотов

В `features/slots` метод `SlotService.findAvailable(long resourceId, Instant from, Instant to)` возвращает свободные слоты ресурса по времени начала. Обе границы обязательны, `from < to`, максимум — 7 суток. В выборку входят только целиком помещающиеся слоты: `startsAt >= from`, `endsAt <= to`. Для неизвестного ресурса сервис выбрасывает `NoSuchElementException`, для существующего неактивного ресурса возвращает пустой список. Неверные ID и диапазоны дают `IllegalArgumentException` до обращения к БД.

`PostgresSlotRepository` одним запросом исключает начавшиеся слоты, подтверждённые брони и HOLD с `expires_at > statement_timestamp()`. Используется время начала SQL-запроса в PostgreSQL. Истёкший HOLD уже не мешает чтению свободных слотов, даже если его статус ещё не обновила очистка; CANCELLED и EXPIRED также не занимают слот. Чтение не меняет статусы. Границы с наносекундами переводятся в микросекунды БД внутрь запрошенного диапазона, чтобы округление не добавляло лишние слоты.

Сервис владеет соединением; оба репозитория используют его и закрывают только свои команды и результаты. Список отражает доступность на момент запроса и не резервирует слот.

HTTP-маршрут `GET /api/v1/resources/{id}/slots` принимает ровно по одному параметру `from` и `to` в ISO 8601 с `Z` или явным смещением UTC. Даты без смещения, пропущенные, пустые и повторяющиеся параметры, неверный ID, `from >= to` и диапазон больше 7 суток возвращают 400. Неизвестный ресурс возвращает 404; существующий неактивный ресурс — 200 с `[]`. Поля слота: `id`, `resourceId`, `startsAt`, `endsAt`; время ответа — строки UTC с `Z`. При передаче положительного смещения знак `+` кодируется как `%2B`.

## Демонстрация холда и конфликта

Сценарий использует локальную демонстрационную БД после `make up`. Запрос в Query Tool pgAdmin создаёт активный ресурс с одним будущим слотом и возвращает его `slot_id`.

```sql
WITH demo_resource AS (
    INSERT INTO resources (name, type, capacity)
    VALUES ('Демо-стол', 'TABLE', 4)
    RETURNING id
), demo_time AS (
    SELECT date_trunc('hour', statement_timestamp(), 'UTC') + INTERVAL '24 hours' AS starts_at
)
INSERT INTO slots (resource_id, starts_at, ends_at)
SELECT demo_resource.id, demo_time.starts_at, demo_time.starts_at + INTERVAL '1 hour'
FROM demo_resource CROSS JOIN demo_time
RETURNING id AS slot_id;
```

Три POST-запроса проверяют ответ 400 с `INVALID_REQUEST`, успешный ответ 201 с `status: HOLD` и повторную попытку на том же слоте. До истечения холда повторная попытка возвращает 409 с `BOOKING_CONFLICT`.

```sh
base_url=http://127.0.0.1:8080
printf 'slot_id: '
read -r slot_id
curl -i -H 'Content-Type: application/json' --data '{"slotId":0}' "$base_url/api/v1/holds"
body=$(printf '{"slotId":%s}' "$slot_id")
curl -i -H 'Content-Type: application/json' --data "$body" "$base_url/api/v1/holds"
curl -i -H 'Content-Type: application/json' --data "$body" "$base_url/api/v1/holds"
```

GET по полю `id` из ответа 201 возвращает ту же бронь с исходным сроком `expiresAt`.

```sh
printf 'UUID брони: '
read -r booking_id
curl -i "$base_url/api/v1/bookings/$booking_id"
```
