/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.skywalking.apm.plugin.dubbo.patch;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.StaticMethodsInterceptPoint;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.ClassStaticMethodsEnhancePluginDefine;
import org.apache.skywalking.apm.agent.core.plugin.match.ClassMatch;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static org.apache.skywalking.apm.agent.core.plugin.match.NameMatch.byName;

/**
 * The dubbo conflict plugins resolve problem that the wrapper class that Dubbo generated cannot compiled. As we know,
 * The Dubbo service class will generate wrapper class by javasist, and it traverse all the methods. all works but this
 * class  enhance by Skywalking. The javasist cannot found the `EnhanceInstance` method when it generate wrapper class.
 *
 * To resolve this problem. the plugin use the way to bury the {@link org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance}
 * methods method to ensure the correct compilation of the code.
 *
 * @author Zhang Xin
 */
public class WrapperInstrumentation extends ClassStaticMethodsEnhancePluginDefine {
    @Override protected StaticMethodsInterceptPoint[] getStaticMethodsInterceptPoints() {
        return new StaticMethodsInterceptPoint[] {
            new StaticMethodsInterceptPoint() {
                @Override public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named("makeWrapper");
                }

                @Override public String getMethodsInterceptor() {
                    return "org.apache.skywalking.apm.plugin.dubbo.patch.MakeWrapperInterceptor";
                }

                @Override public boolean isOverrideArgs() {
                    return false;
                }
            }
        };
    }

    @Override protected ClassMatch enhanceClass() {
        return byName("com.alibaba.dubbo.common.bytecode.Wrapper");
    }
}
