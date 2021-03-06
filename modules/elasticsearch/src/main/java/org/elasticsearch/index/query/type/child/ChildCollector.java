/*
 * Licensed to Elastic Search and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Elastic Search licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.index.query.type.child;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.util.OpenBitSet;
import org.elasticsearch.common.BytesWrap;
import org.elasticsearch.index.cache.id.IdReaderTypeCache;
import org.elasticsearch.search.internal.SearchContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author kimchy (shay.banon)
 */
public class ChildCollector extends Collector {

    private final String parentType;

    private final SearchContext context;

    private final Map<Object, IdReaderTypeCache> typeCacheMap;

    private final Map<Object, OpenBitSet> parentDocs;

    private IdReaderTypeCache typeCache;

    public ChildCollector(String parentType, SearchContext context) {
        this.parentType = parentType;
        this.context = context;
        this.parentDocs = new HashMap<Object, OpenBitSet>();

        // create a specific type map lookup for faster lookup operations per doc
        this.typeCacheMap = new HashMap<Object, IdReaderTypeCache>(context.searcher().subReaders().length);
        for (IndexReader indexReader : context.searcher().subReaders()) {
            typeCacheMap.put(indexReader.getFieldCacheKey(), context.idCache().reader(indexReader).type(parentType));
        }
    }

    public Map<Object, OpenBitSet> parentDocs() {
        return this.parentDocs;
    }

    @Override public void setScorer(Scorer scorer) throws IOException {

    }

    @Override public void collect(int doc) throws IOException {
        BytesWrap parentId = typeCache.parentIdByDoc(doc);
        if (parentId == null) {
            return;
        }
        for (IndexReader indexReader : context.searcher().subReaders()) {
            int parentDocId = typeCacheMap.get(indexReader.getFieldCacheKey()).docById(parentId);
            if (parentDocId != -1 && !indexReader.isDeleted(parentDocId)) {
                OpenBitSet docIdSet = parentDocs().get(indexReader.getFieldCacheKey());
                if (docIdSet == null) {
                    docIdSet = new OpenBitSet(indexReader.maxDoc());
                    parentDocs.put(indexReader.getFieldCacheKey(), docIdSet);
                }
                docIdSet.fastSet(parentDocId);
                return;
            }
        }
    }

    @Override public void setNextReader(IndexReader reader, int docBase) throws IOException {
        typeCache = typeCacheMap.get(reader.getFieldCacheKey());
    }

    @Override public boolean acceptsDocsOutOfOrder() {
        return true;
    }
}
