package iabudiab.maven.plugins.dependencytrack.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Triple;

public abstract class VersionUtil {

	// create version regex pattern:
	// ^ = beginning of the string
	// \d+ = (required) one or more digits (major part)
	// (?:\.\d+){0,2} = (optionally) dot separated minor and patch part
	private static Pattern VERSION_PATTERN = Pattern.compile("^\\d+(?:\\.\\d+){0,2}");
	
	/**
	 * parses a version string into comparable parts (major, minor, patch)
	 * @param versionString the string that should be parsed
	 * @return the three parsed parts as a {@link Triple}
	 */
	public static Triple<Integer, Integer, Integer> parseVersionString(String versionString, boolean ignoreVersionSuffixes) throws IllegalArgumentException {
		
		Matcher versionMatcher = VERSION_PATTERN.matcher(versionString);

		// check if the projects version matches the pattern
		// and cleanup current version, if ignoreVersionSuffixes is set to true
		String cleanedVersion = versionString;
		// if the string at least contains a match ...
		if(versionMatcher.find()) {
			// ... and ignore suffixes is set to true
			if(ignoreVersionSuffixes) {
				// extract that match
				cleanedVersion = versionMatcher.group();
			}
			// ... and ignore suffixes is false but the string DOES contain any suffix
			else if(!versionMatcher.matches()) {
				throw new IllegalArgumentException(String.format(
							"version '%s' does not exactly match expected version scheme (comma separated required major version and optional minor and patch versions)!"
							+" Try setting 'ignoreVersionSuffixes' to 'true'.",
							versionString
						));
			}
		}
		else {
			throw new IllegalArgumentException(String.format(
						"version '%s' does not start with expected version scheme (comma separated required major version and optional minor and patch versions)!",
						versionString
					));
		}
		
		String[] versionParts = cleanedVersion.split("\\.");

		Integer major = Integer.parseInt(versionParts[0]);
		Integer minor = versionParts.length > 1 ? Integer.parseInt(versionParts[1]) : null;
		Integer patch = versionParts.length > 2 ? Integer.parseInt(versionParts[2]) : null;
		
		return Triple.of(major, minor, patch);
	}
}
