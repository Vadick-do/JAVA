CREATE TABLE bookings (
    id UUID PRIMARY KEY,
    slot_id BIGINT NOT NULL REFERENCES slots (id),
    status VARCHAR(9) NOT NULL CHECK (status IN ('HOLD', 'CONFIRMED', 'CANCELLED', 'EXPIRED')),
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    customer_name VARCHAR(100) CHECK (customer_name ~ '[^[:space:]]'),
    customer_contact VARCHAR(200) CHECK (customer_contact ~ '[^[:space:]]'),
    confirmed_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    CONSTRAINT bookings_hold_duration CHECK (isfinite(created_at) AND expires_at = created_at + INTERVAL '10 minutes'),
    CONSTRAINT bookings_confirmation_fields CHECK (
        (confirmed_at IS NULL AND customer_name IS NULL AND customer_contact IS NULL)
        OR (confirmed_at IS NOT NULL AND customer_name IS NOT NULL AND customer_contact IS NOT NULL
            AND confirmed_at >= created_at AND confirmed_at < expires_at)
    ),
    CONSTRAINT bookings_status_fields CHECK (
        (status IN ('HOLD', 'EXPIRED') AND confirmed_at IS NULL AND cancelled_at IS NULL)
        OR (status = 'CONFIRMED' AND confirmed_at IS NOT NULL AND cancelled_at IS NULL)
        OR (status = 'CANCELLED' AND cancelled_at IS NOT NULL
            AND isfinite(cancelled_at) AND cancelled_at >= COALESCE(confirmed_at, created_at))
    )
);

CREATE UNIQUE INDEX bookings_one_active_per_slot ON bookings (slot_id)
    WHERE status IN ('HOLD', 'CONFIRMED');
