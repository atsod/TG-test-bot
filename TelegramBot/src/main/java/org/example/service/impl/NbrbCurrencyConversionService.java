package org.example.service.impl;

import org.example.entity.Currency;
import org.example.service.CurrencyConversionService;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class NbrbCurrencyConversionService implements CurrencyConversionService {

    @Override
    public double getConversionRatio(Currency original, Currency target) throws IOException {
        double originalRate = getRate(original);
        double targetRate = getRate(target);
        return originalRate / targetRate;
    }

    private double getRate(Currency currency) throws IOException {
        if(currency == Currency.BYN) {
            return 1;
        }
        URL url = new URL("https://www.nbrb.by/api/exrates/rates/" + currency.getId());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = reader.readLine()) != null) {
            response.append(inputLine);
        }
        reader.close();
        JSONObject json = new JSONObject(response.toString());
        double rate = json.getDouble("Cur_OfficialRate");
        double scale = json.getDouble("Cur_Scale");
        return rate / scale;
    }
}
