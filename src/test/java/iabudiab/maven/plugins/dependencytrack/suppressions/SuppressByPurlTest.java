package iabudiab.maven.plugins.dependencytrack.suppressions;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import iabudiab.maven.plugins.dependencytrack.client.model.Finding;
import iabudiab.maven.plugins.dependencytrack.client.model.Component;
import org.junit.jupiter.api.Test;

class SuppressByPurlTest {

	/**
	 * Tests for the suppressesFinding method in the SuppressByPurl class.
	 */
	@Test
	void testSuppressesFindingWithExactMatch() {
		// Arrange
		String testPurl = "pkg:maven/group/artifact@1.0.0";
		SuppressByPurl suppressByPurl = new SuppressByPurl();
		suppressByPurl.setPurl(testPurl);
		suppressByPurl.setRegex(false);

		Component component = new Component();
		component.setPurl(testPurl);

		Finding finding = new Finding();
		finding.setComponent(component);

		// Act
		boolean result = suppressByPurl.suppressesFinding(finding);

		// Assert
		assertTrue(result);
	}

	@Test
	void testSuppressesFindingWithExactMatchNonMatchingPurl() {
		// Arrange
		String suppressPurl = "pkg:maven/group/artifact@1.0.0";
		String findingPurl = "pkg:maven/othergroup/otherartifact@2.0.0";
		SuppressByPurl suppressByPurl = new SuppressByPurl();
		suppressByPurl.setPurl(suppressPurl);
		suppressByPurl.setRegex(false);

		Component component = new Component();
		component.setPurl(findingPurl);

		Finding finding = new Finding();
		finding.setComponent(component);

		// Act
		boolean result = suppressByPurl.suppressesFinding(finding);

		// Assert
		assertFalse(result);
	}

	@Test
	void testSuppressesFindingWithRegexMatch() {
		// Arrange
		String regexPurl = "pkg:maven/.*/artifact@.*";
		String findingPurl = "pkg:maven/group/artifact@1.0.0";
		SuppressByPurl suppressByPurl = new SuppressByPurl();
		suppressByPurl.setPurl(regexPurl);
		suppressByPurl.setRegex(true);

		Component component = new Component();
		component.setPurl(findingPurl);

		Finding finding = new Finding();
		finding.setComponent(component);

		// Act
		boolean result = suppressByPurl.suppressesFinding(finding);

		// Assert
		assertTrue(result);
	}

	@Test
	void testSuppressesFindingWithRegexNonMatchingPurl() {
		// Arrange
		String regexPurl = "pkg:maven/.*/artifact@.*";
		String findingPurl = "pkg:npm/package/artifact@1.0.0";
		SuppressByPurl suppressByPurl = new SuppressByPurl();
		suppressByPurl.setPurl(regexPurl);
		suppressByPurl.setRegex(true);

		Component component = new Component();
		component.setPurl(findingPurl);

		Finding finding = new Finding();
		finding.setComponent(component);

		// Act
		boolean result = suppressByPurl.suppressesFinding(finding);

		// Assert
		assertFalse(result);
	}
}