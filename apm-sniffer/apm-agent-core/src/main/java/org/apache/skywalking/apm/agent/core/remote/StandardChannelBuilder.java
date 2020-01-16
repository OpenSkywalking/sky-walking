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

package org.apache.skywalking.apm.agent.core.remote;

import io.grpc.ManagedChannelBuilder;
import io.grpc.internal.DnsNameResolverProvider;
import io.grpc.util.RoundRobinLoadBalancerFactory;

/**
 * @author zhang xin
 */
public class StandardChannelBuilder implements ChannelBuilder {
    private final static int MAX_INBOUND_MESSAGE_SIZE = 1024 * 1024 * 50;

    @Override public ManagedChannelBuilder build(ManagedChannelBuilder managedChannelBuilder) throws Exception {
        return managedChannelBuilder.nameResolverFactory(new DnsNameResolverProvider())
            /**
             * This API may can not be compatible for grpc.version 1.26.0
             */
            .loadBalancerFactory(RoundRobinLoadBalancerFactory.getInstance())
            .maxInboundMessageSize(MAX_INBOUND_MESSAGE_SIZE)
            .usePlaintext();
    }
}
