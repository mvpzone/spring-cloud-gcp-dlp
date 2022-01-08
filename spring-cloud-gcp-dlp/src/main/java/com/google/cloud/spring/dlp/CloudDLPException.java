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

import org.springframework.core.NestedRuntimeException;

/**
 * Describes error conditions that can occur when interfacing with Cloud DLP
 * APIs.
 *
 * @author Vinesh
 *
 * @since 2.0.4
 */
public final class CloudDLPException extends NestedRuntimeException {

    private static final long serialVersionUID = 1L;

    CloudDLPException(String message, Exception errorCause) {
        super(message, errorCause);
    }

    CloudDLPException(String message) {
        super(message);
    }
}