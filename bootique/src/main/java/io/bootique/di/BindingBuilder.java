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

/**
 * A binding builder that helps with fluent binding creation.
 *
 * @param <T> An interface type of the service being bound.
 */
public interface BindingBuilder<T> extends ScopeBuilder {

    ScopeBuilder to(Class<? extends T> implementation) throws DIRuntimeException;

    ScopeBuilder to(Key<? extends T> key) throws DIRuntimeException;

    ScopeBuilder toInstance(T instance) throws DIRuntimeException;

    /**
     * @since 3.0
     * @deprecated in favor of {@link #toProvider(Class)}
     */
    @Deprecated(since = "4.0", forRemoval = true)
    default ScopeBuilder toJakartaProvider(Class<? extends Provider<? extends T>> providerType) throws DIRuntimeException {
        return toProvider(providerType);
    }

    /**
     * @since 4.0
     */
    ScopeBuilder toProvider(Class<? extends Provider<? extends T>> providerType) throws DIRuntimeException;

    /**
     * @since 3.0
     * @deprecated in favor of {@link #toProviderInstance(Provider)}
     */
    @Deprecated(since = "4.0", forRemoval = true)
    default ScopeBuilder toJakartaProviderInstance(Provider<? extends T> provider) throws DIRuntimeException {
        return toProviderInstance(provider);
    }

    /**
     * @since 4.0
     */
    ScopeBuilder toProviderInstance(Provider<? extends T> provider) throws DIRuntimeException;
}
