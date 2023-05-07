package bot;


import model.*;
import org.telegram.telegrambots.bots.*;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.*;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.*;
import org.telegram.telegrambots.meta.exceptions.*;
import util.*;

import java.util.*;

public class Bot extends TelegramLongPollingBot {
    private final TaskManager taskManager = new TaskManager();
    private final Timer timer = new Timer();
    private String username;

    public Bot(String botToken) {
        super(botToken);
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            String messageText = message.getText();
            Long chatId = message.getChatId();

            if (messageText.equals("/start")) {
                Task task = taskManager.createTask(chatId);
                if (task == null) return;

                // Send new message with the task
                SendMessage newTaskMessage = createNewTaskMessage(chatId, task);
                Message taskMessage = sendMessage(newTaskMessage);

                if (taskMessage != null) // Message successfully sent
                    // Bind messageId for further editing
                    task.setMessageId(taskMessage.getMessageId());
            }

            if (message.isReply()) {
                Message repliedMessage = message.getReplyToMessage();
                Task currentTask = taskManager.getTaskInProgress(chatId);
                if (repliedMessage.getText().contains("title")) { // Change title
                    currentTask.setTitle(messageText);
                } else if (repliedMessage.getText().contains("description")) {
                    currentTask.setDescription(messageText);
                }
                deleteMessage(chatId, repliedMessage.getMessageId());
                updateTaskMessage(chatId, currentTask);
            }

            deleteMessage(chatId, message.getMessageId());
        } else if (update.hasCallbackQuery()) { // The button on the task
            System.out.println(update.getCallbackQuery().getMessage().getMessageId() + " " + update.getCallbackQuery().getData());

            CallbackQuery callbackQuery = update.getCallbackQuery();
            Long chatId = callbackQuery.getMessage().getChatId();

            String query = callbackQuery.getData();
            switch (query) {
                case "title" -> { // Change title
                    SendMessage newMessage = new SendMessage();
                    newMessage.setParseMode("Markdown");
                    newMessage.setChatId(chatId);
                    newMessage.setText("Reply with your *title*");
                    newMessage.setReplyMarkup(new ForceReplyKeyboard());
                    sendMessage(newMessage);
                }
                case "description" -> { // Change title
                    SendMessage newMessage = new SendMessage();
                    newMessage.setParseMode("Markdown");
                    newMessage.setChatId(chatId);
                    newMessage.setText("Reply with your *description*");
                    newMessage.setReplyMarkup(new ForceReplyKeyboard());
                    sendMessage(newMessage);
                }
                case "ok" -> {
                    Task task = taskManager.getTaskInProgress(chatId);
                    scheduleTask(String.valueOf(chatId), task);
                }
            }
        }
    }

    private void updateTaskMessage(Long chatId, Task task) {
        String text = """
                _New Task_
                                
                Title: *%s*
                Description: %s
                Date: `%s`""".formatted(task.getTitle(), task.getDescription(), task.getDateTime());

        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(chatId);
        editMessage.setMessageId(taskManager.getTaskInProgress(chatId).getMessageId());
        editMessage.setParseMode("Markdown");
        editMessage.setText(text);

        var inlineKeyboardMarkup = new InlineKeyboardMarkup(getTaskKeyboard());
        editMessage.setReplyMarkup(inlineKeyboardMarkup);

        try {
            execute(editMessage);
        } catch (TelegramApiException e) {
            System.out.println("An error occurred while editing a task message. " + e);
            e.printStackTrace();
        }
    }

    private SendMessage createNewTaskMessage(Long chatId, Task task) {
        String text = """
                _New Task_
                                
                Title: *%s*
                Description: %s
                Date: `%s`""".formatted(task.getTitle(), task.getDescription(), task.getDateTime());
        SendMessage message = new SendMessage();
        message.setText(text);
        message.setChatId(chatId);
        message.setParseMode("Markdown");

        List<List<InlineKeyboardButton>> keyboard = getTaskKeyboard();

        var inlineKeyboardMarkup = new InlineKeyboardMarkup(keyboard);
        message.setReplyMarkup(inlineKeyboardMarkup);

        return message;
    }

    private static List<List<InlineKeyboardButton>> getTaskKeyboard() {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        // 1st row
        List<InlineKeyboardButton> buttonList = new ArrayList<>();
        InlineKeyboardButton titleButton = new InlineKeyboardButton("Set title 📰");
        titleButton.setCallbackData("title");
        buttonList.add(titleButton);

        InlineKeyboardButton descriptionButton = new InlineKeyboardButton("Set description 📝");
        descriptionButton.setCallbackData("description");
        buttonList.add(descriptionButton);
        keyboard.add(buttonList);

        // 2nd row
        buttonList = new ArrayList<>();
        InlineKeyboardButton timeButton = new InlineKeyboardButton("Set time ⌚");
        timeButton.setCallbackData("time");
        buttonList.add(timeButton);

        InlineKeyboardButton dateButton = new InlineKeyboardButton("Set date 📅");
        dateButton.setCallbackData("date");
        buttonList.add(dateButton);
        keyboard.add(buttonList);

        // 3rd row
        buttonList = new ArrayList<>();
        InlineKeyboardButton okButton = new InlineKeyboardButton("Schedule ✅");
        okButton.setCallbackData("ok");
        buttonList.add(okButton);

        InlineKeyboardButton cancelButton = new InlineKeyboardButton("Cancel ❌");
        cancelButton.setCallbackData("cancel");
        buttonList.add(cancelButton);
        keyboard.add(buttonList);
        return keyboard;
    }


    private void scheduleTask(String chatId, Task task) {
        // Acknowledge task
        SendMessage acknowledgement = new SendMessage(chatId, "_I will send you:_```\n" + task + " ```");
        acknowledgement.setParseMode("Markdown");
        sendMessage(acknowledgement);

        // Schedule task
        SendMessage taskMessage = new SendMessage(chatId, task.toString());
        setTimer(() -> sendMessage(taskMessage), task.getMillisBeforeStart());
    }

    /**
     * Delay must be in milliseconds
     */
    private void setTimer(Runnable task, long delay) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                task.run();
            }
        }, delay);
    }

    /**
     * Delay must be in milliseconds
     * RepeatTime - amount of time between the task scheduling
     */
    private void setTimer(Runnable task, long delay, long repeatTime) {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                task.run();
            }
        }, delay, repeatTime);
    }

    private Message sendMessage(SendMessage message) {
        try {
            return execute(message);
        } catch (TelegramApiException e) {
            System.out.println("An error occurred while sending a message. " + e);
            e.printStackTrace();
        }
        return null;
    }

    public boolean deleteMessage(Long chatId, Integer messageId) {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(chatId);
        deleteMessage.setMessageId(messageId);

        try {
            return execute(deleteMessage);
        } catch (TelegramApiException e) {
            System.out.println("An error occurred while deleting a message. " + e);
            e.printStackTrace();
        }
        return false;
    }


    @Override
    public String getBotUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}

