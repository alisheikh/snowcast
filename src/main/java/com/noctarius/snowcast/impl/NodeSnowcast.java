/*
 * Copyright (c) 2014, Christoph Engelbert (aka noctarius) and
 * contributors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.noctarius.snowcast.impl;

import com.hazelcast.config.ServiceConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.HazelcastInstanceImpl;
import com.hazelcast.instance.HazelcastInstanceProxy;
import com.hazelcast.spi.NodeEngine;
import com.hazelcast.spi.impl.NodeEngineImpl;
import com.noctarius.snowcast.Snowcast;
import com.noctarius.snowcast.SnowcastEpoch;
import com.noctarius.snowcast.SnowcastException;
import com.noctarius.snowcast.SnowcastSequencer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.noctarius.snowcast.impl.SnowcastConstants.DEFAULT_MAX_LOGICAL_NODES_13_BITS;

class NodeSnowcast
        implements Snowcast {

    private final NodeEngine nodeEngine;
    private final NodeSequencerService sequencerService;

    NodeSnowcast(HazelcastInstance hazelcastInstance) {
        this.nodeEngine = getNodeEngine(hazelcastInstance);
        this.sequencerService = getSequencerService(nodeEngine);
    }

    @Override
    public SnowcastSequencer createSequencer(String sequencerName, SnowcastEpoch epoch) {
        return createSequencer(sequencerName, epoch, DEFAULT_MAX_LOGICAL_NODES_13_BITS);
    }

    @Override
    public SnowcastSequencer createSequencer(String sequencerName, SnowcastEpoch epoch, int maxLogicalNodeCount) {
        return sequencerService.createSequencer(sequencerName, epoch, maxLogicalNodeCount);
    }

    @Override
    public void destroySequencer(SnowcastSequencer sequencer) {
        sequencerService.destroySequencer(sequencer);
    }

    private NodeSequencerService getSequencerService(NodeEngine nodeEngine) {
        // Ugly hacks due to lack in SPI
        NodeEngineImpl nodeEngineImpl = (NodeEngineImpl) nodeEngine;
        NodeSequencerService service = nodeEngineImpl.getService(SnowcastConstants.SERVICE_NAME);
        if (service != null) {
            return service;
        }

        // More ugly hacks to come here!
        try {
            Field serviceManagerField = NodeEngineImpl.class.getDeclaredField("serviceManager");
            serviceManagerField.setAccessible(true);
            Object serviceManager = serviceManagerField.get(nodeEngineImpl);

            Class<?> serviceManagerClass = serviceManager.getClass();
            Method registerUserService = serviceManagerClass
                    .getDeclaredMethod("registerUserService", Map.class, Map.class, ServiceConfig.class);
            registerUserService.setAccessible(true);

            ServiceConfig serviceConfig = new ServiceConfig().setEnabled(true).setName(SnowcastConstants.SERVICE_NAME)
                                                             .setServiceImpl(new NodeSequencerService());

            registerUserService.invoke(serviceManager, new HashMap<String, Properties>(), Collections.emptyMap(), serviceConfig);
            service = nodeEngineImpl.getService(SnowcastConstants.SERVICE_NAME);
            if (service != null) {
                service.init(nodeEngine, new Properties());
                return service;
            }

            String message = ExceptionMessages.SERVICE_REGISTRATION_FAILED.buildMessage();
            throw new SnowcastException(message);
        } catch (Exception e) {
            String message = ExceptionMessages.SERVICE_REGISTRATION_FAILED.buildMessage();
            throw new SnowcastException(message, e);
        }
    }

    private NodeEngine getNodeEngine(HazelcastInstance hazelcastInstance) {
        try {
            // Ugly hack due to lack in SPI
            Field originalField = HazelcastInstanceProxy.class.getDeclaredField("original");
            originalField.setAccessible(true);
            HazelcastInstanceImpl impl = (HazelcastInstanceImpl) originalField.get(hazelcastInstance);
            return impl.node.getNodeEngine();
        } catch (Exception e) {
            String message = ExceptionMessages.RETRIEVE_NODE_ENGINE_FAILED.buildMessage();
            throw new SnowcastException(message, e);
        }
    }
}
