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

package org.skywalking.apm.collector.core.data;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author wu-sheng
 */
public class AbstractHashMessageTest {
    public class NewMessage extends AbstractHashMessage {
        public NewMessage() {
            super("key");
        }
    }

    @Test
    public void testHash() {
        NewMessage message = new NewMessage();
        Assert.assertEquals("key".hashCode(), message.getHashCode());
    }
}
