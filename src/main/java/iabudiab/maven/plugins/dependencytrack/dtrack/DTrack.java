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
import iabudiab.maven.plugins.dependencytrack.client.model.Finding;
import iabudiab.maven.plugins.dependencytrack.client.model.Project;
import iabudiab.maven.plugins.dependencytrack.client.model.ProjectMetrics;
import iabudiab.maven.plugins.dependencytrack.client.model.ScanSubmitRequest;
import iabudiab.maven.plugins.dependencytrack.client.model.State;
import iabudiab.maven.plugins.dependencytrack.client.model.TokenResponse;
import iabudiab.maven.plugins.dependencytrack.cyclone.BomFormat;
import iabudiab.maven.plugins.dependencytrack.suppressions.Suppression;
import iabudiab.maven.plugins.dependencytrack.suppressions.Suppressions;

public class DTrack {

	private final DTrackClient client;
	private final Suppressions suppressions;
	private final String projectName;
	private final String projectVersion;

	private Project project;
	private List<Finding> findings;
	private ProjectMetrics projectMetrics;

	public DTrack(DTrackClient client,
				Suppressions suppressions,
				String projectName,
				String projectVersion) {
		this.client = client;
		this.suppressions = suppressions;
		this.projectName = projectName;
		this.projectVersion = projectVersion;
	}

	public Suppressions getSuppressions() {
		return suppressions;
	}

	public void uploadScan(Path path) {
		String encodeArtifact = loadAndEncodeArtifactFile(path);

		ScanSubmitRequest payload = ScanSubmitRequest.builder() //
			.projectName(projectName) //
			.projectVersion(projectVersion) //
			.scan(encodeArtifact) //
			.autoCreate(true) //
			.build();

		try {
			client.uploadScan(payload);
		} catch (HttpResponseException e) {
			throw handleCommonErrors(e);
		} catch (IOException e) {
			throw new DTrackException("Error uploading scan: ", e);
		}
	}

	public TokenResponse uploadBom(Path path) throws DTrackException {
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
		} catch (HttpResponseException e) {
			throw handleCommonErrors(e);
		} catch (IOException e) {
			throw new DTrackException("Error uploading bom: ", e);
		}

		return response;
	}

	public File downloadBom(Path path, BomFormat outputFormat) {
		if (project == null) {
			loadProject();
		}

		try {
			return client.downloadBom(project.getUuid(), path, outputFormat);
		} catch (HttpResponseException e) {
			throw handleCommonErrors(e);
		} catch (IOException e) {
			throw new DTrackException("Error downloading BOM: ", e);
		}
	}

	public Project findProject(UUID uuid) throws DTrackException {
		try {
			return client.getProject(uuid);
		} catch (HttpResponseException e) {
			if(e.getStatusCode() == 404) return null;
			else throw handleCommonErrors(e);
		} catch (IOException e) {
			throw new DTrackException("Error loading project: ", e);
		}
	}

	public Project findProject(String projectName, String projectVersion) throws DTrackException {
		try {
			return client.getProject(projectName, projectVersion);
		} catch (HttpResponseException e) {
			if(e.getStatusCode() == 404) return null;
			else throw handleCommonErrors(e);
		} catch (IOException e) {
			throw new DTrackException("Error loading project: ", e);
		}
	}

	private void loadProject() throws DTrackException {
		this.project = findProject(projectName, projectVersion);
		if (this.project == null) {
			throw new DTrackNotFoundException("Project not found: " + projectName + ":" + projectVersion);
		}
	}

	public Project createProject(String projectName, String projectVersion) throws DTrackException {
		try {
			return client.createProject(projectName, projectVersion);
		} catch (HttpResponseException e) {
			throw handleCommonErrors(e);
		} catch (IOException e) {
			throw new DTrackException("Error loading project: ", e);
		}
	}

	public Project applyParentProject(Project project, Project parentProject) {
		try {
			return client.applyProjectParent(project, parentProject);
		} catch (HttpResponseException e) {
			throw handleCommonErrors(e);
		} catch (IOException e) {
			throw new DTrackException("Error applying parent: ", e);
		}
	}

	public boolean pollToken(UUID token, int durationSeconds) {
		try { 
			return client.pollTokenProcessing(token, ForkJoinPool.commonPool())
				.get(durationSeconds, TimeUnit.SECONDS);
		} catch (TimeoutException | InterruptedException | ExecutionException e) {
			Thread.currentThread().interrupt();
			throw new DTrackException("Error polling token: ", e);
		}
	}

	public List<Finding> loadFindings() throws DTrackException {
		if (project == null) {
			loadProject();
		}

		try {
			this.findings = client.getProjectFindings(project.getUuid());
			return findings;
		} catch (HttpResponseException e) {
			throw handleCommonErrors(e);
		} catch (IOException e) {
			throw new DTrackException("Error loading project findings: ", e);
		}
	}

	public ProjectMetrics loadProjectMetrics() throws DTrackException {
		if (project == null) {
			loadProject();
		}

		try {
			this.projectMetrics = client.getProjectMetrics(project.getUuid());
		} catch (HttpResponseException e) {
			throw handleCommonErrors(e);
		} catch (Exception e) {
			throw new DTrackException("Error fetching project metrics", e);
		}

		return projectMetrics;
	}

	public ProjectMetrics loadProjectMetrics(Integer retryDelay, Integer retryLimit) throws DTrackException {
		if (project == null) {
			loadProject();
		}

		try {
			this.projectMetrics = client.getProjectMetrics(project.getUuid(), retryDelay, retryLimit);
		} catch (HttpResponseException e) {
			throw handleCommonErrors(e);
		} catch (Exception e) {
			throw new DTrackException("Error fetching project metrics", e);
		}

		if (projectMetrics == null) {
			throw new DTrackException("Could not retrieve project metrics after '" + retryLimit + "' retries!");
		}

		return projectMetrics;
	}

	public void applySuppressions(boolean resetExpiredSuppressions) throws DTrackException {
		if (suppressions.isEmpty()) {
			return;
		}

		if (findings == null) {
			loadFindings();
		}

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
				analysis.setSuppressed(false);
				analysis.setState(State.NOT_SET);
				analysis.setJustification(AnalysisJustification.NOT_SET);
				analysis.setResponse(AnalysisResponse.NOT_SET);
				analysis.setComment("Expired suppression");
			}

			try {
				client.uploadAnalysis(analysis);
			} catch (HttpResponseException e) {
				throw handleCommonErrors(e);
			} catch (IOException e) {
				throw new DTrackException("Error uploading suppression analysis: ", e);
			}
		}
	}

	private String loadAndEncodeArtifactFile(Path path) throws DTrackException {
		if (!path.toFile().exists()) {
			throw new DTrackException("Could not find artifact: " + path);
		}

		try {
			return Base64.getEncoder().encodeToString(Files.readAllBytes(path));
		} catch (HttpResponseException e) {
			throw handleCommonErrors(e);
		} catch (IOException e) {
			throw new DTrackException("Error encoding artifact", e);
		}
	}

	private static DTrackException handleCommonErrors(HttpResponseException e) {
		if (e.getStatusCode() == 400) {
			return new DTrackNotFoundException("Error uploading scan: " + e.getReasonPhrase(), e);
		} else if (e.getStatusCode() == 401) {
			return new DTrackUnauthenticatedException("Unauthenticated: ", e);
		} else {
			return new DTrackException("Error uploading scan: ", e);
		}
	}
}
