package meditrack.dto;

import lombok.Data;

@Data
public class StatsDTO {
    private long totalAppointments;
    private long confirmedAppointments;
    private long pendingAppointments;
    private long completedAppointments;
    private long cancelledAppointments;
    private long rescheduledAppointments;

    // Optional: Add stats by department or doctor if needed
    // private Map<String, Long> appointmentsByDepartment;
    // private Map<String, Long> appointmentsByDoctor;
}
