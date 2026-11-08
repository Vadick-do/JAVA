package ru.coursework.booking.unit;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.coursework.booking.core.domain.Booking;
import ru.coursework.booking.core.domain.BookingStatus;
import ru.coursework.booking.core.domain.Slot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BookingTest {
    private final Instant startsAt = Instant.parse("2026-11-07T11:00:00Z");
    private final Instant createdAt = startsAt.minusSeconds(1200);
    private final UUID id = UUID.fromString("6dc3cb73-77df-435f-87e8-c19313c3c00d");
    private final Slot slot = new Slot(1, 1, startsAt, startsAt.plusSeconds(3600));
    private final Booking hold = Booking.hold(id, slot, createdAt);

    @Test
    void createsTenMinuteHoldWithoutCustomerData() {
        assertEquals(id, hold.getId());
        assertEquals(slot.getId(), hold.getSlotId());
        assertEquals(BookingStatus.HOLD, hold.getStatus());
        assertEquals(createdAt, hold.getCreatedAt());
        assertEquals(createdAt.plusSeconds(600), hold.getExpiresAt());
        assertNull(hold.getCustomerName());
        assertNull(hold.getCustomerContact());
        assertNull(hold.getConfirmedAt());
        assertNull(hold.getCancelledAt());
    }

    @Test
    void rejectsMissingHoldInput() {
        assertThrows(IllegalArgumentException.class, () -> Booking.hold(null, slot, createdAt));
        assertThrows(IllegalArgumentException.class, () -> Booking.hold(id, null, createdAt));
        assertThrows(IllegalArgumentException.class, () -> Booking.hold(id, slot, null));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 1})
    void cannotHoldSlotAtOrAfterStart(long secondsAfterStart) {
        assertThrows(IllegalStateException.class, () -> Booking.hold(id, slot, startsAt.plusSeconds(secondsAfterStart)));
    }

    @Test
    void confirmsJustBeforeExpirationAndKeepsOriginalHoldUnchanged() {
        Instant now = hold.getExpiresAt().minusNanos(1);
        Booking confirmed = hold.confirm("  Анна\u2003", " anna@example.com ", now);

        assertEquals(BookingStatus.HOLD, hold.getStatus());
        assertEquals(BookingStatus.CONFIRMED, confirmed.getStatus());
        assertEquals(id, confirmed.getId());
        assertEquals(hold.getExpiresAt(), confirmed.getExpiresAt());
        assertEquals("Анна", confirmed.getCustomerName());
        assertEquals("anna@example.com", confirmed.getCustomerContact());
        assertEquals(now, confirmed.getConfirmedAt());
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 1})
    void rejectsConfirmationAtOrAfterDeadline(long secondsAfterDeadline) {
        Instant now = hold.getExpiresAt().plusSeconds(secondsAfterDeadline);

        assertEquals(BookingStatus.HOLD, hold.getStatus());
        assertEquals(BookingStatus.EXPIRED, hold.getEffectiveStatus(now));
        assertThrows(IllegalStateException.class, () -> hold.confirm("Анна", "контакт", now));
    }

    @Test
    void slotStartExpiresHoldEvenBeforeItsDeadline() {
        Booking lateHold = Booking.hold(id, slot, startsAt.minusSeconds(60));

        assertSame(lateHold, lateHold.expire(startsAt.minusNanos(1)));
        assertThrows(IllegalStateException.class, () -> lateHold.confirm("Анна", "контакт", startsAt));
        assertEquals(BookingStatus.EXPIRED, lateHold.expire(startsAt).getStatus());
    }

    @Test
    void repeatedConfirmationAfterSlotStartUsesOriginalDataAndTime() {
        Booking confirmed = hold.confirm("Анна", "контакт", createdAt);

        assertSame(confirmed, confirmed.confirm(" Анна ", " контакт ", startsAt));
        assertThrows(IllegalStateException.class, () -> confirmed.confirm("Анна", "новый контакт", startsAt));
        assertEquals(BookingStatus.CONFIRMED, confirmed.getEffectiveStatus(startsAt));
        assertSame(confirmed, confirmed.expire(startsAt));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" \u2003"})
    void rejectsMissingOrBlankCustomerFields(String value) {
        assertThrows(IllegalArgumentException.class, () -> hold.confirm(value, "контакт", createdAt));
        assertThrows(IllegalArgumentException.class, () -> hold.confirm("Анна", value, createdAt));
    }

    @Test
    void checksCustomerFieldLimitsAfterNormalizationByUnicodeCharacters() {
        String name = "😀".repeat(100);
        String contact = "😀".repeat(200);
        Booking confirmed = hold.confirm(" " + name + " ", " " + contact + " ", createdAt);

        assertEquals(name, confirmed.getCustomerName());
        assertEquals(contact, confirmed.getCustomerContact());
        assertThrows(IllegalArgumentException.class, () -> hold.confirm(name + "я", contact, createdAt));
        assertThrows(IllegalArgumentException.class, () -> hold.confirm(name, contact + "я", createdAt));
    }

    @Test
    void cancelsActiveHoldAndDoesNotAllowItsConfirmation() {
        Booking cancelled = hold.cancel(createdAt);

        assertEquals(BookingStatus.CANCELLED, cancelled.getStatus());
        assertEquals(createdAt, cancelled.getCancelledAt());
        assertNull(cancelled.getConfirmedAt());
        assertSame(cancelled, cancelled.cancel(startsAt));
        assertSame(cancelled, cancelled.expire(startsAt));
        assertThrows(IllegalStateException.class, () -> cancelled.confirm("Анна", "контакт", createdAt));
    }

    @Test
    void cancelsConfirmedBookingAndKeepsItsHistory() {
        Booking confirmed = hold.confirm("Анна", "контакт", createdAt);
        Booking cancelled = confirmed.cancel(startsAt);

        assertEquals(BookingStatus.CANCELLED, cancelled.getStatus());
        assertEquals("Анна", cancelled.getCustomerName());
        assertEquals("контакт", cancelled.getCustomerContact());
        assertEquals(createdAt, cancelled.getConfirmedAt());
        assertEquals(startsAt, cancelled.getCancelledAt());
    }

    @Test
    void expiredHoldBecomesExpiredOnCancellationOrCleanup() {
        Instant now = hold.getExpiresAt();
        Booking expired = hold.cancel(now);

        assertEquals(BookingStatus.EXPIRED, expired.getStatus());
        assertEquals(BookingStatus.EXPIRED, hold.expire(now).getStatus());
        assertNull(expired.getCancelledAt());
        assertSame(expired, expired.cancel(now));
        assertSame(expired, expired.expire(now));
        assertThrows(IllegalStateException.class, () -> expired.confirm("Анна", "контакт", now));
    }
}
