package lk.lankacare.medichannel.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "sessions")
public class ChannelSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "session_type", nullable = false, length = 20)
    private String sessionType; // PHYSICAL, ONLINE

    @Column(name = "max_patients", nullable = false)
    private int maxPatients;

    @Column(name = "minutes_per_patient", nullable = false)
    private int minutesPerPatient;

    @Column(name = "fee_lkr", nullable = false, precision = 10, scale = 2)
    private BigDecimal feeLkr;

    @Column(nullable = false, length = 20)
    private String status = "SCHEDULED"; // SCHEDULED, OPEN, IN_PROGRESS, COMPLETED, CANCELLED

    @Column(name = "delay_minutes", nullable = false)
    private int delayMinutes = 0;

    @Column(name = "current_serving_number", nullable = false)
    private int currentServingNumber = 0;

    @Column(name = "last_issued_number", nullable = false)
    private int lastIssuedNumber = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public int getRemainingNumbers() {
        return Math.max(0, maxPatients - lastIssuedNumber);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Doctor getDoctor() { return doctor; }
    public void setDoctor(Doctor doctor) { this.doctor = doctor; }
    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
    public LocalDate getSessionDate() { return sessionDate; }
    public void setSessionDate(LocalDate sessionDate) { this.sessionDate = sessionDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public String getSessionType() { return sessionType; }
    public void setSessionType(String sessionType) { this.sessionType = sessionType; }
    public int getMaxPatients() { return maxPatients; }
    public void setMaxPatients(int maxPatients) { this.maxPatients = maxPatients; }
    public int getMinutesPerPatient() { return minutesPerPatient; }
    public void setMinutesPerPatient(int minutesPerPatient) { this.minutesPerPatient = minutesPerPatient; }
    public BigDecimal getFeeLkr() { return feeLkr; }
    public void setFeeLkr(BigDecimal feeLkr) { this.feeLkr = feeLkr; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getDelayMinutes() { return delayMinutes; }
    public void setDelayMinutes(int delayMinutes) { this.delayMinutes = delayMinutes; }
    public int getCurrentServingNumber() { return currentServingNumber; }
    public void setCurrentServingNumber(int currentServingNumber) { this.currentServingNumber = currentServingNumber; }
    public int getLastIssuedNumber() { return lastIssuedNumber; }
    public void setLastIssuedNumber(int lastIssuedNumber) { this.lastIssuedNumber = lastIssuedNumber; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
