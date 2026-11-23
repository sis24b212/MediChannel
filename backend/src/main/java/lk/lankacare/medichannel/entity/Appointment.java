package lk.lankacare.medichannel.entity;

import jakarta.persistence.*;
import lk.lankacare.medichannel.security.AesGcmConverter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;

@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_reference", nullable = false, unique = true, length = 32)
    private String bookingReference;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ChannelSession session;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "appointment_number", nullable = false)
    private int appointmentNumber;

    @Column(name = "estimated_time", nullable = false)
    private LocalTime estimatedTime;

    @Convert(converter = AesGcmConverter.class)
    @Column(name = "reason_encrypted", nullable = false, columnDefinition = "TEXT")
    private String reasonForVisit;

    @Column(nullable = false, length = 25)
    private String status = "BOOKED"; // BOOKED, ARRIVED, IN_CONSULTATION, COMPLETED, CANCELLED, NO_SHOW

    @Column(name = "payment_status", nullable = false, length = 20)
    private String paymentStatus = "PENDING"; // PENDING, PAID

    @Column(name = "payment_method", length = 20)
    private String paymentMethod; // CASH, CARD

    @Column(name = "fee_lkr", nullable = false, precision = 10, scale = 2)
    private BigDecimal feeLkr;

    @Column(name = "booked_by_walkin", nullable = false)
    private boolean bookedByWalkin = false;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

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
    public String getBookingReference() { return bookingReference; }
    public void setBookingReference(String bookingReference) { this.bookingReference = bookingReference; }
    public ChannelSession getSession() { return session; }
    public void setSession(ChannelSession session) { this.session = session; }
    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }
    public int getAppointmentNumber() { return appointmentNumber; }
    public void setAppointmentNumber(int appointmentNumber) { this.appointmentNumber = appointmentNumber; }
    public LocalTime getEstimatedTime() { return estimatedTime; }
    public void setEstimatedTime(LocalTime estimatedTime) { this.estimatedTime = estimatedTime; }
    public String getReasonForVisit() { return reasonForVisit; }
    public void setReasonForVisit(String reasonForVisit) { this.reasonForVisit = reasonForVisit; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public BigDecimal getFeeLkr() { return feeLkr; }
    public void setFeeLkr(BigDecimal feeLkr) { this.feeLkr = feeLkr; }
    public boolean isBookedByWalkin() { return bookedByWalkin; }
    public void setBookedByWalkin(boolean bookedByWalkin) { this.bookedByWalkin = bookedByWalkin; }
    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant cancelledAt) { this.cancelledAt = cancelledAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
