-- ============================================================================
-- MediChannel - Hospital Channelling and Appointment System
-- Lanka Care Hospital, Colombo
-- PostgreSQL Schema (13 Core Tables + Constraints + Indexes)
-- Timezone: Asia/Colombo | Currency: LKR (NUMERIC)
-- ============================================================================

-- Set session timezone to Asia/Colombo (BR-11)
SET TIME ZONE 'Asia/Colombo';

-- 1. USERS (Authentication & Role Management)
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email_encrypted TEXT NOT NULL,
    email_hash VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name_encrypted TEXT NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('CUSTOMER', 'DOCTOR', 'MANAGER', 'ADMIN')),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    failed_login_attempts INT NOT NULL DEFAULT 0 CHECK (failed_login_attempts >= 0),
    locked_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. PATIENTS (Encrypted Personal Details + Blind Indexes)
CREATE TABLE IF NOT EXISTS patients (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE RESTRICT,
    nic_encrypted TEXT NOT NULL,
    nic_hash VARCHAR(64) NOT NULL UNIQUE,
    dob_encrypted TEXT NOT NULL,
    gender VARCHAR(20) NOT NULL CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    phone_encrypted TEXT NOT NULL,
    address_encrypted TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. SPECIALIZATIONS
CREATE TABLE IF NOT EXISTS specializations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 4. ROOMS (Physical Channelling Rooms)
CREATE TABLE IF NOT EXISTS rooms (
    id BIGSERIAL PRIMARY KEY,
    room_number VARCHAR(30) NOT NULL UNIQUE,
    floor VARCHAR(40) NOT NULL,
    wing VARCHAR(60),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 5. DOCTORS (Consultant Profiles linked to DOCTOR user accounts)
CREATE TABLE IF NOT EXISTS doctors (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE RESTRICT,
    slmc_reg_no VARCHAR(40) NOT NULL UNIQUE,
    specialization_id BIGINT NOT NULL REFERENCES specializations(id) ON DELETE RESTRICT,
    qualifications VARCHAR(255) NOT NULL,
    consultation_fee NUMERIC(10, 2) NOT NULL CHECK (consultation_fee >= 0),
    bio TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 6. SESSIONS (Physical & Online Channelling Sessions - BR-01, BR-02)
CREATE TABLE IF NOT EXISTS sessions (
    id BIGSERIAL PRIMARY KEY,
    doctor_id BIGINT NOT NULL REFERENCES doctors(id) ON DELETE RESTRICT,
    room_id BIGINT REFERENCES rooms(id) ON DELETE RESTRICT,
    session_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    session_type VARCHAR(20) NOT NULL CHECK (session_type IN ('PHYSICAL', 'ONLINE')),
    max_patients INT NOT NULL CHECK (max_patients BETWEEN 1 AND 60),
    minutes_per_patient INT NOT NULL CHECK (minutes_per_patient BETWEEN 5 AND 30),
    fee_lkr NUMERIC(10, 2) NOT NULL CHECK (fee_lkr >= 0),
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED'
        CHECK (status IN ('SCHEDULED', 'OPEN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    delay_minutes INT NOT NULL DEFAULT 0 CHECK (delay_minutes >= 0),
    current_serving_number INT NOT NULL DEFAULT 0 CHECK (current_serving_number >= 0),
    last_issued_number INT NOT NULL DEFAULT 0 CHECK (last_issued_number >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_session_times CHECK (end_time > start_time),
    CONSTRAINT chk_physical_room CHECK (
        (session_type = 'PHYSICAL' AND room_id IS NOT NULL) OR
        (session_type = 'ONLINE' AND room_id IS NULL)
    ),
    CONSTRAINT chk_issued_within_max CHECK (last_issued_number <= max_patients)
);

-- 7. APPOINTMENTS (BR-03, BR-04, BR-06, BR-07, BR-08, BR-11)
CREATE TABLE IF NOT EXISTS appointments (
    id BIGSERIAL PRIMARY KEY,
    booking_reference VARCHAR(32) NOT NULL UNIQUE,
    session_id BIGINT NOT NULL REFERENCES sessions(id) ON DELETE RESTRICT,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE RESTRICT,
    appointment_number INT NOT NULL CHECK (appointment_number >= 1),
    estimated_time TIME NOT NULL,
    reason_encrypted TEXT NOT NULL,
    status VARCHAR(25) NOT NULL DEFAULT 'BOOKED'
        CHECK (status IN ('BOOKED', 'ARRIVED', 'IN_CONSULTATION', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (payment_status IN ('PENDING', 'PAID')),
    payment_method VARCHAR(20)
        CHECK (payment_method IS NULL OR payment_method IN ('CASH', 'CARD')),
    fee_lkr NUMERIC(10, 2) NOT NULL CHECK (fee_lkr >= 0),
    booked_by_walkin BOOLEAN NOT NULL DEFAULT FALSE,
    cancelled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    -- BR-03: Appointment number is strictly unique per session and never reused
    CONSTRAINT uq_session_appointment_number UNIQUE (session_id, appointment_number)
);

-- BR-04: A patient can have only ONE active (non-cancelled) appointment in a session
CREATE UNIQUE INDEX IF NOT EXISTS uq_active_patient_per_session
    ON appointments (session_id, patient_id)
    WHERE status <> 'CANCELLED';

-- 8. CONSULTATIONS (Encrypted Medical Notes & Prescriptions - BR-09, Privacy Rule)
CREATE TABLE IF NOT EXISTS consultations (
    id BIGSERIAL PRIMARY KEY,
    appointment_id BIGINT NOT NULL UNIQUE REFERENCES appointments(id) ON DELETE RESTRICT,
    diagnosis_encrypted TEXT,
    advice_encrypted TEXT,
    prescription_encrypted TEXT,
    room_open BOOLEAN NOT NULL DEFAULT FALSE,
    started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 9. CHAT MESSAGES (Encrypted Online Consultation Chat - RT-03, BR-09)
CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGSERIAL PRIMARY KEY,
    consultation_id BIGINT NOT NULL REFERENCES consultations(id) ON DELETE RESTRICT,
    sender_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    sender_role VARCHAR(20) NOT NULL CHECK (sender_role IN ('CUSTOMER', 'DOCTOR')),
    message_encrypted TEXT NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 10. NOTIFICATIONS (RT-04 Real-Time User Alerts)
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    title VARCHAR(160) NOT NULL,
    message TEXT NOT NULL,
    notification_type VARCHAR(40) NOT NULL
        CHECK (notification_type IN ('THREE_AWAY', 'CALLED', 'SESSION_DELAYED', 'SESSION_CANCELLED', 'CONSULTATION_STARTED', 'BOOKING_CONFIRMED', 'GENERAL')),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 11. FEEDBACK (One feedback per completed appointment, moderated by Manager)
CREATE TABLE IF NOT EXISTS feedback (
    id BIGSERIAL PRIMARY KEY,
    appointment_id BIGINT NOT NULL UNIQUE REFERENCES appointments(id) ON DELETE RESTRICT,
    doctor_id BIGINT NOT NULL REFERENCES doctors(id) ON DELETE RESTRICT,
    patient_id BIGINT NOT NULL REFERENCES patients(id) ON DELETE RESTRICT,
    rating INT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    moderation_status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (moderation_status IN ('PENDING', 'APPROVED', 'HIDDEN', 'DELETED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 12. NOTICES (Public Hospital Announcements published by Manager)
CREATE TABLE IF NOT EXISTS notices (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(180) NOT NULL,
    content TEXT NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL' CHECK (priority IN ('NORMAL', 'IMPORTANT', 'URGENT')),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    published_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 13. AUDIT LOGS (Immutable Security & Access Log - Never contains medical data)
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    user_role VARCHAR(20),
    action VARCHAR(80) NOT NULL,
    resource_type VARCHAR(60),
    resource_id VARCHAR(60),
    ip_address VARCHAR(64),
    details VARCHAR(300),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- PERFORMANCE INDEXES
-- ============================================================================
CREATE INDEX IF NOT EXISTS idx_sessions_date_status ON sessions (session_date, status);
CREATE INDEX IF NOT EXISTS idx_sessions_doctor_date ON sessions (doctor_id, session_date);
CREATE INDEX IF NOT EXISTS idx_appointments_session_status ON appointments (session_id, status);
CREATE INDEX IF NOT EXISTS idx_appointments_patient ON appointments (patient_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_chat_messages_consultation ON chat_messages (consultation_id, sent_at ASC);
CREATE INDEX IF NOT EXISTS idx_notifications_user_unread ON notifications (user_id, is_read, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_feedback_doctor_status ON feedback (doctor_id, moderation_status);
