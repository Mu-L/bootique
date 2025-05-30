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

package io.bootique.meta.application;

import io.bootique.BootiqueException;
import io.bootique.meta.MetadataNode;
import io.bootique.meta.config.ConfigValueMetadata;

import java.util.*;

/**
 * Metadata object representing current application and its command-line interface.
 */
public class ApplicationMetadata implements MetadataNode {

    private String name;
    private String description;
    private final List<CommandMetadata> commands;
    private final List<OptionMetadata> options;
    // a combination of "commands" and "options"
    private final List<OptionMetadata> cliOptions;
    private final List<ConfigValueMetadata> variables;

    private ApplicationMetadata() {
        this.commands = new ArrayList<>();
        this.options = new ArrayList<>();
        this.cliOptions = new ArrayList<>();
        this.variables = new ArrayList<>();
    }

    public static Builder builder() {
        return new Builder().defaultName();
    }

    public static Builder builder(String name) {
        return new Builder().name(name);
    }

    public static Builder builder(String name, String description) {
        return new Builder().name(name).description(description);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public Collection<CommandMetadata> getCommands() {
        return commands;
    }

    public Collection<OptionMetadata> getOptions() {
        return options;
    }

    /**
     * Returns a combination of commands and options as a single collection of OptionMetadata. This is a view that
     * the users sees on the command line. Options "shortName" property in this collection may differ from that of
     * the original commands or options, as it is adjusted for the application context and the presence of conflicting
     * names.
     *
     * @since 3.0
     */
    public Collection<OptionMetadata> getCliOptions() {
        return cliOptions;
    }

    /**
     * Returns a collection of metadata objects representing publicly exposed environment variables.
     *
     * @return a collection of metadata objects representing publicly exposed environment variables.
     */
    public Collection<ConfigValueMetadata> getVariables() {
        return variables;
    }

    public static class Builder {

        private final ApplicationMetadata application;

        private Builder() {
            this.application = new ApplicationMetadata();
        }

        public ApplicationMetadata build() {
            throwOnConflictingFullNames();
            rewriteConflictingShortNames();

            // set the canonical alphabetic order for display in help, etc.
            Collections.sort(application.cliOptions, Comparator.comparing(OptionMetadata::getName));

            return application;
        }

        private void throwOnConflictingFullNames() {
            if (application.cliOptions.size() > 1) {
                Set<String> distinctNames = new HashSet<>();
                application.cliOptions.forEach(om -> {
                    if (!distinctNames.add(om.getName())) {
                        throw new BootiqueException(1, "Duplicate option name declaration: '" + om.getName() + "'");
                    }
                });
            }
        }

        private void rewriteConflictingShortNames() {
            // We will only disable conflicting short names in the "cliOptions" collection , leaving short names
            // intact in the original commands and options metadata.

            int len = application.cliOptions.size();
            Map<String, List<Integer>> shortNames = new HashMap<>();
            for (int i = 0; i < len; i++) {
                shortNames.computeIfAbsent(application.cliOptions.get(i).getShortName(), sn -> new ArrayList<>(3)).add(i);
            }

            // wipe out short names conflicting with other short names
            for (Map.Entry<String, List<Integer>> e : shortNames.entrySet()) {

                int slen = e.getValue().size();

                // disable short options if there are multiple overlapping options
                if (slen > 1) {
                    for (int i = 0; i < slen; i++) {
                        int oi = e.getValue().get(i);
                        OptionMetadata oldOpt = application.cliOptions.get(oi);
                        application.cliOptions.set(oi, sansShortName(oldOpt));
                    }

                    // clear wiped out short names so that the next check against full names doesn't generate a
                    // conflict for the names already gone
                    e.getValue().clear();
                }
            }

            // wipe out short names conflicting with full names
            for (OptionMetadata o : application.cliOptions) {

                if (o.getName().length() == 1) {
                    List<Integer> conflicting = shortNames.getOrDefault(o.getName(), List.of());

                    // we no longer have multiple short name groups in "shortNames", so the size can only be 0 or 1
                    if (conflicting.size() == 1) {
                        int i = conflicting.get(0);
                        OptionMetadata oldOpt = application.cliOptions.get(i);
                        application.cliOptions.set(i, sansShortName(oldOpt));
                    }
                }
            }
        }

        private OptionMetadata sansShortName(OptionMetadata md) {
            return new OptionMetadata(
                    md.getName(),
                    md.getDescription(),
                    null,
                    md.getValueCardinality(),
                    md.getValueName(),
                    md.getDefaultValue()
            );
        }

        public Builder name(String name) {
            application.name = name;
            return this;
        }

        public Builder defaultName() {
            return name(ApplicationIntrospector.appNameFromRuntime());
        }

        public Builder description(String description) {
            application.description = description;
            return this;
        }

        public Builder addCommand(CommandMetadata commandMetadata) {
            application.commands.add(commandMetadata);
            application.cliOptions.add(commandMetadata.getCommandOption());
            commandMetadata.getOptions().forEach(application.cliOptions::add);
            return this;
        }

        public Builder addCommands(Collection<CommandMetadata> commandMetadata) {
            commandMetadata.forEach(this::addCommand);
            return this;
        }

        public Builder addOption(OptionMetadata option) {
            application.options.add(option);
            application.cliOptions.add(option);
            return this;
        }

        public Builder addOptions(Collection<OptionMetadata> options) {
            options.forEach(this::addOption);
            return this;
        }

        public Builder addVariable(ConfigValueMetadata var) {
            application.variables.add(var);
            return this;
        }

        public Builder addVariables(Collection<ConfigValueMetadata> vars) {
            application.variables.addAll(vars);
            return this;
        }
    }
}
