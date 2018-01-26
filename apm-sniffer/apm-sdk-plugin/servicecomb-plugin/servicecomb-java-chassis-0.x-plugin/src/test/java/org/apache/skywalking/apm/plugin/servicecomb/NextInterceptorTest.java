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

package org.apache.skywalking.apm.plugin.servicecomb;

import io.servicecomb.core.Endpoint;
import io.servicecomb.core.Invocation;
import io.servicecomb.core.definition.OperationMeta;
import io.servicecomb.core.definition.SchemaMeta;
import io.servicecomb.core.provider.consumer.ReferenceConfig;
import io.servicecomb.swagger.invocation.InvocationType;
import io.servicecomb.swagger.invocation.SwaggerInvocation;
import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.skywalking.apm.agent.core.boot.ServiceManager;
import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractTracingSpan;
import org.apache.skywalking.apm.agent.core.context.trace.TraceSegment;
import org.apache.skywalking.apm.agent.core.context.util.KeyValuePair;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.test.helper.SegmentHelper;
import org.apache.skywalking.apm.agent.test.helper.SpanHelper;
import org.apache.skywalking.apm.agent.test.tools.AgentServiceRule;
import org.apache.skywalking.apm.agent.test.tools.SegmentStorage;
import org.apache.skywalking.apm.agent.test.tools.SegmentStoragePoint;
import org.apache.skywalking.apm.agent.test.tools.TracingSegmentRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import static org.apache.skywalking.apm.plugin.servicecomb.NextInterceptor.DEEP;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(TracingSegmentRunner.class)
public class NextInterceptorTest {

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Rule
    public AgentServiceRule agentServiceRule = new AgentServiceRule();

    private NextInterceptor nextInterceptor;
    @Mock
    private OperationMeta operationMeta;

    @Mock
    private MockInvocation enhancedInstance;

    @Mock
    private Endpoint endpoint;

    @Mock
    Response.StatusType statusType;

    @Mock
    ReferenceConfig referenceConfig;

    @Mock
    private SwaggerInvocation swagger;
    private Object[] allArguments;
    private Class[] argumentsType;
    private Object[] swaggerArguments;

    @Mock
    private SchemaMeta schemaMeta;

    @Before
    public void setUp() throws Exception {
        ServiceManager.INSTANCE.boot();
        nextInterceptor = new NextInterceptor();
        PowerMockito.mock(Invocation.class);
        when(operationMeta.getSchemaMeta()).thenReturn(schemaMeta);
        when(endpoint.getAddress()).thenReturn("0.0.0.0:7777");
        when(enhancedInstance.getEndpoint()).thenReturn(endpoint);
        when(enhancedInstance.getInvocationQualifiedName()).thenReturn("consumerTest");
        when(operationMeta.getOperationPath()).thenReturn("/bmi");
        when(enhancedInstance.getOperationMeta()).thenReturn(operationMeta);
        when(enhancedInstance.getStatus()).thenReturn(statusType);
        when(statusType.getStatusCode()).thenReturn(200);
        when(enhancedInstance.getInvocationType()).thenReturn(InvocationType.CONSUMER);
        Config.Agent.APPLICATION_CODE = "serviceComnTestCases-APP";

        allArguments = new Object[] {};
        argumentsType = new Class[] {};
        swaggerArguments = new Class[] {};
    }

    @Test
    public void testConsumer() throws Throwable {
        Integer count = 2;
        DEEP.set(count);
        nextInterceptor.beforeMethod(enhancedInstance, null, allArguments, argumentsType, null);
        count = 1;
        DEEP.set(count);
        nextInterceptor.afterMethod(enhancedInstance, null, allArguments, argumentsType, null);

        Assert.assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);

        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertCombSpan(spans.get(0));
        verify(enhancedInstance, times(1)).getContext();
    }

    private void assertCombSpan(AbstractTracingSpan span) {
        assertThat(span.getOperationName(), is("consumerTest"));
        assertThat(SpanHelper.getComponentId(span), is(27));
        List<KeyValuePair> tags = SpanHelper.getTags(span);
        assertThat(tags.get(0).getValue(), is("/bmi"));
        assertThat(span.isExit(), is(true));
    }

    private class MockInvocation extends Invocation implements EnhancedInstance {
        public MockInvocation(ReferenceConfig referenceConfig, OperationMeta operationMeta, Object[] swaggerArguments) {
            super(referenceConfig, operationMeta, swaggerArguments);
        }

        @Override public Object getSkyWalkingDynamicField() {
            return null;
        }

        @Override public void setSkyWalkingDynamicField(Object value) {

        }

    }
}
