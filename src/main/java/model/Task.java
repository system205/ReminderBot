package model;

import java.time.*;
import java.time.format.*;

public class Task {
    private LocalDateTime dateTime;
    private String title;
    private String description;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd.MM.yyyy");

    public Task(LocalDateTime dateTime, String title) {
        this.dateTime = dateTime;
        this.title = title;
    }

    public Task(LocalDateTime dateTime, String title, String description) {
        this.dateTime = dateTime;
        this.title = title;
        this.description = description;
    }

    public Long getMillisBeforeStart(){
        LocalDateTime currentTime = LocalDateTime.now();

        if (currentTime.isAfter(dateTime))
            throw new IllegalStateException("The task is supposed to be finished. Its date and time is in the past.");

        return Duration.between(currentTime, dateTime).toMillis();
    }

    @Override
    public String toString() {
        return title + "\n" + (description == null ? "" : description + "\n") + dateTime.format(formatter);
    }

    public String getDateTime() {
        return dateTime.format(formatter);
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
