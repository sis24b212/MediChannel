package lk.lankacare.medichannel.entity;

import jakarta.persistence.*;
import lk.lankacare.medichannel.security.AesGcmConverter;

import java.time.Instant;

@Entity
@Table(name = "consultations")
public class Consultation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false, unique = true)
    private Appointment appointment;

    @Convert(converter = AesGcmConverter.class)
    @Column(name = "diagnosis_encrypted", columnDefinition = "TEXT")
    private String diagnosis;

    @Convert(converter = AesGcmConverter.class)
    @Column(name = "advice_encrypted", columnDefinition = "TEXT")
    private String advice;

    @Convert(converter = AesGcmConverter.class)
    @Column(name = "prescription_encrypted", columnDefinition = "TEXT")
    private String prescription;

    @Column(name = "room_open", nullable = false)
    private boolean roomOpen = false;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Appointment getAppointment() { return appointment; }
    public void setAppointment(Appointment appointment) { this.appointment = appointment; }
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public String getAdvice() { return advice; }
    public void setAdvice(String advice) { this.advice = advice; }
    public String getPrescription() { return prescription; }
    public void setPrescription(String prescription) { this.prescription = prescription; }
    public boolean isRoomOpen() { return roomOpen; }
    public void setRoomOpen(boolean roomOpen) { this.roomOpen = roomOpen; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
