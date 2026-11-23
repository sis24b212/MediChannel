package lk.lankacare.medichannel.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public class BookingDtos {

    public static class CreateAppointmentRequest {
        @NotNull(message = "Session ID is required")
        private Long sessionId;

        @NotBlank(message = "Reason for visit is required")
        @Size(min = 4, max = 500, message = "Reason must be between 4 and 500 characters")
        private String reasonForVisit;

        public Long getSessionId() { return sessionId; }
        public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
        public String getReasonForVisit() { return reasonForVisit; }
        public void setReasonForVisit(String reasonForVisit) { this.reasonForVisit = reasonForVisit; }
    }

    public static class WalkInBookingRequest {
        @NotNull(message = "Session ID is required")
        private Long sessionId;

        @NotBlank(message = "Patient NIC is required")
        @Pattern(regexp = "^(\\d{9}[VvXx]|\\d{12})$", message = "Invalid Sri Lankan NIC")
        private String patientNic;

        @NotBlank(message = "Reason for visit is required")
        @Size(min = 4, max = 500)
        private String reasonForVisit;

        public Long getSessionId() { return sessionId; }
        public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
        public String getPatientNic() { return patientNic; }
        public void setPatientNic(String patientNic) { this.patientNic = patientNic; }
        public String getReasonForVisit() { return reasonForVisit; }
        public void setReasonForVisit(String reasonForVisit) { this.reasonForVisit = reasonForVisit; }
    }

    public static class PaymentRequest {
        @NotBlank(message = "Payment status is required")
        @Pattern(regexp = "^(PENDING|PAID)$", message = "Payment status must be PENDING or PAID")
        private String paymentStatus;

        @Pattern(regexp = "^(CASH|CARD)$", message = "Payment method must be CASH or CARD")
        private String paymentMethod;

        public String getPaymentStatus() { return paymentStatus; }
        public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    }

    public static class ConsultationNotesRequest {
        @NotBlank(message = "Diagnosis summary is required")
        @Size(max = 2000)
        private String diagnosis;

        @Size(max = 2000)
        private String advice;

        @Size(max = 3000)
        private String prescription;

        public String getDiagnosis() { return diagnosis; }
        public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
        public String getAdvice() { return advice; }
        public void setAdvice(String advice) { this.advice = advice; }
        public String getPrescription() { return prescription; }
        public void setPrescription(String prescription) { this.prescription = prescription; }
    }

    public static class ChatMessagePayload {
        @NotBlank(message = "Message cannot be empty")
        @Size(max = 1000, message = "Chat messages cannot exceed 1000 characters (Point 5)")
        private String messageText;

        public String getMessageText() { return messageText; }
        public void setMessageText(String messageText) { this.messageText = messageText; }
    }

    public static class FeedbackRequest {
        @NotNull(message = "Appointment ID is required")
        private Long appointmentId;

        @Min(value = 1, message = "Rating must be between 1 and 5")
        @Max(value = 5, message = "Rating must be between 1 and 5")
        private int rating;

        @Size(max = 1000, message = "Comment cannot exceed 1000 characters")
        private String comment;

        public Long getAppointmentId() { return appointmentId; }
        public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }
        public int getRating() { return rating; }
        public void setRating(int rating) { this.rating = rating; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }

    public static class SessionRequest {
        @NotNull private Long doctorId;
        private Long roomId; // Required if PHYSICAL, must be null if ONLINE (BR-01)
        @NotNull private LocalDate sessionDate;
        @NotNull private LocalTime startTime;
        @NotNull private LocalTime endTime;
        @NotBlank @Pattern(regexp = "^(PHYSICAL|ONLINE)$") private String sessionType;
        @Min(1) @Max(60) private int maxPatients;
        @Min(5) @Max(30) private int minutesPerPatient;
        @NotNull @DecimalMin("0.00") private BigDecimal feeLkr;

        public Long getDoctorId() { return doctorId; }
        public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
        public Long getRoomId() { return roomId; }
        public void setRoomId(Long roomId) { this.roomId = roomId; }
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
    }
}
