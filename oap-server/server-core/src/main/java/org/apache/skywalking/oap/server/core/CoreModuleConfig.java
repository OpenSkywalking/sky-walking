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

package org.apache.skywalking.oap.server.core;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.apache.skywalking.oap.server.library.module.ModuleConfig;

@Getter
public class CoreModuleConfig extends ModuleConfig {
    @Setter
    private String role = "Mixed";
    @Setter
    private String nameSpace;
    @Setter
    private String restHost;
    @Setter
    private int restPort;
    @Setter
    private int jettySelectors = 1;
    @Setter
    private String restContextPath;
    @Setter
    private String gRPCHost;
    @Setter
    private int gRPCPort;
    @Setter
    private int maxConcurrentCallsPerConnection;
    @Setter
    private int maxMessageSize;
    @Setter
    private boolean enableDatabaseSession;
    @Setter
    private int topNReportPeriod;
    private final List<String> downsampling;
    /**
     * The period of doing data persistence. Unit is second.
     */
    @Setter
    private long persistentPeriod = 3;
    @Setter
    private boolean enableDataKeeperExecutor = true;
    @Setter
    private int dataKeeperExecutePeriod = 5;
    @Setter
    private int recordDataTTL;
    @Setter
    private int minuteMetricsDataTTL;
    @Setter
    private int hourMetricsDataTTL;
    @Setter
    private int dayMetricsDataTTL;
    @Setter
    private int monthMetricsDataTTL;
    @Setter
    private int gRPCThreadPoolSize;
    @Setter
    private int gRPCThreadPoolQueueSize;
    /**
     * Timeout for cluster internal communication, in seconds.
     */
    @Setter
    private int remoteTimeout = 20;

    /**
     * Following are cache settings for inventory(s)
     */
    private long maxSizeOfServiceInventory = 10_000L;
    private long maxSizeOfServiceInstanceInventory = 1_000_000L;
    private long maxSizeOfEndpointInventory = 1_000_000L;
    private long maxSizeOfNetworkInventory = 1_000_000L;

    /**
     * Following are cache setting for none stream(s)
     */
    private long maxSizeOfProfileTask = 10_000L;

    /**
     * Analyze profile snapshots paging size.
     */
    private int maxPageSizeOfQueryProfileSnapshot = 500;

    /**
     * Analyze profile snapshots max size.
     */
    private int maxSizeOfAnalyzeProfileSnapshot = 12000;

    public CoreModuleConfig() {
        this.downsampling = new ArrayList<>();
    }

    public DataTTLConfig getDataTTL() {
        DataTTLConfig dataTTLConfig = new DataTTLConfig();
        dataTTLConfig.setRecordDataTTL(recordDataTTL);
        dataTTLConfig.setMinuteMetricsDataTTL(minuteMetricsDataTTL);
        dataTTLConfig.setHourMetricsDataTTL(hourMetricsDataTTL);
        dataTTLConfig.setDayMetricsDataTTL(dayMetricsDataTTL);
        dataTTLConfig.setMonthMetricsDataTTL(monthMetricsDataTTL);
        return dataTTLConfig;
    }

    /**
     * OAP server could work in different roles.
     */
    public enum Role {
        /**
         * Default role. OAP works as the {@link #Receiver} and {@link #Aggregator}
         */
        Mixed,
        /**
         * Receiver mode OAP open the service to the agents, analysis and aggregate the results and forward the results
         * to {@link #Mixed} and {@link #Aggregator} roles OAP. The only exception is for {@link
         * org.apache.skywalking.oap.server.core.analysis.record.Record}, they don't require 2nd round distributed
         * aggregation, is being pushed into the storage from the receiver OAP directly.
         */
        Receiver,
        /**
         * Aggregator mode OAP receives data from {@link #Mixed} and {@link #Aggregator} OAP nodes, and do 2nd round
         * aggregation. Then save the final result to the storage.
         */
        Aggregator
    }
}
