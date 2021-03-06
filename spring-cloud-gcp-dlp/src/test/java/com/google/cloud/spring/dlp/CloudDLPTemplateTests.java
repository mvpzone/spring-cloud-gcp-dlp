/*
 * Copyright 2017-2018 the original author or authors.
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;

import com.google.cloud.dlp.v2.DlpServiceClient;
import com.google.cloud.spring.core.DefaultGcpProjectIdProvider;
import com.google.privacy.dlp.v2.InspectContentRequest;
import com.google.privacy.dlp.v2.InspectContentResponse;
import com.google.privacy.dlp.v2.ListInfoTypesRequest;
import com.google.privacy.dlp.v2.ListInfoTypesResponse;
import com.google.privacy.dlp.v2.RedactImageRequest;
import com.google.privacy.dlp.v2.RedactImageResponse;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.mockito.Mockito;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/**
 * Unit tests for the {@link CloudDLPTemplate}.
 *
 * @author Vinesh
 *
 * @since 2.0.4
 */
public class CloudDLPTemplateTests {

    // Resource representing a fake image blob
    private static final Resource FAKE_IMAGE = new ByteArrayResource("fake_image".getBytes());
    private static final InspectContentResponse DEFAULT_INSPECT_API_RESPONSE = InspectContentResponse.newBuilder()
            .build();
    private static final RedactImageResponse DEFAULT_REDACT_API_RESPONSE = RedactImageResponse.newBuilder().build();
    private static final ListInfoTypesResponse DEFAULT_INFOTYPE_RESPONSE = ListInfoTypesResponse.getDefaultInstance();

    private DlpServiceClient dlpClient;
    private CloudDLPTemplate dlpTemplate;

    @Before
    public void setupVisionTemplateMock() {
        this.dlpClient = Mockito.mock(DlpServiceClient.class);
        this.dlpTemplate = new CloudDLPTemplate(new DefaultGcpProjectIdProvider(), this.dlpClient);
    }

    @Test
    public void testInspectImage() throws IOException {
        when(this.dlpClient.inspectContent(any(InspectContentRequest.class))).thenReturn(DEFAULT_INSPECT_API_RESPONSE);

        this.dlpTemplate.inspectImage(FAKE_IMAGE);

        verify(this.dlpClient, times(1)).inspectContent(any(InspectContentRequest.class));
    }

    @Test
    public void testRedactImage() throws IOException {
        when(this.dlpClient.redactImage(any(RedactImageRequest.class))).thenReturn(DEFAULT_REDACT_API_RESPONSE);

        this.dlpTemplate.redactImage(FAKE_IMAGE);

        verify(this.dlpClient, times(1)).redactImage(any(RedactImageRequest.class));
    }

    @Test
    public void testGetSupportedInfoType() throws IOException {
        when(this.dlpClient.listInfoTypes(any(ListInfoTypesRequest.class))).thenReturn(DEFAULT_INFOTYPE_RESPONSE);

        this.dlpTemplate.getSupportedInfoTypes();

        verify(this.dlpClient, times(1)).listInfoTypes(any(ListInfoTypesRequest.class));
    }

    @Test
    public void testIOError() {
        Assert.assertThrows("Failed to read image bytes from provided resource.", CloudDLPException.class,
                new ThrowingRunnable() {
                    public void run() throws Throwable {
                        dlpTemplate.redactImage(new BadResource());
                    }
                });
    }

    private static final class BadResource extends AbstractResource {
        @Override
        public String getDescription() {
            return "bad resource";
        }

        @Override
        public InputStream getInputStream() throws IOException {
            throw new IOException("Failed to open resource.");
        }
    }
}
