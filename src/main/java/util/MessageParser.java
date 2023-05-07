package util;

import model.*;

import java.time.*;
import java.time.format.*;
import java.util.*;

public class MessageParser {
    /**
     * Task template:
     * Напомни or Remind
     * Title
     * Description (optional)
     * Date as dd.MM.yyyy
     * Time as HH:mm
     * */
    public static Task extractTask(String text){
        if (!text.toLowerCase().contains("напомни") && !text.toLowerCase().contains("remind")) {
            return null;
        }

        List<String> lines = Arrays.stream(text.split("\n")).toList();

        if (lines.size() > 5 || lines.size() < 4)
            return null; // No task

        String title = lines.get(1);
        final String description;

        if (lines.size() == 5) {
            description = lines.get(2);
        } else description = null;

        String dateTimeString = lines.get(lines.size()-2) + " " + lines.get(lines.size()-1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        LocalDateTime dateTime = LocalDateTime.parse(dateTimeString, formatter);

        return new Task(dateTime, title, description);
    }
}
