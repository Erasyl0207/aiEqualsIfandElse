import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.json.JSONObject;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.List;

import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Detection;
import com.google.cloud.translate.Translate.TranslateOption;
import com.google.cloud.translate.Translation;

public class Bot extends TelegramLongPollingBot {

    static String lastReceivedMessage = "";

    public void onUpdateReceived(Update update) {
        Translate translate = TranslateOptions.newBuilder().setApiKey("AIzaSyCD96ERQXnvOfW0TcqkWqjVntiY5WAvG_0").build().getService();
        if (update.hasMessage()) {
            long chatId = update.getMessage().getChatId();
            if (update.getMessage().hasText()) {
                String text = "";
                SendMessage message = new SendMessage().enableMarkdown(true).setChatId(chatId);

                String receivedMessage = update.getMessage().getText();

                if (receivedMessage.equals("/start")) {
                    text = "Давай начинай!";
                }
                else if (receivedMessage.startsWith("Покажи ")) {
                    if (receivedMessage.equals("Покажи погоду")) {
                        //59436e2fd907455871d1de4a52242816 - api
                        //api.openweathermap.org/data/2.5/weather?q={city name}&appid={API key} - ulr
                        String line;
                        BufferedReader reader;
                        StringBuilder response = new StringBuilder();

                        try {
                            HttpURLConnection connection =
                                    (HttpURLConnection) new URL("https://api.openweathermap.org/data/2.5/weather?q=Almaty&units=metric&appid=59436e2fd907455871d1de4a52242816").openConnection();
                            connection.setConnectTimeout(5000);
                            connection.setReadTimeout(5000);

                            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                            while ((line = reader.readLine()) != null) {
                                response.append(line).append("\n");
                            }
                            reader.close();
                            connection.disconnect();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        JSONObject obj = new JSONObject(response.toString());
                        double temp = obj.getJSONObject("main").getDouble("temp");
                        text = "Погода на данный момент " + temp + " градусов!";
                        try {
                            execute(message);
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }

                    } else if (receivedMessage.equals("Покажи курс валют")) {
                        double eurToKzt = getExchangeRate("eur");
                        double usdToKzt = getExchangeRate("usd");
                        double rubToKzt = getExchangeRate("rub");

                        double kztToEur = convertToKzt(eurToKzt);
                        double kztToUsd = convertToKzt(usdToKzt);
                        double kztToRub = convertToKzt(rubToKzt);

                        text = "*Changes in the exchange rate to KZT right now!*\n" +
                                "\n1 KZT ---> " + kztToUsd + " USD\n" +
                                "1 KZT ---> " + kztToEur + " EUR\n" +
                                "1 KZT ---> " + kztToRub + " RUB\n" +
                                "\n1 USD ---> " + usdToKzt + " KZT\n" +
                                "1 EUR ---> " + eurToKzt + " KZT\n" +
                                "1 RUB ---> " + rubToKzt + " KZT\n";
                    }
                }
                else if (receivedMessage.startsWith("Открой ")) {
                    if (!Desktop.isDesktopSupported()) {
                        text = "Desktop is not supported";
                    } else {
                        switch (receivedMessage) {
                            case "Открой калькулятор": {
                                File file = new File("C:/Windows/System32/calc.exe");
                                Desktop desktop = Desktop.getDesktop();
                                if (file.exists()) {
                                    try {
                                        desktop.open(file);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                                text = "Калькулятор успешно открылся";
                                break;
                            }
                            case "Открой блокнот": {
                                File file = new File("C:/Windows/System32/notepad.exe");
                                Desktop desktop = Desktop.getDesktop();
                                if (file.exists()) {
                                    try {
                                        desktop.open(file);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                                text = "Блокнот успешно открылся";
                                break;
                            }
                            case "Открой Google": {
                                //C:\Program Files (x86)\Google\Chrome\Application
                                File file = new File("C:/Program Files (x86)/Google/Chrome/Application/chrome.exe");
                                Desktop desktop = Desktop.getDesktop();
                                if (file.exists()) {
                                    try {
                                        desktop.open(file);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                                text = "Google успешно открылся";
                                break;
                            }
                        }
                    }
                }
                else if (receivedMessage.startsWith("Запрос :")) {
                    try {
                        Document document = Jsoup.connect("https://www.google.com/search?q" + receivedMessage.replace("Запрос :", "").replace(" ", "+")).get();
                        text = document.toString();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else if (receivedMessage.startsWith("Перевод")) {
                    text = "Введите текст:";
                } else if (lastReceivedMessage.equals("Перевод")) {
                    Detection detection = translate.detect(receivedMessage);
                    String detectedLanguage = detection.getLanguage();
                    Translation translation = translate.translate(
                            receivedMessage,
                            TranslateOption.sourceLanguage(detectedLanguage),
                            TranslateOption.targetLanguage("kk"));
                    text = translation.getTranslatedText();
                } else {
                    text = "*ERROR*\n";
                }
                message.setText(text);
                try {
                    execute(message);
                    lastReceivedMessage = receivedMessage;
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else if (update.getMessage().hasPhoto()) {
                List<PhotoSize> photos = update.getMessage().getPhoto();
                // Know file_id
                String file_id = Objects.requireNonNull(photos.stream().max(Comparator.comparing(PhotoSize::getFileSize))
                        .orElse(null)).getFileId();

                String line;
                BufferedReader reader;
                StringBuilder response = new StringBuilder();

                try {
                    HttpURLConnection connection = (HttpURLConnection) new URL("https://api.telegram.org/bot" + getBotToken() + "/getFile?file_id=" + file_id).openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);

                    reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    while ((line = reader.readLine()) != null) {
                        response.append(line).append("\n");

                    }
                    reader.close();
                    connection.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                JSONObject obj = new JSONObject(response.toString());
                String path = obj.getJSONObject("result").getString("file_path");
                String photo = "", result = "";

                try {
                    InputStream url = new URL("https://api.telegram.org/file/bot" + getBotToken() + "/" + path).openStream();
                    BufferedImage image = ImageIO.read(url);
                    photo = "C://Users/330S/Desktop/storageForAI/" + path.replace("photos/", "");
                    ImageIO.write(image, "jpg", new File(photo));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                ITesseract instance = new Tesseract();

                try {
                    result = instance.doOCR(new File(photo));
                } catch (TesseractException e) {
                    System.err.println(e.getMessage());
                }

                try {
                    File f = new File(path);
                    boolean b = f.delete();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                SendMessage message = new SendMessage()
                        .setChatId(chatId).setText(result);
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            } else if (update.getMessage().hasVoice()) {
                String text = "audio";

                String file_id = update.getMessage().getVoice().getFileId();
                String line;
                BufferedReader reader;
                StringBuilder response = new StringBuilder();
                try {
                    HttpURLConnection connection = (HttpURLConnection) new URL("https://api.telegram.org/bot" + getBotToken() + "/getFile?file_id=" + file_id).openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);

                    reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    while ((line = reader.readLine()) != null) {
                        response.append(line).append("\n");

                    }
                    reader.close();
                    connection.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                JSONObject obj = new JSONObject(response.toString());
                String path = obj.getJSONObject("result").getString("file_path");
                String audio = "", result = "";

                try {
                    InputStream url = new URL("https://api.telegram.org/file/bot" + getBotToken() + "/" + path).openStream();
                    audio = "C://Users/330S/Desktop/storageForAIAudio/" + path.replace("voice/", "").replace(".oga", ".mp3");
                    OutputStream outputStream = new FileOutputStream(new File(audio));
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = url.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, len);
                    }
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }


                SendMessage message = new SendMessage()
                        .setChatId(chatId).setText(file_id);
                try {
                    execute(message);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String getBotUsername() {
        return "aiEqualsIfandElseBot";
    }

    public String getBotToken() {
        return "1975387278:AAFMrpTUJ-_Vp4zpD4S90riWbN_CiLhC-Tw";
    }

    public double getExchangeRate(String currency) {
        String value = "";
        try {
            Document doc = Jsoup.connect("https://markets.businessinsider.com/currencies/realtime-list/" + currency + "-kzt").get();
            value = doc.getElementsByClass("box table_distance realtime-pricebox").get(0).getElementsByClass("push-data price").get(0).wholeText();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Double.parseDouble(value);
    }

    public double convertToKzt(double exchange) {
        double value = 1.0 / exchange;
        value = Double.parseDouble(String.format("%.6f", value).replace(",", "."));
        return value;
    }
}