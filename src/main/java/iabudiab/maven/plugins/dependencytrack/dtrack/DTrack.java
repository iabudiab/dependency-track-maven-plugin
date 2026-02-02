package iabudiab.maven.plugins.dependencytrack.dtrack;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.http.client.HttpResponseException;

import iabudiab.maven.plugins.dependencytrack.client.DTrackClient;
import iabudiab.maven.plugins.dependencytrack.client.model.Analysis;
import iabudiab.maven.plugins.dependencytrack.client.model.AnalysisJustification;
import iabudiab.maven.plugins.dependencytrack.client.model.AnalysisResponse;
import iabudiab.maven.plugins.dependencytrack.client.model.BomSubmitRequest;
import iabudiab.maven.plugins.dependencytrack.client.model.CollectionLogic;
import iabudiab.maven.plugins.dependencytrack.client.model.Finding;
import iabudiab.maven.plugins.dependencytrack.client.model.Project;
import iabudiab.maven.plugins.dependencytrack.client.model.ProjectMetrics;
import iabudiab.maven.plugins.dependencytrack.client.model.ScanSubmitRequest;
import iabudiab.maven.plugins.dependencytrack.client.model.State;
import iabudiab.maven.plugins.dependencytrack.client.model.Tag;
import iabudiab.maven.plugins.dependencytrack.client.model.TokenResponse;
import iabudiab.maven.plugins.dependencytrack.cyclone.BomFormat;
import iabudiab.maven.plugins.dependencytrack.suppressions.Suppression;
import iabudiab.maven.plugins.dependencytrack.suppressions.Suppressions;
import org.apache.maven.plugin.logging.Log;

/**
 * Core class for interacting with Dependency-Track server.
 * <p>
 * This class provides functionality for uploading and downloading BOMs,
 * finding and creating projects, applying parent projects, polling tokens,
 * loading findings and project metrics, and applying suppressions.
 * </p>
 */
public class DTrack {

	private final DTrackClient client;
	private final Suppressions suppressions;
	private final String projectName;
	private final String projectVersion;
	private final Log log;

	private Project project;
	private List<Finding> findings;
	private ProjectMetrics projectMetrics;

	/**
	 * Constructs a new DTrack instance.
	 *
	 * @param client         The DTrackClient used to communicate with the Dependency-Track server
	 * @param suppressions   The suppressions to apply to findings
	 * @param projectName    The name of the project in Dependency-Track
	 * @param projectVersion The version of the project in Dependency-Track
	 */
	public DTrack(
		DTrackClient client,
		Suppressions suppressions,
		String projectName,
		String projectVersion,
		Log log
	) {
		this.client = client;
		this.suppressions = suppressions;
		this.projectName = projectName;
		this.projectVersion = projectVersion;
		this.log = log;
	}

	/**
	 * Gets the suppressions configured for this DTrack instance.
	 *
	 * @return The suppressions configured for this DTrack instance
	 */
	public Suppressions getSuppressions() {
		return suppressions;
	}

	/**
	 * Uploads a scan to Dependency-Track.
	 * <p>
	 * The scan is uploaded for the project specified in the constructor.
	 * If the project doesn't exist, it will be created automatically.
	 * </p>
	 *
	 * @param path The path to the scan file to upload
	 * @throws DTrackException If an error occurs during the upload process
	 */
	public void uploadScan(Path path) {
		log.info("Uploading scan for project: " + projectName + ":" + projectVersion);
		log.debug("Scan file path: " + path);

		String encodeArtifact = loadAndEncodeArtifactFile(path);

		ScanSubmitRequest payload = ScanSubmitRequest.builder() //
			.projectName(projectName) //
			.projectVersion(projectVersion) //
			.scan(encodeArtifact) //
			.autoCreate(true) //
			.build();

		try {
			client.uploadScan(payload);
			log.info("Successfully uploaded scan for project: " + projectName + ":" + projectVersion);
		} catch (HttpResponseException e) {
			log.error("Failed to upload scan: " + e.getMessage());
			throw handleCommonErrors(e);
		} catch (IOException e) {
			log.error("Error uploading scan: " + e.getMessage());
			throw new DTrackException("Error uploading scan: ", e);
		}
	}

	/**
	 * Uploads a BOM to Dependency-Track.
	 * <p>
	 * The BOM is uploaded for the project specified in the constructor.
	 * If the project doesn't exist, it will be created automatically.
	 * </p>
	 *
	 * @param path The path to the BOM file to upload
	 * @return A TokenResponse containing the token for the uploaded BOM
	 * @throws DTrackException If an error occurs during the upload process
	 */
	public TokenResponse uploadBom(Path path) throws DTrackException {
		log.info("Uploading BOM for project: " + projectName + ":" + projectVersion);
		log.debug("BOM file path: " + path);

		String encodeArtifact = loadAndEncodeArtifactFile(path);

		BomSubmitRequest payload = BomSubmitRequest.builder() //
			.projectName(projectName) //
			.projectVersion(projectVersion) //
			.bom(encodeArtifact) //
			.autoCreate(true) //
			.build();

		TokenResponse response = null;
		try {
			response = client.uploadBom(payload);
			log.info("Successfully uploaded BOM for project: " + projectName + ":" + projectVersion);
			log.debug("BOM token: " + response.getToken());
		} catch (HttpResponseException e) {
			log.error("Failed to upload BOM: " + e.getMessage());
			throw handleCommonErrors(e);
		} catch (IOException e) {
			log.error("Error uploading BOM: " + e.getMessage());
			throw new DTrackException("Error uploading bom: ", e);
		}

		return response;
	}

	/**
	 * Downloads a BOM from Dependency-Track.
	 * <p>
	 * The BOM is downloaded for the project specified in the constructor.
	 * </p>
	 *
	 * @param path The path where the downloaded BOM will be saved
	 * @param outputFormat The format of the BOM to download (XML or JSON)
	 * @return The downloaded BOM file
	 * @throws DTrackException If an error occurs during the download process or if the project doesn't exist
	 */
	public File downloadBom(Path path, BomFormat outputFormat) {
		log.info("Downloading BOM for project: " + projectName + ":" + projectVersion);
		log.debug("BOM output format: " + outputFormat);
		log.debug("BOM destination path: " + path);

		if (project == null) {
			log.debug("Project not loaded, loading project");
			loadProject();
		}

		try {
			File downloadedFile = client.downloadBom(project.getUuid(), path, outputFormat);
			log.info("Successfully downloaded BOM to: " + downloadedFile.getAbsolutePath());
			return downloadedFile;
		} catch (HttpResponseException e) {
			log.error("Failed to download BOM: " + e.getMessage());
			throw handleCommonErrors(e);
		} catch (IOException e) {
			log.error("Error downloading BOM: " + e.getMessage());
			throw new DTrackException("Error downloading BOM: ", e);
		}
	}

	/**
	 * Finds a project in Dependency-Track by its UUID.
	 *
	 * @param uuid The UUID of the project to find
	 * @return The project if found, null otherwise
	 * @throws DTrackException If an error occurs during the request
	 */
	public Project findProject(UUID uuid) throws DTrackException {
		log.debug("Finding project with UUID: " + uuid);
		try {
			Project project = client.getProject(uuid);
			if (project != null) {
				log.debug("Found project: " + project.getName() + ":" + project.getVersion());
			} else {
				log.debug("Project with UUID " + uuid + " not found");
			}
			return project;
		} catch (HttpResponseException e) {
			if(e.getStatusCode() == 404) {
				log.debug("Project with UUID " + uuid + " not found");
				return null;
			}
			else {
				log.error("Failed to find project: " + e.getMessage());
				throw handleCommonErrors(e);
			}
		} catch (IOException e) {
			log.error("Error loading project: " + e.getMessage());
			throw new DTrackException("Error loading project: ", e);
		}
	}

	/**
	 * Finds a project in Dependency-Track by its name and version.
	 *
	 * @param projectName The name of the project to find
	 * @param projectVersion The version of the project to find
	 * @return The project if found, null otherwise
	 * @throws DTrackException If an error occurs during the request
	 */
	public Project findProject(String projectName, String projectVersion) throws DTrackException {
		log.debug("Finding project: " + projectName + ":" + projectVersion);
		try {
			Project project = client.getProject(projectName, projectVersion);
			if (project != null) {
				log.debug("Found project: " + projectName + ":" + projectVersion);
			} else {
				log.debug("Project not found: " + projectName + ":" + projectVersion);
			}
			return project;
		} catch (HttpResponseException e) {
			if(e.getStatusCode() == 404) {
				log.debug("Project not found: " + projectName + ":" + projectVersion);
				return null;
			}
			else {
				log.error("Failed to find project: " + e.getMessage());
				throw handleCommonErrors(e);
			}
		} catch (IOException e) {
			log.error("Error loading project: " + e.getMessage());
			throw new DTrackException("Error loading project: ", e);
		}
	}

	/**
	 * Loads the project specified in the constructor.
	 * <p>
	 * This method is called internally by other methods that require the project to be loaded.
	 * </p>
	 *
	 * @throws DTrackNotFoundException If the project doesn't exist
	 * @throws DTrackException If an error occurs during the request
	 */
	private void loadProject() throws DTrackException {
		log.info("Loading project: " + projectName + ":" + projectVersion);
		this.project = findProject(projectName, projectVersion);
		if (this.project == null) {
			log.error("Project not found: " + projectName + ":" + projectVersion);
			throw new DTrackNotFoundException("Project not found: " + projectName + ":" + projectVersion);
		}
		log.debug("Successfully loaded project: " + projectName + ":" + projectVersion);
	}

	/**
	 * Creates a new project in Dependency-Track.
	 *
	 * @param projectName The name of the project to create
	 * @param projectVersion The version of the project to create
	 * @return The created project
	 * @throws DTrackException If an error occurs during the request
	 */
	public Project createProject(String projectName, String projectVersion) throws DTrackException {
		log.info("Creating project: " + projectName + ":" + projectVersion);
		return createProject(projectName, projectVersion, null, null);
	}

	/**
	 * Creates a new project in Dependency-Track with collection logic and tag.
	 *
	 * @param projectName The name of the project to create
	 * @param projectVersion The version of the project to create
	 * @param collectionLogic The collection logic to apply to the project
	 * @param collectionTag The collection tag to apply to the project
	 * @return The created project
	 * @throws DTrackException If an error occurs during the request
	 */
	public Project createProject(String projectName, String projectVersion, CollectionLogic collectionLogic, Tag collectionTag) throws DTrackException {
		log.info("Creating project: " + projectName + ":" + projectVersion + 
			(collectionLogic != null ? " with collection logic: " + collectionLogic : "") +
			(collectionTag != null ? " and collection tag: " + collectionTag.getName() : ""));
		try {
			Project project = client.createProject(projectName, projectVersion, collectionLogic, collectionTag);
			log.info("Successfully created project: " + projectName + ":" + projectVersion);
			return project;
		} catch (HttpResponseException e) {
			log.error("Failed to create project: " + e.getMessage());
			throw handleCommonErrors(e);
		} catch (IOException e) {
			log.error("Error creating project: " + e.getMessage());
			throw new DTrackException("Error loading project: ", e);
		}
	}

	/**
	 * Applies a parent project to a child project in Dependency-Track.
	 *
	 * @param project The child project to which the parent will be applied
	 * @param parentProject The parent project to apply
	 * @return The updated child project
	 * @throws DTrackException If an error occurs during the request
	 */
	public Project applyParentProject(Project project, Project parentProject) {
		log.info("Applying parent project " + parentProject.getName() + ":" + parentProject.getVersion() + 
			" to project " + project.getName() + ":" + project.getVersion());
		try {
			Project updatedProject = client.applyProjectParent(project, parentProject);
			log.info("Successfully applied parent project");
			return updatedProject;
		} catch (HttpResponseException e) {
			log.error("Failed to apply parent project: " + e.getMessage());
			throw handleCommonErrors(e);
		} catch (IOException e) {
			log.error("Error applying parent project: " + e.getMessage());
			throw new DTrackException("Error applying parent: ", e);
		}
	}

	/**
	 * Applies collection logic and tag to a project in Dependency-Track.
	 *
	 * @param project The project to which the collection logic will be applied
	 * @param collectionLogic The collection logic to apply
	 * @param collectionTag The collection tag to apply
	 * @return The updated project
	 * @throws DTrackException If an error occurs during the request
	 */
	public Project applyCollectionLogic(Project project, CollectionLogic collectionLogic, String collectionTag) {
		log.info("Applying collection logic " + collectionLogic + 
			(collectionTag != null ? " with tag: " + collectionTag : "") + 
			" to project " + project.getName() + ":" + project.getVersion());
		try {
			Project updatedProject = client.applyCollectionLogic(project, collectionLogic, collectionTag);
			log.info("Successfully applied collection logic to project");
			return updatedProject;
		} catch (HttpResponseException e) {
			log.error("Failed to apply collection logic: " + e.getMessage());
			throw handleCommonErrors(e);
		} catch (IOException e) {
			log.error("Error applying collection logic: " + e.getMessage());
			throw new DTrackException("Error applying collection logic: ", e);
		}
	}

	public Project applyActive(UUID projectUuid, boolean active, CollectionLogic collectionLogic, Tag collectionTag) {
		try {
			Project updatedProject = client.patchProjectActive(projectUuid, active, collectionLogic, collectionTag);
			log.info(String.format("Successfully applied active='%s' to project (uuid='%s')", active, projectUuid));
			return updatedProject;
		} catch (HttpResponseException e) {
			log.error(String.format("Failed to apply active='%s' to project (uuid='%s'): %s", active, projectUuid, e.getMessage()), e);
			throw handleCommonErrors(e);
		} catch (IOException e) {
			log.error(String.format("Failed to apply active='%s' to project (uuid='%s'): %s", active, projectUuid, e.getMessage()), e);
			throw new DTrackException("Error applying active='"+ active +"' to project (uuid='"+ projectUuid +"')!", e);
		}
	}


	/**
	 * Polls a token for processing status in Dependency-Track.
	 * <p>
	 * This method waits for the token to be processed for the specified duration.
	 * </p>
	 *
	 * @param token The token to poll
	 * @param durationSeconds The maximum duration to wait for the token to be processed, in seconds
	 * @return true if the token was processed successfully, false otherwise
	 * @throws DTrackException If an error occurs during the polling process
	 */
	public boolean pollToken(UUID token, int durationSeconds) {
		log.info("Polling token: " + token + " with timeout: " + durationSeconds + " seconds");
		try { 
			boolean result = client.pollTokenProcessing(token, ForkJoinPool.commonPool())
				.get(durationSeconds, TimeUnit.SECONDS);
			if (result) {
				log.info("Token processing completed successfully: " + token);
			} else {
				log.warn("Token processing did not complete successfully: " + token);
			}
			return result;
		} catch (TimeoutException e) {
			log.error("Timeout while polling token: " + token);
			Thread.currentThread().interrupt();
			throw new DTrackException("Timeout polling token: ", e);
		} catch (InterruptedException | ExecutionException e) {
			log.error("Error polling token: " + e.getMessage());
			Thread.currentThread().interrupt();
			throw new DTrackException("Error polling token: ", e);
		}
	}

	/**
	 * Loads the findings for the project specified in the constructor.
	 * <p>
	 * If the project hasn't been loaded yet, it will be loaded first.
	 * </p>
	 *
	 * @return The list of findings for the project
	 * @throws DTrackException If an error occurs during the request or if the project doesn't exist
	 */
	public List<Finding> loadFindings() throws DTrackException {
		log.info("Loading findings for project: " + projectName + ":" + projectVersion);

		if (project == null) {
			log.debug("Project not loaded, loading project");
			loadProject();
		}

		try {
			this.findings = client.getProjectFindings(project.getUuid());
			log.info("Successfully loaded " + findings.size() + " findings for project: " + projectName + ":" + projectVersion);
			return findings;
		} catch (HttpResponseException e) {
			log.error("Failed to load findings: " + e.getMessage());
			throw handleCommonErrors(e);
		} catch (IOException e) {
			log.error("Error loading project findings: " + e.getMessage());
			throw new DTrackException("Error loading project findings: ", e);
		}
	}

	/**
	 * Loads the metrics for the project specified in the constructor.
	 * <p>
	 * If the project hasn't been loaded yet, it will be loaded first.
	 * </p>
	 *
	 * @return The metrics for the project
	 * @throws DTrackException If an error occurs during the request or if the project doesn't exist
	 */
	public ProjectMetrics loadProjectMetrics() throws DTrackException {
		log.info("Loading metrics for project: " + projectName + ":" + projectVersion);

		if (project == null) {
			log.debug("Project not loaded, loading project");
			loadProject();
		}

		try {
			this.projectMetrics = client.getProjectMetrics(project.getUuid());
			log.info("Successfully loaded metrics for project: " + projectName + ":" + projectVersion);
		} catch (HttpResponseException e) {
			log.error("Failed to load project metrics: " + e.getMessage());
			throw handleCommonErrors(e);
		} catch (Exception e) {
			log.error("Error fetching project metrics: " + e.getMessage());
			throw new DTrackException("Error fetching project metrics", e);
		}

		return projectMetrics;
	}

	/**
	 * Loads the metrics for the project specified in the constructor with retry capabilities.
	 * <p>
	 * If the project hasn't been loaded yet, it will be loaded first.
	 * This method will retry the request if it fails, with the specified delay between retries.
	 * </p>
	 *
	 * @param retryDelay The delay between retries, in seconds
	 * @param retryLimit The maximum number of retries
	 * @return The metrics for the project
	 * @throws DTrackException If an error occurs during the request, if the project doesn't exist, or if the metrics couldn't be retrieved after the specified number of retries
	 */
	public ProjectMetrics loadProjectMetrics(Integer retryDelay, Integer retryLimit) throws DTrackException {
		log.info("Loading metrics for project: " + projectName + ":" + projectVersion + 
			" with retry delay: " + retryDelay + " seconds, retry limit: " + retryLimit);

		if (project == null) {
			log.debug("Project not loaded, loading project");
			loadProject();
		}

		try {
			this.projectMetrics = client.getProjectMetrics(project.getUuid(), retryDelay, retryLimit);
			if (projectMetrics != null) {
				log.info("Successfully loaded metrics for project: " + projectName + ":" + projectVersion);
			}
		} catch (HttpResponseException e) {
			log.error("Failed to load project metrics: " + e.getMessage());
			throw handleCommonErrors(e);
		} catch (Exception e) {
			log.error("Error fetching project metrics: " + e.getMessage());
			throw new DTrackException("Error fetching project metrics", e);
		}

		if (projectMetrics == null) {
			log.error("Could not retrieve project metrics after '" + retryLimit + "' retries");
			throw new DTrackException("Could not retrieve project metrics after '" + retryLimit + "' retries!");
		}

		return projectMetrics;
	}

	/**
	 * Applies suppressions to findings in Dependency-Track.
	 * <p>
	 * This method applies the suppressions configured for this DTrack instance to the findings
	 * of the project specified in the constructor. If the findings haven't been loaded yet,
	 * they will be loaded first.
	 * </p>
	 * <p>
	 * If resetExpiredSuppressions is true, any expired suppressions will be reset in Dependency-Track.
	 * </p>
	 *
	 * @param resetExpiredSuppressions Whether to reset expired suppressions in Dependency-Track
	 * @throws DTrackException If an error occurs during the request or if the project doesn't exist
	 */
	public void applySuppressions(boolean resetExpiredSuppressions) throws DTrackException {
		log.info("Applying suppressions to project: " + projectName + ":" + projectVersion + 
			(resetExpiredSuppressions ? " (resetting expired suppressions)" : ""));

		if (suppressions.isEmpty()) {
			log.info("No suppressions to apply");
			return;
		}

		if (findings == null) {
			log.debug("Findings not loaded, loading findings");
			loadFindings();
		}

		int appliedCount = 0;
		int resetCount = 0;

		for (Finding finding : findings) {
			Suppression suppression = suppressions.suppressionFor(finding);
			if (suppression == null) {
				continue;
			}

			Analysis analysis = new Analysis();
			analysis.setProjectUuid(project.getUuid());
			analysis.setComponentUuid(finding.getComponent().getUuid());
			analysis.setVulnerabilityUuid(finding.getVulnerability().getUuid());
			analysis.setSuppressed(true);
			analysis.setComment(suppression.getNotes());
			analysis.setState(suppression.getState());
			analysis.setJustification(suppression.getJustification());
			analysis.setResponse(suppression.getResponse());

			if (resetExpiredSuppressions && suppression.isExpired()) {
				log.debug("Resetting expired suppression for vulnerability: " + 
					finding.getVulnerability().getVulnId() + " in component: " + finding.getComponent().getName());
				analysis.setSuppressed(false);
				analysis.setState(State.NOT_SET);
				analysis.setJustification(AnalysisJustification.NOT_SET);
				analysis.setResponse(AnalysisResponse.NOT_SET);
				analysis.setComment("Expired suppression");
				resetCount++;
			} else {
				log.debug("Applying suppression for vulnerability: " + 
					finding.getVulnerability().getVulnId() + " in component: " + finding.getComponent().getName());
				appliedCount++;
			}

			try {
				client.uploadAnalysis(analysis);
			} catch (HttpResponseException e) {
				log.error("Failed to upload suppression analysis: " + e.getMessage());
				throw handleCommonErrors(e);
			} catch (IOException e) {
				log.error("Error uploading suppression analysis: " + e.getMessage());
				throw new DTrackException("Error uploading suppression analysis: ", e);
			}
		}

		log.info("Successfully applied " + appliedCount + " suppressions" + 
			(resetCount > 0 ? " and reset " + resetCount + " expired suppressions" : "") + 
			" for project: " + projectName + ":" + projectVersion);
	}

	/**
	 * Loads a file from the specified path and encodes it as a Base64 string.
	 *
	 * @param path The path to the file to load and encode
	 * @return The Base64-encoded content of the file
	 * @throws DTrackException If the file doesn't exist or an error occurs during loading or encoding
	 */
	private String loadAndEncodeArtifactFile(Path path) throws DTrackException {
		log.debug("Loading and encoding artifact file: " + path);

		if (!path.toFile().exists()) {
			log.error("Could not find artifact: " + path);
			throw new DTrackException("Could not find artifact: " + path);
		}

		try {
			String encoded = Base64.getEncoder().encodeToString(Files.readAllBytes(path));
			log.debug("Successfully loaded and encoded artifact file: " + path);
			return encoded;
		} catch (HttpResponseException e) {
			log.error("Failed to encode artifact: " + e.getMessage());
			throw handleCommonErrors(e);
		} catch (IOException e) {
			log.error("Error encoding artifact: " + e.getMessage());
			throw new DTrackException("Error encoding artifact", e);
		}
	}

	/**
	 * Handles common HTTP response errors and converts them to appropriate DTrackException types.
	 *
	 * @param e The HttpResponseException to handle
	 * @return A DTrackException of the appropriate type based on the HTTP status code
	 */
	private static DTrackException handleCommonErrors(HttpResponseException e) {
		if (e.getStatusCode() == 400) {
			return new DTrackNotFoundException("Bad request: " + e.getReasonPhrase(), e);
		} else if (e.getStatusCode() == 401) {
			return new DTrackUnauthenticatedException("Unauthenticated: ", e);
		} else {
			return new DTrackException("unexpected response: ", e);
		}
	}
}
