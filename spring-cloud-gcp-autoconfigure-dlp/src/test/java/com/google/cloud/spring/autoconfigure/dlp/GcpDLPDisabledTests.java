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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.google.cloud.spring.dlp.CloudDLPTemplate;

import org.junit.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Verifies that GCP Storage may be disabled via the property:
 * "spring.cloud.gcp.storage.enabled=false".
 *
 * @author Daniel Zou
 */
public class GcpDLPDisabledTests {
	private static final String PROJECT_NAME = "hollow-light-of-the-sealed-land";

	ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(CloudDLPAutoConfiguration.class))
			.withPropertyValues("spring.cloud.gcp.dlp.project-id=" + PROJECT_NAME)
			.withPropertyValues("spring.cloud.gcp.dlp.enabled=false");

	@Test
	public void testDLPBeanIsNotProvided() {
		this.contextRunner.run(context -> {
			Throwable thrown = catchThrowable(() -> context.getBean(CloudDLPTemplate.class));
			assertThat(thrown).isInstanceOf(NoSuchBeanDefinitionException.class);
		});
	}
}
