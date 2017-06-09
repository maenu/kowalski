package ch.unibe.scg.kowalski.task;

import java.util.Collections;

import org.apache.maven.settings.building.SettingsBuildingException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.graph.selector.ScopeDependencySelector;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DependencyTest {

	private Artifact artifact;
	private Dependency fetcher;
	private DependencySelector dependencySelector;

	@Before
	public void before() throws SettingsBuildingException {
		this.artifact = new DefaultArtifact("org.neo4j", "neo4j", "jar", "3.0.5");
		this.fetcher = new Dependency(new Maven());
		this.dependencySelector = new ScopeDependencySelector(Collections.singleton(JavaScopes.COMPILE),
				Collections.emptySet());
	}

	@Test
	public void testFetchPage() throws DependencyResolutionException, DependencyCollectionException {
		CollectResult collectResult = this.fetcher.fetchCollectResult(this.artifact, JavaScopes.COMPILE,
				this.dependencySelector);
		DependencyResult dependencyResult = this.fetcher.fetchDependencyResult(collectResult.getRoot(),
				this.dependencySelector);
		Page<Artifact, Artifact> page = this.fetcher.fetchPage(dependencyResult, false);
		Assert.assertTrue(this.areEqual(page.getKey().get(), this.artifact));
		Assert.assertNotNull(page.getKey().get().getFile());
		Assert.assertEquals(page.getElements().size(), 48);
	}

	private boolean areEqual(Artifact a, Artifact b) {
		return a.getGroupId().equals(b.getGroupId()) && a.getArtifactId().equals(b.getArtifactId())
				&& a.getVersion().equals(b.getVersion());
	}

}
