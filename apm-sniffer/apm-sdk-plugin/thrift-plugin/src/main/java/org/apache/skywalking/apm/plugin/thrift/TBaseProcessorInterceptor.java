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

package org.apache.skywalking.apm.plugin.thrift;

import java.lang.reflect.Method;
import java.util.Map;
import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.plugin.thrift.wrapper.Context;
import org.apache.skywalking.apm.plugin.thrift.wrapper.ServerInProtocolWrapper;
import org.apache.thrift.ProcessFunction;
import org.apache.thrift.TBaseAsyncProcessor;
import org.apache.thrift.TBaseProcessor;

/**
 * To transparent the ProcessFunction for getting arguments of method.
 *
 * @see TBaseAsyncProcessor
 * @see TBaseProcessorInterceptor
 */
public class TBaseProcessorInterceptor implements InstanceConstructorInterceptor, InstanceMethodsAroundInterceptor {
    private Map<String, ProcessFunction> processMap;

    @Override
    public void onConstruct(EnhancedInstance objInst, Object[] allArguments) {
        processMap = ((TBaseProcessor) objInst).getProcessMapView();
    }

    @Override
    public void beforeMethod(EnhancedInstance objInst,
                             Method method,
                             Object[] allArguments,
                             Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {
        ServerInProtocolWrapper in = (ServerInProtocolWrapper) allArguments[0];
        in.initial(new Context(processMap));
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst,
                              Method method,
                              Object[] allArguments,
                              Class<?>[] argumentsTypes,
                              Object ret) throws Throwable {
        ContextManager.stopSpan();
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst,
                                      Method method,
                                      Object[] allArguments,
                                      Class<?>[] argumentsTypes,
                                      Throwable t) {
        ContextManager.activeSpan().log(t);
    }
}
