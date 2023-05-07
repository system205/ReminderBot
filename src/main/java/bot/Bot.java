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
    private String username;

    private final Timer timer = new Timer();

    public Bot(String botToken) {
        super(botToken);
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            String messageText = message.getText();
            Long chatId = message.getChatId();

            // Delete received message
            deleteMessage(chatId, message.getMessageId());

            if (messageText.equals("/start")) {
                Task task = taskManager.createTask(chatId);
                if (task == null) return;

                // Delete previous message with editable task if exists
                if (task.getMessageId() != null) {
                    deleteMessage(chatId, task.getMessageId());
                }

                // Send new message with the task
                SendMessage newTaskMessage = createNewTaskMessage(chatId, task);
                Message taskMessage = sendMessage(newTaskMessage);

                if (taskMessage != null) // Message successfully sent
                    // Bind messageId for further editing
                    task.setMessageId(taskMessage.getMessageId());
            }


            if (message.isReply()) { // Reply to change editable task message
                Message repliedMessage = message.getReplyToMessage();
                // Delete prompt message which was replied
                deleteMessage(chatId, repliedMessage.getMessageId());

                // Get currently editable task of the chat
                Task currentTask = taskManager.getEditableTask(chatId);
                if (currentTask == null) {
                    System.out.println("There isn't editable task. Try /start"); // TODO
                    return;
                }

                // Change the task content
                if (repliedMessage.getText().contains("title")) {
                    currentTask.setTitle(messageText);
                } else if (repliedMessage.getText().contains("description")) {
                    currentTask.setDescription(messageText);
                }

                // Edit message with task creation
                updateTaskMessage(chatId, currentTask);
            }


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
                case "description" -> { // Change description
                    SendMessage newMessage = new SendMessage();
                    newMessage.setParseMode("Markdown");
                    newMessage.setChatId(chatId);
                    newMessage.setText("Reply with your *description*");
                    newMessage.setReplyMarkup(new ForceReplyKeyboard());
                    sendMessage(newMessage);
                }
                case "ok" -> {
                    // Check that task is assigned to the message where clicked
                    Task task = taskManager.getEditableTask(chatId);
                    if (task == null || !task.getMessageId().equals(callbackQuery.getMessage().getMessageId())) {
                        // There is an extra message (zombie). Happens after program reloading
                        // Tell a user about it and delete this occasion.
                        Message newMessage = sendMessage(chatId, "Sorry. This message was obsolete. Use /start 😅");
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                deleteMessage(chatId, newMessage.getMessageId());
                            }
                        }, 3000);
                        deleteMessage(chatId, callbackQuery.getMessage().getMessageId());
                        return;
                    }

                    // Try to schedule task. Wrong date might cause a failure
                    boolean scheduled = taskManager.scheduleTask(chatId, this);
                    if (!scheduled) {
                        sendMessage(chatId, "Sorry, check the date and time. Or cancel and /start");
                        return;
                    }

                    // Acknowledge scheduling to the user
                    Message newMessage = sendMessage(chatId, "Your task is scheduled!");
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            deleteMessage(chatId, newMessage.getMessageId());
                        }
                    }, 5000);

                    // Delete editable message with the scheduled task
                    deleteMessage(chatId, callbackQuery.getMessage().getMessageId());
                }
                case "cancel" -> {
                    taskManager.cancelTask(chatId);
                    deleteMessage(chatId, callbackQuery.getMessage().getMessageId());
                }
            }
        }
    }

    private void updateTaskMessage(Long chatId, Task task) {
        String text = getFormattedNewTaskText(task);

        EditMessageText editMessage = new EditMessageText();
        editMessage.setChatId(chatId);
        editMessage.setMessageId(taskManager.getEditableTask(chatId).getMessageId());
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

    private static String getFormattedNewTaskText(Task task) {
        return """
                *New task.* _Press buttons to edit_ ☺
                                
                _Title:_ *%s*
                                
                _Description:_ %s
                                
                _Date:_ `%s`""".formatted(task.getTitle(), task.getDescription(), task.getDateTime());
    }

    private SendMessage createNewTaskMessage(Long chatId, Task task) {
        String text = getFormattedNewTaskText(task);
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


    public Message sendMessage(SendMessage message) {
        try {
            return execute(message);
        } catch (TelegramApiException e) {
            System.out.println("An error occurred while sending a message. " + e);
            e.printStackTrace();
        }
        return null;
    }

    public Message sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
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

