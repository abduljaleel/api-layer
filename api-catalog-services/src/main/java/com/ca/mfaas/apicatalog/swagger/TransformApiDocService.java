/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.swagger;

import com.ca.mfaas.apicatalog.exceptions.ApiDocTransformationException;
import com.ca.mfaas.apicatalog.metadata.EurekaMetadataParser;
import com.ca.mfaas.apicatalog.services.initialisation.InstanceRetrievalService;
import com.ca.mfaas.product.constants.CoreService;
import com.ca.mfaas.product.routing.RoutedService;
import com.ca.mfaas.product.routing.RoutedServices;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.netflix.appinfo.InstanceInfo;
import io.swagger.models.Path;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import io.swagger.util.Json;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.UnexpectedTypeException;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
public class TransformApiDocService {
    private static final String SWAGGER_LOCATION_LINK = "[Swagger/OpenAPI JSON Document]";
    private static final String HIDDEN_TAG = "apimlHidden";
    private static final String CATALOG_VERSION = "/api/v1";
    private static final String CATALOG_APIDOC_ENDPOINT = "/apidoc";
    private static final String HARDCODED_VERSION = "/v1";
    private static final String SEPARATOR = "/";

    private final InstanceRetrievalService instanceRetrievalService;
    private final EurekaMetadataParser metadataParser = new EurekaMetadataParser();

    @Autowired
    public TransformApiDocService(InstanceRetrievalService instanceRetrievalService) {
        this.instanceRetrievalService = instanceRetrievalService;
    }

    public String transformApiDoc(String serviceId, String apiDoc) {
        Swagger swagger;

        try {
            swagger = Json.mapper().readValue(apiDoc, Swagger.class);
        } catch (IOException e) {
            log.error("Could not convert response body to a Swagger object.", e);
            throw new UnexpectedTypeException("Response is not a Swagger type object.");
        }

        boolean hidden = swagger.getTag(HIDDEN_TAG) != null;

        InstanceInfo gatewayInfo = instanceRetrievalService.getInstanceInfo(CoreService.GATEWAY.getServiceId());
        InstanceInfo serviceInfo = instanceRetrievalService.getInstanceInfo(serviceId);
        RoutedServices routes = metadataParser.parseRoutes(serviceInfo.getMetadata());

        updateSchemeHostAndLink(swagger, serviceId, gatewayInfo, hidden);
        updatePaths(swagger, serviceId, routes, hidden);

        try {
            return Json.mapper().writeValueAsString(swagger);
        } catch (JsonProcessingException e) {
            log.error("Could not convert Swagger to JSON", e);
            throw new ApiDocTransformationException("Could not convert Swagger to JSON");
        }
    }

    private void updateSchemeHostAndLink(Swagger swagger, String serviceId, InstanceInfo instanceInfo, boolean hidden) {
        String scheme = instanceInfo.isPortEnabled(InstanceInfo.PortType.SECURE) ? "https" : "http";

        String host = instanceInfo.getHostName();
        if (scheme.equals("http")) {
            if (instanceInfo.getPort() != 80) {
                host += ":" + instanceInfo.getPort();
            }
        } else {
            if (instanceInfo.getPort() != 443) {
                host += ":" + instanceInfo.getSecurePort();
            }
        }

        String link = scheme + "://" + host + CATALOG_VERSION + SEPARATOR + CoreService.API_CATALOG.getServiceId() +
            CATALOG_APIDOC_ENDPOINT + SEPARATOR + serviceId + HARDCODED_VERSION;
        String swaggerLink = "\n\n" + SWAGGER_LOCATION_LINK + "(" + link + ")";

        swagger.setSchemes(Collections.singletonList(Scheme.forValue(scheme)));
        swagger.setHost(host);
        if (!hidden) {
            swagger.getInfo().setDescription(swagger.getInfo().getDescription() + swaggerLink);
        }
    }

    private void updatePaths(Swagger swagger, String serviceId, RoutedServices routes, boolean hidden) {
        Map<String, Path> updatedShortPaths = new HashMap<>();
        Map<String, Path> updatedLongPaths = new HashMap<>();
        Map<String, Path> updatedPaths;
        Set<String> prefixes = new HashSet<>();

        if (swagger.getPaths() != null && !swagger.getPaths().isEmpty()) {
            swagger.getPaths().forEach((originalEndpoint, path) -> {

                // Logging
                log.trace("Swagger Service Id: " + serviceId);
                log.trace("Original Endpoint: " + originalEndpoint);
                log.trace("Base Path: " + swagger.getBasePath());

                // Retrieve route which matches endpoint
                String endPoint = swagger.getBasePath().equals(SEPARATOR) ? originalEndpoint : swagger.getBasePath() + originalEndpoint;
                RoutedService route = routes.getBestMatchingServiceUrl(endPoint, true);

                String updatedShortEndPoint = null;
                String updatedLongEndPoint = null;
                if (route != null) {
                    prefixes.add(route.getGatewayUrl());
                    updatedShortEndPoint = endPoint.replaceFirst(route.getServiceUrl(), "");
                    updatedLongEndPoint = SEPARATOR + route.getGatewayUrl() + SEPARATOR + serviceId + updatedShortEndPoint;
                }
                log.trace("Final Endpoint: " + updatedLongEndPoint);

                // If endpoint not converted, then use original
                if (updatedLongEndPoint != null) {
                    updatedShortPaths.put(updatedShortEndPoint, path);
                    updatedLongPaths.put(updatedLongEndPoint, path);
                } else {
                    log.debug("Could not transform endpoint: " + originalEndpoint + ", original used");
                }
            });
        }

        // update basePath and the original swagger object with the new paths
        if (prefixes.size() == 1) {
            swagger.setBasePath(SEPARATOR + prefixes.iterator().next() + SEPARATOR + serviceId);
            updatedPaths = updatedShortPaths;
        } else {
            swagger.setBasePath("");
            updatedPaths = updatedLongPaths;
        }

        if (!hidden) {
            swagger.setPaths(updatedPaths);
        }
    }
}
