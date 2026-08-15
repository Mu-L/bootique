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

import io.bootique.meta.application.OptionMetadata;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The result of parsing an app command line by {@link CliParser}. Options can be looked up either by their full or
 * short name.
 *
 * @since 4.0
 */
public class ParsedArgs {

    private final List<OptionMetadata> detectedOptions;
    private final Map<String, List<String>> valuesByOptionName;
    private final List<String> standaloneArguments;

    // an index of the detected options by every name they can be referenced by (i.e. full and short)
    private final Map<String, OptionMetadata> detectedOptionsByAnyName;

    protected ParsedArgs(
            List<OptionMetadata> detectedOptions,
            Map<String, List<String>> valuesByOptionName,
            List<String> standaloneArguments) {

        this.detectedOptions = List.copyOf(detectedOptions);
        this.valuesByOptionName = valuesByOptionName;
        this.standaloneArguments = List.copyOf(standaloneArguments);
        this.detectedOptionsByAnyName = indexByAnyName(detectedOptions);
    }

    private static Map<String, OptionMetadata> indexByAnyName(List<OptionMetadata> options) {
        Map<String, OptionMetadata> index = new HashMap<>();
        for (OptionMetadata o : options) {
            index.put(o.getName(), o);
            if (o.getShortName() != null) {
                index.putIfAbsent(o.getShortName(), o);
            }
        }
        return index;
    }

    /**
     * Returns the options present on the command line in the order of their appearance. An option that was specified
     * more than once is present in the returned List more than once.
     */
    public List<OptionMetadata> detectedOptions() {
        return detectedOptions;
    }

    /**
     * Returns true if an option with the given full or short name was present on the command line.
     */
    public boolean hasOption(String name) {
        return detectedOptionsByAnyName.containsKey(name);
    }

    /**
     * Returns the values of an option with the given full or short name in the order of their appearance on the
     * command line. If the option was present, but no values were specified, the option default value is returned,
     * if any.
     */
    public List<String> optionStrings(String name) {
        OptionMetadata option = detectedOptionsByAnyName.get(name);
        if (option == null) {
            return List.of();
        }

        List<String> values = valuesByOptionName.get(option.getName());
        if (values != null && !values.isEmpty()) {
            return List.copyOf(values);
        }

        return option.getDefaultValue() != null ? List.of(option.getDefaultValue()) : List.of();
    }

    /**
     * Returns all the arguments that are neither options nor option values in the order of their appearance on the
     * command line.
     */
    public List<String> standaloneArguments() {
        return standaloneArguments;
    }
}
