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

package org.apache.skywalking.oap.server.receiver.trace.provider.parser;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.*;
import lombok.Setter;
import org.apache.skywalking.apm.network.language.agent.*;
import org.apache.skywalking.oap.server.library.buffer.DataStreamReader;
import org.apache.skywalking.oap.server.library.module.ModuleManager;
import org.apache.skywalking.oap.server.library.util.TimeBucketUtils;
import org.apache.skywalking.oap.server.receiver.trace.provider.parser.decorator.*;
import org.apache.skywalking.oap.server.receiver.trace.provider.parser.listener.*;
import org.apache.skywalking.oap.server.receiver.trace.provider.parser.standardization.*;
import org.slf4j.*;

/**
 * @author peng-yongsheng
 */
public class SegmentParse implements DataStreamReader.CallBack<UpstreamSegment> {

    private static final Logger logger = LoggerFactory.getLogger(SegmentParse.class);

    private final ModuleManager moduleManager;
    private final List<SpanListener> spanListeners;
    private final SegmentParserListenerManager listenerManager;
    private final SegmentCoreInfo segmentCoreInfo;
    @Setter private SegmentStandardizationWorker standardizationWorker;

    public SegmentParse(ModuleManager moduleManager, SegmentParserListenerManager listenerManager) {
        this.moduleManager = moduleManager;
        this.listenerManager = listenerManager;
        this.spanListeners = new LinkedList<>();
        this.segmentCoreInfo = new SegmentCoreInfo();
        this.segmentCoreInfo.setStartTime(Long.MAX_VALUE);
        this.segmentCoreInfo.setEndTime(Long.MIN_VALUE);
    }

    @Override public boolean call(UpstreamSegment segment) {
        return parse(segment, Source.Buffer);
    }

    public boolean parse(UpstreamSegment segment, Source source) {
        createSpanListeners();

        try {
            List<UniqueId> traceIds = segment.getGlobalTraceIdsList();
            TraceSegmentObject segmentObject = parseBinarySegment(segment);

            SegmentDecorator segmentDecorator = new SegmentDecorator(segmentObject);

            if (!preBuild(traceIds, segmentDecorator)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("This segment id exchange not success, write to buffer file, id: {}", segmentCoreInfo.getSegmentId());
                }

                if (source.equals(Source.Agent)) {
                    writeToBufferFile(segmentCoreInfo.getSegmentId(), segment);
                }
                return false;
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("This segment id exchange success, id: {}", segmentCoreInfo.getSegmentId());
                }

                notifyListenerToBuild();
                buildSegment(segmentCoreInfo.getSegmentId(), segmentDecorator.toByteArray());
                return true;
            }
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
            return true;
        }
    }

    private TraceSegmentObject parseBinarySegment(UpstreamSegment segment) throws InvalidProtocolBufferException {
        return TraceSegmentObject.parseFrom(segment.getSegment());
    }

    private boolean preBuild(List<UniqueId> traceIds, SegmentDecorator segmentDecorator) {
        StringBuilder segmentIdBuilder = new StringBuilder();

        for (int i = 0; i < segmentDecorator.getTraceSegmentId().getIdPartsList().size(); i++) {
            if (i == 0) {
                segmentIdBuilder.append(segmentDecorator.getTraceSegmentId().getIdPartsList().get(i));
            } else {
                segmentIdBuilder.append(".").append(segmentDecorator.getTraceSegmentId().getIdPartsList().get(i));
            }
        }

        for (UniqueId uniqueId : traceIds) {
            notifyGlobalsListener(uniqueId);
        }

        segmentCoreInfo.setSegmentId(segmentIdBuilder.toString());
        segmentCoreInfo.setApplicationId(segmentDecorator.getApplicationId());
        segmentCoreInfo.setApplicationInstanceId(segmentDecorator.getApplicationInstanceId());

        for (int i = 0; i < segmentDecorator.getSpansCount(); i++) {
            SpanDecorator spanDecorator = segmentDecorator.getSpans(i);

            if (!SpanIdExchanger.getInstance(moduleManager).exchange(spanDecorator, segmentCoreInfo.getApplicationId())) {
                return false;
            } else {
                for (int j = 0; j < spanDecorator.getRefsCount(); j++) {
                    ReferenceDecorator referenceDecorator = spanDecorator.getRefs(j);
                    if (!ReferenceIdExchanger.getInstance(moduleManager).exchange(referenceDecorator, segmentCoreInfo.getApplicationId())) {
                        return false;
                    }
                }
            }

            if (segmentCoreInfo.getStartTime() > spanDecorator.getStartTime()) {
                segmentCoreInfo.setStartTime(spanDecorator.getStartTime());
            }
            if (segmentCoreInfo.getEndTime() < spanDecorator.getEndTime()) {
                segmentCoreInfo.setEndTime(spanDecorator.getEndTime());
            }
            segmentCoreInfo.setError(spanDecorator.getIsError() || segmentCoreInfo.isError());
        }

        long minuteTimeBucket = TimeBucketUtils.INSTANCE.getMinuteTimeBucket(segmentCoreInfo.getStartTime());
        segmentCoreInfo.setMinuteTimeBucket(minuteTimeBucket);

        for (int i = 0; i < segmentDecorator.getSpansCount(); i++) {
            SpanDecorator spanDecorator = segmentDecorator.getSpans(i);

            if (spanDecorator.getSpanId() == 0) {
                notifyFirstListener(spanDecorator);
            }

            if (SpanType.Exit.equals(spanDecorator.getSpanType())) {
                notifyExitListener(spanDecorator);
            } else if (SpanType.Entry.equals(spanDecorator.getSpanType())) {
                notifyEntryListener(spanDecorator);
            } else if (SpanType.Local.equals(spanDecorator.getSpanType())) {
                notifyLocalListener(spanDecorator);
            } else {
                logger.error("span type value was unexpected, span type name: {}", spanDecorator.getSpanType().name());
            }
        }

        return true;
    }

    private void buildSegment(String id, byte[] dataBinary) {
//        Segment segment = new Segment();
//        segment.setId(id);
//        segment.setDataBinary(dataBinary);
//        segment.setTimeBucket(segmentCoreInfo.getMinuteTimeBucket());
    }

    private void writeToBufferFile(String id, UpstreamSegment upstreamSegment) {
        if (logger.isDebugEnabled()) {
            logger.debug("push to segment buffer write worker, id: {}", id);
        }

        SegmentStandardization standardization = new SegmentStandardization(id);
        standardization.setUpstreamSegment(upstreamSegment);

        standardizationWorker.in(standardization);
    }

    private void notifyListenerToBuild() {
        spanListeners.forEach(SpanListener::build);
    }

    private void notifyExitListener(SpanDecorator spanDecorator) {
        spanListeners.forEach(listener -> {
            if (listener.containsPoint(SpanListener.Point.Exit)) {
                ((ExitSpanListener)listener).parseExit(spanDecorator, segmentCoreInfo);
            }
        });
    }

    private void notifyEntryListener(SpanDecorator spanDecorator) {
        spanListeners.forEach(listener -> {
            if (listener.containsPoint(SpanListener.Point.Entry)) {
                ((EntrySpanListener)listener).parseEntry(spanDecorator, segmentCoreInfo);
            }
        });
    }

    private void notifyLocalListener(SpanDecorator spanDecorator) {
        spanListeners.forEach(listener -> {
            if (listener.containsPoint(SpanListener.Point.Local)) {
                ((LocalSpanListener)listener).parseLocal(spanDecorator, segmentCoreInfo);
            }
        });
    }

    private void notifyFirstListener(SpanDecorator spanDecorator) {
        spanListeners.forEach(listener -> {
            if (listener.containsPoint(SpanListener.Point.First)) {
                ((FirstSpanListener)listener).parseFirst(spanDecorator, segmentCoreInfo);
            }
        });
    }

    private void notifyGlobalsListener(UniqueId uniqueId) {
        spanListeners.forEach(listener -> {
            if (listener.containsPoint(SpanListener.Point.GlobalTraceIds)) {
                ((GlobalTraceIdsListener)listener).parseGlobalTraceId(uniqueId, segmentCoreInfo);
            }
        });
    }

    private void createSpanListeners() {
        listenerManager.getSpanListenerFactories().forEach(spanListenerFactory -> spanListeners.add(spanListenerFactory.create(moduleManager)));
    }

    public enum Source {
        Agent, Buffer
    }
}
