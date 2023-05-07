package util;

import model.*;

import java.time.*;
import java.time.temporal.*;
import java.util.*;

public class TaskManager {
    // ChatId to List of scheduled tasks
    private final Map<Long, List<TimerTask>> timerTasks = new Hashtable<>();
    private final Map<Long, Task> tasksInProgress = new Hashtable<>();

    public Task createTask(Long chatId) {
        if (chatId == null) return null;

        Task task = new Task(LocalDateTime.now().plus(1, ChronoUnit.MINUTES),
                "Warm up in 1 minute");
        task.setChatId(chatId);

        tasksInProgress.put(chatId, task);

        return task;
    }

    public Task getTaskInProgress(Long chatId) {
        return tasksInProgress.get(chatId);
    }
}
