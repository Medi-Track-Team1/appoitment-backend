package meditrack.util;

import java.util.Random;

public class AppointmentIdGenerator {

    public static String generateId() {
        Random random = new Random();
        int number = 1000 + random.nextInt(9000); // Generates a 4-digit number
        return "APP-" + number;
    }
}
