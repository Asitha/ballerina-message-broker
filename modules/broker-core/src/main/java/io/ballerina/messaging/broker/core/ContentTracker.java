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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Keep track of the size of the message content on flight. When high limit is reached notifies the
 * observers.
 */
public class ContentTracker extends Observable {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationTask.class);

    private final AtomicLong trackedContentSize;

    private final ExecutorService eventDispatcherService;

    private final BlockingQueue<EventType> eventQueue;

    private final List<TrackedContentLimitListener> listeners;

    private long highWatermark;

    private long lowWatermark;

    private final NotificationTask notificationTask;

    private enum EventType {
        HIGH,
        LOW,
        NEW_LISTENER
    }

    public ContentTracker(long highWatermark, long lowWatermark) {
        trackedContentSize = new AtomicLong();
        eventDispatcherService = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setName("ContentTrackerThread");
            return thread;
        });
        this.highWatermark = highWatermark;
        listeners = new ArrayList<>(1);
        this.lowWatermark = lowWatermark;
        eventQueue = new LinkedBlockingDeque<>();
        notificationTask = new NotificationTask();
    }

    public void track(long size) {
        long limit = trackedContentSize.addAndGet(size);
        if (limit > highWatermark) {
            eventQueue.add(EventType.HIGH);
        }
    }

    public void start() {
        eventDispatcherService.submit(notificationTask);
    }

    public void untrack(long size) {
        long limit = trackedContentSize.addAndGet(-1 * size);
        if (limit < lowWatermark) {
            eventQueue.add(EventType.LOW);
        }
    }

    public void addListener(TrackedContentLimitListener listener) {
        listeners.add(listener);
        eventQueue.add(EventType.NEW_LISTENER);
    }

    private class NotificationTask implements Runnable {

        private AtomicBoolean active;

        NotificationTask() {
            active = new AtomicBoolean(true);
        }

        @Override
        public void run() {
            boolean updated = active.compareAndSet(false, true);
            if (!updated) {
                LOGGER.error("Task already running not running the new task");
            }
            while (active.get()) {
                try {
                    notifyListeners(eventQueue.take());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    LOGGER.error("Error occurred while running task", e);
                }
            }

            LOGGER.info("ContentTracker notification task stopped");
        }

        private void notifyListeners(EventType event) {
            if (event == EventType.HIGH) {
                listeners.forEach(TrackedContentLimitListener::highWatermarkReached);
            } else if (event == EventType.LOW) {
                listeners.forEach(TrackedContentLimitListener::lowWatermarkReached);
            }
        }

        void stop() {
            active.set(false);
        }

        void addListener(TrackedContentLimitListener listener) {
            listeners.add(listener);
        }
    }
}
