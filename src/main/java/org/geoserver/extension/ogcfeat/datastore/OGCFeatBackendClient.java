package org.geoserver.extension.ogcfeat.datastore;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.geotools.http.HTTPConnectionPooling;
import org.geotools.http.HTTPProxy;
import org.geotools.http.HTTPResponse;
import org.geotools.util.factory.GeoTools;
import org.geotools.util.logging.Logging;

/*
 * Status:
 * This exists to try to solve issues with backend authentication - with limited success
 * Initial auth not working at the moment and not reaching this code at all?
 * 
 */
public class OGCFeatBackendClient implements HTTPConnectionPooling, HTTPProxy {

	protected static final Logger LOGGER = Logging.getLogger(OGCFeatBackendClient.class.getName());

	private final PoolingHttpClientConnectionManager connectionManager;

	private HttpClient client;

	private RequestConfig connectionConfig;

	private String acceptHeaderValue;
	private String username;
	private String password;

	public OGCFeatBackendClient(String user, String pass, int poolMax, int timeoutInMs, String accept) {
		acceptHeaderValue = accept;
		username = user;
		password = pass;
		connectionManager = new PoolingHttpClientConnectionManager();
		connectionManager.setMaxTotal(poolMax);
		connectionManager.setDefaultMaxPerRoute(poolMax);
		connectionConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.DEFAULT).setExpectContinueEnabled(true)
				.setSocketTimeout(timeoutInMs).setConnectionRequestTimeout(timeoutInMs).setConnectTimeout(timeoutInMs)
				.build();

		client = builder().build();
	}

	private HttpClientBuilder builder() {
		HttpClientBuilder builder = HttpClientBuilder.create()
				.setUserAgent(String.format("OGCFeat/%s (%s)", GeoTools.getVersion(), this.getClass().getSimpleName()))
				.useSystemProperties().setConnectionManager(connectionManager);
		return builder;
	}

	public HttpMethodResponse post(final URL url, final InputStream postContent, final String postContentType)
			throws IOException {

		throw new IOException("Not supported");
	}

	public HTTPResponse get(final URL url) throws IOException {
		LOGGER.info("HTTP CLIENT GET " + url);
		return this.get(url, null);
	}

	public HTTPResponse get(URL url, Map<String, String> headers) throws IOException {
		LOGGER.info("HTTP CLIENT GET " + url);

		HttpGet method = new HttpGet(url.toExternalForm());
		method.setConfig(connectionConfig);
		method.setHeader("Accept-Encoding", "gzip");
		method.setHeader("Accept", acceptHeaderValue);
		if (headers != null) {
			for (Map.Entry<String, String> headerNameValue : headers.entrySet()) {
				method.setHeader(headerNameValue.getKey(), headerNameValue.getValue());
			}
		}

		HttpMethodResponse response = null;

		HttpResponse resp;
		if (username != null) {
			// https://stackoverflow.com/questions/20914311/httpclientbuilder-basic-auth/21592593#21592593
			HttpHost targetHost = new HttpHost(method.getURI().getHost(), method.getURI().getPort(),
					method.getURI().getScheme());
			BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
			credsProvider.setCredentials(new AuthScope(targetHost.getHostName(), targetHost.getPort()),
					new UsernamePasswordCredentials(username, password));

			AuthCache authCache = new BasicAuthCache();
			BasicScheme basicAuth = new BasicScheme();
			authCache.put(targetHost, basicAuth);

			HttpClientContext context = HttpClientContext.create();
			context.setCredentialsProvider(credsProvider);
			context.setAuthCache(authCache);
			resp = client.execute(method, context);
		} else {
			resp = client.execute(method);
		}

		response = new HttpMethodResponse(resp);

		if (200 != response.getStatusCode()) {
			method.releaseConnection();
			throw new IOException(
					"Server returned HTTP error code " + response.getStatusCode() + " for URL " + url.toExternalForm());
		}
		return response;
	}

	@Override
	public void close() {
		this.connectionManager.shutdown();
	}

	static class HttpMethodResponse implements HTTPResponse {

		private org.apache.http.HttpResponse methodResponse;

		private InputStream responseBodyAsStream;

		public HttpMethodResponse(final org.apache.http.HttpResponse methodResponse) {
			this.methodResponse = methodResponse;
		}

		/** @return */
		public int getStatusCode() {
			if (methodResponse != null) {
				StatusLine statusLine = methodResponse.getStatusLine();
				return statusLine.getStatusCode();
			} else {
				return -1;
			}
		}

		@Override
		public void dispose() {
			if (responseBodyAsStream != null) {
				try {
					responseBodyAsStream.close();
				} catch (IOException e) {
					// ignore
				}
			}

			if (methodResponse != null) {
				methodResponse = null;
			}
		}

		@Override
		public String getContentType() {
			return getResponseHeader("Content-Type");
		}

		@Override
		public String getResponseHeader(final String headerName) {
			Header responseHeader = methodResponse.getFirstHeader(headerName);
			return responseHeader == null ? null : responseHeader.getValue();
		}

		@Override
		public InputStream getResponseStream() throws IOException {
			if (responseBodyAsStream == null) {
				responseBodyAsStream = methodResponse.getEntity().getContent();
				// commons httpclient does not handle gzip encoding automatically, we have to
				// check
				// ourselves: https://issues.apache.org/jira/browse/HTTPCLIENT-816
				Header header = methodResponse.getFirstHeader("Content-Encoding");
				if (header != null && "gzip".equals(header.getValue())) {
					responseBodyAsStream = new GZIPInputStream(responseBodyAsStream);
				}
			}
			return responseBodyAsStream;
		}

		/** @see org.geotools.data.ows.HTTPResponse#getResponseCharset() */
		@Override
		public String getResponseCharset() {
			final Header encoding = methodResponse.getEntity().getContentEncoding();
			return encoding == null ? null : encoding.getValue();
		}
	}

	@Override
	public int getMaxConnections() {
		return connectionManager.getMaxTotal();
	}

	@Override
	public void setMaxConnections(int maxConnections) {
		connectionManager.setMaxTotal(maxConnections);
		connectionManager.setDefaultMaxPerRoute(maxConnections);
	}
}
