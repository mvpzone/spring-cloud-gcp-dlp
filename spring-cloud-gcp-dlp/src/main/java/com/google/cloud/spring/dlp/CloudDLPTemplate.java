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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.cloud.dlp.v2.DlpServiceClient;
import com.google.cloud.spring.core.GcpProjectIdProvider;
import com.google.privacy.dlp.v2.ByteContentItem;
import com.google.privacy.dlp.v2.ByteContentItem.BytesType;
import com.google.privacy.dlp.v2.ContentItem;
import com.google.privacy.dlp.v2.InfoType;
import com.google.privacy.dlp.v2.InfoTypeDescription;
import com.google.privacy.dlp.v2.InspectConfig;
import com.google.privacy.dlp.v2.InspectContentRequest;
import com.google.privacy.dlp.v2.InspectContentResponse;
import com.google.privacy.dlp.v2.Likelihood;
import com.google.privacy.dlp.v2.ListInfoTypesRequest;
import com.google.privacy.dlp.v2.ListInfoTypesResponse;
import com.google.privacy.dlp.v2.LocationName;
import com.google.privacy.dlp.v2.RedactImageRequest;
import com.google.privacy.dlp.v2.RedactImageRequest.ImageRedactionConfig;
import com.google.privacy.dlp.v2.RedactImageResponse;
import com.google.protobuf.ByteString;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Spring Template offering convenience methods for interacting with the Cloud
 * DLP APIs.
 *
 * @author vinesh
 *
 * @since 2.0.4
 */
public class CloudDLPTemplate {
    private final GcpProjectIdProvider projectProvider;
    private final DlpServiceClient dlpClient;
    private final String location;
    private int maxFindings = 0;
    private boolean includeQuote = true;
    private boolean includeFindings = true;
    private String inspectionTemplate = null;
    private final ByteContentUtil util = new ByteContentUtil();
    /*
     * The minimum likelihood required before returning a match: See:
     * https://cloud.google.com/dlp/docs/likelihood
     */
    private Likelihood minLikelihood = Likelihood.POSSIBLE;

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

    public int getMaxfindings() {
        return this.maxFindings;
    }

    public void setMaxfindings(final int maxfindings) {
        if (maxfindings < 0) {
            throw new IllegalArgumentException("maxFindings must be positive");
        }
        this.maxFindings = maxfindings;
    }

    public boolean isIncludeQuote() {
        return this.includeQuote;
    }

    public void setIncludeQuote(final boolean includeQuote) {
        this.includeQuote = includeQuote;
    }

    public Likelihood getMinLikelihood() {
        return this.minLikelihood;
    }

    public void setMinLikelihood(final Likelihood minLikelihood) {
        this.minLikelihood = minLikelihood;
    }

    public boolean isIncludeFindings() {
        return this.includeFindings;
    }

    public void setIncludeFindings(final boolean includeFindings) {
        this.includeFindings = includeFindings;
    }

    public String getInspectionTemplate() {
        return this.inspectionTemplate;
    }

    public void setInspectionTemplate(final String inspectionTemplate) {
        this.inspectionTemplate = inspectionTemplate;
    }

    /**
     * Inspect an image and redact based on defaul info types.
     * 
     * @param imgResource the image one wishes to analyze. The Cloud Vision APIs
     *                    support image formats described here:
     *                    https://cloud.google.com/vision/docs/supported-files
     * @param bytesType   The content type to be inspected @see BytesType
     * @return the redact response.
     * @throws CloudDLPException if the image could not be read or if a malformed
     *                           response is received from the Cloud Vision APIs
     */
    public RedactImageResponse redactImage(final Resource imgResource, final BytesType bytesType,
            final List<String> infoTypes) {
        Assert.notNull(imgResource, "Resource not provided");
        Assert.notNull(bytesType, "BytesType not supplied");
        Assert.isTrue(util.isImageType(bytesType), "Invalid bytesType not supported image.");

        final ByteContentItem byteItem = util.createByteContent(imgResource, bytesType);

        // Do not specify the type of info to redact using default info types.
        final RedactImageRequest.Builder request = RedactImageRequest.newBuilder()
                .setParent(LocationName.of(projectProvider.getProjectId(), this.location).toString())
                .setByteItem(byteItem).setIncludeFindings(isIncludeFindings());

        final InspectConfig.Builder inspectConfig = InspectConfig.newBuilder().setIncludeQuote(isIncludeQuote());
        try {
            final List<InfoType> infoTypeList = toInfoTypes(infoTypes);
            if (!infoTypeList.isEmpty()) {
                inspectConfig.addAllInfoTypes(infoTypeList).build();

                // Prepare redaction configs.
                final List<ImageRedactionConfig> imageRedactionConfigs = infoTypeList.stream()
                        .map(infoType -> ImageRedactionConfig.newBuilder().setInfoType(infoType).build())
                        .collect(Collectors.toList());

                // Do not specify the type of info to redact using default info types.
                request.addAllImageRedactionConfigs(imageRedactionConfigs);
            }
            request.setInspectConfig(inspectConfig);

            // Use the client to send the API request.
            return dlpClient.redactImage(request.build());
        } catch (final Exception e) {
            throw new CloudDLPException("Failed to receive valid response from DLP APIs; empty response received.", e);
        }
    }

    public RedactImageResponse redactImage(final Resource imgResource, final List<String> infoTypes) {
        return redactImage(imgResource, BytesType.IMAGE, infoTypes);
    }

    public RedactImageResponse redactImage(final Resource imgResource, final String... infoTypes) {
        return this.redactImage(imgResource, Arrays.asList(infoTypes));
    }

    public RedactImageResponse redactImage(final Resource imgResource) {
        return this.redactImage(imgResource, Collections.emptyList());
    }

    /**
     * Inspect an image and redact based on defaul info types.
     * 
     * @param text      the text one wishes to analyze.
     * @param bytesType The content type to be inspected @see BytesType
     * @return the inspect response.
     * @throws CloudDLPException if the image could not be read or if a malformed
     *                           response is received from the Cloud DLP APIs
     */
    public InspectContentResponse inspectContent(final String text, final BytesType bytesType,
            final String inspectionTemplate, final List<String> infoTypes) {
        Assert.notNull(text, "text not provided");
        Assert.notNull(bytesType, "BytesType not provided");
        Assert.isTrue(BytesType.BYTES_TYPE_UNSPECIFIED != bytesType, "Invalid bytesType not supported type.");

        final ByteContentItem byteItem = util.createByteContent(text, bytesType);
        try {
            return doInpsect(byteItem, inspectionTemplate, infoTypes);
        } catch (final Exception e) {
            throw new CloudDLPException("Failed to receive valid response from DLP APIs; no response received.", e);
        }
    }

    /**
     * Inspect an image and redact based on defaul info types.
     * 
     * @param resource  the resource one wishes to analyze. The Cloud DLP APIs
     *                  support file formats described here:
     *                  https://cloud.google.com/dlp/docs/supported-file-types
     * @param bytesType The content type to be inspected @see BytesType
     * @return the inspect response.
     * @throws CloudDLPException if the image could not be read or if a malformed
     *                           response is received from the Cloud DLP APIs
     */
    public InspectContentResponse inspectContent(final Resource resource, final BytesType bytesType,
            final String inspectionTemplate, final List<String> infoTypes) {
        Assert.notNull(resource, "Resource not provided");
        Assert.notNull(bytesType, "BytesType not provided");
        Assert.isTrue(BytesType.BYTES_TYPE_UNSPECIFIED != bytesType, "Invalid bytesType not supported type.");

        final ByteContentItem byteItem = util.createByteContent(resource, bytesType);
        try {
            return doInpsect(byteItem, inspectionTemplate, infoTypes);
        } catch (final Exception e) {
            throw new CloudDLPException("Failed to receive valid response from DLP APIs; no response received.", e);
        }
    }

    private InspectContentResponse doInpsect(final ByteContentItem byteItem, final String inspectionTemplate,
            final List<String> infoTypes) {
        // The maximum number of findings to report (0 = server maximum)
        final InspectConfig.FindingLimits limits = InspectConfig.FindingLimits.newBuilder()
                .setMaxFindingsPerItem(getMaxfindings()).setMaxFindingsPerRequest(getMaxfindings()).build();

        final InspectConfig.Builder inspectionConfig = InspectConfig.newBuilder().setIncludeQuote(isIncludeQuote())
                .setLimits(limits).setMinLikelihood(getMinLikelihood());

        final List<InfoType> infoTypeList = toInfoTypes(infoTypes);
        if (!infoTypeList.isEmpty()) {
            inspectionConfig.addAllInfoTypes(infoTypeList);
        }

        // Do not specify the type of info to redact using default info types.
        final InspectContentRequest.Builder request = InspectContentRequest.newBuilder()
                .setParent(LocationName.of(projectProvider.getProjectId(), this.location).toString())
                .setItem(ContentItem.newBuilder().setByteItem(byteItem).build()).setInspectConfig(inspectionConfig);

        if (StringUtils.hasText(getInspectionTemplate())) {
            request.setInspectTemplateName(getInspectionTemplate());
        }

        // override if passed in
        if (StringUtils.hasText(inspectionTemplate)) {
            request.setInspectTemplateName(inspectionTemplate);
        }

        // Use the client to send the API request.
        return dlpClient.inspectContent(request.build());
    }

    public InspectContentResponse inspectContent(final Resource resource, final BytesType bytesType) {
        Assert.isTrue(util.isDocType(bytesType), "Invalid bytesType not supported doc.");
        return inspectContent(resource, bytesType, null, Collections.emptyList());
    }

    public InspectContentResponse inspectImage(final Resource imgResource, final BytesType bytesType,
            final String inspectionTemplate, final List<String> infoTypes) {
        Assert.isTrue(util.isImageType(bytesType), "Invalid bytesType not supported image.");
        return inspectContent(imgResource, bytesType, inspectionTemplate, infoTypes);
    }

    public InspectContentResponse inspectText(final String text, final BytesType bytesType,
            final String inspectionTemplate, final List<String> infoTypes) {
        Assert.isTrue(util.isTextType(bytesType), "Invalid bytesType not supported text type.");
        return inspectContent(text, bytesType, inspectionTemplate, infoTypes);
    }

    public InspectContentResponse inspectImage(final Resource imgResource, final String inspectionTemplate,
            final List<String> infoTypes) {
        return inspectImage(imgResource, BytesType.IMAGE, inspectionTemplate, infoTypes);
    }

    public InspectContentResponse inspectText(final String text, final String inspectionTemplate,
            final List<String> infoTypes) {
        return inspectText(text, BytesType.TEXT_UTF8, inspectionTemplate, infoTypes);
    }

    public InspectContentResponse inspectImage(final Resource imgResource, final String... infoTypes) {
        return this.inspectImage(imgResource, null, Arrays.asList(infoTypes));
    }

    public InspectContentResponse inspectText(final String text, final String... infoTypes) {
        return this.inspectText(text, null, Arrays.asList(infoTypes));
    }

    public InspectContentResponse inspectImage(final Resource imgResource, final String inspectionTemplate) {
        return this.inspectImage(imgResource, inspectionTemplate, Collections.emptyList());
    }

    public InspectContentResponse inspectText(final String text, final String inspectionTemplate) {
        return this.inspectText(text, inspectionTemplate, Collections.emptyList());
    }

    public InspectContentResponse inspectImage(final Resource imgResource) {
        return this.inspectImage(imgResource, null, Collections.emptyList());
    }

    public InspectContentResponse inspectText(final String text) {
        return this.inspectText(text, null, Collections.emptyList());
    }

    /**
     * Return infoTypes supported by certain parts of the API. Supported filters are
     * "supported_by=INSPECT" and "supported_by=RISK_ANALYSIS" Defaults to
     * "supported_by=INSPECT"
     * 
     * @param filter "supported_by=INSPECT" or "supported_by=RISK_ANALYSIS"
     * @param locale language code eg: en-US, BCP-47 language code for localized
     *               infoType friendly names. Defaults to "en_US"
     * @return List of InfoTypeDescription
     */
    public List<InfoTypeDescription> getSupportedInfoTypes(final String filter, final Locale locale) {
        // Construct the request to be sent by the client
        final ListInfoTypesRequest listInfoTypesRequest = ListInfoTypesRequest.newBuilder()
                .setFilter("supported_by=INSPECT").setLanguageCode(locale.getLanguage() + '-' + locale.getCountry())
                .build();

        // Use the client to send the API request.
        final ListInfoTypesResponse response = dlpClient.listInfoTypes(listInfoTypesRequest);
        return response.getInfoTypesList();
    }

    public List<InfoTypeDescription> getSupportedInfoTypes(final String filter) {
        return this.getSupportedInfoTypes(filter, Locale.US);
    }

    public List<InfoTypeDescription> getSupportedInfoTypes() {
        return this.getInspectInfoTypes();
    }

    public List<InfoTypeDescription> getInspectInfoTypes() {
        return this.getSupportedInfoTypes("supported_by=INSPECT", Locale.US);
    }

    public List<InfoTypeDescription> getRiskInfoTypes() {
        return this.getSupportedInfoTypes("supported_by=RISK_ANALYSIS", Locale.US);
    }

    private List<InfoType> toInfoTypes(final List<String> infoTypes) {
        if (Objects.isNull(infoTypes) || infoTypes.isEmpty()) {
            return Collections.emptyList();
        }

        final List<InfoType> infoTypeList = new ArrayList<>(infoTypes.size());
        // See https://cloud.google.com/dlp/docs/infotypes-reference for infoTypes list.
        for (final String typeName : infoTypes) {
            infoTypeList.add(InfoType.newBuilder().setName(typeName).build());
        }

        return infoTypeList;
    }

    private static final class ByteContentUtil {
        private ByteString readContentBytes(final Resource resource) {
            try {
                return ByteString.readFrom(resource.getInputStream());
            } catch (final IOException ex) {
                throw new CloudDLPException("Failed to read content bytes from provided resource.", ex);
            }
        }

        private boolean isImageType(final BytesType type) {
            if (type == null) {
                return false;
            }

            return type == BytesType.IMAGE || type == BytesType.IMAGE_JPEG || type == BytesType.IMAGE_PNG
                    || type == BytesType.IMAGE_BMP || type == BytesType.IMAGE_SVG;
        }

        private boolean isTextType(final BytesType type) {
            if (type == null) {
                return false;
            }

            return type == BytesType.TEXT_UTF8 || type == BytesType.CSV || type == BytesType.TSV;
        }

        private boolean isDocType(final BytesType type) {
            if (type == null) {
                return false;
            }

            return type == BytesType.PDF || type == BytesType.WORD_DOCUMENT;
        }

        private ByteContentItem createByteContent(final Resource resource, final BytesType bytesType) {
            return ByteContentItem.newBuilder().setType(bytesType)
                    .setData(readContentBytes(resource)).build();
        }

        private ByteContentItem createByteContent(final String text, final BytesType bytesType) {
            Assert.isTrue(isTextType(bytesType), "Invalid bytesType not supported text type.");
            return ByteContentItem.newBuilder()
                    .setType(bytesType)
                    .setData(ByteString.copyFromUtf8(text))
                    .build();
        }
    }
}
