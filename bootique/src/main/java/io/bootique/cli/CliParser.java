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
import io.bootique.meta.application.OptionValueCardinality;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses app command line arguments against a known set of options.
 *
 * <p>An option is referenced either by its full name behind a double dash ({@code --opt}), or by its single-char
 * short name behind a single dash ({@code -o}). An option value is either attached with a "="
 * ({@code --opt=value}, {@code -o=value}) or provided as the next argument ({@code --opt value}, {@code -o value}).
 *
 * <p>A single dash is always followed by single-char names, so a longer token behind it is a cluster of such names,
 * and never a full name. I.e. {@code -abc} means "-a -b -c", and {@code -opt} is not the same as {@code --opt}. The
 * first option in a cluster that takes a value consumes the rest of the cluster as that value, so both
 * {@code -abovalue} and {@code -abo value} pass "value" to option "o".
 *
 * <p>Full names are matched exactly. So {@code --conf} will not be matched to an option named "config".
 *
 * <p>A standalone "--" terminates option parsing, so everything following it is treated as a standalone argument.
 *
 * @since 4.0
 */
public class CliParser {

    private static final String HYPHEN = "-";
    private static final String DOUBLE_HYPHEN = "--";

    private final Map<String, OptionMetadata> optionsByAnyName;

    public CliParser(Collection<OptionMetadata> options) {
        this.optionsByAnyName = indexByAnyName(options);
    }

    private static Map<String, OptionMetadata> indexByAnyName(Collection<OptionMetadata> options) {
        Map<String, OptionMetadata> index = new HashMap<>();

        // index short names first, so that in case of an overlap a full name of another option would win
        for (OptionMetadata o : options) {
            if (o.getShortName() != null) {
                index.put(o.getShortName(), o);
            }
        }

        for (OptionMetadata o : options) {
            index.put(o.getName(), o);
        }

        return index;
    }

    /**
     * Parses the provided arguments, throwing {@link io.bootique.BootiqueException} if they can not be matched to the known
     * options.
     */
    public ParsedArgs parse(String[] args) {
        return new ParserState(args).parse();
    }

    private OptionMetadata optionFor(String name) {
        OptionMetadata option = optionsByAnyName.get(name);
        if (option == null) {
            throw new BootiqueException(1, name + " is not a recognized option");
        }

        return option;
    }

    private class ParserState {

        private final String[] args;
        private final List<OptionMetadata> detectedOptions;
        private final Map<String, List<String>> valuesByOptionName;
        private final List<String> standaloneArguments;

        private int next;
        private boolean noMoreOptions;

        ParserState(String[] args) {
            this.args = args;
            this.detectedOptions = new ArrayList<>();
            this.valuesByOptionName = new HashMap<>();
            this.standaloneArguments = new ArrayList<>();
        }

        ParsedArgs parse() {

            while (next < args.length) {
                String arg = args[next++];

                if (noMoreOptions) {
                    standaloneArguments.add(arg);
                } else if (isOptionTerminator(arg)) {
                    noMoreOptions = true;
                } else if (isLongOptionToken(arg)) {
                    onLongOptionToken(arg);
                } else if (isShortOptionToken(arg)) {
                    onShortOptionToken(arg);
                } else {
                    standaloneArguments.add(arg);
                }
            }

            return new ParsedArgs(detectedOptions, valuesByOptionName, standaloneArguments);
        }

        private void onLongOptionToken(String arg) {
            int eq = arg.indexOf('=', DOUBLE_HYPHEN.length());
            String name = eq < 0 ? arg.substring(DOUBLE_HYPHEN.length()) : arg.substring(DOUBLE_HYPHEN.length(), eq);
            String value = eq < 0 ? null : arg.substring(eq + 1);

            onOption(optionFor(name), value);
        }

        private void onShortOptionToken(String arg) {
            int eq = arg.indexOf('=', HYPHEN.length());
            String name = eq < 0 ? arg.substring(HYPHEN.length()) : arg.substring(HYPHEN.length(), eq);
            String value = eq < 0 ? null : arg.substring(eq + 1);

            // only single-char names are allowed behind a single dash, so anything longer is a cluster of such
            // names, and never a full name. I.e. "-opt" is not the same as "--opt"
            if (name.length() == 1) {
                onOption(optionFor(name), value);
            } else {
                onShortOptionCluster(arg, name);
            }
        }

        private void onShortOptionCluster(String arg, String name) {
            char[] chars = arg.substring(HYPHEN.length()).toCharArray();
            validateShortOptionCluster(chars, name);

            for (int i = 0; i < chars.length; i++) {
                OptionMetadata option = optionFor(String.valueOf(chars[i]));

                // an option taking a value swallows the rest of the cluster as its value
                if (acceptsValue(option) && i + 1 < chars.length) {
                    onOption(option, new String(chars, i + 1, chars.length - i - 1));
                    return;
                }

                onOption(option, null);
            }
        }

        private void validateShortOptionCluster(char[] chars, String name) {
            // stop validating past the first option taking a value, as the rest of the cluster is that option's value
            for (char c : chars) {
                OptionMetadata option = optionsByAnyName.get(String.valueOf(c));
                if (option == null) {
                    throw new BootiqueException(1, unrecognizedInClusterMessage(c, name));
                }

                if (acceptsValue(option)) {
                    return;
                }
            }
        }

        private String unrecognizedInClusterMessage(char c, String name) {
            // a common reason for a cluster not to resolve is an attempt to spell a full option name behind a
            // single dash, so provide a hint when that's what happened
            return optionsByAnyName.containsKey(name)
                    ? c + " is not a recognized option. Did you mean \"--" + name + "\"?"
                    : c + " is not a recognized option";
        }

        private void onOption(OptionMetadata option, String value) {

            if (!acceptsValue(option)) {
                addOption(option);
                return;
            }

            if (value != null && !value.isEmpty()) {
                addOption(option, value);
                return;
            }

            switch (option.getValueCardinality()) {
                case REQUIRED -> {
                    if (next >= args.length) {
                        throw new BootiqueException(1, "Option " + allNames(option) + " requires an argument");
                    }
                    addOption(option, args[next++]);
                }
                case OPTIONAL -> {
                    if (next < args.length && !looksLikeAnOption(args[next])) {
                        addOption(option, args[next++]);
                    } else {
                        addOption(option);
                    }
                }
                default -> addOption(option);
            }
        }

        private void addOption(OptionMetadata option) {
            detectedOptions.add(option);
            valuesByOptionName.computeIfAbsent(option.getName(), n -> new ArrayList<>(3));
        }

        private void addOption(OptionMetadata option, String value) {
            detectedOptions.add(option);
            valuesByOptionName.computeIfAbsent(option.getName(), n -> new ArrayList<>(3)).add(value);
        }

        private static String allNames(OptionMetadata option) {
            return option.getShortName() != null
                    ? option.getShortName() + "/" + option.getName()
                    : option.getName();
        }

        private static boolean acceptsValue(OptionMetadata option) {
            return option.getValueCardinality() != OptionValueCardinality.NONE;
        }

        private static boolean isOptionTerminator(String arg) {
            return DOUBLE_HYPHEN.equals(arg);
        }

        private static boolean isLongOptionToken(String arg) {
            return arg.startsWith(DOUBLE_HYPHEN) && !isOptionTerminator(arg);
        }

        private static boolean isShortOptionToken(String arg) {
            return arg.startsWith(HYPHEN) && !HYPHEN.equals(arg) && !isLongOptionToken(arg);
        }

        private static boolean looksLikeAnOption(String arg) {
            return isShortOptionToken(arg) || isLongOptionToken(arg);
        }
    }
}
