/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.skywalking.apm.collector.storage.es;

import org.skywalking.apm.collector.cluster.ModuleRegistration;
import org.skywalking.apm.collector.core.util.Const;

/**
 * @author peng-yongsheng
 */
public class StorageModuleEsRegistration extends ModuleRegistration {

    private final String virtualHost;
    private final int virtualPort;

    StorageModuleEsRegistration(String virtualHost, int virtualPort) {
        this.virtualHost = virtualHost;
        this.virtualPort = virtualPort;
    }

    @Override public Value buildValue() {
        return new Value(this.virtualHost, virtualPort, Const.EMPTY_STRING);
    }
}
