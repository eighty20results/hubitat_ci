/*
 * Copyright 2025 Eighty/20 Results by Thomas Sjolshagen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.biocomp.hubitat_ci.api.http
/**
 * Stub for groovyx.net.http.HttpResponseException.
 * Thrown when an HTTP response indicates a failure (4xx/5xx).
 * See: https://docs2.hubitat.com/en/developer/allowed-imports
 */
class HttpResponseException extends RuntimeException {
    /** The HTTP response status code. */
    int statusCode
    /** The response body (may be null). */
    def response
    /**
     * @param statusCode HTTP status code
     * @param message    error message
     * @param response   optional raw response object
     */
    HttpResponseException(int statusCode, String message, def response = null) {
        super(message)
        this.statusCode = statusCode
        this.response = response
    }
    /** @return HTTP status code */
    int getStatusCode() { statusCode }
    /** @return the raw response object */
    def getResponse() { response }
}
