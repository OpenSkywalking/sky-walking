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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.skywalking.oap.server.recevier.log.provider.handler;

import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.network.common.v3.Commands;
import org.apache.skywalking.apm.network.logging.v3.LogData;
import org.apache.skywalking.apm.network.logging.v3.LogReportServiceGrpc;
import org.apache.skywalking.oap.server.analyzer.module.AnalyzerModule;
import org.apache.skywalking.oap.server.analyzer.provider.log.ILogProcessService;
import org.apache.skywalking.oap.server.analyzer.provider.log.LogProcessor;
import org.apache.skywalking.oap.server.library.module.ModuleManager;
import org.apache.skywalking.oap.server.library.server.grpc.GRPCHandler;
import org.apache.skywalking.oap.server.telemetry.TelemetryModule;
import org.apache.skywalking.oap.server.telemetry.api.CounterMetrics;
import org.apache.skywalking.oap.server.telemetry.api.HistogramMetrics;
import org.apache.skywalking.oap.server.telemetry.api.MetricsCreator;
import org.apache.skywalking.oap.server.telemetry.api.MetricsTag;

@Slf4j
public class LogReportServiceHandler extends LogReportServiceGrpc.LogReportServiceImplBase implements GRPCHandler {

    private final ModuleManager moduleManager;
    private HistogramMetrics histogram;
    private CounterMetrics errorCounter;
    private LogProcessor logProcessor;

    public LogReportServiceHandler(final ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
        MetricsCreator metricsCreator = moduleManager.find(TelemetryModule.NAME)
                                                     .provider()
                                                     .getService(MetricsCreator.class);
        this.logProcessor = moduleManager.find(AnalyzerModule.NAME)
                                         .provider()
                                         .getService(ILogProcessService.class)
                                         .createProcessor();

        histogram = metricsCreator.createHistogramMetric(
            "log_in_latency", "The process latency of log",
            new MetricsTag.Keys("protocol"), new MetricsTag.Values("grpc")
        );
        errorCounter = metricsCreator.createCounter("log_analysis_error_count", "The error number of log analysis",
                                                    new MetricsTag.Keys("protocol"), new MetricsTag.Values("grpc")
        );
    }

    @Override
    public StreamObserver<LogData> collect(final StreamObserver<Commands> responseObserver) {
        return new StreamObserver<LogData>() {
            @Override
            public void onNext(final LogData logData) {
                if (log.isDebugEnabled()) {
                    log.debug("received log in streaming");
                }
                HistogramMetrics.Timer timer = histogram.createTimer();
                try {
                    logProcessor.process(logData);
                } catch (Exception e) {
                    errorCounter.inc();
                    log.error(e.getMessage(), e);
                } finally {
                    timer.finish();
                }
            }

            @Override
            public void onError(final Throwable throwable) {
                log.error(throwable.getMessage(), throwable);
                responseObserver.onCompleted();
            }

            @Override
            public void onCompleted() {
                responseObserver.onNext(Commands.newBuilder().build());
                responseObserver.onCompleted();
            }
        };
    }
}
