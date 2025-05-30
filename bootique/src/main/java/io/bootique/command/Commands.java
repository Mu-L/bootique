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

package io.bootique.command;

import io.bootique.BQCoreModule;
import io.bootique.BQModule;
import io.bootique.ModuleCrate;
import io.bootique.annotation.BQInternal;
import io.bootique.annotation.DefaultCommand;
import io.bootique.di.Binder;
import io.bootique.di.Injector;
import io.bootique.di.Key;
import io.bootique.di.Provides;
import io.bootique.di.SetBuilder;
import io.bootique.help.HelpCommand;
import io.bootique.log.BootLogger;
import io.bootique.meta.application.ApplicationMetadata;
import io.bootique.meta.application.OptionMetadata;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A builder of non-standard application command sets. It produces a Bootique module that overrides the default
 * command set.
 */
public class Commands implements BQModule {

    private final Collection<Class<? extends Command>> commandTypes;
    private final Collection<Command> commands;
    private final Collection<OptionMetadata> options;
    private boolean noModuleCommands;
    private boolean noModuleOptions;

    private Commands() {
        this.commandTypes = new HashSet<>();
        this.commands = new HashSet<>();
        this.options = new HashSet<>();
    }

    static SetBuilder<Command> contributeExtraCommands(Binder binder) {
        return binder.bindSet(Command.class, ExtraCommands.class);
    }

    @SafeVarargs
    public static Builder builder(Class<? extends Command>... commandTypes) {
        return new Builder().add(commandTypes);
    }

    // copy/paste from BQCoreModule
    private static Optional<Command> defaultCommand(Injector injector) {
        // default is optional, so check via injector whether it is bound...
        Key<Command> key = Key.get(Command.class, DefaultCommand.class);
        if (injector.hasProvider(key)) {
            Provider<Command> commandProvider = injector.getProvider(key);
            return Optional.of(commandProvider.get());
        }
        return Optional.empty();
    }

    @Override
    public ModuleCrate crate() {
        return ModuleCrate.of(this)
                .overrides(BQCoreModule.class)
                .moduleName("Commands")
                .description("Custom command policies and overrides").build();
    }

    @Override
    public void configure(Binder binder) {

        // pass "commandTypes" through DI to ensure proper instantiation and injection. We don't technically need to
        // it with "commands", but it is convenient to keep everything in a single collection
        SetBuilder<Command> extraCommandsBinder = Commands.contributeExtraCommands(binder);
        commandTypes.forEach(extraCommandsBinder::add);
        commands.forEach(extraCommandsBinder::addInstance);
    }

    @Provides
    @Singleton
    CommandManager provideCommandManager(
            Set<Command> commands,
            HelpCommand helpCommand,
            Injector injector,
            @ExtraCommands Set<Command> extraCommands,
            BootLogger bootLogger) {

        return new CommandManagerWithOverridesBuilder(commands, bootLogger)
                .defaultCommand(defaultCommand(injector))
                .helpCommand(helpCommand)
                .hideBaseCommands(noModuleCommands)
                .overrideWith(extraCommands)
                .build();
    }

    @Provides
    @Singleton
    ApplicationMetadata provideApplicationMetadata(@BQInternal ApplicationMetadata internalMetadata, BootLogger logger) {

        // TODO: Somehow centralize spread out metadata override logic - handle overrides of commands and variables here,
        //  not just options. Notes:
        //   1. We don't yet support variable filtering. This would be a new feature
        //   2. Overridden commands are already hidden by "internalMetadata" because they are are marked as "hidden".
        //      To move that processing here, we'll need to reimplement command mapping in a way similar to servlets:
        //      MappedCommand<T extends Command> to keep all commands (help, default, hidden) in a single injectable
        //      collection

        List<OptionMetadata> opts = new ArrayList<>();
        if (!noModuleOptions) {
            internalMetadata.getOptions().forEach(opts::add);
        }

        this.options.forEach(opts::add);

        return ApplicationMetadata
                .builder(internalMetadata.getName())
                .description(internalMetadata.getDescription())
                .addOptions(opts)
                .addCommands(internalMetadata.getCommands())
                .addVariables(internalMetadata.getVariables())
                .build();
    }

    public static class Builder {

        private final Commands commands;

        private Builder() {
            this.commands = new Commands();
        }

        public Commands module() {
            return commands;
        }

        @SafeVarargs
        public final Builder add(Class<? extends Command>... commandTypes) {
            Collections.addAll(commands.commandTypes, commandTypes);
            return this;
        }

        public final Builder add(Command... commands) {
            Collections.addAll(this.commands.commands, commands);
            return this;
        }

        /**
         * @since 3.0
         */
        public Builder addOption(OptionMetadata option) {
            commands.options.add(option);
            return this;
        }

        /**
         * Hides all commands defined across application modules as well as CLI options declared by those commands.
         * This will even hide the help command. Options declared at the module level (i.e. outside individual commands)
         * will be preserved. To suppress the options as well, use {@link #noModuleOptions()}.
         */
        public Builder noModuleCommands() {
            commands.noModuleCommands = true;
            return this;
        }

        /**
         * Hides all top-level options defined across application modules. Does not affect the options defined by
         * commands.
         *
         * @since 3.0
         */
        public Builder noModuleOptions() {
            commands.noModuleOptions = true;
            return this;
        }
    }
}
