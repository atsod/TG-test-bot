package org.example.service;

import org.example.entity.Currency;
import org.example.service.impl.NbrbCurrencyConversionService;

import java.io.IOException;
import java.net.MalformedURLException;

public interface CurrencyConversionService {

    static CurrencyConversionService getInstance() {return new NbrbCurrencyConversionService();}

    double getConversionRatio(Currency original, Currency target) throws IOException;
}
