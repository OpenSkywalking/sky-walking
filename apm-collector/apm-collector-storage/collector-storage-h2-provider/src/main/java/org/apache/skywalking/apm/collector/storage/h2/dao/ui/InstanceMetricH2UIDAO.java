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

package org.apache.skywalking.apm.collector.storage.h2.dao.ui;

import java.sql.*;
import java.util.*;
import org.apache.skywalking.apm.collector.client.h2.*;
import org.apache.skywalking.apm.collector.core.util.Const;
import org.apache.skywalking.apm.collector.storage.base.sql.SqlBuilder;
import org.apache.skywalking.apm.collector.storage.dao.ui.IInstanceMetricUIDAO;
import org.apache.skywalking.apm.collector.storage.h2.base.dao.H2DAO;
import org.apache.skywalking.apm.collector.storage.table.MetricSource;
import org.apache.skywalking.apm.collector.storage.table.instance.InstanceMetricTable;
import org.apache.skywalking.apm.collector.storage.ui.common.Step;
import org.apache.skywalking.apm.collector.storage.ui.server.AppServerInfo;
import org.apache.skywalking.apm.collector.storage.utils.*;
import org.slf4j.*;

/**
 * @author peng-yongsheng, clevertension
 */
public class InstanceMetricH2UIDAO extends H2DAO implements IInstanceMetricUIDAO {

    private final Logger logger = LoggerFactory.getLogger(InstanceMetricH2UIDAO.class);
    private static final String GET_TPS_METRIC_SQL = "select * from {0} where {1} = ?";

    public InstanceMetricH2UIDAO(H2Client client) {
        super(client);
    }

    @Override public List<AppServerInfo> getServerThroughput(int applicationId, Step step, long startTimeBucket,
        long endTimeBucket,
        int minutesBetween, int topN, MetricSource metricSource) {
        return null;
    }

    @Override
    public List<Integer> getServerThroughputTrend(int instanceId, Step step, List<DurationPoint> durationPoints) {
        H2Client client = getClient();
        String tableName = TimePyramidTableNameBuilder.build(step, InstanceMetricTable.TABLE);

        String sql = SqlBuilder.buildSql(GET_TPS_METRIC_SQL, tableName, InstanceMetricTable.ID.getName());

        List<Integer> throughputTrend = new LinkedList<>();
        durationPoints.forEach(durationPoint -> {
            String id = durationPoint.getPoint() + Const.ID_SPLIT + instanceId + Const.ID_SPLIT + MetricSource.Callee.getValue();
            try (ResultSet rs = client.executeQuery(sql, new Object[] {id})) {
                if (rs.next()) {
                    long callTimes = rs.getLong(InstanceMetricTable.TRANSACTION_CALLS.getName());
                    throughputTrend.add((int)(callTimes / durationPoint.getMinutesBetween()));
                } else {
                    throughputTrend.add(0);
                }
            } catch (SQLException | H2ClientException e) {
                logger.error(e.getMessage(), e);
            }
        });

        return throughputTrend;
    }

    @Override public List<Integer> getResponseTimeTrend(int instanceId, Step step, List<DurationPoint> durationPoints) {
        H2Client client = getClient();

        String tableName = TimePyramidTableNameBuilder.build(step, InstanceMetricTable.TABLE);
        String sql = SqlBuilder.buildSql(GET_TPS_METRIC_SQL, tableName, InstanceMetricTable.ID.getName());

        List<Integer> responseTimeTrends = new LinkedList<>();
        durationPoints.forEach(durationPoint -> {
            String id = durationPoint.getPoint() + Const.ID_SPLIT + instanceId + Const.ID_SPLIT + MetricSource.Callee.getValue();
            try (ResultSet rs = client.executeQuery(sql, new Object[] {id})) {
                if (rs.next()) {
                    long callTimes = rs.getLong(InstanceMetricTable.TRANSACTION_CALLS.getName());
                    long durationSum = rs.getLong(InstanceMetricTable.TRANSACTION_DURATION_SUM.getName());
                    responseTimeTrends.add((int)(durationSum / callTimes));
                } else {
                    responseTimeTrends.add(0);
                }
            } catch (SQLException | H2ClientException e) {
                logger.error(e.getMessage(), e);
            }
        });
        return responseTimeTrends;
    }
}
