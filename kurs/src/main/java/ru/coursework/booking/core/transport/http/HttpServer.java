package ru.coursework.booking.core.transport.http;

import io.javalin.Javalin;
import io.javalin.http.HttpResponseException;
import io.javalin.json.JavalinJackson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.coursework.booking.features.resources.transport.http.ResourceHandler;

import java.util.Map;

public final class HttpServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServer.class);

    private HttpServer() {
    }

    public static Javalin create(ResourceHandler resources) {
        return Javalin.create(config -> {
            config.concurrency.useVirtualThreads = false;
            config.jsonMapper(new JavalinJackson());
            config.routes.get("/health", context -> context.json(Map.of("status", "UP")));
            config.routes.get("/api/v1/resources", resources::findAll);
            config.routes.get("/api/v1/resources/{id}", resources::findById);
            config.routes.exception(HttpResponseException.class, (exception, context) -> {
                String code = "HTTP_ERROR";
                String message = "Запрос не может быть выполнен.";
                if (exception.getStatus() == 400) {
                    code = "INVALID_REQUEST";
                    message = "Проверьте параметры запроса.";
                } else if (exception.getStatus() == 404) {
                    code = "NOT_FOUND";
                    message = "Ресурс или маршрут не найден.";
                }
                context.status(exception.getStatus()).json(new ErrorResponse(code, message));
            });
            config.routes.exception(Exception.class, (exception, context) -> {
                LOGGER.error("HTTP: ошибка обработки запроса, тип=" + exception.getClass().getSimpleName());
                context.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Внутренняя ошибка сервера."));
            });
            config.events.serverStarted(() -> LOGGER.info("HTTP-сервер запущен."));
            config.events.serverStopped(() -> LOGGER.info("HTTP-сервер остановлен."));
        });
    }
}
