
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

            // Init bot
            Bot bot = new Bot(getPropertyFromConfig("bot.token"));
            bot.setUsername(getPropertyFromConfig("bot.username"));

            // Register the bot
            botsApi.registerBot(bot);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private static String getPropertyFromConfig(String propertyName)  {
        Properties props = new Properties();

        // Load properties from the config.properties
        try (FileInputStream in = new FileInputStream("src/main/resources/config.properties")){
            props.load(in);
        } catch (IOException e) {
            System.err.println("Unable to read config.properties file. " + e);
            e.printStackTrace();
        }

        return props.getProperty(propertyName);
    }

}