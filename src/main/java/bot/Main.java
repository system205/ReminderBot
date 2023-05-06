
package bot;

import org.telegram.telegrambots.meta.*;
import org.telegram.telegrambots.meta.exceptions.*;
import org.telegram.telegrambots.updatesreceivers.*;

import java.io.*;
import java.util.*;

class Main {
    public static void main(String[] args) {
        try {
            // Instantiate Telegram Bots API
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            // Register the bot
            botsApi.registerBot(new Bot(getBotToken()));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private static String getBotToken()  {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream("src/main/resources/config.properties")){
            props.load(in);
        } catch (IOException e) {
            System.err.println("Unable to read config.properties file. " + e);
            e.printStackTrace();
        }

        String token = props.getProperty("bot.token");
//        System.out.println(token);

        return token;
    }
}