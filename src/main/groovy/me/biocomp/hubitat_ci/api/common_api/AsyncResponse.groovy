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
