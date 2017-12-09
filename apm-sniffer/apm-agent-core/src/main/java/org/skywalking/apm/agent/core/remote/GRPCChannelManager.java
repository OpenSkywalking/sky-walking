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

package org.skywalking.apm.agent.core.remote;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.internal.DnsNameResolverProvider;
import io.grpc.netty.NettyChannelBuilder;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.skywalking.apm.agent.core.boot.BootService;
import org.skywalking.apm.agent.core.boot.DefaultNamedThreadFactory;
import org.skywalking.apm.agent.core.conf.Config;
import org.skywalking.apm.agent.core.conf.RemoteDownstreamConfig;
import org.skywalking.apm.agent.core.logging.api.ILog;
import org.skywalking.apm.agent.core.logging.api.LogManager;

/**
 * @author wusheng
 */
public class GRPCChannelManager implements BootService, Runnable {
    private static final ILog logger = LogManager.getLogger(GRPCChannelManager.class);

    private volatile ManagedChannel managedChannel = null;
    private volatile ScheduledFuture<?> connectCheckFuture;
    private volatile boolean reconnect = true;
    private Random random = new Random();
    private List<GRPCChannelListener> listeners = Collections.synchronizedList(new LinkedList<GRPCChannelListener>());

    @Override
    public void beforeBoot() throws Throwable {

    }

    @Override
    public void boot() throws Throwable {
        connectCheckFuture = Executors
            .newSingleThreadScheduledExecutor(new DefaultNamedThreadFactory("GRPCChannelManager"))
            .scheduleAtFixedRate(this, 0, Config.Collector.GRPC_CHANNEL_CHECK_INTERVAL, TimeUnit.SECONDS);
    }

    @Override
    public void afterBoot() throws Throwable {

    }

    @Override
    public void shutdown() throws Throwable {
        connectCheckFuture.cancel(true);
        if (managedChannel != null) {
            managedChannel.shutdownNow();
        }
        logger.debug("Selected collector grpc service shutdown.");
    }

    @Override
    public void run() {
        logger.debug("Selected collector grpc service running, reconnect:{}.", reconnect);
        if (reconnect) {
            if (RemoteDownstreamConfig.Collector.GRPC_SERVERS.size() > 0) {
                String server = "";
                try {
                    int index = Math.abs(random.nextInt()) % RemoteDownstreamConfig.Collector.GRPC_SERVERS.size();
                    server = RemoteDownstreamConfig.Collector.GRPC_SERVERS.get(index);
                    String[] ipAndPort = server.split(":");
                    ManagedChannelBuilder<?> channelBuilder =
                        NettyChannelBuilder.forAddress(ipAndPort[0], Integer.parseInt(ipAndPort[1]))
                            .nameResolverFactory(new DnsNameResolverProvider())
                            .maxInboundMessageSize(1024 * 1024 * 50)
                            .usePlaintext(true);
                    managedChannel = channelBuilder.build();
                    if (!managedChannel.isShutdown() && !managedChannel.isTerminated()) {
                        reconnect = false;
                        notify(GRPCChannelStatus.CONNECTED);
                    } else {
                        notify(GRPCChannelStatus.DISCONNECT);
                    }
                    return;
                } catch (Throwable t) {
                    logger.error(t, "Create channel to {} fail.", server);
                    notify(GRPCChannelStatus.DISCONNECT);
                }
            }

            logger.debug("Selected collector grpc service is not available. Wait {} seconds to retry", Config.Collector.GRPC_CHANNEL_CHECK_INTERVAL);
        }
    }

    public void addChannelListener(GRPCChannelListener listener) {
        listeners.add(listener);
    }

    public ManagedChannel getManagedChannel() {
        return managedChannel;
    }

    /**
     * If the given expcetion is triggered by network problem, connect in background.
     *
     * @param throwable
     */
    public void reportError(Throwable throwable) {
        if (isNetworkError(throwable)) {
            reconnect = true;
        }
    }

    private void notify(GRPCChannelStatus status) {
        for (GRPCChannelListener listener : listeners) {
            try {
                listener.statusChanged(status);
            } catch (Throwable t) {
                logger.error(t, "Fail to notify {} about channel connected.", listener.getClass().getName());
            }
        }
    }

    private boolean isNetworkError(Throwable throwable) {
        if (throwable instanceof StatusRuntimeException) {
            StatusRuntimeException statusRuntimeException = (StatusRuntimeException)throwable;
            return statusEquals(statusRuntimeException.getStatus(),
                Status.UNAVAILABLE,
                Status.PERMISSION_DENIED,
                Status.UNAUTHENTICATED,
                Status.RESOURCE_EXHAUSTED,
                Status.UNKNOWN
            );
        }
        return false;
    }

    private boolean statusEquals(Status sourceStatus, Status... potentialStatus) {
        for (Status status : potentialStatus) {
            if (sourceStatus.getCode() == status.getCode()) {
                return true;
            }
        }
        return false;
    }
}
