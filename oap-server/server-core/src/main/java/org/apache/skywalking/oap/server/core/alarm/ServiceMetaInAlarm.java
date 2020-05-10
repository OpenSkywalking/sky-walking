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

package org.apache.skywalking.oap.server.core.alarm;

import lombok.Getter;
import lombok.Setter;
import org.apache.skywalking.oap.server.core.Const;
import org.apache.skywalking.oap.server.core.source.DefaultScopeDefine;

@Getter
@Setter
public class ServiceMetaInAlarm extends MetaInAlarm {
    private String metricsName;

    private String id;
    private String name;
    private String[] tags;
    private String[] properties;

    @Override
    public String getScope() {
        return DefaultScopeDefine.SERVICE_CATALOG_NAME;
    }

    @Override
    public int getScopeId() {
        return DefaultScopeDefine.SERVICE;
    }

    @Override
    public String getId0() {
        return id;
    }

    @Override
    public String getId1() {
        return Const.EMPTY_STRING;
    }
}
