package util;

import bot.*;
import model.*;
import org.telegram.telegrambots.meta.api.methods.send.*;

import java.time.*;
import java.time.temporal.*;
import java.util.*;

public class TaskManager {
    // ChatId to List of scheduled tasks
    private final Map<Long, List<Task>> scheduledTasks = new Hashtable<>();
    private final Map<Long, Task> editableTasks = new Hashtable<>();

    private final Timer timer = new Timer();

    /**
     * Returns an existing editable task
     * or
     * Create a new one to be edited
     */
    public Task createTask(Long chatId) {
        if (chatId == null) return null;

        // Check existences
        if (editableTasks.containsKey(chatId)) return editableTasks.get(chatId);

        Task task = new Task(LocalDateTime.now().plus(1, ChronoUnit.MINUTES),
                "Warm up in 1 minute");
        task.setChatId(chatId);

        // Add task to be edited
        editableTasks.put(chatId, task);

        return task;
    }

    public Task getEditableTask(Long chatId) {
        return editableTasks.get(chatId);
    }

    public boolean scheduleTask(Long chatId, Bot bot) {
        if (chatId == null) return false;

        // Pop edited task
        Task task = editableTasks.remove(chatId);
        if (task == null) return false;
        if (!task.canSchedule()) {
            // put back to edit - the date and time are in the past
            editableTasks.put(chatId, task);
            return false;
        }

        // Get all scheduled tasks or create a list
        List<Task> tasks = scheduledTasks.get(chatId);
        if (tasks == null) tasks = new ArrayList<>();

        // Create message and schedule it
        SendMessage taskMessage = new SendMessage(chatId.toString(), getTaskMessageText(task));
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                bot.sendMessage(taskMessage);
            }
        };
        task.setTimerTask(timerTask); // Add for future tracking
        timer.schedule(timerTask, task.getMillisBeforeStart());

        // Add to the list of all scheduled messages of this chat
        tasks.add(task);
        scheduledTasks.put(chatId, tasks);
        return true;
    }

    private String getTaskMessageText(Task task) {
        return """
                ❕ Reminder ❕
                                
                👉 %s
                %s""".formatted(task.getTitle(), task.getDescription().length() != 0 ? ("Details: " + task.getDescription()) : "");
    }


    public void cancelTask(Long chatId) {
        if (chatId == null) return;
        editableTasks.remove(chatId);
    }

    public List<Task> getScheduledTasks(Long chatId) {
        List<Task> scheduledTasks = this.scheduledTasks.get(chatId);
        if (scheduledTasks == null) return null;

        // Filter tasks that can be scheduled. Remove those expired.
        List<Task> tasks = new ArrayList<>(scheduledTasks.size());
        for (Task scheduledTask : scheduledTasks) {
            if (scheduledTask.canSchedule())
                tasks.add(scheduledTask);
        }

        // Update scheduled tasks to clear version
        this.scheduledTasks.put(chatId, scheduledTasks);

        return tasks;
    }
}
