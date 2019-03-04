/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.service.security.service.security.config;

import com.broadcom.apiml.library.service.security.test.integration.error.impl.ErrorServiceImpl;
import com.broadcom.apiml.library.service.security.test.integration.error.ErrorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeansConfiguration {
    @Bean(name = "securityErrorService")
    public ErrorService errorService() {
        return new ErrorServiceImpl("/security-messages.yml");
    }

    @Bean(name = "securityObjectMapper")
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
