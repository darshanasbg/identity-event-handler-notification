/*
 * Copyright (c) 2016-2024, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.email.mgt;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.email.mgt.constants.I18nMgtConstants;
import org.wso2.carbon.email.mgt.dao.RegistryBasedTemplateManager;
import org.wso2.carbon.email.mgt.dao.TemplatePersistenceManager;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtClientException;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtException;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtInternalException;
import org.wso2.carbon.email.mgt.exceptions.I18nEmailMgtServerException;
import org.wso2.carbon.email.mgt.exceptions.I18nMgtEmailConfigException;
import org.wso2.carbon.email.mgt.internal.I18nMgtDataHolder;
import org.wso2.carbon.email.mgt.model.EmailTemplate;
import org.wso2.carbon.email.mgt.util.I18nEmailUtil;
import org.wso2.carbon.identity.base.IdentityValidationUtil;
import org.wso2.carbon.identity.governance.IdentityMgtConstants;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerClientException;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerException;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerInternalException;
import org.wso2.carbon.identity.governance.exceptions.notiification.NotificationTemplateManagerServerException;
import org.wso2.carbon.identity.governance.model.NotificationTemplate;
import org.wso2.carbon.identity.governance.service.notification.NotificationChannels;
import org.wso2.carbon.identity.governance.service.notification.NotificationTemplateManager;

import java.util.List;

import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.APP_TEMPLATE_PATH;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.EMAIL_TEMPLATE_NAME;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.EMAIL_TEMPLATE_PATH;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.EMAIL_TEMPLATE_TYPE_REGEX;
import static org.wso2.carbon.email.mgt.constants.I18nMgtConstants.ErrorCodes.EMAIL_TEMPLATE_TYPE_NOT_FOUND;
import static org.wso2.carbon.email.mgt.util.I18nEmailUtil.buildEmailTemplate;
import static org.wso2.carbon.email.mgt.util.I18nEmailUtil.buildNotificationTemplateFromEmailTemplate;
import static org.wso2.carbon.email.mgt.util.I18nEmailUtil.getDefaultNotificationLocale;
import static org.wso2.carbon.email.mgt.util.I18nEmailUtil.resolveNotificationChannel;
import static org.wso2.carbon.email.mgt.util.I18nEmailUtil.validateDisplayNameOfTemplateType;
import static org.wso2.carbon.email.mgt.util.I18nEmailUtil.validateNotificationTemplate;
import static org.wso2.carbon.email.mgt.util.I18nEmailUtil.validateTemplateLocale;
import static org.wso2.carbon.identity.base.IdentityValidationUtil.ValidatorPattern.REGISTRY_INVALID_CHARS_EXISTS;
import static org.wso2.carbon.registry.core.RegistryConstants.PATH_SEPARATOR;

/**
 * Provides functionality to manage email templates used in notification emails.
 */
public class EmailTemplateManagerImpl implements EmailTemplateManager, NotificationTemplateManager {

    private I18nMgtDataHolder dataHolder = I18nMgtDataHolder.getInstance();
    private TemplatePersistenceManager notificationTemplateDAO = new RegistryBasedTemplateManager();

    private static final Log log = LogFactory.getLog(EmailTemplateManagerImpl.class);

    private static final String TEMPLATE_REGEX_KEY = I18nMgtConstants.class.getName() + "_" + EMAIL_TEMPLATE_NAME;
    private static final String REGISTRY_INVALID_CHARS = I18nMgtConstants.class.getName() + "_" + "registryInvalidChar";

    static {
        IdentityValidationUtil.addPattern(TEMPLATE_REGEX_KEY, EMAIL_TEMPLATE_TYPE_REGEX);
        IdentityValidationUtil.addPattern(REGISTRY_INVALID_CHARS, REGISTRY_INVALID_CHARS_EXISTS.getRegex());
    }

    @Override
    public void addEmailTemplateType(String emailTemplateDisplayName, String tenantDomain) throws
            I18nEmailMgtException {

        try {
            addNotificationTemplateType(emailTemplateDisplayName, NotificationChannels.EMAIL_CHANNEL.getChannelType(),
                    tenantDomain);
        } catch (NotificationTemplateManagerClientException e) {
            throw new I18nEmailMgtClientException(e.getMessage(), e);
        } catch (NotificationTemplateManagerInternalException e) {
            if (StringUtils.isNotBlank(e.getErrorCode())) {
                String errorCode = e.getErrorCode();
                if (errorCode.contains(I18nMgtConstants.ErrorMessages.ERROR_CODE_DUPLICATE_TEMPLATE_TYPE.getCode())) {
                    throw new I18nEmailMgtInternalException(
                            I18nMgtConstants.ErrorCodes.EMAIL_TEMPLATE_TYPE_ALREADY_EXISTS, e.getMessage(), e);
                }
            }
            throw new I18nEmailMgtInternalException(e.getMessage(), e);
        } catch (NotificationTemplateManagerException e) {
            throw new I18nEmailMgtServerException(e.getMessage(), e);
        }
    }

    @Override
    public void addNotificationTemplateType(String displayName, String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerException {

        addNotificationTemplateType(displayName, notificationChannel, tenantDomain, null);
    }

    @Override
    public void addNotificationTemplateType(String displayName, String notificationChannel,
                                            String tenantDomain, String applicationUuid)
            throws NotificationTemplateManagerException {

        validateDisplayNameOfTemplateType(displayName);

        try {
            if (notificationTemplateDAO.isNotificationTemplateTypeExists(displayName, notificationChannel,
                    applicationUuid, tenantDomain)) {
                String code = I18nEmailUtil.prependOperationScenarioToErrorCode(
                        I18nMgtConstants.ErrorMessages.ERROR_CODE_DUPLICATE_TEMPLATE_TYPE.getCode(),
                        I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);
                String message =
                        String.format(I18nMgtConstants.ErrorMessages.ERROR_CODE_DUPLICATE_TEMPLATE_TYPE.getMessage(),
                                displayName, tenantDomain);
                throw new NotificationTemplateManagerInternalException(code, message);
            }
            notificationTemplateDAO.addNotificationTemplateType(displayName, notificationChannel, applicationUuid,
                    tenantDomain);
        } catch (NotificationTemplateManagerServerException e) {
            String code = I18nEmailUtil.prependOperationScenarioToErrorCode(
                    I18nMgtConstants.ErrorMessages.ERROR_CODE_ERROR_ADDING_TEMPLATE.getCode(),
                    I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);
            String message =
                    String.format(I18nMgtConstants.ErrorMessages.ERROR_CODE_ERROR_ADDING_TEMPLATE.getMessage(),
                            displayName, tenantDomain);
            throw new NotificationTemplateManagerServerException(code, message);
        }
    }

    @Override
    public void deleteEmailTemplateType(String emailTemplateDisplayName, String tenantDomain) throws
            I18nEmailMgtException {

        validateTemplateType(emailTemplateDisplayName, tenantDomain);

        try {
            notificationTemplateDAO.deleteNotificationTemplateTypeByName(emailTemplateDisplayName,
                    NotificationChannels.EMAIL_CHANNEL.getChannelType(), tenantDomain);
        } catch (NotificationTemplateManagerException ex) {
            String errorMsg = String.format
                    ("Error deleting email template type %s from %s tenant.", emailTemplateDisplayName, tenantDomain);
            handleServerException(errorMsg, ex);
        }
    }

    /**
     * @param tenantDomain
     * @return
     * @throws I18nEmailMgtServerException
     */
    @Override
    public List<String> getAvailableTemplateTypes(String tenantDomain) throws I18nEmailMgtServerException {

        try {
            return notificationTemplateDAO.getNotificationTemplateTypes(NotificationChannels.EMAIL_CHANNEL.getChannelType(),
                    tenantDomain);
        } catch (NotificationTemplateManagerServerException ex) {
            String errorMsg = String.format("Error when retrieving email template types of %s tenant.", tenantDomain);
            throw new I18nEmailMgtServerException(errorMsg, ex);
        }
    }

    @Override
    public List<EmailTemplate> getAllEmailTemplates(String tenantDomain) throws I18nEmailMgtException {

        try {
            return notificationTemplateDAO.getAllEmailTemplates(tenantDomain);
        } catch (NotificationTemplateManagerServerException e) {
            String error = String.format("Error when retrieving email templates of %s tenant.", tenantDomain);
            throw new I18nEmailMgtServerException(error, e);
        }
    }

    @Override
    public EmailTemplate getEmailTemplate(String templateDisplayName, String locale, String tenantDomain)
            throws I18nEmailMgtException {

        return getEmailTemplate(templateDisplayName, locale, tenantDomain, null);
    }

    @Override
    public List<EmailTemplate> getEmailTemplateType(String templateDisplayName, String tenantDomain)
            throws I18nEmailMgtException {

        return getEmailTemplateType(templateDisplayName, tenantDomain, null);
    }

    @Override
    public List<EmailTemplate> getEmailTemplateType(
            String templateDisplayName, String tenantDomain, String applicationUuid) throws I18nEmailMgtException {

        validateTemplateType(templateDisplayName, tenantDomain);

        try {
            if (!notificationTemplateDAO.isNotificationTemplateTypeExists(templateDisplayName,NotificationChannels.EMAIL_CHANNEL.getChannelType(), null, tenantDomain)) {
                String message =
                        String.format("Email Template Type: %s not found in %s tenant registry.", templateDisplayName,
                                tenantDomain);
                throw new I18nEmailMgtClientException(EMAIL_TEMPLATE_TYPE_NOT_FOUND, message);
            }
            return notificationTemplateDAO.getEmailTemplates(templateDisplayName, applicationUuid, tenantDomain);
        } catch (NotificationTemplateManagerServerException ex) {
            String error = "Error when retrieving '%s' template type from %s tenant registry.";
            throw new I18nEmailMgtServerException(String.format(error, templateDisplayName, tenantDomain), ex);
        }
    }

    @Override
    public NotificationTemplate getNotificationTemplate(String notificationChannel, String templateType, String locale,
            String tenantDomain) throws NotificationTemplateManagerException {

        return getNotificationTemplate(notificationChannel, templateType, locale, tenantDomain, null);
    }

    @Override
    public NotificationTemplate getNotificationTemplate(String notificationChannel, String templateType, String locale,
            String tenantDomain, String applicationUuid) throws NotificationTemplateManagerException {

        // Resolve channel to either SMS or EMAIL.
        notificationChannel = resolveNotificationChannel(notificationChannel);
        validateTemplateLocale(locale);
        validateDisplayNameOfTemplateType(templateType);
        NotificationTemplate notificationTemplate = notificationTemplateDAO.getNotificationTemplate(templateType,
                locale, notificationChannel, tenantDomain, applicationUuid);

        // Handle not having the requested SMS template type in required locale for this tenantDomain.
        if (notificationTemplate == null) {
            String defaultLocale = getDefaultNotificationLocale(notificationChannel);
            if (StringUtils.equalsIgnoreCase(defaultLocale, locale)) {

                // Template is not available in the default locale. Therefore, breaking the flow at the consuming side
                // to avoid NPE.
                String error = String
                        .format(IdentityMgtConstants.ErrorMessages.ERROR_CODE_NO_TEMPLATE_FOUND.getMessage(),
                                templateType, locale, tenantDomain);
                throw new NotificationTemplateManagerServerException(
                        IdentityMgtConstants.ErrorMessages.ERROR_CODE_NO_TEMPLATE_FOUND.getCode(), error);
            } else {
                if (log.isDebugEnabled()) {
                    String message = String
                            .format("'%s' template in '%s' locale was not found in '%s' tenant. Trying to return the "
                                            + "template in default locale : '%s'", templateType, locale, tenantDomain,
                                    defaultLocale);
                    log.debug(message);
                }
                // Try to get the template type in default locale.
                return getNotificationTemplate(notificationChannel, templateType, defaultLocale, tenantDomain);
            }
        }
        return notificationTemplate;
    }

    @Override
    public void addNotificationTemplate(NotificationTemplate notificationTemplate, String tenantDomain)
            throws NotificationTemplateManagerException {

        addNotificationTemplate(notificationTemplate, tenantDomain, null);
    }

    @Override
    public void addNotificationTemplate(NotificationTemplate notificationTemplate,
                                        String tenantDomain, String applicationUuid)
            throws NotificationTemplateManagerException {

        validateNotificationTemplate(notificationTemplate);

        String displayName = notificationTemplate.getDisplayName();
        String locale = notificationTemplate.getLocale();

        try {
            notificationTemplateDAO.addOrUpdateNotificationTemplate(notificationTemplate, applicationUuid,
                    tenantDomain);
        } catch (NotificationTemplateManagerServerException e) {
            String code = I18nEmailUtil.prependOperationScenarioToErrorCode(
                    I18nMgtConstants.ErrorMessages.ERROR_CODE_ERROR_ERROR_ADDING_TEMPLATE.getCode(),
                    I18nMgtConstants.ErrorScenarios.EMAIL_TEMPLATE_MANAGER);
            String message =
                    String.format(I18nMgtConstants.ErrorMessages.ERROR_CODE_ERROR_ERROR_ADDING_TEMPLATE.getMessage(),
                            displayName, locale, tenantDomain);
            throw new NotificationTemplateManagerServerException(code, message);
        }
    }

    @Override
    public void addEmailTemplate(EmailTemplate emailTemplate, String tenantDomain) throws I18nEmailMgtException {

        addEmailTemplate(emailTemplate, tenantDomain, null);
    }


    @Override
    public void deleteEmailTemplate(String templateTypeName, String localeCode, String tenantDomain) throws
            I18nEmailMgtException {

        deleteEmailTemplate(templateTypeName, localeCode, tenantDomain, null);
    }

    @Override
    public void deleteEmailTemplates(String templateTypeName, String tenantDomain) throws I18nEmailMgtException {

        validateTemplateType(templateTypeName, tenantDomain);

        try {
            notificationTemplateDAO.deleteNotificationTemplates(templateTypeName,
                    NotificationChannels.EMAIL_CHANNEL.getChannelType(), tenantDomain);
        } catch (NotificationTemplateManagerServerException ex) {
            String errorMsg = String.format
                    ("Error deleting email template type %s from %s tenant.", templateTypeName, tenantDomain);
            handleServerException(errorMsg, ex);
        }
    }

    @Override
    public void deleteEmailTemplates(String templateTypeName, String tenantDomain, String applicationUuid)
            throws I18nEmailMgtException {

        validateTemplateType(templateTypeName, tenantDomain);

        String templateType = I18nEmailUtil.getNormalizedName(templateTypeName);
        String path = EMAIL_TEMPLATE_PATH + PATH_SEPARATOR + templateType + APP_TEMPLATE_PATH +
                PATH_SEPARATOR + applicationUuid;

        try {
            notificationTemplateDAO.deleteNotificationTemplates(templateTypeName,
                    NotificationChannels.EMAIL_CHANNEL.getChannelType(), applicationUuid, tenantDomain);
        } catch (NotificationTemplateManagerServerException ex) {
            String errorMsg = String.format("Error deleting email template type %s from %s tenant for application %s.",
                    templateType, tenantDomain, applicationUuid);
            handleServerException(errorMsg, ex);
        }
    }

    @Override
    public void addDefaultEmailTemplates(String tenantDomain) throws I18nEmailMgtException {

        try {
            addDefaultNotificationTemplates(NotificationChannels.EMAIL_CHANNEL.getChannelType(), tenantDomain);
        } catch (NotificationTemplateManagerClientException e) {
            throw new I18nEmailMgtClientException(e.getMessage(), e);
        } catch (NotificationTemplateManagerInternalException e) {
            if (StringUtils.isNotBlank(e.getErrorCode())) {
                String errorCode = e.getErrorCode();
                if (errorCode.contains(I18nMgtConstants.ErrorMessages.ERROR_CODE_DUPLICATE_TEMPLATE_TYPE.getCode())) {
                    throw new I18nEmailMgtInternalException(
                            I18nMgtConstants.ErrorCodes.EMAIL_TEMPLATE_TYPE_ALREADY_EXISTS, e.getMessage(), e);
                }
            }
            throw new I18nEmailMgtInternalException(e.getMessage(), e);
        } catch (NotificationTemplateManagerException e) {
            throw new I18nEmailMgtServerException(e.getMessage(), e);
        }
    }

    /**
     * Add the default notification templates which matches the given notification channel to the respective tenants
     * registry.
     *
     * @param notificationChannel Notification channel (Eg: SMS, EMAIL)
     * @param tenantDomain Tenant domain
     * @throws NotificationTemplateManagerException Error adding the default notification templates
     */
    @Override
    public void addDefaultNotificationTemplates(String notificationChannel, String tenantDomain)
            throws NotificationTemplateManagerException {

        // Get the list of Default notification templates.
        List<NotificationTemplate> notificationTemplates =
                getDefaultNotificationTemplates(notificationChannel);
        int numberOfAddedTemplates = 0;
        try {
            for (NotificationTemplate template : notificationTemplates) {
                String displayName = template.getDisplayName();
                String locale = template.getLocale();

            /*Check for existence of each category, since some template may have migrated from earlier version
            This will also add new template types provided from file, but won't update any existing template*/
                if (!notificationTemplateDAO.isNotificationTemplateExists(displayName, locale, notificationChannel,
                        null, tenantDomain)) {
                    try {
                        addNotificationTemplate(template, tenantDomain);
                        if (log.isDebugEnabled()) {
                            String msg = "Default template added to %s tenant registry : %n%s";
                            log.debug(String.format(msg, tenantDomain, template.toString()));
                        }
                        numberOfAddedTemplates++;
                    } catch (NotificationTemplateManagerInternalException e) {
                        log.warn("Template : " + displayName + "already exists in the registry. Hence " +
                                "ignoring addition");
                    }
                }
            }
            if (log.isDebugEnabled()) {
                log.debug(String.format("Added %d default %s templates to the tenant registry : %s",
                        numberOfAddedTemplates, notificationChannel, tenantDomain));
            }
        } catch (NotificationTemplateManagerServerException ex) {
            String error = "Error when tried to check for default email templates in tenant registry : %s";
            log.error(String.format(error, tenantDomain), ex);
        }
    }

    /**
     * Get the notification templates which matches the given notification template type.
     *
     * @param notificationChannel Notification channel type. (Eg: EMAIL, SMS)
     * @return List of default notification templates
     */
    @Override
    public List<NotificationTemplate> getDefaultNotificationTemplates(String notificationChannel) {

        if (NotificationChannels.SMS_CHANNEL.getChannelType().equals(notificationChannel)) {
            return I18nMgtDataHolder.getInstance().getDefaultSMSTemplates();
        }
        return I18nMgtDataHolder.getInstance().getDefaultEmailTemplates();
    }

    @Override
    public boolean isEmailTemplateExists(String templateTypeDisplayName, String locale, String tenantDomain)
            throws I18nEmailMgtException {

        return isEmailTemplateExists(templateTypeDisplayName, locale, tenantDomain, null);
    }

    @Override
    public boolean isEmailTemplateExists(String templateTypeDisplayName, String locale,
                                         String tenantDomain, String applicationUuid) throws I18nEmailMgtException {

        try {
            return notificationTemplateDAO.isNotificationTemplateExists(templateTypeDisplayName, locale,
                    NotificationChannels.EMAIL_CHANNEL.getChannelType(), applicationUuid, tenantDomain);
        } catch (NotificationTemplateManagerServerException e) {
            String error = String.format("Error when retrieving email templates of %s tenant.", tenantDomain);
            throw new I18nEmailMgtServerException(error, e);
        }
    }

    @Override
    public boolean isEmailTemplateTypeExists(String templateTypeDisplayName, String tenantDomain)
            throws I18nEmailMgtException {

        try {
            return notificationTemplateDAO.isNotificationTemplateTypeExists(templateTypeDisplayName,
                    NotificationChannels.EMAIL_CHANNEL.getChannelType(), tenantDomain);
        } catch (NotificationTemplateManagerServerException e) {
            String error = String.format("Error when retrieving email templates of %s tenant.", tenantDomain);
            throw new I18nEmailMgtServerException(error, e);
        }
    }

    @Override
    public void addEmailTemplate(EmailTemplate emailTemplate, String tenantDomain, String applicationUuid)
            throws I18nEmailMgtException {

        NotificationTemplate notificationTemplate = buildNotificationTemplateFromEmailTemplate(emailTemplate);
        try {
            addNotificationTemplate(notificationTemplate, tenantDomain, applicationUuid);
        } catch (NotificationTemplateManagerClientException e) {
            throw new I18nEmailMgtClientException(e.getMessage(), e);
        } catch (NotificationTemplateManagerInternalException e) {
            if (StringUtils.isNotBlank(e.getErrorCode())) {
                String errorCode = e.getErrorCode();
                if (errorCode.contains(I18nMgtConstants.ErrorMessages.ERROR_CODE_DUPLICATE_TEMPLATE_TYPE.getCode())) {
                    throw new I18nEmailMgtInternalException(
                            I18nMgtConstants.ErrorCodes.EMAIL_TEMPLATE_TYPE_ALREADY_EXISTS, e.getMessage(), e);
                }
            }
            throw new I18nEmailMgtInternalException(e.getMessage(), e);
        } catch (NotificationTemplateManagerException e) {
            throw new I18nEmailMgtServerException(e.getMessage(), e);
        }
    }

    @Override
    public void deleteEmailTemplate(String templateTypeName, String localeCode, String tenantDomain,
                                    String applicationUuid) throws I18nEmailMgtException {

        // Validate the name and locale code.
        if (StringUtils.isBlank(templateTypeName)) {
            throw new I18nEmailMgtClientException("Cannot Delete template. Email displayName cannot be null.");
        }

        if (StringUtils.isBlank(localeCode)) {
            throw new I18nEmailMgtClientException("Cannot Delete template. Email locale cannot be null.");
        }

        try {
            notificationTemplateDAO.deleteNotificationTemplate(templateTypeName, localeCode,
                    NotificationChannels.EMAIL_CHANNEL.getChannelType(), applicationUuid, tenantDomain);
        } catch (NotificationTemplateManagerServerException ex) {
            String msg = String.format("Error deleting %s:%s template from %s tenant registry.", templateTypeName,
                    localeCode, tenantDomain);
            handleServerException(msg, ex);
        }
    }

    @Override
    public EmailTemplate getEmailTemplate(String templateType, String locale, String tenantDomain,
                                          String applicationUuid) throws I18nEmailMgtException {

        try {
            NotificationTemplate notificationTemplate = getNotificationTemplate(
                    NotificationChannels.EMAIL_CHANNEL.getChannelType(), templateType, locale,
                    tenantDomain, applicationUuid);
            return buildEmailTemplate(notificationTemplate);
        } catch (NotificationTemplateManagerException exception) {
            String errorCode = exception.getErrorCode();
            String errorMsg = exception.getMessage();
            Throwable throwable = exception.getCause();

            // Match NotificationTemplateManagerExceptions with the existing I18nEmailMgtException error types.
            if (StringUtils.isNotEmpty(exception.getErrorCode())) {
                if (IdentityMgtConstants.ErrorMessages.ERROR_CODE_INVALID_NOTIFICATION_TEMPLATE.getCode()
                        .equals(errorCode) || IdentityMgtConstants.ErrorMessages.ERROR_CODE_NO_CONTENT_IN_TEMPLATE
                        .getCode().equals(errorCode) ||
                        I18nMgtConstants.ErrorMessages.ERROR_CODE_INVALID_CHARACTERS_IN_TEMPLATE_NAME.getCode()
                                .equals(errorCode) ||
                        I18nMgtConstants.ErrorMessages.ERROR_CODE_INVALID_CHARACTERS_IN_LOCALE
                                .getCode().equals(errorCode)) {
                    throw new I18nEmailMgtClientException(errorMsg, throwable);
                } else if (IdentityMgtConstants.ErrorMessages.ERROR_CODE_INVALID_EMAIL_TEMPLATE_CONTENT.getCode()
                        .equals(errorCode)) {
                    throw new I18nMgtEmailConfigException(errorMsg, throwable);
                } else if (IdentityMgtConstants.ErrorMessages.ERROR_CODE_NO_TEMPLATE_FOUND.getCode()
                        .equals(errorCode)) {
                    throw new I18nEmailMgtInternalException(I18nMgtConstants.ErrorCodes.EMAIL_TEMPLATE_TYPE_NODE_FOUND,
                            errorMsg, throwable);
                }
            }
            throw new I18nEmailMgtServerException(exception.getMessage(), exception.getCause());
        }
    }

    /**
     * Validate the displayName of a template type.
     *
     * @param templateDisplayName Display name of the notification template
     * @throws I18nEmailMgtClientException Invalid notification template
     */
    private void validateTemplateType(String templateDisplayName, String tenantDomain)
            throws I18nEmailMgtClientException {

        try {
            validateDisplayNameOfTemplateType(templateDisplayName);
        } catch (NotificationTemplateManagerClientException e) {
            if (StringUtils.isNotBlank(e.getErrorCode())) {
                String errorCode = e.getErrorCode();
                if (errorCode.contains(I18nMgtConstants.ErrorMessages.ERROR_CODE_EMPTY_TEMPLATE_NAME.getCode())) {
                    throw new I18nEmailMgtClientException("Template Type display name cannot be null", e);
                }
                if (errorCode.contains(
                        I18nMgtConstants.ErrorMessages.ERROR_CODE_INVALID_CHARACTERS_IN_TEMPLATE_NAME.getCode())) {
                    throw new I18nEmailMgtClientException(e.getMessage(), e);
                }
            }
            throw new I18nEmailMgtClientException("Invalid notification template", e);
        }
    }

    private void handleServerException(String errorMsg, Throwable ex) throws I18nEmailMgtServerException {

        log.error(errorMsg);
        throw new I18nEmailMgtServerException(errorMsg, ex);
    }
}
