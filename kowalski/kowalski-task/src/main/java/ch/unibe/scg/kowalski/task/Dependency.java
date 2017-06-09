package ch.unibe.scg.kowalski.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;

public class Dependency {

	protected Maven maven;

	public Dependency(Maven maven) {
		this.maven = maven;
	}

	/**
	 *
	 * @param artifact
	 * @param scope
	 *            {@link org.eclipse.aether.util.artifact.JavaScopes}
	 * @param dependencySelector
	 * @return
	 * @throws DependencyCollectionException
	 */
	public CollectResult fetchCollectResult(Artifact artifact, String scope, DependencySelector dependencySelector)
			throws DependencyCollectionException {
		RepositorySystem repositorySystem = this.maven.getRepositorySystem();
		CollectRequest collectRequest = this.maven.newCollectRequest(artifact, scope);
		DefaultRepositorySystemSession repositorySystemSession = this.maven.newRepositorySystemSession();
		repositorySystemSession.setDependencySelector(dependencySelector);
		return repositorySystem.collectDependencies(repositorySystemSession, collectRequest);
	}

	public DependencyResult fetchDependencyResult(DependencyNode root, DependencySelector dependencySelector)
			throws DependencyResolutionException {
		DependencyRequest dependencyRequest = this.maven.newDependencyRequest(root);
		DefaultRepositorySystemSession repositorySystemSession = this.maven.newRepositorySystemSession();
		repositorySystemSession.setDependencySelector(dependencySelector);
		return this.maven.getRepositorySystem().resolveDependencies(repositorySystemSession, dependencyRequest);
	}

	public Page<Artifact, Artifact> fetchPage(DependencyResult dependencyResult, boolean includeUnresolved) {
		PreorderNodeListGenerator nodeListGenerator = new PreorderNodeListGenerator();
		dependencyResult.getRoot().accept(nodeListGenerator);
		// remove root from elements, should only include dependencies, not
		// itself
		List<Artifact> elements = new ArrayList<>(nodeListGenerator.getArtifacts(includeUnresolved));
		Artifact root = dependencyResult.getRoot().getArtifact();
		elements.remove(root);
		return new Page<>(Optional.of(root), elements);
	}

}
