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

package org.wso2.carbon.email.mgt.store;

import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerServerException;
import org.wso2.carbon.identity.governance.model.NotificationTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class serves as a unified template management system that delegates the template persistence operations
 * to both a database-based manager and an in-memory manager.
 * This class maintains two separate instances of TemplatePersistenceManager:
 * one for managing templates stored in a database {@link DBBasedTemplateManager},
 * and another for managing templates stored in memory {@link InMemoryBasedTemplateManager}.
 */
public class DefaultTemplateManager implements TemplatePersistenceManager {

    private final TemplatePersistenceManager dbBasedTemplateManager = new DBBasedTemplateManager();
    private final TemplatePersistenceManager inMemoryTemplateManager = new InMemoryBasedTemplateManager();

    @Override
    public void addNotificationTemplateType(String displayName, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        dbBasedTemplateManager.addNotificationTemplateType(displayName, notificationChannel, tenantDomain);
    }

    @Override
    public boolean isNotificationTemplateTypeExists(String displayName, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        return dbBasedTemplateManager.isNotificationTemplateTypeExists(displayName, notificationChannel,
                tenantDomain) ||
                inMemoryTemplateManager.isNotificationTemplateTypeExists(displayName, notificationChannel,
                        tenantDomain);
    }

    @Override
    public List<String> listNotificationTemplateTypes(String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        List<String> dbBasedTemplateTypes = dbBasedTemplateManager.listNotificationTemplateTypes(notificationChannel,
                tenantDomain);
        List<String> inMemoryTemplateTypes = inMemoryTemplateManager.listNotificationTemplateTypes(notificationChannel,
                tenantDomain);

        return mergeAndRemoveDuplicates(dbBasedTemplateTypes, inMemoryTemplateTypes);
    }

    @Override
    public void deleteNotificationTemplateType(String displayName, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        if (dbBasedTemplateManager.isNotificationTemplateTypeExists(displayName, notificationChannel, tenantDomain)) {
            dbBasedTemplateManager.deleteNotificationTemplateType(displayName, notificationChannel, tenantDomain);
        }
    }

    @Override
    public void addOrUpdateNotificationTemplate(NotificationTemplate notificationTemplate, String applicationUuid,
                                                String tenantDomain) throws NotificationTemplateManagerServerException {

        dbBasedTemplateManager.addOrUpdateNotificationTemplate(notificationTemplate, applicationUuid, tenantDomain);
    }

    @Override
    public boolean isNotificationTemplateExists(String displayName, String locale, String notificationChannel,
                                                String applicationUuid, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        return dbBasedTemplateManager.isNotificationTemplateExists(displayName, locale, notificationChannel,
                applicationUuid, tenantDomain) ||
                inMemoryTemplateManager.isNotificationTemplateExists(displayName, locale, notificationChannel,
                        applicationUuid, tenantDomain);
    }

    @Override
    public NotificationTemplate getNotificationTemplate(String displayName, String locale, String notificationChannel,
                                                        String applicationUuid, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        if (dbBasedTemplateManager.isNotificationTemplateExists(displayName, locale, notificationChannel,
                applicationUuid, tenantDomain)) {
            return dbBasedTemplateManager.getNotificationTemplate(displayName, locale, notificationChannel,
                    applicationUuid, tenantDomain);
        } else {
            return inMemoryTemplateManager.getNotificationTemplate(displayName, locale, notificationChannel,
                    applicationUuid, tenantDomain);
        }
    }

    @Override
    public List<NotificationTemplate> listNotificationTemplates(String templateType, String notificationChannel,
                                                                String applicationUuid, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        List<NotificationTemplate> dbBasedTemplates = new ArrayList<>();
        if (dbBasedTemplateManager.isNotificationTemplateTypeExists(templateType, notificationChannel,
                tenantDomain)) {
            dbBasedTemplates =
                    dbBasedTemplateManager.listNotificationTemplates(templateType, notificationChannel, applicationUuid,
                            tenantDomain);
        }

        List<NotificationTemplate> inMemoryBasedTemplates = new ArrayList<>();
        if (inMemoryTemplateManager.isNotificationTemplateTypeExists(templateType, notificationChannel,
                tenantDomain)) {
            inMemoryBasedTemplates =
                    inMemoryTemplateManager.listNotificationTemplates(templateType, notificationChannel,
                            applicationUuid, tenantDomain);
        }

        return mergeAndRemoveDuplicates(dbBasedTemplates, inMemoryBasedTemplates);
    }

    @Override
    public List<NotificationTemplate> listAllNotificationTemplates(String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        List<NotificationTemplate> dbBasedTemplates =
                dbBasedTemplateManager.listAllNotificationTemplates(notificationChannel, tenantDomain);
        List<NotificationTemplate> inMemoryBasedTemplates =
                inMemoryTemplateManager.listAllNotificationTemplates(notificationChannel, tenantDomain);

        return mergeAndRemoveDuplicates(dbBasedTemplates, inMemoryBasedTemplates);
    }

    @Override
    public void deleteNotificationTemplate(String displayName, String locale, String notificationChannel,
                                           String applicationUuid, String tenantDomain)
            throws NotificationTemplateManagerServerException {

        if (dbBasedTemplateManager.isNotificationTemplateExists(displayName, locale, notificationChannel,
                applicationUuid, tenantDomain)) {
            dbBasedTemplateManager.deleteNotificationTemplate(displayName, locale, notificationChannel, applicationUuid,
                    tenantDomain);
        }
    }

    @Override
    public void deleteNotificationTemplates(String displayName, String notificationChannel, String applicationUuid,
                                            String tenantDomain) throws NotificationTemplateManagerServerException {

        if (dbBasedTemplateManager.isNotificationTemplateTypeExists(displayName, notificationChannel, tenantDomain)) {
            dbBasedTemplateManager.deleteNotificationTemplates(displayName, notificationChannel, applicationUuid,
                    tenantDomain);
        }
    }

    /**
     * Merges two lists and removes duplicates.
     *
     * @param dbBasedTemplates DbBasedTemplates
     * @param inMemoryTemplates InMemoryTemplates
     * @return Merged list without duplicates.
     */
    private <T> List<T> mergeAndRemoveDuplicates(List<T> dbBasedTemplates, List<T> inMemoryTemplates) {

        Set<T> uniqueElements = new HashSet<>();
        uniqueElements.addAll(dbBasedTemplates);
        uniqueElements.addAll(inMemoryTemplates);
        return new ArrayList<>(uniqueElements);
    }
}
