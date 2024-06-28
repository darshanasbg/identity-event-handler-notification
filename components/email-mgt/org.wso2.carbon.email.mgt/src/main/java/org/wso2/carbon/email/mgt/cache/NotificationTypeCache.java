/*
 * Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
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

package org.wso2.carbon.email.mgt.cache;

import org.wso2.carbon.identity.core.cache.BaseCache;

/**
 * Cache implementation for NotificationTypes.
 */
public class NotificationTypeCache extends BaseCache<NotificationTypeCacheKey, String> {

    private static final String CACHE_NAME = "NotificationTypeCache";
    private static final NotificationTypeCache instance = new NotificationTypeCache();

    private NotificationTypeCache() {

        super(CACHE_NAME);
    }

    public static NotificationTypeCache getInstance() {

        return instance;
    }
}
