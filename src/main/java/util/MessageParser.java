package util;

import model.*;

import java.time.*;

public class MessageParser {
    public static Task extractTask(String text){

        String[] tokens = text.split("\n");
        String title = tokens[0];
        String[] time = tokens[1].split(":");

        int hours = Integer.parseInt(time[0]);
        int minutes = Integer.parseInt(time[1]);

        LocalDateTime dateTime = LocalDateTime.now().withHour(hours).withMinute(minutes);

        return new Task(dateTime, title);
    }
}
