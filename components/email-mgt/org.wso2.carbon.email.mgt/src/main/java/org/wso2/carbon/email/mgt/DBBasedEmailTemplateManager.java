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

package org.wso2.carbon.email.mgt;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.email.mgt.dao.AppNotificationTemplateDAO;
import org.wso2.carbon.email.mgt.dao.NotificationScenarioDAO;
import org.wso2.carbon.email.mgt.dao.OrgNotificationTemplateDAO;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtException;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtServerException;
import org.wso2.carbon.email.mgt.internal.I18nMgtDataHolder;
import org.wso2.carbon.email.mgt.model.EmailTemplate;
import org.wso2.carbon.email.mgt.model.NotificationTemplate;

import java.util.List;

import static org.wso2.carbon.email.mgt.util.I18nEmailUtil.*;

/**
 * Provides functionality to manage email template-scenarios & email templates.
 */
public class DBBasedEmailTemplateManager implements EmailTemplateManager {
    private static final Log log = LogFactory.getLog(DBBasedEmailTemplateManager.class);
    private final String EMAIL_CHANNEL = "EMAIL";
    private final NotificationScenarioDAO notificationScenarioDAO = new NotificationScenarioDAO();
    private final OrgNotificationTemplateDAO orgNotificationTemplateDAO = new OrgNotificationTemplateDAO();
    private final AppNotificationTemplateDAO appNotificationTemplateDAO = new AppNotificationTemplateDAO();
    @Override
    public void addEmailTemplateType(String emailTemplateType, String tenantDomain) throws I18nEmailMgtException {
        log.info("Test addEmailTemplateType()");
        try {
            notificationScenarioDAO.addNotificationScenario(emailTemplateType, emailTemplateType, EMAIL_CHANNEL, getTenantId(tenantDomain));
        } catch (Exception e) {
            throw new I18nEmailMgtServerException("Error while adding the template type", e);
        }
    }

    @Override
    public boolean isEmailTemplateTypeExists(String templateTypeDisplayName, String tenantDomain) throws I18nEmailMgtException {

        log.info("Test type isEmailTemplateTypeExists()");
        try {
            String scenarioName = notificationScenarioDAO.getNotificationScenario(templateTypeDisplayName, EMAIL_CHANNEL, getTenantId(tenantDomain));
            return StringUtils.isNotBlank(scenarioName);
        } catch (Exception e) {
            throw new I18nEmailMgtServerException("Error while checking the existence of the template types", e);
        }
    }

    @Override
    public List<String> getAvailableTemplateTypes(String tenantDomain) throws I18nEmailMgtServerException {

        log.info("Test type getAvailableTemplateTypes()");
        try {
            List<String> templateTypes = notificationScenarioDAO.listNotificationScenarios(EMAIL_CHANNEL, getTenantId(tenantDomain));
            return templateTypes;
        } catch (Exception e) {
            throw new I18nEmailMgtServerException("Error while listing the template types", e);
        }
    }

    @Override
    public void deleteEmailTemplateType(String templateDisplayName, String tenantDomain) throws I18nEmailMgtException {

        log.info("Test type deleteEmailTemplateType()");
        try {
            notificationScenarioDAO.removeNotificationScenario(templateDisplayName, EMAIL_CHANNEL, getTenantId(tenantDomain));
        } catch (Exception e) {
            throw new I18nEmailMgtServerException("Error while deleting the template types", e);
        }
    }

    @Override
    public void addEmailTemplate(EmailTemplate emailTemplate, String tenantDomain) throws I18nEmailMgtException {

        log.info("Test org addEmailTemplate(): " + emailTemplate.getTemplateDisplayName());
        try {
            orgNotificationTemplateDAO.addNotificationTemplate(convertToNotificationTemplate(emailTemplate), EMAIL_CHANNEL, getTenantId(tenantDomain));
        } catch (Exception e) {
            throw new I18nEmailMgtServerException("Error while adding the template", e);
        }
    }

    @Override
    public EmailTemplate getEmailTemplate(String templateType, String locale, String tenantDomain) throws I18nEmailMgtException {

        log.info("Test org getEmailTemplate()");
        try {
            NotificationTemplate notificationTemplate = orgNotificationTemplateDAO.getNotificationTemplate(locale, templateType, EMAIL_CHANNEL, getTenantId(tenantDomain));
            return convertToEmailTemplate(notificationTemplate);
        } catch (Exception e) {
            throw new I18nEmailMgtServerException("Error while retrieving the template", e);
        }
    }

    @Override
    public boolean isEmailTemplateExists(String templateTypeDisplayName, String locale, String tenantDomain)
            throws I18nEmailMgtException {

        log.info("Test org isEmailTemplateExists()");
        try {
            NotificationTemplate notificationTemplate = orgNotificationTemplateDAO.getNotificationTemplate(locale, templateTypeDisplayName, EMAIL_CHANNEL, getTenantId(tenantDomain));
            return notificationTemplate != null;
        } catch (Exception e) {
            throw new I18nEmailMgtServerException("Error while checking the existence of the template", e);
        }

    }

    @Override
    public List<EmailTemplate> getEmailTemplateType(String templateDisplayName, String tenantDomain)
            throws I18nEmailMgtException {

        log.info("Test org getEmailTemplate()");
        try {
            List<NotificationTemplate> notificationTemplates = orgNotificationTemplateDAO.listNotificationTemplates(templateDisplayName, EMAIL_CHANNEL, getTenantId(tenantDomain));
            return convertToEmailTemplates(notificationTemplates);
        } catch (Exception e) {
            throw new I18nEmailMgtServerException("Error while listing the templates", e);
        }
    }

    @Override
    public void deleteEmailTemplate(String templateTypeName, String localeCode, String tenantDomain) throws I18nEmailMgtException {

        log.info("Test org deleteEmailTemplate()");
        try {
            orgNotificationTemplateDAO.removeNotificationTemplate(localeCode, templateTypeName, EMAIL_CHANNEL, getTenantId(tenantDomain));
        } catch (Exception e) {
            throw new I18nEmailMgtServerException("Error while deleting the template", e);
        }
    }

    @Override
    public void deleteEmailTemplates(String templateTypeName, String tenantDomain) throws I18nEmailMgtException {

        log.info("Test org deleteEmailTemplates()");
        try {
            orgNotificationTemplateDAO.removeNotificationTemplates(templateTypeName, EMAIL_CHANNEL, getTenantId(tenantDomain));
        } catch (Exception e) {
            throw new I18nEmailMgtServerException("Error while deleting the template", e);
        }
    }

    @Override
    public void addEmailTemplate(EmailTemplate emailTemplate, String tenantDomain, String applicationUuid) throws I18nEmailMgtException {

        log.info("Test app addEmailTemplate()");
        try {
            appNotificationTemplateDAO.addNotificationTemplate(convertToNotificationTemplate(emailTemplate), EMAIL_CHANNEL, applicationUuid, getTenantId(tenantDomain));
        } catch (Exception e) {
            throw new I18nEmailMgtServerException("Error while adding the app template", e);
        }
    }

    @Override
    public EmailTemplate getEmailTemplate(String templateType, String locale, String tenantDomain, String applicationUuid) throws I18nEmailMgtException {

        log.info("Test app getEmailTemplate()");
        try {
            NotificationTemplate notificationTemplate = appNotificationTemplateDAO.getNotificationTemplate(locale, templateType, EMAIL_CHANNEL, applicationUuid, getTenantId(tenantDomain));
            return convertToEmailTemplate(notificationTemplate);
        } catch (Exception e) {
            throw new I18nEmailMgtServerException("Error while retrieving the app template", e);
        }
    }

    @Override
    public boolean isEmailTemplateExists(String templateTypeDisplayName, String locale, String tenantDomain, String applicationUuid) throws I18nEmailMgtException {

        log.info("Test app isEmailTemplateExists()");
        try {
            NotificationTemplate notificationTemplate = appNotificationTemplateDAO.getNotificationTemplate(locale, templateTypeDisplayName, EMAIL_CHANNEL, applicationUuid, getTenantId(tenantDomain));
            return notificationTemplate != null;
        } catch (Exception e) {
            throw new I18nEmailMgtServerException("Error while checking the existence of the app template", e);
        }
    }

    @Override
    public List<EmailTemplate> getEmailTemplateType(String templateDisplayName, String tenantDomain, String applicationUuid) throws I18nEmailMgtException {

        if (StringUtils.isBlank(applicationUuid)) {
            return getEmailTemplateType(templateDisplayName, tenantDomain);
        }

        log.info("Test app getEmailTemplateType() -> ie listEmailTemplates()");
        try {
            List<NotificationTemplate> notificationTemplates = appNotificationTemplateDAO.listNotificationTemplates(templateDisplayName, EMAIL_CHANNEL, applicationUuid, getTenantId(tenantDomain));
            return convertToEmailTemplates(notificationTemplates);
        } catch (Exception e) {
            throw new I18nEmailMgtServerException("Error while listing the templates", e);
        }
    }

    @Override
    public void deleteEmailTemplate(String templateTypeName, String localeCode, String tenantDomain, String applicationUuid) throws I18nEmailMgtException {

        log.info("Test app deleteEmailTemplates()");
        try {
            appNotificationTemplateDAO.removeNotificationTemplate(localeCode, templateTypeName, EMAIL_CHANNEL, applicationUuid, getTenantId(tenantDomain));
        } catch (Exception e) {
            throw new I18nEmailMgtServerException("Error while deleting the app templates", e);
        }
    }

    @Override
    public void deleteEmailTemplates(String templateTypeName, String tenantDomain, String applicationUuid) throws I18nEmailMgtException {

        log.info("Test app deleteEmailTemplates()");
        try {
            appNotificationTemplateDAO.removeNotificationTemplates(templateTypeName, EMAIL_CHANNEL, applicationUuid, getTenantId(tenantDomain));
        } catch (Exception e) {
            throw new I18nEmailMgtServerException("Error while deleting the app templates", e);
        }
    }

    @Override
    public List<EmailTemplate> getAllEmailTemplates(String tenantDomain) throws I18nEmailMgtException {
        return getDefaultEmailTemplates();
    }

    @Override
    public void addDefaultEmailTemplates(String tenantDomain) throws I18nEmailMgtException {

        getDefaultEmailTemplates().forEach(emailTemplate -> {
            try {
                if (!isEmailTemplateTypeExists(emailTemplate.getTemplateType(),tenantDomain)) {
                    addEmailTemplateType(emailTemplate.getTemplateType(), tenantDomain);
                }
                addEmailTemplate(emailTemplate, tenantDomain);
            } catch (I18nEmailMgtException e) {
                log.error("Error while adding default email templates for the tenant : " + tenantDomain, e);
            }
        });
    }
}