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
    int getStatus() { throw new UnsupportedOperationException("AsyncResponse.getStatus() is a compile-time stub only and cannot be used at runtime.") }
    /** @return HTTP response body as a String */
    String getData() { throw new UnsupportedOperationException("AsyncResponse.getData() is a compile-time stub only and cannot be used at runtime.") }
    /** @return HTTP response headers as a Map */
    Map getHeaders() { throw new UnsupportedOperationException("AsyncResponse.getHeaders() is a compile-time stub only and cannot be used at runtime.") }
    /** @return a specific header value by name */
    String getHeader(String name) { throw new UnsupportedOperationException("AsyncResponse.getHeader(String) is a compile-time stub only and cannot be used at runtime.") }
    /** @return response body parsed as JSON into a Map/List */
    def getJson() { throw new UnsupportedOperationException("AsyncResponse.getJson() is a compile-time stub only and cannot be used at runtime.") }
    /** @return response body parsed as XML (GPathResult) */
    def getXml() { throw new UnsupportedOperationException("AsyncResponse.getXml() is a compile-time stub only and cannot be used at runtime.") }
    /** @return true if the response indicates an error (status >= 400) */
    boolean hasError() { throw new UnsupportedOperationException("AsyncResponse.hasError() is a compile-time stub only and cannot be used at runtime.") }
    /** @return the error message if hasError() is true */
    String getErrorMessage() { throw new UnsupportedOperationException("AsyncResponse.getErrorMessage() is a compile-time stub only and cannot be used at runtime.") }
    /** @return response body as raw bytes */
    byte[] getByteData() { throw new UnsupportedOperationException("AsyncResponse.getByteData() is a compile-time stub only and cannot be used at runtime.") }
}
