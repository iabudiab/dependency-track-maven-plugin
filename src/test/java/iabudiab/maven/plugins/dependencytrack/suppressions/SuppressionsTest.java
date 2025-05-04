package iabudiab.maven.plugins.dependencytrack.suppressions;

import iabudiab.maven.plugins.dependencytrack.client.model.Finding;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SuppressionsTest {

	/**
	 * Test class for the `Suppressions` class, specifically the `shouldSuppress` method.
	 * The `shouldSuppress` method checks if a given `Finding` object should be suppressed
	 * based on the list of defined `Suppression` objects.
	 */
	@Test
	void shouldSuppress_FindingIsSuppressed_ReturnsTrue() {
		// Arrange
		Suppression suppression = mock(Suppression.class);
		Finding finding = mock(Finding.class);
		when(suppression.shouldSuppress(finding)).thenReturn(true);

		Suppressions suppressions = new Suppressions(Collections.singletonList(suppression));

		// Act
		boolean result = suppressions.shouldSuppress(finding);

		// Assert
		assertTrue(result);
		verify(suppression).shouldSuppress(finding);
	}

	@Test
	void shouldSuppress_FindingIsNotSuppressed_ReturnsFalse() {
		// Arrange
		Suppression suppression = mock(Suppression.class);
		Finding finding = mock(Finding.class);
		when(suppression.shouldSuppress(finding)).thenReturn(false);

		Suppressions suppressions = new Suppressions(Collections.singletonList(suppression));

		// Act
		boolean result = suppressions.shouldSuppress(finding);

		// Assert
		assertFalse(result);
		verify(suppression).shouldSuppress(finding);
	}

	@Test
	void shouldSuppress_NoSuppressions_ReturnsFalse() {
		// Arrange
		Finding finding = mock(Finding.class);
		Suppressions suppressions = new Suppressions(Collections.emptyList());

		// Act
		boolean result = suppressions.shouldSuppress(finding);

		// Assert
		assertFalse(result);
	}

	@Test
	void shouldSuppress_MultipleSuppressionsOneMatches_ReturnsTrue() {
		// Arrange
		Suppression suppression1 = mock(Suppression.class);
		Suppression suppression2 = mock(Suppression.class);
		Finding finding = mock(Finding.class);

		when(suppression1.shouldSuppress(finding)).thenReturn(false);
		when(suppression2.shouldSuppress(finding)).thenReturn(true);

		Suppressions suppressions = new Suppressions(Arrays.asList(suppression1, suppression2));

		// Act
		boolean result = suppressions.shouldSuppress(finding);

		// Assert
		assertTrue(result);
		verify(suppression1).shouldSuppress(finding);
		verify(suppression2).shouldSuppress(finding);
	}

	@Test
	void shouldSuppress_MultipleSuppressionsNoneMatches_ReturnsFalse() {
		// Arrange
		Suppression suppression1 = mock(Suppression.class);
		Suppression suppression2 = mock(Suppression.class);
		Finding finding = mock(Finding.class);

		when(suppression1.shouldSuppress(finding)).thenReturn(false);
		when(suppression2.shouldSuppress(finding)).thenReturn(false);

		Suppressions suppressions = new Suppressions(Arrays.asList(suppression1, suppression2));

		// Act
		boolean result = suppressions.shouldSuppress(finding);

		// Assert
		assertFalse(result);
		verify(suppression1).shouldSuppress(finding);
		verify(suppression2).shouldSuppress(finding);
	}
}