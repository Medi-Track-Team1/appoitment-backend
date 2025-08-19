package meditrack.dto;

import lombok.Data;

@Data
public class RevisitRequest {
    private String reason;
    private String newDate; // Format: yyyy-MM-dd
    private String newTime; // Format: HH:mm
}
