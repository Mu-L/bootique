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

package io.bootique.di.tck;

import io.bootique.di.tck.accessories.SpareTire;
import jakarta.inject.Inject;
import jakarta.inject.Named;

public class V8Engine extends GasEngine {

    public V8Engine() {
        publicNoArgsConstructorInjected = true;
    }

    @Inject void injectPackagePrivateMethod() {
        if (subPackagePrivateMethodInjected) {
            overriddenPackagePrivateMethodInjectedTwice = true;
        }
        subPackagePrivateMethodInjected = true;
    }

    /**
     * Qualifiers are swapped from how they appear in the superclass.
     */
    public void injectQualifiers(Seat seatA, @Drivers Seat seatB,
            Tire tireA, @Named("spare") Tire tireB) {
        if ((seatA instanceof DriversSeat)
                || !(seatB instanceof DriversSeat)
                || (tireA instanceof SpareTire)
                || !(tireB instanceof SpareTire)) {
            qualifiersInheritedFromOverriddenMethod = true;
        }
    }

    void injectPackagePrivateMethodForOverride() {
        subPackagePrivateMethodForOverrideInjected = true;
    }

    @Inject public void injectTwiceOverriddenWithOmissionInMiddle() {
        overriddenTwiceWithOmissionInMiddleInjected = true;
    }

    public void injectTwiceOverriddenWithOmissionInSubclass() {
        overriddenTwiceWithOmissionInSubclassInjected = true;
    }
}
