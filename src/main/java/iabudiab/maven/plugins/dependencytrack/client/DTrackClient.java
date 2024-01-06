package iabudiab.maven.plugins.dependencytrack.client;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.ObjectMapper;
import iabudiab.maven.plugins.dependencytrack.BomFormat;
import iabudiab.maven.plugins.dependencytrack.client.model.*;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.io.InputStreamFacade;

public class DTrackClient {

	private static final String DEPENDENCY_TRACK_API_KEY_HEADER = "X-Api-Key";
	private static final int DEFAULT_TIMEOUT = 30;

	// API Paths
	private static final String API_V1 = "/api/v1/";
	private static final String API_UPLOAD_SCAN = "scan";
	private static final String API_UPLOAD_BOM = "bom";
	private static final String API_DOWNLOAD_BOM = "bom/cyclonedx/project/";
	private static final String API_TOKEN_PROCESSING = "bom/token/";
	private static final String API_PROJECT = "project";
	private static final String API_PROJECT_LOOKUP = "project/lookup";
	private static final String API_PROJECT_FINDINGS = "finding/project/";
	private static final String API_PROJECT_METRICS = "metrics/project/";
	private static final String API_ANALYSIS = "analysis";

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final String dependencyTrackApiKey;
	private final Log log;
	private final CloseableHttpClient client;
	private final URI baseUri;

	private boolean logPayloads;

	public DTrackClient(String dependencyTrackUrl, String dependencyTrackApiKey, Log log) throws URISyntaxException {
		this.dependencyTrackApiKey = dependencyTrackApiKey;
		this.baseUri = new URI(dependencyTrackUrl).resolve(API_V1);
		this.log = log;
		this.logPayloads = false;

		RequestConfig config = RequestConfig.custom() //
			.setConnectTimeout(DEFAULT_TIMEOUT * 1000) //
			.setConnectionRequestTimeout(DEFAULT_TIMEOUT * 1000) //
			.setSocketTimeout(DEFAULT_TIMEOUT * 1000) //
			.build();

		this.client = HttpClients.custom() //
			.setDefaultRequestConfig(config) //
			.setDefaultHeaders(apiHeaders()) //
			.setRedirectStrategy(new LaxRedirectStrategy()) //
			.build();
		log.info("Using API v1 at: " + baseUri);
	}

	public void setLogPayloads(boolean logPayloads) {
		this.logPayloads = logPayloads;
	}

	public List<Header> apiHeaders() {
		Header contentType = new BasicHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
		Header apiKey = new BasicHeader(DEPENDENCY_TRACK_API_KEY_HEADER, dependencyTrackApiKey);
		List<Header> headersList = new ArrayList<>();
		headersList.add(contentType);
		headersList.add(apiKey);
		return headersList;
	}

	public void uploadAnalysis(Analysis payload) throws IOException {
		URI uri = baseUri.resolve(API_ANALYSIS);
		String payloadAsString = objectMapper.writeValueAsString(payload);
		HttpPut request = httpPut(uri, payloadAsString);
		log.info("Uploading analysis for project: " + payload.getProjectUuid()
			+ ", component=" + payload.getComponentUuid() + ", vulnerability=" + payload.getVulnerabilityUuid());
		if (logPayloads) {
			log.info("Analysis payload: ");
		}
		client.execute(request, responseBodyHandler());
	}

	public void uploadScan(ScanSubmitRequest payload) throws IOException {
		URI uri = baseUri.resolve(API_UPLOAD_SCAN);
		String payloadAsString = objectMapper.writeValueAsString(payload);
		HttpPut request = httpPut(uri, payloadAsString);
		log.info("Uploading scan artifact to: " + uri);
		client.execute(request, responseBodyHandler());
	}

	public TokenResponse uploadBom(BomSubmitRequest payload) throws IOException {
		URI uri = baseUri.resolve(API_UPLOAD_BOM);
		String payloadAsString = objectMapper.writeValueAsString(payload);
		HttpPut request = httpPut(uri, payloadAsString);
		log.info("Uploading bom artifact to: " + uri);
		TokenResponse response = client.execute(request, responseBodyHandler(TokenResponse.class));
		log.info("BOM response token: " + response.getToken());
		return response;
	}

	public File downloadBom(UUID projectId, Path destinationPath, BomFormat format) throws IOException {
		URI uri = baseUri.resolve(API_DOWNLOAD_BOM + projectId.toString() + "?format=" + format);
		HttpGet request = httpGet(uri);
		return client.execute(request, downloadResponseHandler(destinationPath.toFile()));
	}

	public TokenProcessedResponse checkIfTokenIsBeingProcessed(UUID token) throws IOException {
		URI uri = baseUri.resolve(API_TOKEN_PROCESSING + token.toString());
		HttpGet request = httpGet(uri);
		return client.execute(request, responseBodyHandler(TokenProcessedResponse.class));
	}

	public CompletableFuture<Boolean> pollTokenProcessing(UUID token, Executor executor) {
		Supplier<Boolean> checkToken = () -> {
			try {
				log.info("Polling token [" + Instant.now() + "]: " + token);
				return checkIfTokenIsBeingProcessed(token).isProcessing();
			} catch (Exception e) {
				throw new CompletionException("Error during token polling", e);
			}
		};

		return CompletableFuture.supplyAsync(checkToken, executor) //
			.thenCompose(isProcessing -> {
				if (isProcessing) {
					try {
						log.info("Token is still being processed, will retry in 5 seconds");
						return pollTokenProcessing(token,
							CompletableFutureUtils.delayedExecutor(5, TimeUnit.SECONDS));
					} catch (Exception e) {
						throw new CompletionException("Error during token polling", e);
					}
				}
				return CompletableFuture.completedFuture(isProcessing);
			});
	}

	public Project getProject(String name) throws IOException {
		URI uri = baseUri.resolve(API_PROJECT + "?name=" + name);
		HttpGet request = httpGet(uri);
		return client.execute(request, responseBodyHandler(Project.class));
	}

	public Project getProject(String name, String version) throws IOException {
		URI uri = baseUri.resolve(API_PROJECT_LOOKUP + "?name=" + name + "&version=" + version);
		HttpGet request = httpGet(uri);
		return client.execute(request, responseBodyHandler(Project.class));
	}

	public List<Finding> getProjectFindings(UUID projectId) throws IOException {
		URI uri = baseUri.resolve(API_PROJECT_FINDINGS + projectId.toString() + "?suppressed=true");
		log.debug("Invoking uri => " + uri);
		HttpGet request = httpGet(uri);
		Finding[] findings = client.execute(request, responseBodyHandler(Finding[].class));
		return Arrays.asList(findings);
	}

	public ProjectMetrics getProjectMetrics(UUID projectId, int retryDelay, int retryLimit) throws IOException {
		if (retryLimit <= 0) return getProjectMetrics(projectId);
		if (retryDelay < 0) throw new IllegalArgumentException("Project metrics retry delay must be >= 0");

		Supplier<ProjectMetrics> projectMetricsSupplier = () -> {
			ProjectMetrics metrics = null;
			try {
				metrics = getProjectMetrics(projectId);
			} catch (Exception ex) {
				log.warn("Got exception while obtaining project metrics for project '" + projectId + "'", ex);
				return null;
			}
			return metrics;
		};
		return CompletableFutureUtils.retry(projectMetricsSupplier, Objects::isNull, retryDelay, 0, retryLimit, log).join();
	}

	public ProjectMetrics getProjectMetrics(UUID projectId) throws IOException {
		URI uri = baseUri.resolve(API_PROJECT_METRICS + projectId.toString() + "/current");
		log.debug("Invoking uri => " + uri);
		HttpGet request = httpGet(uri);
		return client.execute(request, responseBodyHandler(ProjectMetrics.class));
	}

	private <R> ResponseHandler<R> responseBodyHandler(final Class<R> responseType) {
		return response -> {
			processResponseStatus(response);
			String responseString = EntityUtils.toString(response.getEntity());
			if (responseString != null) {
				if (logPayloads) {
					log.info("Response string " + responseString);
				}
				return objectMapper.readValue(responseString, responseType);
			} else {
				log.warn("Unable to find response string, returning null ");
				return null;
			}
		};
	}

	private <R> ResponseHandler<R> responseBodyHandler() {
		return response -> {
			processResponseStatus(response);
			return null;
		};
	}

	private ResponseHandler<File> downloadResponseHandler(File target) {
		return response -> {
			InputStreamFacade source = () -> response.getEntity().getContent();
			FileUtils.copyStreamToFile(source, target);
			return target;
		};
	}

	private void processResponseStatus(HttpResponse response) throws HttpResponseException {
		StatusLine statusLine = response.getStatusLine();
		logResponseCode(statusLine.getStatusCode());
		if (statusLine.getStatusCode() >= 300) {
			handleNonSuccessCode(response);
		}
	}

	private void handleNonSuccessCode(HttpResponse response) throws HttpResponseException {
		StatusLine statusLine = response.getStatusLine();
		String detail;
		try {
			String body = EntityUtils.toString(response.getEntity());
			detail = String.format("[%s] %s", statusLine.getReasonPhrase(), body);
		} catch (Exception e) {
			detail = statusLine.getReasonPhrase();
		}
		throw new HttpResponseException(statusLine.getStatusCode(), detail);
	}

	private void logResponseCode(int statusCode) {
		switch (statusCode) {
			case 200:
				log.debug("Request successful");
				break;
			case 400:
				log.error("Bad request. Probably an error in the plugin itself.");
				break;
			case 401:
				log.error("Unauthenticated. Check your API Key");
				break;
			case 403:
				log.error("Unauthorized. Check the permissions of the provided API Key. "
					+ "Required are: SCAN_UPLOAD and either PROJECT_CREATION_UPLOAD or PORTFOLIO_MANAGEMENT");
				break;
			default:
				log.warn("Received status code: " + statusCode);
				break;
		}
	}

	private HttpPut httpPut(URI uri, String body) {
		HttpPut request = new HttpPut();
		request.setURI(uri);
		request.setEntity(EntityBuilder.create().setText(body).build());
		return request;
	}

	private HttpGet httpGet(URI uri) {
		HttpGet request = new HttpGet();
		request.setURI(uri);
		return request;
	}
}
