package iabudiab.maven.plugins.dependencytrack;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

public abstract class AbstractDependencyTrackMojo extends AbstractMojo {

	@Parameter(property = "dependencyTrackUrl", required = true)
	private String dependencyTrackUrl;

	@Parameter(property = "dependencyTrackApiKey", required = true)
	private String dependencyTrackApiKey;

	@Parameter(defaultValue = "${project.groupId}.${project.artifactId}", property = "projectName", required = false)
	protected String projectName;

	@Parameter(defaultValue = "${project.version}", property = "projectVersion", required = false)
	protected String projectVersion;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		logConfiguration();

		URI baseUri;
		try {
			baseUri = new URI(dependencyTrackUrl);
			baseUri = baseUri.resolve("/api/v1/");
			getLog().info("Using API v1 at: " + baseUri);
		} catch (URISyntaxException e) {
			throw new MojoExecutionException("Invalid DepenencyTrack URL: " + dependencyTrackUrl, e);
		}

		HttpClient client = HttpClient.newHttpClient();
		HttpRequest.Builder requestBuilder = HttpRequest.newBuilder() //
				.header("X-Api-Key", dependencyTrackApiKey);

		doWork(client, requestBuilder, baseUri);
	}

	protected abstract void doWork(HttpClient client, HttpRequest.Builder requestBuilder, URI baseUri)
			throws MojoExecutionException;

	private void logConfiguration() {
		getLog().info("DependencyTrack Maven Plugin");
		getLog().info("DependencyTrack URL   : " + dependencyTrackUrl);
		getLog().info("Project name          : " + projectName);
		getLog().info("Project version       : " + projectVersion);
	}
}
