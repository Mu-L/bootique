
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

package io.bootique.di.mock;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

public class MockImplementation2_ConstructorProvider implements MockInterface2 {

    private Provider<MockInterface1> provider;

    @Inject
    public MockImplementation2_ConstructorProvider(Provider<MockInterface1> provider) {
        this.provider = provider;
    }

    public String getAlteredName() {
        return "altered_" + provider.get().getName();
    }

    public String getName() {
        return "MockImplementation2Name";
    }

}
