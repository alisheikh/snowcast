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

import com.hazelcast.nio.serialization.ArrayDataSerializableFactory;
import com.hazelcast.nio.serialization.DataSerializableFactory;
import com.hazelcast.nio.serialization.DataSerializerHook;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import com.hazelcast.util.ConstructorFunction;
import com.noctarius.snowcast.impl.operations.AttachLogicalNodeOperation;
import com.noctarius.snowcast.impl.operations.CreateSequencerDefinitionOperation;
import com.noctarius.snowcast.impl.operations.DestroySequencerDefinitionOperation;
import com.noctarius.snowcast.impl.operations.DestroySequencerOperation;
import com.noctarius.snowcast.impl.operations.DetachLogicalNodeOperation;

public class SequencerDataSerializerHook
        implements DataSerializerHook {

    public static final int FACTORY_ID;

    public static final int TYPE_ATTACH_LOGICAL_NODE = 0;
    public static final int TYPE_DETACH_LOGICAL_NODE = 1;
    public static final int TYPE_CREATE_SEQUENCER_DEFINITION = 2;
    public static final int TYPE_DESTROY_SEQUENCER_DEFINITION = 3;
    public static final int TYPE_DESTROY_SEQUENCER = 4;

    private static final int LEN = TYPE_DESTROY_SEQUENCER + 1;

    private static final int DEFAULT_FACTORY_ID = 78412;

    static {
        FACTORY_ID = Integer.getInteger("com.noctarius.snowcast.factoryid", DEFAULT_FACTORY_ID);
    }

    @Override
    public int getFactoryId() {
        return FACTORY_ID;
    }

    @Override
    public DataSerializableFactory createFactory() {
        ConstructorFunction<Integer, IdentifiedDataSerializable>[] constructors = new ConstructorFunction[LEN];
        constructors[TYPE_ATTACH_LOGICAL_NODE] = new ConstructorFunction<Integer, IdentifiedDataSerializable>() {
            @Override
            public IdentifiedDataSerializable createNew(Integer id) {
                return new AttachLogicalNodeOperation();
            }
        };
        constructors[TYPE_DETACH_LOGICAL_NODE] = new ConstructorFunction<Integer, IdentifiedDataSerializable>() {
            @Override
            public IdentifiedDataSerializable createNew(Integer arg) {
                return new DetachLogicalNodeOperation();
            }
        };
        constructors[TYPE_CREATE_SEQUENCER_DEFINITION] = new ConstructorFunction<Integer, IdentifiedDataSerializable>() {
            @Override
            public IdentifiedDataSerializable createNew(Integer arg) {
                return new CreateSequencerDefinitionOperation();
            }
        };
        constructors[TYPE_DESTROY_SEQUENCER_DEFINITION] = new ConstructorFunction<Integer, IdentifiedDataSerializable>() {
            @Override
            public IdentifiedDataSerializable createNew(Integer arg) {
                return new DestroySequencerDefinitionOperation();
            }
        };
        constructors[TYPE_DESTROY_SEQUENCER] = new ConstructorFunction<Integer, IdentifiedDataSerializable>() {
            @Override
            public IdentifiedDataSerializable createNew(Integer arg) {
                return new DestroySequencerOperation();
            }
        };
        return new ArrayDataSerializableFactory(constructors);
    }
}
