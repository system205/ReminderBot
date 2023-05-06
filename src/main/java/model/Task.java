package model;

import java.time.*;

public class Task {
    private LocalDateTime dateTime;
    private String title;
    private String description;

    public Task(LocalDateTime dateTime, String title) {
        this.dateTime = dateTime;
        this.title = title;
    }

    public Long getMillisBeforeStart(){
        LocalDateTime currentTime = LocalDateTime.now();

        if (currentTime.isAfter(dateTime))
            throw new IllegalStateException("The task is supposed to be finished. Its date and time is in the past.");

        return Duration.between(currentTime, dateTime).toMillis();
    }

    @Override
    public String toString() {
        return title + " " + (description == null ? "" : description) + ". Start time: " + dateTime.toString();
    }

    public LocalDateTime getDateTime() {
        return dateTime;
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
