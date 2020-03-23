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
 */

package org.apache.skywalking.apm.agent.core.context;

import org.apache.skywalking.apm.agent.core.base64.Base64;
import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.util.StringUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Correlation context, use to propagation user custom data.
 * Working on the protocol and delegate set/get method.
 */
public class CorrelationContext {

    private final Map<String, String> data;

    public CorrelationContext() {
        this.data = new HashMap<>(0);
    }

    public Optional<String> set(String key, String value) {
        // key must not null
        if (key == null) {
            return Optional.empty();
        }
        if (value == null) {
            value = "";
        }

        // check value length
        if (value.length() > Config.Correlation.VALUE_MAX_LENGTH) {
            return Optional.empty();
        }

        // already contain key
        if (data.containsKey(key)) {
            final String previousValue = data.put(key, value);
            return Optional.of(previousValue);
        }

        // check keys count
        if (data.size() >= Config.Correlation.ELEMENT_MAX_NUMBER) {
            return Optional.empty();
        }

        // setting
        data.put(key, value);
        return Optional.empty();
    }

    public Optional<String> get(String key) {
        if (key == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(data.get(key));
    }

    /**
     * Serialize this {@link CorrelationContext} to a {@link String}
     *
     * @return the serialization string.
     */
    String serialize() {
        if (data.isEmpty()) {
            return "";
        }

        return data.entrySet().stream()
            .map(entry -> Base64.encode(entry.getKey()) + ":" + Base64.encode(entry.getValue()))
            .collect(Collectors.joining(","));
    }

    /**
     * Deserialize data from {@link String}
     */
    void deserialize(String value) {
        if (StringUtil.isEmpty(value)) {
            return;
        }

        for (String perData : value.split(",")) {
            final String[] parts = perData.split(":");
            String perDataKey = parts[0];
            String perDataValue = parts.length > 1 ? parts[1] : "";
            data.put(Base64.decode2UTFString(perDataKey), Base64.decode2UTFString(perDataValue));
        }
    }

    /**
     * Prepare for the cross-process propagation. Inject the {@link #data} into {@link ContextCarrier#getCorrelationContext()}
     */
    void inject(ContextCarrier carrier) {
        carrier.getCorrelationContext().data.putAll(this.data);
    }

    /**
     * Extra the {@link ContextCarrier#getCorrelationContext()} into this context.
     */
    void extract(ContextCarrier carrier) {
        this.data.putAll(carrier.getCorrelationContext().data);
    }

    /**
     * Clone the context data, work for capture to cross-thread.
     */
    public CorrelationContext clone() {
        final CorrelationContext context = new CorrelationContext();
        context.data.putAll(this.data);
        return context;
    }

    void continued(ContextSnapshot snapshot) {
        this.data.putAll(snapshot.getCorrelationContext().data);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CorrelationContext that = (CorrelationContext) o;
        return Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data);
    }
}
