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

package org.skywalking.apm.plugin.jdbc.h2.define;

import org.skywalking.apm.agent.core.plugin.match.ClassMatch;
import org.skywalking.apm.plugin.jdbc.define.AbstractDriverInstrumentation;

import static org.skywalking.apm.agent.core.plugin.match.NameMatch.byName;

/**
 * {@link DriverInstrumentation} presents that skywalking intercepts {@link org.h2.Driver}.
 *
 * @author zhangxin
 */
public class DriverInstrumentation extends AbstractDriverInstrumentation {
    private static final String CLASS_OF_INTERCEPT_H2_DRIVER = "org.h2.Driver";

    @Override
    protected ClassMatch enhanceClass() {
        return byName(CLASS_OF_INTERCEPT_H2_DRIVER);
    }
}
