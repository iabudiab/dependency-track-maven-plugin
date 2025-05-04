package iabudiab.maven.plugins.dependencytrack.suppressions;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import iabudiab.maven.plugins.dependencytrack.client.model.Finding;
import iabudiab.maven.plugins.dependencytrack.client.model.Vulnerability;

public class SuppressCveTest {

	@Test
	void testSuppressesFinding_MatchingCve() {
		// Arrange
		SuppressCve suppressCve = new SuppressCve();
		suppressCve.setCve("CVE-1234-5678");

		Vulnerability vulnerability = new Vulnerability();
		vulnerability.setVulnId("CVE-1234-5678");

		Finding finding = new Finding();
		finding.setVulnerability(vulnerability);

		// Act
		boolean result = suppressCve.suppressesFinding(finding);

		// Assert
		assertTrue(result);
	}

	@Test
	void testSuppressesFinding_NonMatchingCve() {
		// Arrange
		SuppressCve suppressCve = new SuppressCve();
		suppressCve.setCve("CVE-1234-5678");

		Vulnerability vulnerability = new Vulnerability();
		vulnerability.setVulnId("CVE-9876-5432");

		Finding finding = new Finding();
		finding.setVulnerability(vulnerability);

		// Act
		boolean result = suppressCve.suppressesFinding(finding);

		// Assert
		assertFalse(result);
	}

	@Test
	void testSuppressesFinding_NullVulnerability() {
		// Arrange
		SuppressCve suppressCve = new SuppressCve();
		suppressCve.setCve("CVE-1234-5678");

		Finding finding = new Finding();
		finding.setVulnerability(null);

		// Act
		boolean result = suppressCve.suppressesFinding(finding);

		// Assert
		assertFalse(result);
	}

	@Test
	void testSuppressesFinding_NullFinding() {
		// Arrange
		SuppressCve suppressCve = new SuppressCve();
		suppressCve.setCve("CVE-1234-5678");

		// Act
		boolean result = suppressCve.suppressesFinding(null);

		// Assert
		assertFalse(result);
	}
}