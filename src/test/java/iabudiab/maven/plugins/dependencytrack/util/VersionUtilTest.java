package iabudiab.maven.plugins.dependencytrack.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Test;


public class VersionUtilTest {
	
	@Test
	public void parseVersionString_testSuccessWithMatchingScheme() {
		List<Pair<String, Triple<Integer, Integer, Integer>>> inputAndExpectedOutput = Arrays.asList(
			Pair.of("0", Triple.of(0, null, null)),
			Pair.of("0.0", Triple.of(0, 0, null)),
			Pair.of("0.0.0", Triple.of(0, 0, 0)),
			Pair.of("0.0.1", Triple.of(0, 0, 1)),
			Pair.of("0.0.01", Triple.of(0, 0, 1)),
			Pair.of("0.1.0", Triple.of(0, 1, 0)),
			Pair.of("0.1.1", Triple.of(0, 1, 1)),
			Pair.of("1", Triple.of(1, null, null)),
			Pair.of("1.0", Triple.of(1, 0, null)),
			Pair.of("1.0.0", Triple.of(1, 0, 0)),
			Pair.of("1.2", Triple.of(1, 2, null)),
			Pair.of("1.2.3", Triple.of(1, 2, 3)),
			Pair.of("3.12.5", Triple.of(3, 12, 5))
		);

		for(Pair<String, Triple<Integer, Integer, Integer>> inOut : inputAndExpectedOutput) {
			assertEquals(inOut.getRight(), VersionUtil.parseVersionString(inOut.getLeft(), false));
		}
	}

	@Test
	public void parseVersionString_testSuccessWithIgnoredSuffixes() {
		List<Pair<String, Triple<Integer, Integer, Integer>>> inputAndExpectedOutput = Arrays.asList(
			Pair.of("3-SNAPSHOT", Triple.of(3, null, null)),
			Pair.of("3.12-SNAPSHOT", Triple.of(3, 12, null)),
			Pair.of("3.12.5-SNAPSHOT", Triple.of(3, 12, 5)),
			Pair.of("3.12.05-SNAPSHOT", Triple.of(3, 12, 5)),
			Pair.of("3.12.5-beta.1", Triple.of(3, 12, 5)),
			Pair.of("3.12.5.ALPHA-2", Triple.of(3, 12, 5)),
			Pair.of("3.12.5.RELEASE", Triple.of(3, 12, 5)),
			Pair.of("3.12.5.RC.3", Triple.of(3, 12, 5)),
			Pair.of("3.", Triple.of(3, null, null)),
			Pair.of("3..", Triple.of(3, null, null)),
			Pair.of("3...", Triple.of(3, null, null)),
			Pair.of("3..5", Triple.of(3, null, null)),
			Pair.of("3..5.", Triple.of(3, null, null)),
			Pair.of("3.12.5.", Triple.of(3, 12, 5)),
			Pair.of("3.12.5a.", Triple.of(3, 12, 5)),
			Pair.of("3.12.a5", Triple.of(3, 12, null)),
			Pair.of("3.12a.5.", Triple.of(3, 12, null)),
			Pair.of("3.a12.5", Triple.of(3, null, null)),
			Pair.of("3a.12.5.", Triple.of(3, null, null))
		);

		for(Pair<String, Triple<Integer, Integer, Integer>> inOut : inputAndExpectedOutput) {
			assertEquals(inOut.getRight(), VersionUtil.parseVersionString(inOut.getLeft(), true));
		}
	}


	@Test
	public void parseVersionString_testFail() {
		List<String> inputVersions = Arrays.asList(
			"3-SNAPSHOT",
			"3.12-SNAPSHOT",
			"3.12.5-SNAPSHOT",
			"3.12.05-SNAPSHOT",
			"3.12.5-beta.1",
			"3.12.5.ALPHA-2",
			"3.12.5.RELEASE",
			"3.12.5.RC.3",
			"3.",
			"3..",
			"3...",
			"3..5",
			"3..5.",
			"3.12.5.",
			"a.b.c",
			"3.1a.5",
			"3.12.5withsuffix"
		);

		for(String in : inputVersions) {
			assertThrows(IllegalArgumentException.class, () -> VersionUtil.parseVersionString(in, false));
		}
	}


	@Test
	public void parseVersionString_testFailWithIgnoredSuffixes() {
		List<String> inputVersions = Arrays.asList(
			"a.b.c",
			".3.1a.5",
			"PREFIX-3.5.12"
		);

		for(String in : inputVersions) {
			assertThrows(IllegalArgumentException.class, () -> VersionUtil.parseVersionString(in, true));
		}
	}
	
}
