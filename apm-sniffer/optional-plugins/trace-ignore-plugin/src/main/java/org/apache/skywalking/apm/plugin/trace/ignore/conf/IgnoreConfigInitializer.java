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

package org.apache.skywalking.apm.plugin.trace.ignore.conf;

import org.apache.skywalking.apm.agent.core.boot.AgentPackageNotFoundException;
import org.apache.skywalking.apm.agent.core.boot.AgentPackagePath;
import org.apache.skywalking.apm.agent.core.conf.ConfigNotFoundException;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.util.ConfigInitializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @author liujc [liujunc1993@163.com]
 *
 */
public class IgnoreConfigInitializer {
    private static final ILog logger = LogManager.getLogger(IgnoreConfigInitializer.class);
    private static String CONFIG_FILE_NAME = "/config/apm-trace-ignore-plugin.config";
    private static String ENV_KEY_PREFIX = "skywalking.";

    /**
     * Try to locate `apm-trace-ignore-plugin.config`, which should be in the /optional-plugins/apm-trace-ignore-plugin/ dictionary of agent package.
     * <p>
     * Also try to override the config by system.env and system.properties. All the keys in these two places should
     * start with {@link #ENV_KEY_PREFIX}. e.g. in env `skywalking.trace.ignore_path=your_path` to override
     * `trace.ignore_path` in apm-trace-ignore-plugin.config file.
     * <p>
     */
    public static void initialize() throws ConfigNotFoundException, AgentPackageNotFoundException {
        InputStream configFileStream;
        try {
            configFileStream = loadConfigFromAgentFolder();
            Properties properties = new Properties();
            properties.load(configFileStream);
            ConfigInitializer.initialize(properties, IgnoreConfig.class);
        } catch (Exception e) {
            logger.error(e, "Failed to read the config file, skywalking is going to run in default config.");
        }

        try {
            overrideConfigBySystemEnv();
        } catch (Exception e) {
            logger.error(e, "Failed to read the system env.");
        }
    }

    private static void overrideConfigBySystemEnv() throws IllegalAccessException {
        Properties properties = new Properties();
        Properties systemProperties = System.getProperties();
        Iterator<Map.Entry<Object, Object>> entryIterator = systemProperties.entrySet().iterator();
        while (entryIterator.hasNext()) {
            Map.Entry<Object, Object> prop = entryIterator.next();
            if (prop.getKey().toString().startsWith(ENV_KEY_PREFIX)) {
                String realKey = prop.getKey().toString().substring(ENV_KEY_PREFIX.length());
                properties.put(realKey, prop.getValue());
            }
        }

        if (!properties.isEmpty()) {
            ConfigInitializer.initialize(properties, IgnoreConfig.class);
        }
    }


    /**
     * Load the config file, where the agent jar is.
     *
     * @return the config file {@link InputStream}, or null if not needEnhance.
     */
    private static InputStream loadConfigFromAgentFolder() throws AgentPackageNotFoundException, ConfigNotFoundException {
        File configFile = new File(AgentPackagePath.getPath(), CONFIG_FILE_NAME);
        if (configFile.exists() && configFile.isFile()) {
            try {
                logger.info("Ignore config file found in {}.", configFile);
                return new FileInputStream(configFile);
            } catch (FileNotFoundException e) {
                throw new ConfigNotFoundException("Fail to load apm-trace-ignore-plugin.config", e);
            }
        }
        throw new ConfigNotFoundException("Fail to load ignore config file.");
    }
}
