package com.instachat.android.util;

import org.apache.http.conn.ssl.AllowAllHostnameVerifier;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * 
 * A great alternative to the apache-commons and android-exteneded
 * HttpDefaultClient
 * 
 * We do not need to collect the certificates from the server to make SSL
 * requests.
 * 
 * That alone is reason enough to get away from all that crap.
 * 
 */
public final class HttpMessage {

	private static final String TAG = HttpMessage.class.getSimpleName();

	/**
	 * Constructs a new HttpMessage that can be used to communicate with the
	 * servlet at the specified URL.
	 * 
	 * @param url
	 *            the server resource (typically a servlet) with which to
	 *            communicate
	 */

	public HttpMessage(final String url) {
		try {
			this.url = new URL(url);
			if (url.startsWith("https://")) {
				isSecure = true;
			}
		} catch (final MalformedURLException e) {
		}
	}

	/**
	 * Designed for performance; converts the returned data into an int. Of
	 * course, the returned data better be an int!
	 * 
	 * @return
	 * @throws Exception
	 */
	public int getInt() throws Exception {
		final URLConnection urlConnection = openConnection();
		assignCommonAttributesToHttpURLConnection(urlConnection, true, false);
		((HttpURLConnection) urlConnection).setRequestMethod("GET");
		urlConnection.setRequestProperty("Content-Type", "text/plain;charset=utf-8");
		final BufferedReader is = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()), 32);
		final int someInt = Integer.parseInt(is.readLine());
		is.close();
		return someInt;
	}

	/**
	 * Could be receiving some potentially large returns, so make sure the
	 * return is gzipped
	 * 
	 * @return
	 * @throws Exception
	 */
	public String getString() throws Exception {
		final URLConnection urlConnection = openConnection();
		assignCommonAttributesToHttpURLConnection(urlConnection, true, false);
		((HttpURLConnection) urlConnection).setRequestMethod("GET");
		urlConnection.setRequestProperty("Content-Type", "text/plain;charset=utf-8");
		urlConnection.setRequestProperty("Accept-Encoding", "gzip");
		final InputStream is = urlConnection.getInputStream();

		final boolean isGzip = responseIsGzip(urlConnection);
		final InputStream ois = isGzip ? new GZIPInputStream(is) : is;

		final String string = getString(ois);
		is.close();
		return string;
	}

	public byte[] getBytes() throws Exception {
		final URLConnection urlConnection = openConnection();
		assignCommonAttributesToHttpURLConnection(urlConnection, true, false);
		((HttpURLConnection) urlConnection).setRequestMethod("GET");
		urlConnection.setRequestProperty("Content-Type", "application/octet-stream");
		final InputStream is = urlConnection.getInputStream();
		final byte[] bytes = getBytes(is);
		is.close();
		return bytes;
	}

	private byte[] getBytes(final InputStream is) throws IOException {

		int len = 0;
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		final byte[] buf = new byte[4096];
		while ((len = is.read(buf, 0, buf.length)) != -1)
			bos.write(buf, 0, len);
		return bos.toByteArray();
	}

	private URLConnection openConnection() throws IOException {
		initializeSSL();
		return isSecure ? openSecureConnection() : url.openConnection();
	}

	/**
	 * To open secure connection, use this
	 * 
	 * @return
	 * @throws Exception
	 */
	private URLConnection openSecureConnection() throws IOException {
		return (HttpsURLConnection) url.openConnection();
	}

	/**
	 * 
	 * Apparently, since this is not based on the HttpDefaultClient
	 * implementation we do not need to collect the certificates from the
	 * server.
	 */
	public static void setDefaultHostnameVerifier() throws Exception {

		// Install the all-trusting trust manager:
		final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			@SuppressWarnings("unused")
			public boolean isServerTrusted(X509Certificate[] certs) {
				return true;
			}

			@SuppressWarnings("unused")
			public boolean isClientTrusted(X509Certificate[] certs) {
				return true;
			}

			public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			}

			public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			}
		} };

		final SSLContext sc = SSLContext.getInstance("TLS");
		sc.init(null, trustAllCerts, new java.security.SecureRandom());

		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		HttpsURLConnection.setDefaultHostnameVerifier(new AllowAllHostnameVerifier());
	}

	public String post(final String name, final String value) throws Exception {
		final StringBuilder params = new StringBuilder();
		params.append(URLEncoder.encode(name, UTF8)).append('=').append(URLEncoder.encode(value, UTF8));
		return postData(params.toString().getBytes(UTF8), "POST");
	}

	private void assignCommonAttributesToHttpURLConnection(final URLConnection urlConnection, final boolean doInput, final boolean doOutput) {
		urlConnection.setUseCaches(false);

		/*
		 * the following lines mess stuff up a lot especially in later versions
		 * of android ics
		 */
		if (doInput)
			urlConnection.setDoInput(true);

		if (doOutput)
			urlConnection.setDoOutput(true);

		urlConnection.setConnectTimeout(15000);
		urlConnection.setReadTimeout(15000);
		// HttpURLConnection.setFollowRedirects(true);
		// /HttpURLConnection.setDefaultAllowUserInteraction(true);
		/*
		 * Note: if you set chunked to zero it will fail if you set chunked at
		 * all, a lot of stuff fails so dont set it
		 */
		// ((HttpURLConnection) urlConnection).setChunkedStreamingMode(0);

		// HttpURLConnection.setFollowRedirects(true);
		// HttpURLConnection.setDefaultAllowUserInteraction(true);
	}

	private String postData(final byte[] data, final String httpMethod) throws Exception {
		final URLConnection urlConnection = openConnection();
		assignCommonAttributesToHttpURLConnection(urlConnection, true, !httpMethod.equals("DELETE"));
		((HttpURLConnection) urlConnection).setRequestMethod(httpMethod);
		urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
		urlConnection.setRequestProperty("Content-Length", "" + data.length);
		urlConnection.setRequestProperty("Accept-Encoding", "gzip");

		if (!httpMethod.equals("DELETE")) {
			// Send post data request
			final DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
			wr.write(data);
			wr.flush();
			wr.close();
		}

		// Get Response
		final BufferedReader rd = responseIsGzip(urlConnection) ? new BufferedReader(new InputStreamReader(new GZIPInputStream(urlConnection.getInputStream()),
				UTF8), BUFFERED_READER_SIZE) : new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), UTF8), BUFFERED_READER_SIZE);

		final StringBuilder response = new StringBuilder();
		String line;
		while ((line = rd.readLine()) != null) {
			response.append(line);
		}
		rd.close();
		return response.toString();
	}

	public String post(final Map<String, String> formParams) throws Exception {

		final StringBuilder params = new StringBuilder();
		if (formParams != null) {

			for (final String key : formParams.keySet()) {
				params.append(URLEncoder.encode(key, UTF8)).append('=').append(URLEncoder.encode(formParams.get(key), UTF8)).append('&');
			}

			params.deleteCharAt(params.length() - 1);
		}
		return postData(params.toString().getBytes(UTF8), "POST");
	}

	public String delete() throws Exception {

		final StringBuilder params = new StringBuilder();
		return postData(params.toString().getBytes(UTF8), "DELETE");
	}

	public String post(final HashMap<String,String> paramMap) throws Exception {

		final StringBuilder params = new StringBuilder();
		if (paramMap != null) {

			for (final String param : paramMap.keySet()) {
				params.append(URLEncoder.encode(param, UTF8)).append('=').append(URLEncoder.encode(paramMap.get(param), UTF8)).append('&');
			}
			params.deleteCharAt(params.length() - 1);
		}
		return postData(params.toString().getBytes(UTF8), "POST");
	}

	private String getString(final InputStream is) throws Exception {

		final StringBuilder sb = new StringBuilder(16000);
		final byte buf[] = new byte[16000];
		int read = 0;
		while ((read = is.read(buf)) != -1) {
			sb.append(new String(buf, 0, read, UTF8));
		}
		return sb.toString();
	}

	public String postFile(final Map<String, String> formParams, final String fileParamName, final File file) throws Exception {
		return postBytes(formParams, fileParamName, LocalFileUtils.getBytesFromFile(file));
	}

	/**
	 * Posting stuff; doesn't need to be gzip optimized on the return since
	 * we're posting and just usually expecting some return code.
	 * 
	 * @param formParams
	 * @param bytesName
	 * @param bytes
	 * @return
	 * @throws Exception
	 */
	public String postBytes(final Map<String, String> formParams, final String bytesName, final byte[] bytes) throws Exception {
		final URLConnection urlConnection = openConnection();
		assignCommonAttributesToHttpURLConnection(urlConnection, false, true);
		((HttpURLConnection) urlConnection).setRequestMethod("POST");
		final MyHttpPost req = new MyHttpPost(urlConnection);
		req.setFileContentType("application/octet-stream");

		final Iterator<String> keys = formParams.keySet().iterator();
		while (keys.hasNext()) {
			final String key = keys.next();
			final String value = formParams.get(key);
			if (value == null) {
				continue;
			}
			req.setParameter(key, value);
		}

		req.setParameter("filepart", bytesName, bytes);

		final InputStream is = req.post();
		final BufferedReader br = new BufferedReader(new InputStreamReader(is), BUFFERED_READER_SIZE);
		final String line = br.readLine();
		is.close();
		return line;
	}

	private boolean responseIsGzip(final URLConnection urlConnection) {

		final Map<String, List<String>> responseHeaders = (Map<String, List<String>>) urlConnection.getHeaderFields();
		if (responseHeaders != null) {
			final Iterator<String> i = responseHeaders.keySet().iterator();
			while (i.hasNext()) {
				final String key = i.next();
				// System.out.println("header key = " + key);
				final List<String> values = responseHeaders.get(key);
				for (final String value : values) {
					// System.out.println("\t\theader value = " + value);
					if (value.equals("gzip")) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public static void initializeSSL() {
		if (_initializedSSL) {
			return;
		}
		try {
         setDefaultHostnameVerifier();
			_initializedSSL = true;
		} catch (final Exception e) {
			MLog.e(TAG, "", e);
		}
	}

	private URL url = null;
	private boolean isSecure = false;
	private static volatile boolean _initializedSSL = false;
	private static final String UTF8 = "UTF-8";
	private static final int BUFFERED_READER_SIZE = 8192;
}
