/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.messaging.broker.core;

/**
 * Handles creation of content chunk objects. This will handle object pooling and maintains the number of messages on
 * flight with high and low water mark levels.
 *
 */
public class ContentChunkFactory {

    private ContentTracker tracker;

    ContentChunkFactory(ContentTracker tracker) {
        this.tracker = tracker;
    }

    public ContentChunk getNewChunk(long offset, byte[] content) {
        return new ContentChunk(tracker, offset, content);
    }
}
