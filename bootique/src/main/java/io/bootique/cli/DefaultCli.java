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

import java.util.List;

/**
 * A {@link Cli} implementation on top of the Bootique CLI parser.
 *
 * @since 4.0
 */
public class DefaultCli implements Cli {

    private final ParsedArgs parsed;
    private final String commandName;

    public DefaultCli(ParsedArgs parsed, String commandName) {
        this.parsed = parsed;
        this.commandName = commandName;
    }

    @Override
    public String commandName() {
        return commandName;
    }

    @Override
    public boolean hasOption(String optionName) {
        return parsed.hasOption(optionName);
    }

    @Override
    public List<String> optionStrings(String name) {
        return parsed.optionStrings(name);
    }

    @Override
    public List<String> standaloneArguments() {
        return parsed.standaloneArguments();
    }

    @Override
    public List<OptionMetadata> detectedOptions() {
        return parsed.detectedOptions();
    }
}
