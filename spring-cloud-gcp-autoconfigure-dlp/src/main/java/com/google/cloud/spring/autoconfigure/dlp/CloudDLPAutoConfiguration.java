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

package com.google.cloud.spring.autoconfigure.dlp;

import java.io.IOException;
import java.util.concurrent.Executors;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedExecutorProvider;
import com.google.cloud.dlp.v2.DlpServiceClient;
import com.google.cloud.dlp.v2.DlpServiceSettings;
import com.google.cloud.spring.core.DefaultCredentialsProvider;
import com.google.cloud.spring.core.UserAgentHeaderProvider;
import com.google.cloud.spring.dlp.CloudDLPTemplate;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides Spring Beans for using Cloud DLP API.
 *
 * @author Vinesh
 * @since 2.0.4
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CloudDLPProperties.class)
@ConditionalOnClass(CloudDLPTemplate.class)
@ConditionalOnProperty(value = "spring.cloud.gcp.dlp.enabled", matchIfMissing = true)
public class CloudDLPAutoConfiguration {
	private final CloudDLPProperties cloudDLPProperties;

	private final CredentialsProvider credentialsProvider;

	public CloudDLPAutoConfiguration(CloudDLPProperties properties, CredentialsProvider credentialsProvider)
			throws IOException {
		this.cloudDLPProperties = properties;

		if (properties.getCredentials().hasKey()) {
			this.credentialsProvider = new DefaultCredentialsProvider(properties);
		} else {
			this.credentialsProvider = credentialsProvider;
		}
	}

	/**
	 * Configure the Cloud DLP API client {@link DlpServiceClient}. The
	 * spring-cloud-gcp-starter autowires a {@link CredentialsProvider} object that
	 * provides the GCP credentials, required to authenticate and authorize DLP API
	 * calls.
	 * <p>
	 * Cloud DLP API client implements {@link AutoCloseable}, which is automatically
	 * honored by Spring bean lifecycle.
	 * <p>
	 * Most of the Google Cloud API clients are thread-safe heavy objects. I.e.,
	 * it's better to produce a singleton and re-using the client object for
	 * multiple requests.
	 * 
	 * @return a Cloud DLP API client
	 * @throws IOException if an exception occurs creating the DlpServiceClient
	 */
	@Bean
	@ConditionalOnMissingBean
	public DlpServiceClient dlpServiceClient() throws IOException {
		DlpServiceSettings clientSettings = DlpServiceSettings.newBuilder()
				.setCredentialsProvider(this.credentialsProvider)
				.setHeaderProvider(new UserAgentHeaderProvider(CloudDLPAutoConfiguration.class))
				.setExecutorProvider(FixedExecutorProvider
						.create(Executors.newScheduledThreadPool(this.cloudDLPProperties.getExecutorThreadsCount())))
				.build();

		return DlpServiceClient.create(clientSettings);
	}

	@Bean
	@ConditionalOnMissingBean
	public CloudDLPTemplate cloudDLPTemplate(final DlpServiceClient dlpClient) {
		final CloudDLPTemplate template = new CloudDLPTemplate(cloudDLPProperties.getProjectIdProvider(),
				cloudDLPProperties.getLocation(), dlpClient);
		template.setMaxfindings(cloudDLPProperties.getMaxFindings());
		template.setIncludeFindings(cloudDLPProperties.isIncludeFindings());
		template.setIncludeQuote(cloudDLPProperties.isIncludeQuote());
		template.setMinLikelihood(cloudDLPProperties.getMinLikelihood());
		return template;
	}
}
