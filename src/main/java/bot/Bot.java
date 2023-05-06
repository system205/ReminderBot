package bot;


import model.*;
import org.telegram.telegrambots.bots.*;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.*;
import util.*;

import java.util.*;

public class Bot extends TelegramLongPollingBot {
    private final Timer timer = new Timer();
    public Bot(String botToken) {
        super(botToken);
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            String messageText = message.getText();
            String chatId = String.valueOf(message.getChatId());

            Task task = MessageParser.extractTask(messageText);

            // Schedule task
            SendMessage taskMessage = new SendMessage(chatId, task.toString());
            setTimer(() -> sendMessage(taskMessage), task.getMillisBeforeStart());

            // Acknowledge task
            SendMessage acknowledgement = new SendMessage(chatId, "I will send you: " + task);
            sendMessage(acknowledgement);

        }
    }

    /**
     * Delay must be in milliseconds
     * */
    private void setTimer(Runnable task, long delay){
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
     * */
    private void setTimer(Runnable task, long delay, long repeatTime){
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                task.run();
            }
        }, delay, repeatTime);
    }

    private void sendMessage(SendMessage message){
        try {
            execute(message);
        } catch (TelegramApiException e) {
            System.out.println("An error occurred while sending a message. " + e);
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return "TestBot";
    }

}

