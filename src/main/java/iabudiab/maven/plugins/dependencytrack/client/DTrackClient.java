package iabudiab.maven.plugins.dependencytrack.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
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
import iabudiab.maven.plugins.dependencytrack.client.model.*;

public class DTrackClient {

	private final String dependencyTrackApiKey;
	private final Log log;
	private final CloseableHttpClient client;
	private final URI baseUri;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final int timeout = 30;
	private final String DEPENDENCY_TRACK_API_KEY_HEADER = "X-Api-Key";

	// API Paths
	private final String dependencyTrackPathPrefix = "/api/v1/";
	private final String apiPathUploadScan = "scan";
	private final String apiPathUploadBom = "bom";
	private final String apiPathTokenProcessing = "bom/token/";
	private final String apiPathGetProject = "project/lookup";
	private final String apiPathGetProjectFindings = "finding/project/";
	private final String apiPathGetProjectMetrics = "metrics/project/";

	public DTrackClient(String dependencyTrackUrl, String dependencyTrackApiKey, Log log) throws URISyntaxException {
		this.dependencyTrackApiKey = dependencyTrackApiKey;
		this.log = log;

		RequestConfig config = RequestConfig.custom()
				.setConnectTimeout(timeout * 1000)
				.setConnectionRequestTimeout(timeout * 1000)
				.setSocketTimeout(timeout * 1000).build();

		CloseableHttpClient httpClient = HttpClients.custom().
				setDefaultRequestConfig(config).
				setDefaultHeaders(apiHeaders()).
				setRedirectStrategy(new LaxRedirectStrategy()).
				build();
		this.client = httpClient;

		this.baseUri = new URI(dependencyTrackUrl).resolve(dependencyTrackPathPrefix);
		log.info("Using API v1 at: " + baseUri);
	}

	public List<Header> apiHeaders() {
		Header contentType = new BasicHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString());
		Header apiKey = new BasicHeader(DEPENDENCY_TRACK_API_KEY_HEADER, dependencyTrackApiKey);
		List headersList = new ArrayList();
		headersList.add(contentType);
		headersList.add(apiKey);
		return headersList;
	}

	public void uploadScan(ScanSubmitRequest payload) throws IOException{
		URI uri = baseUri.resolve(apiPathUploadScan);
		String payloadAsString = objectMapper.writeValueAsString(payload);
		HttpPut request = httpPut(uri, payloadAsString);
		log.info("Uploading scan artifact to: {}" + uri);
		client.execute(request,responseBodyHandler());
	}


	public TokenResponse uploadBom(BomSubmitRequest payload) throws IOException {
		URI uri = baseUri.resolve(apiPathUploadBom);
		String payloadAsString = objectMapper.writeValueAsString(payload);
		HttpPut request = httpPut(uri, payloadAsString);
		log.info("Uploading bom artifact to: " + uri);
		TokenResponse response = client.execute(request,responseBodyHandler(TokenResponse.class));
		log.info("BOM response token: " + response.getToken());
		return response;
	}

	public TokenProcessedResponse checkIfTokenIsBeingProcessed(UUID token) throws IOException{
		URI uri = baseUri.resolve(apiPathTokenProcessing + token.toString());
		HttpGet request = httpGet(uri);
		TokenProcessedResponse response = client.execute(request,responseBodyHandler(TokenProcessedResponse.class));
		return response;
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

		CompletableFuture<Boolean> result = CompletableFuture.supplyAsync(checkToken, executor) //
				.thenCompose(isProcessing -> {
					if (isProcessing) {
						try {
							log.info("Token is still being processed, will retry in 5 seconds");
							Executor delayedExecutor = Executors.newSingleThreadScheduledExecutor();
							((ScheduledExecutorService) delayedExecutor).schedule(() -> {}, 5, TimeUnit.SECONDS);
							return pollTokenProcessing(token, delayedExecutor);
						} catch (Exception e) {
							throw new CompletionException("Error during token polling", e);
						}
					}
					return CompletableFuture.completedFuture(isProcessing);
				});

		return result;
	}

	public Project getProject(String name, String version) throws IOException {
		URI uri = baseUri.resolve(apiPathGetProject + "?name=" + name + "&version=" + version);
		HttpGet request = httpGet(uri);
		Project response = client.execute(request,responseBodyHandler(Project.class));
		return response;
	}

	public List<Finding> getProjectFindinds(UUID projectId) throws IOException {
		URI uri = baseUri.resolve(apiPathGetProjectFindings + projectId.toString());
		log.debug("Invoking uri => " + uri.toString());
		HttpGet request = httpGet(uri);
		Finding[] findings = client.execute(request,responseBodyHandler(Finding[].class));
		return Arrays.asList(findings);
	}

	public ProjectMetrics getProjectMetrics(UUID projectId) throws IOException {
		URI uri = baseUri.resolve(apiPathGetProjectMetrics + projectId.toString() + "/current");
		log.debug("Invoking uri => " + uri.toString());
		HttpGet request = httpGet(uri);
		ProjectMetrics response = client.execute(request,responseBodyHandler(ProjectMetrics.class));
		return response;
	}

	private <R> ResponseHandler<R> responseBodyHandler(
			final Class<R> responseType) {
		return response -> {
			processResponseStatus(response);
			String responseString = EntityUtils.toString(response.getEntity());
			if(responseString != null) {
        log.info("Response string " + responseString);
				return objectMapper.readValue(responseString, responseType);
			}else {
				log.warn("Unable to find response string, returning null ");
				return null;
			}
		};
	}

	private <R> ResponseHandler<R>  responseBodyHandler() {
		return response -> {
			processResponseStatus(response);
			return null;
		};
	}

	private void processResponseStatus(HttpResponse response) throws HttpResponseException{
		StatusLine statusLine = response.getStatusLine();
		logResponseCode(statusLine.getStatusCode());
		if (statusLine.getStatusCode() >= 300) {
			handleNonSuccessCode(response);
		}
	}

	private String handleNonSuccessCode(HttpResponse response) throws HttpResponseException {
		StatusLine statusLine = response.getStatusLine();
			String detail;
			try {
				String body = EntityUtils.toString(response.getEntity());
				detail = String.format("[%s] %s",
						statusLine.getReasonPhrase(), body);
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

	public HttpPut httpPut(URI uri, String body) {
		HttpPut request = new HttpPut();
		request.setURI(uri);
		request.setEntity(EntityBuilder.create().setText(body).build());
		return request;
	}

	public HttpGet httpGet(URI uri) {
		HttpGet request = new HttpGet();
		request.setURI(uri);
		return request;
	}
}
