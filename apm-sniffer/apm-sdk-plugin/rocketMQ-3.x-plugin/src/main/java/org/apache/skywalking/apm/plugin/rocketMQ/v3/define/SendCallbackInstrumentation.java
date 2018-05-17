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

package org.apache.skywalking.apm.plugin.rocketMQ.v3.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.apache.skywalking.apm.agent.core.plugin.bytebuddy.ArgumentTypeNameMatch.takesArgumentWithType;
import static org.apache.skywalking.apm.agent.core.plugin.match.HierarchyMatch.byHierarchyMatch;

/**
 * {@link SendCallbackInstrumentation} intercepts {@link com.alibaba.rocketmq.client.producer.SendCallback#onSuccess(com.alibaba.rocketmq.client.producer.SendResult sendResult)}
 * method by using {@link org.apache.skywalking.apm.plugin.rocketMQ.v3.OnSuccessInterceptor} and also intercepts {@link
 * com.alibaba.rocketmq.client.producer.SendCallback#onException(Throwable)} by using {@link
 * org.apache.skywalking.apm.plugin.rocketMQ.v3.OnExceptionInterceptor}.
 *
 * @author carlvine500
 */
public class SendCallbackInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {

    private static final String ENHANCE_CLASS = "com.alibaba.rocketmq.client.producer.SendCallback";
    private static final String ON_SUCCESS_ENHANCE_METHOD = "onSuccess";
    private static final String ON_SUCCESS_INTERCEPTOR = "org.apache.skywalking.apm.plugin.rocketMQ.v3.OnSuccessInterceptor";
    private static final String ON_EXCEPTION_METHOD = "onException";
    private static final String ON_EXCEPTION_INTERCEPTOR = "org.apache.skywalking.apm.plugin.rocketMQ.v3.OnExceptionInterceptor";

    @Override protected ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[0];
    }

    @Override protected InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        return new InstanceMethodsInterceptPoint[] {
            new InstanceMethodsInterceptPoint() {
                @Override public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named(ON_SUCCESS_ENHANCE_METHOD).and(takesArgumentWithType(0, "com.alibaba.rocketmq.client.producer.SendResult"));
                }

                @Override public String getMethodsInterceptor() {
                    return ON_SUCCESS_INTERCEPTOR;
                }

                @Override public boolean isOverrideArgs() {
                    return false;
                }
            },
            new InstanceMethodsInterceptPoint() {
                @Override public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named(ON_EXCEPTION_METHOD).and(takesArgumentWithType(0, "java.lang.Throwable"));
                }

                @Override public String getMethodsInterceptor() {
                    return ON_EXCEPTION_INTERCEPTOR;
                }

                @Override public boolean isOverrideArgs() {
                    return false;
                }
            }
        };
    }

    @Override protected ClassMatch enhanceClass() {
        return byHierarchyMatch(new String[] {ENHANCE_CLASS});
    }
}
