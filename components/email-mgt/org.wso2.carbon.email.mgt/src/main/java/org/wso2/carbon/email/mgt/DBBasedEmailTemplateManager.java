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
import org.wso2.carbon.email.mgt.constants.I18nMgtConstants;
import org.wso2.carbon.email.mgt.dao.AppNotificationTemplateDAO;
import org.wso2.carbon.email.mgt.dao.NotificationScenarioDAO;
import org.wso2.carbon.email.mgt.dao.OrgNotificationTemplateDAO;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtClientException;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtException;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtInternalException;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtServerException;
import org.wso2.carbon.email.mgt.model.EmailTemplate;
import org.wso2.carbon.email.mgt.util.I18nEmailUtil;
import org.wso2.carbon.identity.governance.IdentityMgtConstants;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerClientException;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerServerException;
import org.wso2.carbon.identity.governance.model.NotificationTemplate;
import org.wso2.carbon.identity.governance.service.notification.NotificationChannels;

import java.util.List;

import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.DEFAULT_EMAIL_LOCALE;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.ErrorCodes.EMAIL_TEMPLATE_TYPE_NOT_FOUND;
import static org.wso2.carbon.email.mgt.util.I18nEmailUtil.buildEmailTemplate;
import static org.wso2.carbon.email.mgt.util.I18nEmailUtil.buildNotificationTemplateFromEmailTemplate;
import static org.wso2.carbon.email.mgt.util.I18nEmailUtil.convertToEmailTemplates;
import static org.wso2.carbon.email.mgt.util.I18nEmailUtil.getDefaultEmailTemplates;
import static org.wso2.carbon.email.mgt.util.I18nEmailUtil.getDefaultNotificationLocale;
import static org.wso2.carbon.email.mgt.util.I18nEmailUtil.getTenantId;
import static org.wso2.carbon.email.mgt.util.I18nEmailUtil.validateDisplayNameOfTemplateType;
import static org.wso2.carbon.email.mgt.util.I18nEmailUtil.validateNotificationTemplate;
import static org.wso2.carbon.email.mgt.util.I18nEmailUtil.validateTemplateLocale;

/**
 * Provides functionality to manage email template-scenarios & email templates.
 */
public class DBBasedEmailTemplateManager implements EmailTemplateManager {

    private static final Log log = LogFactory.getLog(DBBasedEmailTemplateManager.class);

    private final NotificationScenarioDAO notificationScenarioDAO = new NotificationScenarioDAO();
    private final OrgNotificationTemplateDAO orgNotificationTemplateDAO = new OrgNotificationTemplateDAO();
    private final AppNotificationTemplateDAO appNotificationTemplateDAO = new AppNotificationTemplateDAO();

    private static final String EMAIL_CHANNEL = NotificationChannels.EMAIL_CHANNEL.getChannelType();

    @Override
    public void addEmailTemplateType(String emailTemplateTypeDisplayName, String tenantDomain)
            throws I18nEmailMgtException {

        //TODO: Check this can be moved to the API layer
        validateTemplateTypeDisplayNameWithNormalization(emailTemplateTypeDisplayName);

        if (isEmailTemplateTypeExists(emailTemplateTypeDisplayName, tenantDomain)) {
            String message =
                    String.format(I18nMgtConstants.ErrorMessages.ERROR_CODE_DUPLICATE_TEMPLATE_TYPE.getMessage(),
                            emailTemplateTypeDisplayName, tenantDomain);
            throw new I18nEmailMgtInternalException(I18nMgtConstants.ErrorCodes.EMAIL_TEMPLATE_TYPE_ALREADY_EXISTS,
                    message);
        }

        try {
            notificationScenarioDAO.addNotificationScenario(emailTemplateTypeDisplayName, emailTemplateTypeDisplayName,
                    EMAIL_CHANNEL, getTenantId(tenantDomain));
            if (log.isDebugEnabled()) {
                log.debug(String.format("Email template scenario type: %s for tenant: %s successfully added.",
                        emailTemplateTypeDisplayName, tenantDomain));
            }
        } catch (NotificationTemplateManagerServerException e) {
            throw new I18nEmailMgtServerException(e.getMessage(), e);
        }
    }

    @Override
    public boolean isEmailTemplateTypeExists(String templateTypeDisplayName, String tenantDomain)
            throws I18nEmailMgtException {

        //TODO: Check this can be moved to the API layer
        validateTemplateTypeDisplayName(templateTypeDisplayName);

        try {
            String scenarioName =
                    notificationScenarioDAO.getNotificationScenario(templateTypeDisplayName, EMAIL_CHANNEL,
                            getTenantId(tenantDomain));
            boolean isEmailTemplateTypeExists = StringUtils.isNotBlank(scenarioName);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Email template scenario type: %s for tenant: %s is exists: %s.",
                        templateTypeDisplayName, tenantDomain, isEmailTemplateTypeExists));
            }
            return isEmailTemplateTypeExists;
        } catch (NotificationTemplateManagerServerException e) {
            throw new I18nEmailMgtServerException(e.getMessage(), e);
        }
    }

    @Override
    public List<String> getAvailableTemplateTypes(String tenantDomain) throws I18nEmailMgtException {

        try {
            List<String> templateTypesDisplayNames =
                    notificationScenarioDAO.listNotificationScenarios(EMAIL_CHANNEL, getTenantId(tenantDomain));
            if (log.isDebugEnabled()) {
                log.debug(String.format("Email template scenario types for tenant: %s successfully listed.",
                        tenantDomain));
            }
            return templateTypesDisplayNames;
        } catch (NotificationTemplateManagerServerException e) {
            throw new I18nEmailMgtServerException(e.getMessage(), e);
        }
    }

    @Override
    public void deleteEmailTemplateType(String templateDisplayName, String tenantDomain) throws I18nEmailMgtException {

        //TODO: Check this can be moved to the API layer
        validateTemplateTypeDisplayNameWithNormalization(templateDisplayName);

        try {
            notificationScenarioDAO.removeNotificationScenario(templateDisplayName, EMAIL_CHANNEL,
                    getTenantId(tenantDomain));
            if (log.isDebugEnabled()) {
                log.debug(String.format("Email template scenario type: %s for tenant: %s successfully deleted.",
                        templateDisplayName, tenantDomain));
            }
        } catch (NotificationTemplateManagerServerException e) {
            throw new I18nEmailMgtServerException(e.getMessage(), e);
        }
    }

    @Override
    public void addEmailTemplate(EmailTemplate emailTemplate, String tenantDomain) throws I18nEmailMgtException {

        NotificationTemplate notificationTemplate = buildNotificationTemplateFromEmailTemplate(emailTemplate);

        //TODO: Check this can be moved to the API layer
        validateNotificationTemplateAndHandleErrors(notificationTemplate);

        // Registry impl creates template type if not exists
        if (!isEmailTemplateTypeExists(emailTemplate.getTemplateDisplayName(), tenantDomain)) {
            addEmailTemplateType(emailTemplate.getTemplateDisplayName(), tenantDomain);
        }

        try {
            if (isEmailTemplateExists(emailTemplate.getTemplateDisplayName(), emailTemplate.getLocale(),
                    tenantDomain)) {
                // Registry impl updates the template if exists
                orgNotificationTemplateDAO.updateNotificationTemplate(notificationTemplate, EMAIL_CHANNEL,
                        getTenantId(tenantDomain));
                if (log.isDebugEnabled()) {
                    log.debug(String.format(
                            "Org email template with locale: %s for scenario: %s for tenant: %s successfully updated.",
                            emailTemplate.getLocale(), emailTemplate.getTemplateDisplayName(), tenantDomain));
                }
            } else {
                orgNotificationTemplateDAO.addNotificationTemplate(notificationTemplate, EMAIL_CHANNEL,
                        getTenantId(tenantDomain));
                if (log.isDebugEnabled()) {
                    log.debug(String.format(
                            "Org email template with locale: %s for scenario: %s for tenant: %s successfully added.",
                            emailTemplate.getLocale(), emailTemplate.getTemplateDisplayName(), tenantDomain));
                }
            }
        } catch (NotificationTemplateManagerServerException e) {
            throw new I18nEmailMgtServerException(e.getMessage(), e);
        }
    }

    @Override
    public EmailTemplate getEmailTemplate(String templateTypeDisplayName, String locale, String tenantDomain)
            throws I18nEmailMgtException {

        validateLocaleAndTemplateTypeDisplayName(templateTypeDisplayName, locale);

        NotificationTemplate notificationTemplate;
        try {
            notificationTemplate =
                    orgNotificationTemplateDAO.getNotificationTemplate(locale, templateTypeDisplayName, EMAIL_CHANNEL,
                            getTenantId(tenantDomain));
        } catch (NotificationTemplateManagerServerException e) {
            throw new I18nEmailMgtServerException(e.getMessage(), e);
        }

        // TODO: Check why its only SMS here in registry impl
        // Handle not having the requested SMS template type in required locale for this tenantDomain.
        if (notificationTemplate == null) {
            return getDefaultTemplate(templateTypeDisplayName, locale, tenantDomain);
        }

        // TODO: Remove following as its not needed when retrieving, but registry impl has it
        validateContent(templateTypeDisplayName, locale, notificationTemplate);

        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "Org email template with locale: %s for scenario: %s for tenant: %s successfully retrieved.",
                    locale, templateTypeDisplayName, tenantDomain));
        }
        return buildEmailTemplate(notificationTemplate);
    }

    @Override
    public boolean isEmailTemplateExists(String templateTypeDisplayName, String locale, String tenantDomain)
            throws I18nEmailMgtException {

        //TODO: Check this can be moved to the API layer
        validateTemplateTypeDisplayName(templateTypeDisplayName);

        try {
            NotificationTemplate notificationTemplate =
                    orgNotificationTemplateDAO.getNotificationTemplate(locale, templateTypeDisplayName, EMAIL_CHANNEL,
                            getTenantId(tenantDomain));
            boolean isEmailTemplateExists = notificationTemplate != null;
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Org email template with locale: %s for scenario: %s for tenant: %s is exists: %s.", locale,
                        templateTypeDisplayName, tenantDomain, isEmailTemplateExists));
            }
            return isEmailTemplateExists;
        } catch (NotificationTemplateManagerServerException e) {
            throw new I18nEmailMgtServerException(e.getMessage(), e);
        }
    }

    @Override
    public List<EmailTemplate> getEmailTemplateType(String templateDisplayName, String tenantDomain)
            throws I18nEmailMgtException {

        //TODO: Check this can be moved to the API layer
        validateTemplateTypeDisplayNameWithNormalization(templateDisplayName);

        if (isEmailTemplateTypeExists(templateDisplayName, tenantDomain)) {
            try {
                List<NotificationTemplate> notificationTemplates =
                        orgNotificationTemplateDAO.listNotificationTemplates(templateDisplayName, EMAIL_CHANNEL,
                                getTenantId(tenantDomain));
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Org email templates for scenario: %s for tenant: %s successfully listed.",
                            templateDisplayName, tenantDomain));
                }
                return convertToEmailTemplates(notificationTemplates);
            } catch (NotificationTemplateManagerServerException e) {
                throw new I18nEmailMgtServerException(e.getMessage(), e);
            }
        } else {
            String message =
                    String.format("Email Template Type: %s not found in %s tenant registry.", templateDisplayName,
                            tenantDomain);
            throw new I18nEmailMgtClientException(EMAIL_TEMPLATE_TYPE_NOT_FOUND, message);
        }
    }

    @Override
    public void deleteEmailTemplate(String templateTypeDisplayName, String locale, String tenantDomain)
            throws I18nEmailMgtException {

        //TODO: Check this can be moved to the API layer
        // Validate the name and locale code.
        validateNameAndLocaleWithNormalized(templateTypeDisplayName, locale);

        try {
            orgNotificationTemplateDAO.removeNotificationTemplate(locale, templateTypeDisplayName, EMAIL_CHANNEL,
                    getTenantId(tenantDomain));
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Org email template with locale: %s for scenario: %s for tenant: %s successfully deleted.",
                        locale, templateTypeDisplayName, tenantDomain));
            }
        } catch (NotificationTemplateManagerServerException e) {
            throw new I18nEmailMgtServerException(e.getMessage(), e);
        }
    }

    @Override
    public void deleteEmailTemplates(String templateDisplayName, String tenantDomain) throws I18nEmailMgtException {

        //TODO: Check this can be moved to the API layer
        validateTemplateTypeDisplayNameWithNormalization(templateDisplayName);

        try {
            orgNotificationTemplateDAO.removeNotificationTemplates(templateDisplayName, EMAIL_CHANNEL,
                    getTenantId(tenantDomain));
            if (log.isDebugEnabled()) {
                log.debug(String.format("Org email templates for scenario: %s for tenant: %s successfully deleted.",
                        templateDisplayName, tenantDomain));
            }
        } catch (NotificationTemplateManagerServerException e) {
            throw new I18nEmailMgtServerException(e.getMessage(), e);
        }
    }

    @Override
    public void addEmailTemplate(EmailTemplate emailTemplate, String tenantDomain, String applicationUuid)
            throws I18nEmailMgtException {

        NotificationTemplate notificationTemplate = buildNotificationTemplateFromEmailTemplate(emailTemplate);

        //TODO: Check this can be moved to the API layer
        validateNotificationTemplateAndHandleErrors(notificationTemplate);

        // Registry impl creates template type if not exists
        if (!isEmailTemplateTypeExists(emailTemplate.getTemplateDisplayName(), tenantDomain)) {
            addEmailTemplateType(emailTemplate.getTemplateDisplayName(), tenantDomain);
        }

        try {
            if (isEmailTemplateExists(emailTemplate.getTemplateDisplayName(), emailTemplate.getLocale(), tenantDomain,
                    applicationUuid)) {
                // Registry impl updates the template if exists
                appNotificationTemplateDAO.updateNotificationTemplate(notificationTemplate, EMAIL_CHANNEL,
                        applicationUuid, getTenantId(tenantDomain));
                if (log.isDebugEnabled()) {
                    log.debug(String.format(
                            "App email template with locale: %s for scenario: %s for application: %s tenant: %s " +
                                    "successfully updated.",
                            emailTemplate.getLocale(), emailTemplate.getTemplateDisplayName(), applicationUuid,
                            tenantDomain));
                }
            } else {
                appNotificationTemplateDAO.addNotificationTemplate(notificationTemplate, EMAIL_CHANNEL, applicationUuid,
                        getTenantId(tenantDomain));
                if (log.isDebugEnabled()) {
                    log.debug(String.format(
                            "App email template with locale: %s for scenario: %s for application: %s tenant: %s " +
                                    "successfully added.",
                            emailTemplate.getLocale(), emailTemplate.getTemplateDisplayName(), applicationUuid,
                            tenantDomain));
                }
            }
        } catch (NotificationTemplateManagerServerException e) {
            throw new I18nEmailMgtServerException(e.getMessage(), e);
        }
    }

    @Override
    public EmailTemplate getEmailTemplate(String templateTypeDisplayName, String locale, String tenantDomain,
                                          String applicationUuid) throws I18nEmailMgtException {

        validateLocaleAndTemplateTypeDisplayName(templateTypeDisplayName, locale);

        NotificationTemplate notificationTemplate;
        try {
            notificationTemplate =
                    appNotificationTemplateDAO.getNotificationTemplate(locale, templateTypeDisplayName, EMAIL_CHANNEL,
                            applicationUuid, getTenantId(tenantDomain));
        } catch (NotificationTemplateManagerServerException e) {
            throw new I18nEmailMgtServerException(e.getMessage(), e);
        }

        // TODO: Check why its only SMS here in registry impl
        // Handle not having the requested SMS template type in required locale for this tenantDomain.
        if (notificationTemplate == null) {
            return getDefaultTemplate(templateTypeDisplayName, locale, tenantDomain);
        }

        // TODO: Remove following as its not needed when retrieving, but registry impl has it
        validateContent(templateTypeDisplayName, locale, notificationTemplate);

        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "App email template with locale: %s for scenario: %s for application: %s tenant: %s " +
                            "successfully retrieved.",
                    locale, templateTypeDisplayName, applicationUuid, tenantDomain));
        }
        return buildEmailTemplate(notificationTemplate);
    }

    @Override
    public boolean isEmailTemplateExists(String templateTypeDisplayName, String locale, String tenantDomain,
                                         String applicationUuid) throws I18nEmailMgtException {

        //TODO: Check this can be moved to the API layer
        validateTemplateTypeDisplayName(templateTypeDisplayName);

        try {
            NotificationTemplate notificationTemplate =
                    appNotificationTemplateDAO.getNotificationTemplate(locale, templateTypeDisplayName, EMAIL_CHANNEL,
                            applicationUuid, getTenantId(tenantDomain));
            boolean isEmailTemplateExists = notificationTemplate != null;
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "App email template with locale: %s for scenario: %s for application: %s tenant: %s " +
                                "is exists: %s",
                        locale, templateTypeDisplayName, applicationUuid, tenantDomain, isEmailTemplateExists));
            }
            return isEmailTemplateExists;
        } catch (NotificationTemplateManagerServerException e) {
            throw new I18nEmailMgtServerException(e.getMessage(), e);
        }
    }

    @Override
    public List<EmailTemplate> getEmailTemplateType(String templateDisplayName, String tenantDomain,
                                                    String applicationUuid) throws I18nEmailMgtException {

        if (StringUtils.isBlank(applicationUuid)) {
            return getEmailTemplateType(templateDisplayName, tenantDomain);
        }

        //TODO: Check this can be moved to the API layer
        validateTemplateTypeDisplayNameWithNormalization(templateDisplayName);

        if (isEmailTemplateTypeExists(templateDisplayName, tenantDomain)) {
            try {
                List<NotificationTemplate> notificationTemplates =
                        appNotificationTemplateDAO.listNotificationTemplates(templateDisplayName, EMAIL_CHANNEL,
                                applicationUuid, getTenantId(tenantDomain));
                if (log.isDebugEnabled()) {
                    log.debug(String.format(
                            "App email templates for scenario: %s for application: %s tenant: %s successfully listed.",
                            templateDisplayName, applicationUuid, tenantDomain));
                }
                return convertToEmailTemplates(notificationTemplates);
            } catch (NotificationTemplateManagerServerException e) {
                throw new I18nEmailMgtServerException(e.getMessage(), e);
            }
        } else {
            String message =
                    String.format("Email Template Type: %s not found in %s tenant registry.", templateDisplayName,
                            tenantDomain);
            throw new I18nEmailMgtClientException(EMAIL_TEMPLATE_TYPE_NOT_FOUND, message);
        }
    }

    @Override
    public void deleteEmailTemplate(String templateTypeDisplayName, String locale, String tenantDomain,
                                    String applicationUuid) throws I18nEmailMgtException {

        //TODO: Check this can be moved to the API layer
        // Validate the name and locale code.
        validateNameAndLocaleWithNormalized(templateTypeDisplayName, locale);

        try {
            appNotificationTemplateDAO.removeNotificationTemplate(locale, templateTypeDisplayName, EMAIL_CHANNEL,
                    applicationUuid, getTenantId(tenantDomain));
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "App email template with locale: %s for scenario: %s for application: %s tenant: %s " +
                                "successfully deleted.",
                        locale, templateTypeDisplayName, applicationUuid, tenantDomain));
            }
        } catch (NotificationTemplateManagerServerException e) {
            throw new I18nEmailMgtServerException(e.getMessage(), e);
        }
    }

    @Override
    public void deleteEmailTemplates(String templateDisplayName, String tenantDomain, String applicationUuid)
            throws I18nEmailMgtException {

        //TODO: Check this can be moved to the API layer
        validateTemplateTypeDisplayNameWithNormalization(templateDisplayName);

        try {
            appNotificationTemplateDAO.removeNotificationTemplates(templateDisplayName, EMAIL_CHANNEL, applicationUuid,
                    getTenantId(tenantDomain));
            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "App email templates for scenario: %s for application: %s tenant: %s successfully deleted.",
                        templateDisplayName, applicationUuid, tenantDomain));
            }
        } catch (NotificationTemplateManagerServerException e) {
            throw new I18nEmailMgtServerException(e.getMessage(), e);
        }
    }

    @Override
    public List<EmailTemplate> getAllEmailTemplates(String tenantDomain) throws I18nEmailMgtException {

        if (log.isDebugEnabled()) {
            log.debug(String.format("Default email templates for tenant: %s successfully listed.", tenantDomain));
        }
        return getDefaultEmailTemplates();
    }

    @Override
    public void addDefaultEmailTemplates(String tenantDomain) throws I18nEmailMgtException {

        int numberOfAddedTemplates = 0;

        for (EmailTemplate emailTemplate : getDefaultEmailTemplates()) {
            if (!isEmailTemplateTypeExists(emailTemplate.getTemplateDisplayName(), tenantDomain)) {
                addEmailTemplateType(emailTemplate.getTemplateDisplayName(), tenantDomain);
            }

            // From registry impl:
            // Check for existence of each category, since some template may have migrated from earlier version.
            // This will also add new template types provided from file, but won't update any existing template.
            if (!isEmailTemplateExists(emailTemplate.getTemplateDisplayName(), emailTemplate.getLocale(),
                    tenantDomain)) {
                addEmailTemplate(emailTemplate, tenantDomain);
                if (log.isDebugEnabled()) {
                    log.debug(String.format(
                            "Default email template with locale: %s for scenario: %s for tenant: %s successfully added.",
                            emailTemplate.getLocale(), emailTemplate.getTemplateDisplayName(), tenantDomain));
                }
                numberOfAddedTemplates++;
            }
        }

        if (log.isDebugEnabled()) {
            log.debug(String.format("Added %d default email templates to the tenant: %s", numberOfAddedTemplates,
                    tenantDomain));
        }
    }

    private static void validateNotificationTemplateAndHandleErrors(NotificationTemplate notificationTemplate)
            throws I18nEmailMgtClientException {

        try {
            validateNotificationTemplate(notificationTemplate);
            I18nEmailUtil.getNormalizedName(notificationTemplate.getDisplayName());
        } catch (NotificationTemplateManagerClientException e) {
            throw new I18nEmailMgtClientException(e.getMessage(), e);
        }
    }

    private static void validateLocaleAndTemplateTypeDisplayName(String templateTypeDisplayName, String locale)
            throws I18nEmailMgtClientException {

        try {
            validateTemplateLocale(locale);
            validateDisplayNameOfTemplateType(templateTypeDisplayName);
        } catch (NotificationTemplateManagerClientException e) {
            //TODO: ERROR_CODE_EMPTY_LOCALE & ERROR_CODE_EMPTY_TEMPLATE_NAME error codes are not handled in registry impl
            throw new I18nEmailMgtClientException(e.getMessage(), e);
        }
    }

    private EmailTemplate getDefaultTemplate(String templateTypeDisplayName, String locale, String tenantDomain)
            throws I18nEmailMgtException {

        String defaultLocale = getDefaultNotificationLocale(EMAIL_CHANNEL);
        if (StringUtils.equalsIgnoreCase(defaultLocale, locale)) {
            // Template is not available in the default locale. Therefore, breaking the flow at the consuming side to avoid NPE.
            String error = String.format(IdentityMgtConstants.ErrorMessages.ERROR_CODE_NO_TEMPLATE_FOUND.getMessage(),
                    templateTypeDisplayName, locale, tenantDomain);
            throw new I18nEmailMgtInternalException(I18nMgtConstants.ErrorCodes.EMAIL_TEMPLATE_TYPE_NODE_FOUND, error);
        } else {
            if (log.isDebugEnabled()) {
                String message = String.format(
                        "'%s' template in '%s' locale was not found in '%s' tenant. Trying to return the template in default locale : '%s'",
                        templateTypeDisplayName, locale, tenantDomain, DEFAULT_EMAIL_LOCALE);
                log.debug(message);
            }
            // Try to get the template type in default locale.
            return getEmailTemplate(templateTypeDisplayName, defaultLocale, tenantDomain);
        }
    }

    private static void validateContent(String templateTypeDisplayName, String locale,
                                        NotificationTemplate notificationTemplate) throws I18nEmailMgtClientException {

        if (StringUtils.isBlank(notificationTemplate.getBody())) {
            String error = String.format(IdentityMgtConstants.ErrorMessages.
                    ERROR_CODE_NO_CONTENT_IN_TEMPLATE.getMessage(), templateTypeDisplayName, locale);
            throw new I18nEmailMgtClientException(IdentityMgtConstants.ErrorMessages.
                    ERROR_CODE_NO_CONTENT_IN_TEMPLATE.getCode(), error);
        }
    }

    private static void validateTemplateTypeDisplayName(String templateTypeDisplayName) {

//        try {
//            EmailTemplateManagerImpl.validateDisplayNameOfTemplateType(templateTypeDisplayName);
//        } catch (NotificationTemplateManagerClientException e) {
//            throw new I18nEmailMgtClientException(e.getMessage(), e);
//        }
        I18nEmailUtil.getNormalizedName(templateTypeDisplayName);
    }

    private static void validateTemplateTypeDisplayNameWithNormalization(String templateDisplayName)
            throws I18nEmailMgtClientException {

        try {
            validateDisplayNameOfTemplateType(templateDisplayName);
            I18nEmailUtil.getNormalizedName(templateDisplayName);
        } catch (NotificationTemplateManagerClientException e) {
            throw new I18nEmailMgtClientException(e.getMessage(), e);
        }
    }

    private static void validateNameAndLocaleWithNormalized(String templateTypeDisplayName, String locale)
            throws I18nEmailMgtClientException {

        if (StringUtils.isBlank(templateTypeDisplayName)) {
            throw new I18nEmailMgtClientException("Cannot Delete template. Email displayName cannot be null.");
        }
        if (StringUtils.isBlank(locale)) {
            throw new I18nEmailMgtClientException("Cannot Delete template. Email locale cannot be null.");
        }
        I18nEmailUtil.getNormalizedName(templateTypeDisplayName);
    }
}