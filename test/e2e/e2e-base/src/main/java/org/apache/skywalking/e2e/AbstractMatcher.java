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

package org.apache.skywalking.e2e;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author kezhenxu94
 */
public class AbstractMatcher {
    private static final Pattern NE_MATCHER = Pattern.compile("ne\\s+(?<val>.+)");
    private static final Pattern GT_MATCHER = Pattern.compile("gt\\s+(?<val>.+)");
    private static final Pattern GE_MATCHER = Pattern.compile("ge\\s+(?<val>.+)");
    private static final Pattern NN_MATCHER = Pattern.compile("^not null$");

    protected void doVerify(String expected, String actual) {
        Matcher matcher = NN_MATCHER.matcher(expected);
        if (matcher.find()) {
            assertThat(actual).isNotNull();
            return;
        }

        matcher = NE_MATCHER.matcher(expected);
        if (matcher.find()) {
            assertThat(actual).isNotEqualTo(matcher.group("val"));
            return;
        }
        matcher = GT_MATCHER.matcher(expected);

        if (matcher.find()) {
            String val = matcher.group("val");

            assertThat(val).isNotBlank();
            assertThat(Double.parseDouble(actual)).isGreaterThan(Double.parseDouble(val));
            return;
        }

        matcher = GE_MATCHER.matcher(expected);
        if (matcher.find()) {
            String val = matcher.group("val");

            assertThat(val).isNotBlank();
            assertThat(Double.parseDouble(actual)).isGreaterThanOrEqualTo(Double.parseDouble(val));
            return;
        }

        assertThat(actual).isEqualTo(expected);
    }

}
