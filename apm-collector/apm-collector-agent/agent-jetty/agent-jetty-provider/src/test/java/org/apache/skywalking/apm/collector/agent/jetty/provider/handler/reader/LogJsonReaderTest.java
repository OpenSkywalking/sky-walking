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

package org.apache.skywalking.apm.collector.agent.jetty.provider.handler.reader;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.apache.skywalking.apm.network.proto.LogMessage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import static java.lang.System.out;

/**
 * @author lican
 */
//@RunWith(MockitoJUnitRunner.class)
public class LogJsonReaderTest {

    private LogJsonReader logJsonReader;

    @Before
    public void setUp() throws Exception {
        logJsonReader = new LogJsonReader();
    }

    @Test
    public void read() throws IOException {
        Gson gson = new Gson();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("k", "hello");
        jsonObject.addProperty("v", "world");

        JsonArray array = new JsonArray();
        array.add(jsonObject);

        long l = System.currentTimeMillis();

        JsonObject json = new JsonObject();
        json.addProperty("ti", l);
        json.add("ld", array);
        String s = gson.toJson(json);
        out.println(s);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(s.getBytes());
        InputStreamReader inputStreamReader = new InputStreamReader(byteArrayInputStream);

        JsonReader reader = new JsonReader(inputStreamReader);
        reader.setLenient(true);

        LogMessage read = logJsonReader.read(reader);

        Assert.assertEquals(read.getTime(), l);
    }


}