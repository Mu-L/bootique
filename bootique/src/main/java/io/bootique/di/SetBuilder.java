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

package io.bootique.di;

import jakarta.inject.Provider;

import java.util.Collection;

/**
 * A binding builder for set configurations.
 *
 * @param <T> A type of set elements
 * @since 2.0
 */
public interface SetBuilder<T> extends ScopeBuilder {

    SetBuilder<T> add(Class<? extends T> interfaceType) throws DIRuntimeException;

    /**
     * @since 2.0
     */
    SetBuilder<T> addInstance(T value) throws DIRuntimeException;

    SetBuilder<T> add(Key<? extends T> valueKey) throws DIRuntimeException;

    /**
     * @since 3.0
     * @deprecated in favor of {@link #addProviderInstance(Provider)}
     */
    @Deprecated(since = "4.0", forRemoval = true)
    default SetBuilder<T> addJakartaProviderInstance(Provider<? extends T> value) throws DIRuntimeException {
        return addProviderInstance(value);
    }
    
    /**
     * @since 4.0
     */
    SetBuilder<T> addProviderInstance(Provider<? extends T> value) throws DIRuntimeException;

    /**
     * @since 3.0
     * @deprecated in favor of {@link #addProvider(Class)}
     */
    @Deprecated(since = "4.0", forRemoval = true)
    default SetBuilder<T> addJakartaProvider(Class<? extends Provider<? extends T>> value) throws DIRuntimeException {
        return addProvider(value);
    }

    /**
     * @since 4.0
     */
    SetBuilder<T> addProvider(Class<? extends Provider<? extends T>> value) throws DIRuntimeException;

    /**
     * @since 2.0
     */
    SetBuilder<T> addInstances(Collection<T> values) throws DIRuntimeException;
}
