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

package com.google.cloud.spring.autoconfigure.dlp;

import com.google.cloud.spring.core.Credentials;
import com.google.cloud.spring.core.CredentialsSupplier;
import com.google.cloud.spring.core.DefaultGcpProjectIdProvider;
import com.google.cloud.spring.core.GcpProjectIdProvider;
import com.google.cloud.spring.core.GcpScope;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Additional settings for use with Cloud DLP APIs.
 *
 * @author Vinesh
 * @since 2.0.4
 */
@ConfigurationProperties("spring.cloud.gcp.dlp")
public class CloudDLPProperties implements CredentialsSupplier {
	// Overrides the GCP OAuth2 credentials specified in the Core module.
	@NestedConfigurationProperty
	private final Credentials credentials = new Credentials(GcpScope.CLOUD_PLATFORM.getUrl());

	private final GcpProjectIdProvider projectIdProvider = new DefaultGcpProjectIdProvider();
	private String location = "global";
	private int executorThreadsCount = 1;

	public Credentials getCredentials() {
		return this.credentials;
	}

	public GcpProjectIdProvider getProjectIdProvider(){
		return this.projectIdProvider;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public int getExecutorThreadsCount() {
		return executorThreadsCount;
	}

	public void setExecutorThreadsCount(int executorThreadsCount) {
		this.executorThreadsCount = executorThreadsCount;
	}

}
