package iabudiab.maven.plugins.dependencytrack.suppressions;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import iabudiab.maven.plugins.dependencytrack.client.model.Component;
import iabudiab.maven.plugins.dependencytrack.client.model.Finding;
import iabudiab.maven.plugins.dependencytrack.client.model.Vulnerability;
import org.junit.jupiter.api.Test;

public class SuppressCveOfPurlTest {

	/**
	 * `SuppressCveOfPurl` class test for the `suppressesFinding` method.
	 * The method suppresses a given finding if both `cve` and `purl`
	 * match the `Finding` object's CVE and PURL details. The match may be
	 * regex-based or an exact string comparison depending on the `regex` flag.
	 */

	@Test
	public void testSuppressesFinding_ExactMatch_Success() {
		SuppressCveOfPurl suppressor = new SuppressCveOfPurl();
		suppressor.setCve("CVE-2023-1234");
		suppressor.setPurl("pkg:maven/group/artifact@1.0.0");
		suppressor.setRegex(false);

		Component component = new Component();
		component.setPurl("pkg:maven/group/artifact@1.0.0");

		Vulnerability vulnerability = new Vulnerability();
		vulnerability.setVulnId("CVE-2023-1234");

		Finding finding = new Finding();
		finding.setVulnerability(vulnerability);
		finding.setComponent(component);

		assertTrue(suppressor.suppressesFinding(finding));
	}

	@Test
	public void testSuppressesFinding_ExactMatch_Failure_CveMismatch() {
		SuppressCveOfPurl suppressor = new SuppressCveOfPurl();
		suppressor.setCve("CVE-2023-1234");
		suppressor.setPurl("pkg:maven/group/artifact@1.0.0");
		suppressor.setRegex(false);

		Component component = new Component();
		component.setPurl("pkg:maven/group/artifact@1.0.0");

		Vulnerability vulnerability = new Vulnerability();
		vulnerability.setVulnId("CVE-2023-5678");

		Finding finding = new Finding();
		finding.setVulnerability(vulnerability);
		finding.setComponent(component);

		assertFalse(suppressor.suppressesFinding(finding));
	}

	@Test
	public void testSuppressesFinding_RegexMatch_Success() {
		SuppressCveOfPurl suppressor = new SuppressCveOfPurl();
		suppressor.setCve("CVE-2023-1234");
		suppressor.setPurl("pkg:maven/.*/artifact@.*");
		suppressor.setRegex(true);

		Component component = new Component();
		component.setPurl("pkg:maven/group/artifact@1.0.0");

		Vulnerability vulnerability = new Vulnerability();
		vulnerability.setVulnId("CVE-2023-1234");

		Finding finding = new Finding();
		finding.setVulnerability(vulnerability);
		finding.setComponent(component);

		assertTrue(suppressor.suppressesFinding(finding));
	}

	@Test
	public void testSuppressesFinding_RegexMatch_Failure_PurlMismatch() {
		SuppressCveOfPurl suppressor = new SuppressCveOfPurl();
		suppressor.setCve("CVE-2023-1234");
		suppressor.setPurl("pkg:maven/.*/artifact@.*");
		suppressor.setRegex(true);

		Component component = new Component();
		component.setPurl("pkg:npm/somegroup/someartifact@2.0.0");

		Vulnerability vulnerability = new Vulnerability();
		vulnerability.setVulnId("CVE-2023-1234");

		Finding finding = new Finding();
		finding.setVulnerability(vulnerability);
		finding.setComponent(component);

		assertFalse(suppressor.suppressesFinding(finding));
	}

	@Test
	public void testSuppressesFinding_RegexFlagDisabled() {
		SuppressCveOfPurl suppressor = new SuppressCveOfPurl();
		suppressor.setCve("CVE-2023-1234");
		suppressor.setPurl("pkg:maven/.*/artifact@.*");
		suppressor.setRegex(false);

		Component component = new Component();
		component.setPurl("pkg:maven/group/artifact@1.0.0");

		Vulnerability vulnerability = new Vulnerability();
		vulnerability.setVulnId("CVE-2023-1234");

		Finding finding = new Finding();
		finding.setVulnerability(vulnerability);
		finding.setComponent(component);

		assertFalse(suppressor.suppressesFinding(finding));
	}
}