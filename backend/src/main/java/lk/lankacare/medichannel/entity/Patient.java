package lk.lankacare.medichannel.entity;

import jakarta.persistence.*;
import lk.lankacare.medichannel.security.AesGcmConverter;

import java.time.Instant;

@Entity
@Table(name = "patients")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Convert(converter = AesGcmConverter.class)
    @Column(name = "nic_encrypted", nullable = false, columnDefinition = "TEXT")
    private String nic;

    @Column(name = "nic_hash", nullable = false, unique = true, length = 64)
    private String nicHash;

    @Convert(converter = AesGcmConverter.class)
    @Column(name = "dob_encrypted", nullable = false, columnDefinition = "TEXT")
    private String dateOfBirth;

    @Column(nullable = false, length = 20)
    private String gender; // MALE, FEMALE, OTHER

    @Convert(converter = AesGcmConverter.class)
    @Column(name = "phone_encrypted", nullable = false, columnDefinition = "TEXT")
    private String phone;

    @Convert(converter = AesGcmConverter.class)
    @Column(name = "address_encrypted", nullable = false, columnDefinition = "TEXT")
    private String address;

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
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getNic() { return nic; }
    public void setNic(String nic) { this.nic = nic; }
    public String getNicHash() { return nicHash; }
    public void setNicHash(String nicHash) { this.nicHash = nicHash; }
    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
