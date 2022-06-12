package iabudiab.maven.plugins.dependencytrack.cyclone;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import iabudiab.maven.plugins.dependencytrack.BomFormat;
import org.apache.maven.plugin.MojoExecutionException;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.parsers.JsonParser;
import org.cyclonedx.parsers.XmlParser;

public class BomUtils {

	public static DiffResult computeDiff(Path from, Path to) throws MojoExecutionException {
		Bom fromBom = readBomAtPath(from);
		Bom toBom = readBomAtPath(to);

		return DiffUtils.compute(fromBom, toBom);
	}

	public static BomFormat probeFormat(Path path) throws MojoExecutionException {
		String contentType;
		try {
			contentType = Files.probeContentType(path);
		} catch (IOException e) {
			throw new MojoExecutionException("Error reading BOM at path: " + path, e);
		}

		if (contentType.equals("application/json")) {
			return BomFormat.JSON;
		} else if (contentType.equals("application/xml")) {
			return BomFormat.XML;
		} else {
			throw new MojoExecutionException("Couldn't determine BOM type at path: " + path);
		}
	}

	public static Bom readBomAtPath(Path path) throws MojoExecutionException {
		BomFormat format = probeFormat(path);

		try {
			File file = path.toFile();
			switch (format) {
				case JSON: {
					JsonParser parser = new JsonParser();
					return parser.parse(file);
				}
				case XML: {
					XmlParser parser = new XmlParser();
					return parser.parse(file);
				}
				default:
					throw new MojoExecutionException("Unknown BOM type");
			}
		} catch (ParseException e) {
			throw new MojoExecutionException("Error parsing BOM at path: " + path, e);
		}
	}
}
