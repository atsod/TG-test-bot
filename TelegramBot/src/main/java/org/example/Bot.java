package org.example;

import org.example.entity.Currency;
import org.example.service.CurrencyConversionService;
import org.example.service.CurrencyModeService;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class Bot extends TelegramLongPollingBot {

    private final CurrencyModeService currencyModeService = CurrencyModeService.getInstance();

    private final CurrencyConversionService currencyConversionService = CurrencyConversionService.getInstance();

    @Override
    public String getBotUsername() {
        return "@test_1254535452525Bot";
    }

    @Override
    public String getBotToken() {
        return "5601438752:AAGb8RbTkoyVQ43y4hcffpwT2vmSkKgjIXk";
    }

    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasCallbackQuery()) {
            try {
                handleCallback(update.getCallbackQuery());
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
        else if(update.hasMessage()) {
            try {
                handleMessage(update.getMessage());
            } catch (TelegramApiException | IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void handleCallback(CallbackQuery callbackQuery) throws TelegramApiException {
        Message message = callbackQuery.getMessage();
        String[] param = callbackQuery.getData().split(":");
        String action = param[0];
        Currency newCurrency = Currency.valueOf(param[1]);
        switch (action) {
            case "ORIGINAL" -> currencyModeService.setOriginalCurrency(message.getChatId(), newCurrency);
            case "TARGET" -> currencyModeService.setTargetCurrency(message.getChatId(), newCurrency);
        }
        List<List<InlineKeyboardButton>> buttons = buttonsCreating(message);
        execute(EditMessageReplyMarkup.builder()
                .chatId(message.getChatId())
                .messageId(message.getMessageId())
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(buttons).build())
                .build());
    }

    private void handleMessage(Message message) throws TelegramApiException, IOException {
        //handle command
        if(message.hasText() && message.hasEntities()) {
            Optional<MessageEntity> commandEntity = message.getEntities()
                    .stream()
                    .filter(e -> "bot_command".equals(e.getType()))
                    .findFirst();
            if(commandEntity.isPresent()) {
                String command = message.getText().substring(commandEntity.get().getOffset(),
                        commandEntity.get().getLength());
                switch (command) {
                    case "/start":
                        execute(SendMessage.builder()
                                .chatId(message.getChatId())
                                .text("Welcome to my test bot! \n Here you can: \n\n 1) Convert currency")
                                .build());
                        break;
                    case "/set_currency":
                        List<List<InlineKeyboardButton>> buttons = buttonsCreating(message);
                        execute(SendMessage.builder()
                                .chatId(message.getChatId())
                                .text("Please choose Original and Target currencies")
                                .replyMarkup(InlineKeyboardMarkup.builder()
                                        .keyboard(buttons)
                                        .build())
                                .build());
                        break;
                }
            }
        }
        else if(message.hasText()) {
            String messageText = message.getText();
            Optional<Double> value = parseDouble(messageText);
            Currency originalCurrency = currencyModeService.getOriginalCurrency(message.getChatId());
            Currency targetCurrency = currencyModeService.getTargetCurrency(message.getChatId());
            double conversionRatio = currencyConversionService.getConversionRatio(originalCurrency, targetCurrency);
            if(value.isPresent()) execute(SendMessage.builder()
                    .chatId(message.getChatId())
                    .text(String.format("%4.2f %s is %4.2f %s",
                            value.get(),
                            originalCurrency,
                            (value.get() * conversionRatio),
                            targetCurrency))
                    .build());
        }
    }

    private Optional<Double> parseDouble(String messageText) {
        try {
            return Optional.of(Double.parseDouble(messageText));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private List<List<InlineKeyboardButton>> buttonsCreating(Message message) {
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        Currency originalCurrency = currencyModeService.getOriginalCurrency(message.getChatId());
        Currency targetCurrency = currencyModeService.getTargetCurrency(message.getChatId());
        for(Currency currency : Currency.values()) {
            buttons.add(Arrays.asList(
                    InlineKeyboardButton.builder()
                            .text(getCurrencyButton(originalCurrency, currency))
                            .callbackData("ORIGINAL:" + currency)
                            .build(),
                    InlineKeyboardButton.builder()
                            .text(getCurrencyButton(targetCurrency, currency))
                            .callbackData("TARGET:" + currency)
                            .build()));
        }
        return buttons;
    }

    private String getCurrencyButton(Currency saved, Currency current) {
        return saved == current ? current + " ⭐️" : current.name();
    }

    public static void main(String[] args) throws TelegramApiException {
        Bot bot = new Bot();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(bot);
    }
}
