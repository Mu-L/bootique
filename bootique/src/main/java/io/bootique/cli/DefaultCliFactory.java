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
import io.bootique.command.CommandManager;
import jakarta.inject.Provider;

import java.util.HashSet;
import java.util.Set;

/**
 * @since 4.0
 */
public class DefaultCliFactory implements CliFactory {

    private final Provider<CommandManager> commandManagerProvider;
    private final CliParser parser;

    public DefaultCliFactory(Provider<CommandManager> commandManagerProvider, CliParser parser) {

        // injecting CommandManager via provider for an obscure reason - it is injected here and also in
        // ApplicationMetadata provider (on which this class indirectly depends). So when there's an error
        // during CommandManager construction, it is thrown twice, causing ProvisionException to lose its "cause",
        // complicating exception analysis.

        this.commandManagerProvider = commandManagerProvider;
        this.parser = parser;
    }

    @Override
    public Cli createCli(String[] args) {
        if (args.length == 0) {
            return new NoArgsCli();
        }

        ParsedArgs parsed = parser.parse(args);
        return new DefaultCli(parsed, commandName(parsed));
    }

    protected String commandName(ParsedArgs parsed) {

        Set<String> matches = new HashSet<>(3);
        commandManagerProvider.get().getAllCommands().forEach((name, mc) -> {
            if (!mc.isHidden() && !mc.isDefault() && parsed.hasOption(name)) {
                matches.add(name);
            }
        });

        return switch (matches.size()) {
            // default command should be invoked
            case 0 -> null;
            case 1 -> matches.iterator().next();
            default -> {
                String opts = String.join(", ", matches);
                throw new BootiqueException(1, String.format("CLI options match multiple commands: %s.", opts));
            }
        };
    }
}
