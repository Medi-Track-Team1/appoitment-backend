package meditrack.dto;

import lombok.Data;

@Data
public class StatsDTO {
    private long totalAppointments;
//    private long emergencyCases;
    private long confirmedAppointments;
    private long pendingAppointments;
    private long completedAppointments;
}
