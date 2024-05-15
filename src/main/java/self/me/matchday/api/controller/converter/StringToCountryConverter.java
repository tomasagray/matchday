/*
 * Copyright (c) 2022.
 *
 * This file is part of Matchday.
 *
 * Matchday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Matchday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Matchday.  If not, see <http://www.gnu.org/licenses/>.
 */

package self.me.matchday.api.controller.converter;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.Converter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import self.me.matchday.model.Country;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class StringToCountryConverter implements Converter<String, Country> {

    @Value("${artwork.flag-uri-format}")
    private String FLAG_URI_FORMATTER;

    @Override
    public Country convert(@NotNull String name) {

        final List<Locale> locales =
                Arrays.stream(Locale.getAvailableLocales())
                        .filter(locale -> locale.getDisplayCountry().equals(name))
                        .filter(locale -> locale.toString().matches("[a-z]{2}_[A-Z]{2}"))
                        .collect(Collectors.toList());
        if (!locales.isEmpty()) {
            final Locale primaryLocale = locales.get(0);
            final String countryCode = primaryLocale.getCountry();
            final String flagPath = String.format(FLAG_URI_FORMATTER, countryCode);
            return new Country(name, locales, flagPath);
        } else {
            throw new IllegalArgumentException("No country matching: " + name);
        }
    }

    @Override
    public JavaType getInputType(@NotNull TypeFactory typeFactory) {
        return typeFactory.constructType(String.class);
    }

    @Override
    public JavaType getOutputType(@NotNull TypeFactory typeFactory) {
        return typeFactory.constructType(Country.class);
    }
}
