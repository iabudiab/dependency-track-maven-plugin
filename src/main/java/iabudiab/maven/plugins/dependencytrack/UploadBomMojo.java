package iabudiab.maven.plugins.dependencytrack;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import iabudiab.maven.plugins.dependencytrack.client.model.CollectionLogic;
import iabudiab.maven.plugins.dependencytrack.client.model.Finding;
import iabudiab.maven.plugins.dependencytrack.client.model.Project;
import iabudiab.maven.plugins.dependencytrack.client.model.ProjectMetrics;
import iabudiab.maven.plugins.dependencytrack.client.model.Tag;
import iabudiab.maven.plugins.dependencytrack.client.model.TokenResponse;
import iabudiab.maven.plugins.dependencytrack.dtrack.DTrack;
import iabudiab.maven.plugins.dependencytrack.dtrack.DTrackException;
import iabudiab.maven.plugins.dependencytrack.dtrack.FindingsReport;
import iabudiab.maven.plugins.dependencytrack.dtrack.FindingsThresholdSecurityGate;
import iabudiab.maven.plugins.dependencytrack.dtrack.InfoPrinter;
import iabudiab.maven.plugins.dependencytrack.dtrack.SecurityGateDecision;
import iabudiab.maven.plugins.dependencytrack.suppressions.Suppressions;

/**
 * Mojo for uploading a <a href="https://cyclonedx.org">CycloneDX</a> SBOM to
 * <a href="https://dependencytrack.org">Dependency-Track</a>
 *
 * @author Iskandar Abudiab
 */
@Mojo(name = "upload-bom", defaultPhase = LifecyclePhase.VERIFY, requiresOnline = true)
public class UploadBomMojo extends AbstractDependencyTrackMojo {

	/**
	 * CycloneDX SBOM directory.
	 */
	@Parameter(defaultValue = "${project.build.directory}", property = "artifactDir", required = true)
	private File artifactDirectory;

	/**
	 * CycloneDX SBOM filename.
	 */
	@Parameter(defaultValue = "bom.xml", property = "artifactName", required = true)
	private String artifactName;

	/**
	 * Whether to poll the pending token for processing.
	 * <p>
	 * Default is <code>true</code>, which would poll the token for <code>tokenPollingDuration</code>
	 * and apply the <code>SecurityGate</code> afterwards.
	 * <p>
	 * If set to <code>false</code> then this goal would upload the BOM, write the token
	 * to a file at <code>tokenFile</code> and then exit.
	 */
	@Parameter(defaultValue = "true", property = "pollToken", required = true)
	private boolean pollToken;

	/**
	 * The token file path into which the token UUID value should be written.
	 */
	@Parameter(defaultValue = "${project.build.directory}/dependency-track/pendingToken", property = "tokenFile", required = true)
	private String tokenFile;

	/**
	 * Polling timeout for the uploaded BOM token.
	 * <p>
	 * Upon uploading a BOM to Dependency-Track a token is returned, which can be
	 * checked for processing status. Once the token is processed, the findings are
	 * available and can be fetched for further analysis.
	 */
	@Parameter(defaultValue = "60", property = "tokenPollingDuration", required = true)
	private Integer tokenPollingDuration;

	@Parameter(defaultValue = "3", property = "projectMetricsRetryLimit", required = true)
	private Integer projectMetricsRetryLimit;

	@Parameter(defaultValue = "5", property = "projectMetricsRetryDelay", required = true)
	private Integer projectMetricsRetryDelay;

	/**
	 * Configurable thresholds for the allowed number of <code>critical</code>,
	 * <code>high</code>, <code>medium</code> and <code>low</code> findings from
	 * Dependency-Track, which would fail the build if not met.
	 */
	@Parameter(property = "securityGate", required = false)
	private FindingsThresholdSecurityGate securityGate = FindingsThresholdSecurityGate.strict();

	/**
	 * Whether matching local suppressions for actual findings should be applied remotely in Dependency-Track.
	 */
	@Parameter(property = "uploadMatchingSuppressions", defaultValue = "false", required = false)
	protected boolean uploadMatchingSuppressions;

	/**
	 * Whether matching local suppressions for actual findings, that are expired, should be reset in dependency track.
	 */
	@Parameter(property = "resetExpiredSuppressions", defaultValue = "true", required = false)
	protected boolean resetExpiredSuppressions;

	/**
	 * Whether suppressions file should be created from effective suppressions.
	 */
	@Parameter(property = "cleanupSuppressions", defaultValue = "true", required = false)
	protected boolean cleanupSuppressions;

	/**
	 * Suppressions file path containing effective suppressions.
	 */
	@Parameter(defaultValue = "${project.build.directory}/dependency-track/suppressions.json", property = "cleanupSuppressionsFile", required = false)
	private String cleanupSuppressionsFile;

	/**
	 * The name of the parent project in Dependency-Track
	 */
	@Parameter(property = "parentIdentifier", defaultValue = "", required = false)
	protected String parentIdentifier;

	/**
	 * The name of the parent project in Dependency-Track
	 */
	@Parameter(property = "parentName", defaultValue = "", required = false)
	protected String parentName;

	/**
	 * The version of the parent project in Dependency-Track
	 */
	@Parameter(property = "parentVersion", defaultValue = "", required = false)
	protected String parentVersion;

	/**
	 * Whether the parent project should be created or not, if no such project found i Dependency-Track
	 */
	@Parameter(property = "autCreateParent", defaultValue = "false", required = false)
	private boolean autoCreateParent;

	/**
	 * The collection logic that should be applied to this project's autocreated parent in Dependency-Track, thus only takes effect when 'autoCreateParent' is set to 'true' and no such parent found
	 */
	@Parameter(property = "parentCollectionLogic", defaultValue = "", required = false)
	protected String parentCollectionLogic;

	/**
	 * The collection logic that should be applied to this project's autocreated parent in Dependency-Track, thus only takes effect when 'autoCreateParent' is set to 'true' and no such parent found
	 */
	@Parameter(property = "parentCollectionTag", defaultValue = "", required = false)
	protected String parentCollectionTag;

	/**
	 * The collection logic that should be applied to this project in Dependency-Track
	 */
	@Parameter(property = "collectionLogic", defaultValue = "", required = false)
	protected String collectionLogic;

	/**
	 * The collection tag that should be applied to the project in Dependency-Track
	 */
	@Parameter(property = "collectionTag", defaultValue = "", required = false)
	protected String collectionTag;


	@Override
	protected void logGoalConfiguration() {
		getLog().info("Using artifact directory        : " + artifactDirectory);
		getLog().info("Using artifact                  : " + artifactName);
		getLog().info("Upload matching suppressions    : " + uploadMatchingSuppressions);
		getLog().info("Reset expired suppressions      : " + resetExpiredSuppressions);
		getLog().info("ProjectMetrics retry delay      : " + projectMetricsRetryDelay);
		getLog().info("ProjectMetrics retry limit      : " + projectMetricsRetryLimit);
		getLog().info("Parent identifier               : " + parentIdentifier);
		getLog().info("Parent name                     : " + parentName);
		getLog().info("Parent version                  : " + parentVersion);
		getLog().info("Auto create parent              : " + autoCreateParent);
		getLog().info("Parent's Collection logic       : " + parentCollectionLogic);
		getLog().info("Parent's Collection tag         : " + parentCollectionTag);
		getLog().info("Collection logic                : " + collectionLogic);
		getLog().info("Collection tag                  : " + collectionTag);
	}

	@Override
	protected void doWork(DTrack dtrack) throws DTrackException, MojoExecutionException {
		Path path = Paths.get(artifactDirectory.getPath(), artifactName);
		if (getLog().isDebugEnabled()) {
			getLog().debug("Start uploading bom ...");
		}

		// even if the bom upload failed, we want to continue
		TokenResponse tokenResponse = null;
		try {
			tokenResponse = dtrack.uploadBom(path);
		} catch (DTrackException ex) {
			getLog().warn("Got exception when uploading bom!", ex);
		}

		// Try to apply parent to current project in dependency track
		applyParent(dtrack);

		// Try to apply collection logic to current project in dependency track
		applyCollectionLogic(dtrack);

		// When the bom upload failed, we want to stop execution here, since the further steps require a valid token response
		if (tokenResponse == null) {
			return;
		}

		try {
			Path tokenFilePath = Paths.get(tokenFile);
			writeToPath(tokenResponse, tokenFilePath);
			getLog().info("Token has been written to: " + tokenFilePath);
		} catch (IOException e) {
			throw new MojoExecutionException("Error writing token: ", e);
		}

		if (!pollToken) {
			getLog().info("Token polling is disabled. Nothing more to do.");
			return;
		}

		boolean stillProcessingToken = dtrack.pollToken(tokenResponse.getToken(), tokenPollingDuration);

		if (stillProcessingToken) {
			getLog().info("Timeout while waiting for BOM token, bailing out.");
			return;
		}

		List<Finding> findings = dtrack.loadFindings();
		FindingsReport findingsReport = new FindingsReport(findings);
		getLog().info(InfoPrinter.print(findingsReport));

		ProjectMetrics projectMetrics = dtrack.loadProjectMetrics(projectMetricsRetryDelay, projectMetricsRetryLimit);
		getLog().info(InfoPrinter.print(projectMetrics));

		Suppressions suppressions = dtrack.getSuppressions();
		getLog().info(securityGate.print());
		getLog().info(suppressions.print());

		if (uploadMatchingSuppressions) {
			getLog().info("Applying suppressions");
			dtrack.applySuppressions(resetExpiredSuppressions);
		}

		SecurityGateDecision decision = securityGate.checkAgainst(findings, suppressions);
		decision.execute(getLog());

		if (cleanupSuppressions) {
			getLog().info("Cleaning suppression file");
			Path cleanupSuppressionsFilePath = Paths.get(cleanupSuppressionsFile);
			decision.getReport().cleanupSuppressionsFile(cleanupSuppressionsFilePath);
			getLog().info("Effective suppressions have been written to: " + cleanupSuppressionsFilePath);
		}
	}

	private void applyParent(DTrack dtrack) {
		if (ObjectUtils.isEmpty(parentIdentifier) && (ObjectUtils.isEmpty(parentName))) {
			getLog().debug("No parent specified");
			return;
		}

		if (getLog().isDebugEnabled()) {
			getLog().debug(
				parentIdentifier != null
					? String.format("Try to apply parent by identifier '%s'", parentIdentifier)
					: String.format("Try to apply parent '%s:%s'", parentName, parentVersion)
			);
		}

		Project project = null;
		try {
			if (getLog().isDebugEnabled()) {
				getLog().debug("Try to obtain current project");
			}
			project = dtrack.findProject(projectName, projectVersion);
		} catch (Exception ex) {
			getLog().warn("Something went wrong loading project!", ex);
			return;
		}

		UUID parentUuid = null;
		try {
			parentUuid = !ObjectUtils.isEmpty(parentIdentifier) ? UUID.fromString(parentIdentifier) : null;
		} catch (IllegalArgumentException ex) {
			getLog().warn(String.format("The given parent identifier '%s' could not be parsed to a valid UUID! Skip applying parent.", parentIdentifier), ex);
			return;
		}

		// check if the desired parent is not already set
		Project parentProject = project.getParent();

		if (parentProject != null) {
			boolean sameParentUuid = (parentUuid != null && parentUuid.equals(parentProject.getUuid()));
			boolean sameParentNameAndVersion =
				(parentName != null && parentName.equals(parentProject.getName())) &&
				(parentVersion != null && parentVersion.equals(parentProject.getVersion()));

			if (sameParentUuid || sameParentNameAndVersion) {
				getLog().info(String.format("The parent '%s:%s' is already assigned, so no need to apply it again",
					parentProject.getName(), parentProject.getVersion()
				));
				return;
			}
		}

		// try to obtain the parent project
		if (getLog().isDebugEnabled()) {
			getLog().debug(
				String.format("Try to obtain parent project either by identifier '%s' or by name and version '%s:%s'",
					parentUuid, parentName, parentVersion)
			);
		}

		parentProject = parentUuid != null
			? dtrack.findProject(parentUuid)
			: dtrack.findProject(parentName, parentVersion);

		// Auto-create should only be applied when parent project is specified by 'projectName' and 'projectVersion' and not by parent uuid
		if (parentProject == null && parentUuid == null) {
			// for debugging purposes we split the conditional statement into two
			if (autoCreateParent) {
				// if no such parent project found, create it
				getLog().info(String.format(
					"Parent project '%s:%s' not found, but since 'autoCreateParent' is set to 'true', trying to create it",
					parentName, parentVersion
				));

				// parsing parent's collectionLogic
				CollectionLogic logic = null;
				try {
					logic = CollectionLogic.valueOf(parentCollectionLogic);
				} catch (Exception ex) {
					getLog().warn(String.format("Could not parse value '%s' to a valid collection logic strategy! Continue and handle it as 'null'", collectionLogic), ex);
				}

				// parsing parent's collectionTag
				Tag tag = !ObjectUtils.isEmpty(parentCollectionTag) ? new Tag(parentCollectionTag) : null;
				parentProject = dtrack.createProject(parentName, parentVersion, logic, tag);

				getLog().info(String.format("Parent project '%s:%s' successfully created with uuid '%s'",
					parentName, parentVersion, parentProject.getUuid()
				));
			} else {
				getLog().info(String.format(
					"Parent project '%s:%s' not found and 'autoCreateParent' is set to 'false', so not trying to create it",
					parentName, parentVersion
				));
			}
		}

		if (parentProject != null) {
			if (getLog().isDebugEnabled()) {
				getLog().debug("Try to apply parent project");
			}
			dtrack.applyParentProject(project, parentProject);
		} else {
			getLog().warn("Skip applying parent project");
		}

		getLog().info(
			parentIdentifier != null
				? String.format("Successfully applied parent by identifier '%s'", parentIdentifier)
				: String.format("Successfully applied parent '%s:%s'", parentName, parentVersion)
		);
	}

	private void applyCollectionLogic(DTrack dtrack) {
		if (ObjectUtils.isEmpty(collectionLogic)) {
			if (getLog().isDebugEnabled()) {
				getLog().debug("No collection logic specified");
			}
			return;
		}

		if (getLog().isDebugEnabled()) {
			getLog().debug(String.format("Try to apply collection logic '%s'", collectionLogic));
		}

		CollectionLogic logic = null;
		try {
			logic = CollectionLogic.valueOf(collectionLogic);
		} catch (Exception ex) {
			getLog().warn(
				String.format("Could not parse value '%s' to a valid collection logic strategy! Skip applying collection logic.", collectionLogic), ex
			);
			return;
		}
		String tag = (collectionTag == null || collectionTag.isEmpty()) ? null : collectionTag.trim();

		Project project = null;
		try {
			if (getLog().isDebugEnabled()) {
				getLog().debug("Try to obtain current project");
			}
			project = dtrack.findProject(projectName, projectVersion);
		} catch (Exception ex) {
			getLog().warn("Something went wrong loading project!", ex);
			return;
		}

		try {
			dtrack.applyCollectionLogic(project, logic, tag);
		} catch (Exception ex) {
			getLog().warn(
				String.format("Something went wrong applying collection logic '%s' and tag '%s'!", logic, tag), ex
			);
		}

		getLog().info(String.format("Successfully applied collection logic '%s'", collectionLogic));
	}

	private void writeToPath(TokenResponse token, Path path) throws IOException {
		Files.createDirectories(path.getParent());
		byte[] tokenBytes = token.toString().getBytes(StandardCharsets.UTF_8);
		Files.write(path, tokenBytes,
			StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
	}
}
