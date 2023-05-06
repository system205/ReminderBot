package bot;


import org.telegram.telegrambots.bots.*;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.*;

public class Bot extends TelegramLongPollingBot {

    public Bot(String botToken) {
        super(botToken);
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            String messageText = message.getText();
            String chatId = String.valueOf(message.getChatId());

            SendMessage newMessage = new SendMessage(chatId, messageText);
            sendMessage(newMessage);
        }
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

