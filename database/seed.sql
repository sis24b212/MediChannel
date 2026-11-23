-- ============================================================================
-- MediChannel - Seed Data (Fictional Reference Data for Lanka Care Hospital)
-- NOTE: Encrypted user/patient/consultation records are automatically seeded
--       at Spring Boot startup by SeedDataRunner.java using your real
--       AES-256-GCM ENCRYPTION_KEY_BASE64 and HMAC_SECRET environment variables.
--       This SQL file seeds non-encrypted catalog records (Specializations & Rooms)
--       and can be run safely after schema.sql.
-- ============================================================================

INSERT INTO specializations (name, description, active) VALUES
    ('Cardiology', 'Heart, blood vessel, and cardiovascular consultations and ECG follow-ups.', TRUE),
    ('Pediatrics', 'Specialist healthcare for infants, children, and adolescents.', TRUE),
    ('Dermatology', 'Diagnosis and treatment of skin, hair, and nail conditions.', TRUE),
    ('General Medicine', 'Comprehensive adult internal medicine and chronic care consultations.', TRUE),
    ('Orthopedics', 'Bone, joint, ligament, and sports injury specialist channelling.', TRUE),
    ('Neurology', 'Brain, spine, and nervous system specialist clinics.', TRUE),
    ('Obstetrics & Gynecology', 'Women health, antenatal care, and specialist consultations.', TRUE),
    ('ENT Surgery', 'Ear, nose, throat, and head & neck specialist consultations.', TRUE)
ON CONFLICT (name) DO NOTHING;

INSERT INTO rooms (room_number, floor, wing, active) VALUES
    ('Room 101', '1st Floor', 'East Wing - Outpatient', TRUE),
    ('Room 102', '1st Floor', 'East Wing - Outpatient', TRUE),
    ('Room 201', '2nd Floor', 'Central Wing - Heart Centre', TRUE),
    ('Room 204', '2nd Floor', 'Central Wing - Child Care', TRUE),
    ('Room 301', '3rd Floor', 'West Wing - Specialist Suites', TRUE),
    ('Room 305', '3rd Floor', 'West Wing - Specialist Suites', TRUE)
ON CONFLICT (room_number) DO NOTHING;
