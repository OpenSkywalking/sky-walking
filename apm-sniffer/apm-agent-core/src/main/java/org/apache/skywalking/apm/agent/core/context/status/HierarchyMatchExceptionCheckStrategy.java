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

package org.apache.skywalking.apm.agent.core.context.status;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.apache.skywalking.apm.agent.core.boot.ServiceManager;

/**
 * The exception listed in org.apache.skywalking.apm.agent.core.conf.Config.StatusCheck#IGNORED_EXCEPTIONS will not be
 * thought as error, also affects its subclasses.
 */
public class HierarchyMatchExceptionCheckStrategy implements ExceptionCheckStrategy {

    private final Set<Class<? extends Throwable>> ignoredExceptions = new CopyOnWriteArraySet<>();
    private final Set<Class<? extends Throwable>> exceptions = new CopyOnWriteArraySet<>();

    @Override
    public boolean isError(final Throwable e) {
        Class<? extends Throwable> clazz = e.getClass();
        if (ignoredExceptions.contains(clazz)) {
            return false;
        }
        if (exceptions.contains(clazz)) {
            return true;
        }
        StatusCheckService statusTriggerService = ServiceManager.INSTANCE.findService(StatusCheckService.class);
        String[] ignoredExceptionNames = statusTriggerService.getIgnoredExceptionNames();
        for (final String ignoredExceptionName : ignoredExceptionNames) {
            try {
                Class<?> parentClazz = Class.forName(ignoredExceptionName, true, clazz.getClassLoader());
                if (parentClazz.isAssignableFrom(clazz)) {
                    ignoredExceptions.add(clazz);
                    return false;
                }
            } catch (ClassNotFoundException ignore) {
            }
        }
        exceptions.add(clazz);
        return true;
    }
}
