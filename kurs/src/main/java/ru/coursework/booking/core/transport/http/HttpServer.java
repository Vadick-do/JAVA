package ru.coursework.booking.core.transport.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import io.javalin.Javalin;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpResponseException;
import io.javalin.json.JavalinJackson;
import io.javalin.router.Endpoint;
import ru.coursework.booking.core.logging.ApplicationLog;
import ru.coursework.booking.features.bookings.transport.http.BookingHandler;
import ru.coursework.booking.features.resources.transport.http.ResourceHandler;
import ru.coursework.booking.features.slots.transport.http.SlotHandler;

import java.util.Map;

public final class HttpServer {
    private HttpServer() {
    }

    public static Javalin create(ResourceHandler resources, SlotHandler slots, BookingHandler bookings) {
        return create(resources, slots, bookings, ApplicationLog.CONSOLE_ONLY);
    }

    public static Javalin create(ResourceHandler resources, SlotHandler slots, BookingHandler bookings, ApplicationLog log) {
        return Javalin.create(config -> {
            config.concurrency.useVirtualThreads = false;
            config.jsonMapper(new JavalinJackson().updateMapper(mapper -> {
                mapper.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
                mapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
            }));
            config.routes.get("/health", context -> context.json(Map.of("status", "UP")));
            config.routes.get("/api/v1/resources", resources::findAll);
            config.routes.get("/api/v1/resources/{id}", resources::findById);
            config.routes.get("/api/v1/resources/{id}/slots", slots::findAvailable);
            config.routes.post("/api/v1/holds", bookings::createHold);
            config.routes.get("/api/v1/bookings/{id}", bookings::findById);
            config.routes.post("/api/v1/bookings/{id}/confirm", bookings::confirmBooking);
            config.routes.post("/api/v1/bookings/{id}/cancel", bookings::cancelBooking);
            config.routes.exception(JsonProcessingException.class, (exception, context) ->
                    context.status(400).json(new ErrorResponse("INVALID_REQUEST", "Проверьте параметры запроса.")));
            config.routes.exception(HttpResponseException.class, (exception, context) -> {
                String code = "HTTP_ERROR";
                String message = "Запрос не может быть выполнен.";
                if (exception.getStatus() == 400) {
                    code = "INVALID_REQUEST";
                    message = "Проверьте параметры запроса.";
                } else if (exception.getStatus() == 404) {
                    code = "NOT_FOUND";
                    message = "Ресурс или маршрут не найден.";
                } else if (exception.getStatus() == 409) {
                    code = "BOOKING_CONFLICT";
                    message = "Слот недоступен для создания холда.";
                }
                context.status(exception.getStatus()).json(new ErrorResponse(code, message));
            });
            config.routes.exception(Exception.class, (exception, context) -> {
                log.error("HTTP: ошибка обработки запроса", exception);
                context.status(500).json(new ErrorResponse("INTERNAL_ERROR", "Внутренняя ошибка сервера."));
            });
            config.requestLogger.http((context, milliseconds) -> {
                Endpoint endpoint = context.endpoints().matchedHttpEndpoint();
                if (endpoint != null && endpoint.method == HandlerType.POST && context.statusCode() < 500) {
                    log.event("HTTP POST " + endpoint.path + " → " + context.statusCode());
                }
            });
            config.events.serverStarted(() -> log.event("HTTP-сервер запущен."));
            config.events.serverStopped(() -> log.event("HTTP-сервер остановлен."));
        });
    }
}
