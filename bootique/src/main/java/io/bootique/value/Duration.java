/*
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.bootique.value;

import com.fasterxml.jackson.annotation.JsonValue;

import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a time duration value. Used as a value object to deserialize durations in application configurations.
 */
public class Duration implements Comparable<Duration> {

    public static final Duration ZERO = new Duration(java.time.Duration.ZERO.toMillis());
    private static final Pattern TOKENIZER = Pattern.compile("^([0-9]*\\.?[0-9]+)\\s*([a-z]+)$");
    private static final Map<String, TemporalUnit> UNIT_VOCABULARY;

    static {
        UNIT_VOCABULARY = new HashMap<>();
        UNIT_VOCABULARY.put("ms", ChronoUnit.MILLIS);
        UNIT_VOCABULARY.put("s", ChronoUnit.SECONDS);
        UNIT_VOCABULARY.put("sec", ChronoUnit.SECONDS);
        UNIT_VOCABULARY.put("second", ChronoUnit.SECONDS);
        UNIT_VOCABULARY.put("seconds", ChronoUnit.SECONDS);
        UNIT_VOCABULARY.put("min", ChronoUnit.MINUTES);
        UNIT_VOCABULARY.put("minute", ChronoUnit.MINUTES);
        UNIT_VOCABULARY.put("minutes", ChronoUnit.MINUTES);
        UNIT_VOCABULARY.put("h", ChronoUnit.HOURS);
        UNIT_VOCABULARY.put("hr", ChronoUnit.HOURS);
        UNIT_VOCABULARY.put("hrs", ChronoUnit.HOURS);
        UNIT_VOCABULARY.put("hour", ChronoUnit.HOURS);
        UNIT_VOCABULARY.put("hours", ChronoUnit.HOURS);
        UNIT_VOCABULARY.put("d", ChronoUnit.DAYS);
        UNIT_VOCABULARY.put("day", ChronoUnit.DAYS);
        UNIT_VOCABULARY.put("days", ChronoUnit.DAYS);
    }

    private final java.time.Duration duration;
    private final String stringDuration;

    /**
     * Creates a Duration instance from a String representation. The String has a numeric part, an optional space and
     * a unit part. E.g. "3ms", "5 sec", "5 s".
     *
     * @param value a String value representing duration.
     */
    public Duration(String value) {
        this.duration = parse(value);
        this.stringDuration = value;
    }

    /**
     * Creates a Duration instance from milliseconds.
     *
     * @param value duration in milliseconds.
     */
    public Duration(long value) {
        this.duration = java.time.Duration.ofMillis(value);
        this.stringDuration = value + "ms";
    }

    static java.time.Duration parse(String value) {
        Objects.requireNonNull(value, "Null 'value' argument");

        if (value.isEmpty()) {
            throw new IllegalArgumentException("Empty 'value' argument");
        }

        Matcher matcher = TOKENIZER.matcher(value);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid Duration format: " + value);
        }

        TemporalUnit unit = parseUnit(matcher.group(2));
        double amount = parseAmount(matcher.group(1));
        return createDuration(amount, unit);
    }

    private static double parseAmount(String amount) {
        try {
            return Double.parseDouble(amount);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid time amount: " + amount);
        }
    }

    private static TemporalUnit parseUnit(String unitString) {
        TemporalUnit unit = UNIT_VOCABULARY.get(unitString);
        if (unit == null) {
            throw new IllegalArgumentException("Invalid time unit: " + unitString);
        }

        return unit;
    }

    private static java.time.Duration createDuration(double amount, TemporalUnit unit) {
        if (amount == Math.floor(amount)) {
            return java.time.Duration.of((long) amount, unit);
        } else {
            return java.time.Duration.of(convert(amount, unit), ChronoUnit.MILLIS);
        }
    }

    private static long convert(double amount, TemporalUnit unit) {
        return switch ((ChronoUnit) unit) {
            case DAYS -> Math.round(amount * 24 * 60 * 60 * 1000);
            case HOURS -> Math.round(amount * 60 * 60 * 1000);
            case MINUTES -> Math.round(amount * 60 * 1000);
            case SECONDS -> Math.round(amount * 1000);
            default -> throw new IllegalArgumentException("Invalid time amount: " + amount);
        };
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Duration d && duration.equals(d.duration);
    }

    @Override
    public int hashCode() {
        return duration.hashCode();
    }

    public java.time.Duration getDuration() {
        return duration;
    }

    @Override
    public int compareTo(Duration o) {
        return duration.compareTo(o.duration);
    }

    @Override
    @JsonValue
    public String toString() {
        return stringDuration;
    }
}
