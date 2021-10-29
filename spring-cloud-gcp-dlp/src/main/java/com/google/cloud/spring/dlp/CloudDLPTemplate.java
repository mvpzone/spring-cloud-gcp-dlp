/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.spring.dlp;

import java.io.IOException;

import com.google.cloud.dlp.v2.DlpServiceClient;
import com.google.cloud.spring.core.GcpProjectIdProvider;
import com.google.privacy.dlp.v2.ByteContentItem;
import com.google.privacy.dlp.v2.ByteContentItem.BytesType;
import com.google.privacy.dlp.v2.ContentItem;
import com.google.privacy.dlp.v2.InspectConfig;
import com.google.privacy.dlp.v2.InspectContentRequest;
import com.google.privacy.dlp.v2.InspectContentResponse;
import com.google.privacy.dlp.v2.LocationName;
import com.google.privacy.dlp.v2.RedactImageRequest;
import com.google.privacy.dlp.v2.RedactImageRequest.ImageRedactionConfig;
import com.google.privacy.dlp.v2.RedactImageResponse;
import com.google.protobuf.ByteString;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Spring Template offering convenience methods for interacting with the Cloud
 * Vision APIs.
 *
 * @author Daniel Zou
 *
 * @since 1.1
 */
public class CloudDLPTemplate {
    private final GcpProjectIdProvider projectProvider;
    private final DlpServiceClient dlpClient;
    private final String location;

    public CloudDLPTemplate(final GcpProjectIdProvider projectProvider, final String location,
            final DlpServiceClient dlpClient) {
        Assert.notNull(projectProvider, "GcpProjectIdProvider must not be null.");
        Assert.notNull(dlpClient, "DlpServiceClient must not be null.");

        this.dlpClient = dlpClient;
        this.projectProvider = projectProvider;
        this.location = location;
    }

    public CloudDLPTemplate(final GcpProjectIdProvider projectProvider, final DlpServiceClient dlpClient) {
        this(projectProvider, "global", dlpClient);
    }

    /**
     * Inspect an image and redact based on defaul info types.
     * 
     * @param imgResource the image one wishes to analyze. The Cloud Vision APIs
     *                    support image formats described here:
     *                    https://cloud.google.com/vision/docs/supported-files
     * @return the redact response.
     * @throws CloudDLPException if the image could not be read or if a malformed
     *                           response is received from the Cloud Vision APIs
     */
    public RedactImageResponse redactImage(final Resource imgResource) {
        final ByteContentItem byteItem = readImageBytes(imgResource);
        try {
            final ImageRedactionConfig redactionConfig = ImageRedactionConfig.newBuilder().build();

            // Do not specify the type of info to redact using default info types.
            final RedactImageRequest request = RedactImageRequest.newBuilder()
                    .setParent(LocationName.of(projectProvider.getProjectId(), this.location).toString())
                    .setByteItem(byteItem).setIncludeFindings(true).addImageRedactionConfigs(redactionConfig).build();

            // Use the client to send the API request.
            return dlpClient.redactImage(request);
        } catch (final Exception e) {
            throw new CloudDLPException("Failed to receive valid response from DLP APIs; empty response received.", e);
        }
    }

    /**
     * Inspect an image and redact based on defaul info types.
     * 
     * @param imgResource the image one wishes to analyze. The Cloud Vision APIs
     *                    support image formats described here:
     *                    https://cloud.google.com/vision/docs/supported-files
     * @return the inspect response.
     * @throws CloudDLPException if the image could not be read or if a malformed
     *                           response is received from the Cloud Vision APIs
     */
    public InspectContentResponse inspectImage(final Resource imgResource) {
        final ByteContentItem byteItem = readImageBytes(imgResource);
        try {
            final InspectConfig inspectionConfig = InspectConfig.newBuilder().setIncludeQuote(true).build();

            // Do not specify the type of info to redact using default info types.
            final InspectContentRequest request = InspectContentRequest.newBuilder()
                    .setParent(LocationName.of(projectProvider.getProjectId(), this.location).toString())
                    .setItem(ContentItem.newBuilder().setByteItem(byteItem).build()).setInspectConfig(inspectionConfig)
                    .build();

            // Use the client to send the API request.
            return dlpClient.inspectContent(request);
        } catch (final Exception e) {
            throw new CloudDLPException("Failed to receive valid response from DLP APIs; empty response received.", e);
        }
    }

    private ByteContentItem readImageBytes(final Resource imgResource) {
        try {
            return ByteContentItem.newBuilder().setType(BytesType.IMAGE_JPEG)
                    .setData(ByteString.readFrom(imgResource.getInputStream())).build();
        } catch (final IOException ex) {
            throw new CloudDLPException("Failed to read image bytes from provided resource.", ex);
        }

    }
}
