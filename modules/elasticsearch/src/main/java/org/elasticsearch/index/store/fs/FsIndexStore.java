/*
 * Licensed to Elastic Search and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Elastic Search licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.index.store.fs;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.AbstractIndexComponent;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.LocalNodeId;
import org.elasticsearch.index.settings.IndexSettings;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.index.store.IndexStore;

import java.io.File;

/**
 * @author kimchy (shay.banon)
 */
public abstract class FsIndexStore extends AbstractIndexComponent implements IndexStore {

    private final File location;

    public FsIndexStore(Index index, @IndexSettings Settings indexSettings, Environment environment, @LocalNodeId String localNodeId) {
        super(index, indexSettings);
        this.location = new File(new File(new File(environment.workWithClusterFile(), "indices"), localNodeId), index.name());

        if (!location.exists()) {
            boolean result = false;
            for (int i = 0; i < 5; i++) {
                result = location.mkdirs();
                if (result) {
                    break;
                }
            }
        }
    }

    @Override public boolean persistent() {
        return true;
    }

    public File location() {
        return location;
    }

    public File shardLocation(ShardId shardId) {
        return new File(new File(location, Integer.toString(shardId.id())), "index");
    }
}