package meditrack.dto;


import lombok.Data;
import java.util.List;

@Data
public class DoctorDTO {
    private String id;
    private String fullName;
    private String specialization;
    private List<String> qualifications;
    private String department;
    private String contactNumber;
    private String email;
}