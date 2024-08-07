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

import java.io.Serializable;
import java.util.Objects;

/**
 * This class represent cache key for {@link AppNotificationTemplateListCache}.
 */
public class AppNotificationTemplateListCacheKey implements Serializable {

    private String templateType;
    private String channelName;
    private String applicationUuid;

    public AppNotificationTemplateListCacheKey(String templateType, String channelName, String applicationUuid) {
        this.templateType = templateType.toLowerCase();
        this.channelName = channelName;
        this.applicationUuid = applicationUuid;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppNotificationTemplateListCacheKey that = (AppNotificationTemplateListCacheKey) o;
        return Objects.equals(templateType, that.templateType) &&
                Objects.equals(channelName, that.channelName) &&
                Objects.equals(applicationUuid, that.applicationUuid);
    }

    @Override
    public int hashCode() {

        return Objects.hash(templateType, channelName, applicationUuid);
    }
}
