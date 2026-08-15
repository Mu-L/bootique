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

package io.bootique.cli;

import io.bootique.BootiqueException;
import io.bootique.meta.application.OptionMetadata;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CliParserTest {

    static final CliParser parser = new CliParser(List.of(
            OptionMetadata.builder("me").shortName('m').build(),
            OptionMetadata.builder("val").shortName('v').valueOptional("x").build(),
            OptionMetadata.builder("req").shortName('r').valueRequired("x").build(),
            OptionMetadata.builder("def").shortName('d').valueOptionalWithDefault("x", "DEF").build(),

            // options whose short names were suppressed by ApplicationMetadata due to conflicts
            OptionMetadata.builder("noshort").shortName(null).build(),
            OptionMetadata.builder("n").shortName(null).build()
    ));

    static ParsedArgs parse(String... args) {
        return parser.parse(args);
    }

    @Test
    public void noArgs() {
        ParsedArgs parsed = parse();
        assertFalse(parsed.hasOption("me"));
        assertEquals(List.of(), parsed.detectedOptions());
        assertEquals(List.of(), parsed.standaloneArguments());
    }

    @Test
    public void longAndShortForms() {
        assertTrue(parse("--me").hasOption("me"));
        assertTrue(parse("-m").hasOption("me"));

        // an option can be looked up by either of its names
        assertTrue(parse("-m").hasOption("m"));
        assertFalse(parse("-m").hasOption("val"));

        // a single-dash token spelling out a full option name is that option, not a cluster
        assertTrue(parse("-me").hasOption("me"));
    }

    @Test
    public void suppressedShortName() {
        assertTrue(parse("--noshort").hasOption("noshort"));

        // suppressing a short name only takes away the single-char form, a single-dash full name still works
        assertTrue(parse("-noshort").hasOption("noshort"));
        assertThrows(BootiqueException.class, () -> parse("-N"));

        // a single-char full name is still usable in both forms
        assertTrue(parse("--n").hasOption("n"));
        assertTrue(parse("-n").hasOption("n"));
    }

    @Test
    public void optionalValue() {
        assertEquals(List.of(), parse("--val").optionStrings("val"));
        assertEquals(List.of("1"), parse("--val=1").optionStrings("val"));
        assertEquals(List.of("1"), parse("--val", "1").optionStrings("val"));
        assertEquals(List.of("1"), parse("-v", "1").optionStrings("val"));
        assertEquals(List.of("1"), parse("-v=1").optionStrings("val"));
        assertEquals(List.of("1"), parse("-v1").optionStrings("val"));

        // an empty value is the same as no value
        assertEquals(List.of(), parse("--val=").optionStrings("val"));
    }

    @Test
    public void optionalValue_NotStolenFromOptions() {
        ParsedArgs parsed = parse("--val", "--me");
        assertEquals(List.of(), parsed.optionStrings("val"));
        assertTrue(parsed.hasOption("me"));

        // "--" is not consumed as a value either
        assertEquals(List.of(), parse("--val", "--", "a").optionStrings("val"));
        assertEquals(List.of("a"), parse("--val", "--", "a").standaloneArguments());
    }

    @Test
    public void requiredValue() {
        assertEquals(List.of("1"), parse("--req=1").optionStrings("req"));
        assertEquals(List.of("1"), parse("--req", "1").optionStrings("req"));
        assertEquals(List.of("1"), parse("-r", "1").optionStrings("req"));
        assertEquals(List.of("1"), parse("-r1").optionStrings("req"));

        // unlike an optional value, a required one is taken from the next arg even if it looks like an option
        assertEquals(List.of("--me"), parse("--req", "--me").optionStrings("req"));
    }

    @Test
    public void requiredValue_Missing() {
        BootiqueException e = assertThrows(BootiqueException.class, () -> parse("--req"));
        assertEquals("Option r/req requires an argument", e.getMessage());
        assertThrows(BootiqueException.class, () -> parse("--req="));
    }

    @Test
    public void defaultValue() {
        assertEquals(List.of("DEF"), parse("--def").optionStrings("def"));
        assertEquals(List.of("DEF"), parse("-d").optionStrings("def"));
        assertEquals(List.of("1"), parse("--def=1").optionStrings("def"));

        // a default value is only applied to an option present on the command line
        assertEquals(List.of(), parse("--me").optionStrings("def"));
    }

    @Test
    public void noValueOption_IgnoresValue() {
        assertTrue(parse("--me=whatever").hasOption("me"));
        assertEquals(List.of(), parse("--me=whatever").optionStrings("me"));
    }

    @Test
    public void multipleValues() {
        ParsedArgs parsed = parse("--val=1", "--val", "--val", "2");
        assertEquals(List.of("1", "2"), parsed.optionStrings("val"));
        assertEquals(3, parsed.detectedOptions().size());
    }

    @Test
    public void shortOptionCluster() {
        ParsedArgs parsed = parse("-mn");
        assertTrue(parsed.hasOption("me"));
        assertTrue(parsed.hasOption("n"));

        // an option taking a value swallows the rest of the cluster
        assertEquals(List.of("m"), parse("-vm").optionStrings("val"));
        assertFalse(parse("-vm").hasOption("me"));

        // ... but a value-less option preceding it does not
        parsed = parse("-mv1");
        assertTrue(parsed.hasOption("me"));
        assertEquals(List.of("1"), parsed.optionStrings("val"));
    }

    @Test
    public void unrecognizedOption() {
        BootiqueException e = assertThrows(BootiqueException.class, () -> parse("--bogus"));
        assertEquals("bogus is not a recognized option", e.getMessage());

        assertThrows(BootiqueException.class, () -> parse("-x"));
        assertThrows(BootiqueException.class, () -> parse("-mx"));

        // names are matched exactly, a name prefix resolves to nothing
        assertThrows(BootiqueException.class, () -> parse("--nosh"));
    }

    @Test
    public void standaloneArguments() {
        assertEquals(List.of("a", "b"), parse("a", "--me", "b").standaloneArguments());

        // a lone dash is a standalone argument
        assertEquals(List.of("-"), parse("-").standaloneArguments());

        // everything past "--" is standalone, options included
        assertEquals(List.of("a", "--me", "-v"), parse("--val=1", "a", "--", "--me", "-v").standaloneArguments());
        assertFalse(parse("--", "--me").hasOption("me"));
    }

    @Test
    public void detectedOptions_CliOrder() {
        List<OptionMetadata> detected = parse("--val=1", "--me", "-d", "--val=2").detectedOptions();
        assertEquals(List.of("val", "me", "def", "val"), detected.stream().map(OptionMetadata::getName).toList());
    }
}
