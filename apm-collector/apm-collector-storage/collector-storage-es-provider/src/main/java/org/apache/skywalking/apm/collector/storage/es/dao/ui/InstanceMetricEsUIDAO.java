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

package org.apache.skywalking.apm.collector.storage.es.dao.ui;

import java.util.*;
import org.apache.skywalking.apm.collector.client.elasticsearch.ElasticSearchClient;
import org.apache.skywalking.apm.collector.core.util.Const;
import org.apache.skywalking.apm.collector.storage.dao.ui.IInstanceMetricUIDAO;
import org.apache.skywalking.apm.collector.storage.es.base.dao.EsDAO;
import org.apache.skywalking.apm.collector.storage.table.MetricSource;
import org.apache.skywalking.apm.collector.storage.table.application.ApplicationMetricTable;
import org.apache.skywalking.apm.collector.storage.table.instance.InstanceMetricTable;
import org.apache.skywalking.apm.collector.storage.ui.common.Step;
import org.apache.skywalking.apm.collector.storage.ui.server.AppServerInfo;
import org.apache.skywalking.apm.collector.storage.utils.*;
import org.elasticsearch.action.get.*;
import org.elasticsearch.action.search.*;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.*;
import org.elasticsearch.search.aggregations.metrics.sum.Sum;

/**
 * @author peng-yongsheng
 */
public class InstanceMetricEsUIDAO extends EsDAO implements IInstanceMetricUIDAO {

    public InstanceMetricEsUIDAO(ElasticSearchClient client) {
        super(client);
    }

    @Override
    public List<AppServerInfo> getServerThroughput(int applicationId, Step step, long startTimeBucket,
        long endTimeBucket, int minutesBetween, int topN, MetricSource metricSource) {
        String tableName = TimePyramidTableNameBuilder.build(step, InstanceMetricTable.TABLE);

        SearchRequestBuilder searchRequestBuilder = getClient().prepareSearch(tableName);
        searchRequestBuilder.setTypes(InstanceMetricTable.TABLE_TYPE);
        searchRequestBuilder.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        boolQuery.must().add(QueryBuilders.rangeQuery(InstanceMetricTable.TIME_BUCKET.getName()).gte(startTimeBucket).lte(endTimeBucket));
        if (applicationId != 0) {
            boolQuery.must().add(QueryBuilders.termQuery(InstanceMetricTable.APPLICATION_ID.getName(), applicationId));
        }
        boolQuery.must().add(QueryBuilders.termQuery(InstanceMetricTable.SOURCE_VALUE.getName(), metricSource.getValue()));

        searchRequestBuilder.setQuery(boolQuery);
        searchRequestBuilder.setSize(0);

        TermsAggregationBuilder aggregationBuilder = AggregationBuilders.terms(InstanceMetricTable.INSTANCE_ID.getName()).field(InstanceMetricTable.INSTANCE_ID.getName()).size(2000);
        aggregationBuilder.subAggregation(AggregationBuilders.sum(InstanceMetricTable.TRANSACTION_CALLS.getName()).field(InstanceMetricTable.TRANSACTION_CALLS.getName()));

        searchRequestBuilder.addAggregation(aggregationBuilder);
        SearchResponse searchResponse = searchRequestBuilder.execute().actionGet();

        List<AppServerInfo> appServerInfoList = new LinkedList<>();
        Terms instanceIdTerms = searchResponse.getAggregations().get(InstanceMetricTable.INSTANCE_ID.getName());
        instanceIdTerms.getBuckets().forEach(instanceIdTerm -> {
            int instanceId = instanceIdTerm.getKeyAsNumber().intValue();
            Sum callSum = instanceIdTerm.getAggregations().get(ApplicationMetricTable.TRANSACTION_CALLS.getName());
            long calls = (long)callSum.getValue();
            int callsPerMinute = (int)(minutesBetween == 0 ? 0 : calls / minutesBetween);

            AppServerInfo appServerInfo = new AppServerInfo();
            appServerInfo.setId(instanceId);
            appServerInfo.setCpm(callsPerMinute);
            appServerInfoList.add(appServerInfo);
        });

        appServerInfoList.sort((first, second) -> Integer.compare(second.getCpm(), first.getCpm()));
        if (appServerInfoList.size() <= topN) {
            return appServerInfoList;
        } else {
            List<AppServerInfo> newCollection = new LinkedList<>();
            for (int i = 0; i < topN; i++) {
                newCollection.add(appServerInfoList.get(i));
            }
            return newCollection;
        }
    }

    @Override
    public List<Integer> getServerThroughputTrend(int instanceId, Step step, List<DurationPoint> durationPoints) {
        String tableName = TimePyramidTableNameBuilder.build(step, InstanceMetricTable.TABLE);
        MultiGetRequestBuilder prepareMultiGet = getClient().prepareMultiGet(durationPoints, new ElasticSearchClient.MultiGetRowHandler<DurationPoint>() {
            @Override
            public void accept(DurationPoint durationPoint) {
                String id = durationPoint.getPoint() + Const.ID_SPLIT + instanceId + Const.ID_SPLIT + MetricSource.Callee.getValue();
                add(tableName, InstanceMetricTable.TABLE_TYPE, id);
            }
        });

        List<Integer> throughputTrend = new LinkedList<>();
        MultiGetResponse multiGetResponse = prepareMultiGet.get();

        for (int i = 0; i < multiGetResponse.getResponses().length; i++) {
            MultiGetItemResponse response = multiGetResponse.getResponses()[i];
            if (response.getResponse().isExists()) {
                long callTimes = ((Number)response.getResponse().getSource().get(InstanceMetricTable.TRANSACTION_CALLS.getName())).longValue();
                throughputTrend.add((int)(callTimes / durationPoints.get(i).getMinutesBetween()));
            } else {
                throughputTrend.add(0);
            }
        }
        return throughputTrend;
    }

    @Override
    public List<Integer> getResponseTimeTrend(int instanceId, Step step, List<DurationPoint> durationPoints) {
        String tableName = TimePyramidTableNameBuilder.build(step, InstanceMetricTable.TABLE);
        MultiGetRequestBuilder prepareMultiGet = getClient().prepareMultiGet(durationPoints, new ElasticSearchClient.MultiGetRowHandler<DurationPoint>() {
            @Override
            public void accept(DurationPoint durationPoint) {
                String id = durationPoint.getPoint() + Const.ID_SPLIT + instanceId + Const.ID_SPLIT + MetricSource.Callee.getValue();
                add(tableName, InstanceMetricTable.TABLE_TYPE, id);
            }

        });

        List<Integer> responseTimeTrends = new LinkedList<>();
        MultiGetResponse multiGetResponse = prepareMultiGet.get();
        for (MultiGetItemResponse response : multiGetResponse.getResponses()) {
            if (response.getResponse().isExists()) {
                long callTimes = ((Number)response.getResponse().getSource().get(InstanceMetricTable.TRANSACTION_CALLS.getName())).longValue();
                long durationSum = ((Number)response.getResponse().getSource().get(InstanceMetricTable.TRANSACTION_DURATION_SUM.getName())).longValue();
                responseTimeTrends.add((int)(durationSum / callTimes));
            } else {
                responseTimeTrends.add(0);
            }
        }
        return responseTimeTrends;
    }
}
