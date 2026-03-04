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

package me.biocomp.hubitat_ci.api.common_api
/**
 * Stub for hubitat.scheduling.AsyncResponse - returned from async HTTP callbacks.
 * See: https://docs2.hubitat.com/en/developer/allowed-imports
 */
class AsyncResponse {
    /** @return HTTP response status code */
    int getStatus() { throw new UnsupportedOperationException("Stub") }
    /** @return HTTP response body as a String */
    String getData() { throw new UnsupportedOperationException("Stub") }
    /** @return HTTP response headers as a Map */
    Map getHeaders() { throw new UnsupportedOperationException("Stub") }
    /** @return a specific header value by name */
    String getHeader(String name) { throw new UnsupportedOperationException("Stub") }
    /** @return response body parsed as JSON into a Map/List */
    def getJson() { throw new UnsupportedOperationException("Stub") }
    /** @return response body parsed as XML (GPathResult) */
    def getXml() { throw new UnsupportedOperationException("Stub") }
    /** @return true if the response indicates an error (status >= 400) */
    boolean hasError() { throw new UnsupportedOperationException("Stub") }
    /** @return the error message if hasError() is true */
    String getErrorMessage() { throw new UnsupportedOperationException("Stub") }
    /** @return response body as raw bytes */
    byte[] getByteData() { throw new UnsupportedOperationException("Stub") }
}
