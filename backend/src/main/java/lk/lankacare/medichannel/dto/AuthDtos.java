package lk.lankacare.medichannel.dto;

import jakarta.validation.constraints.*;

public class AuthDtos {

    public static class RegisterRequest {
        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 120, message = "Full name must be between 2 and 120 characters")
        private String fullName;

        @NotBlank(message = "NIC is required")
        @Pattern(regexp = "^(\\d{9}[VvXx]|\\d{12})$", message = "NIC must be 9 digits + V/X or 12 digits")
        private String nic;

        @NotBlank(message = "Date of birth is required")
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date of birth must be YYYY-MM-DD")
        private String dateOfBirth;

        @NotBlank(message = "Gender is required")
        @Pattern(regexp = "^(MALE|FEMALE|OTHER)$", message = "Gender must be MALE, FEMALE, or OTHER")
        private String gender;

        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^07\\d{8}$", message = "Phone must follow Sri Lankan format 07XXXXXXXX")
        private String phone;

        @NotBlank(message = "Email is required")
        @Email(message = "Please enter a valid email address")
        private String email;

        @NotBlank(message = "Address is required")
        @Size(min = 5, max = 300, message = "Address must be between 5 and 300 characters")
        private String address;

        @NotBlank(message = "Password is required")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
                message = "Password must be 8+ characters with a letter, a number, and a symbol"
        )
        private String password;

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getNic() { return nic; }
        public void setNic(String nic) { this.nic = nic; }
        public String getDateOfBirth() { return dateOfBirth; }
        public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class LoginRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Valid email is required")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class ChangePasswordRequest {
        @NotBlank(message = "Current password is required")
        private String currentPassword;

        @NotBlank(message = "New password is required")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$",
                message = "New password must be 8+ characters with a letter, a number, and a symbol"
        )
        private String newPassword;

        public String getCurrentPassword() { return currentPassword; }
        public void setCurrentPassword(String currentPassword) { this.currentPassword = currentPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    public static class AuthResponse {
        private String token;
        private Long userId;
        private String fullName;
        private String email;
        private String role;
        private boolean mustChangePassword;

        public AuthResponse(String token, Long userId, String fullName, String email, String role, boolean mustChangePassword) {
            this.token = token;
            this.userId = userId;
            this.fullName = fullName;
            this.email = email;
            this.role = role;
            this.mustChangePassword = mustChangePassword;
        }

        public String getToken() { return token; }
        public Long getUserId() { return userId; }
        public String getFullName() { return fullName; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
        public boolean isMustChangePassword() { return mustChangePassword; }
    }
}
