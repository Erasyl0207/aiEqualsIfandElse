import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

/**
 *
 * @author Erasyl Kyrykbay
 */

public class Main {
    public static void main(String[] args) {

        ApiContextInitializer.init();

        TelegramBotsApi api = new TelegramBotsApi();

        try{
            api.registerBot(new Bot());
        }catch (TelegramApiRequestException e){
            e.printStackTrace();
        }

    }
}
