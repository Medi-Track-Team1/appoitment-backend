package meditrack.dto;

import lombok.Data;
import jakarta.validation.constraints.*;

import java.util.List;

@Data
public class PatientDTO {
    private String patientId;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotNull(message = "Age is required")
    @Min(value = 0, message = "Age must be positive")
    private Integer age;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @Email(message = "Invalid email format")
    private String email;

    private String medicalHistory;
    private List<String> allergies;
}
