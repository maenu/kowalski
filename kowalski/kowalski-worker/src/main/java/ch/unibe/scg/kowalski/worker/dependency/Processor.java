package ch.unibe.scg.kowalski.worker.dependency;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

import ch.unibe.scg.kowalski.task.Dependency;
import ch.unibe.scg.kowalski.task.Page;

public class Processor implements ItemProcessor<Artifact, Page<Artifact, Artifact>> {

	protected static final Logger LOGGER = LoggerFactory.getLogger(Processor.class);

	protected Dependency fetcher;
	protected boolean catchDependencyCollectionException;
	protected boolean catchDependencyResolutionException;
	protected String scope;
	protected DependencySelector dependencySelector;
	protected boolean includeUnresolved;

	public Processor(Dependency fetcher, boolean catchDependencyCollectionException,
			boolean catchDependencyResolutionException, String scope, DependencySelector dependencySelector,
			boolean includeUnresolved) {
		super();
		this.fetcher = fetcher;
		this.catchDependencyCollectionException = catchDependencyCollectionException;
		this.catchDependencyResolutionException = catchDependencyResolutionException;
		this.scope = scope;
		this.dependencySelector = dependencySelector;
		this.includeUnresolved = includeUnresolved;
	}

	@Override
	public Page<Artifact, Artifact> process(Artifact artifact)
			throws DependencyResolutionException, DependencyCollectionException {
		LOGGER.info("Processing {}", artifact);
		CollectResult collectResult;
		try {
			collectResult = this.fetcher.fetchCollectResult(artifact, this.scope, this.dependencySelector);
		} catch (DependencyCollectionException exception) {
			if (!this.catchDependencyCollectionException) {
				throw exception;
			}
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Catched DependencyCollectionException while collecting dependencies for {}", artifact,
						exception);
			} else {
				Throwable cause = exception;
				while (cause.getCause() != null && cause.getCause() != cause) {
					cause = cause.getCause();
				}
				LOGGER.info("Catched DependencyCollectionException while collecting dependencies for {}: {}", artifact,
						cause.getMessage());
			}
			collectResult = exception.getResult();
			// if root could not be collected, we are screwed, so fail
			if (collectResult.getRoot() == null) {
				throw exception;
			}
		}
		DependencyResult dependencyResult;
		try {
			dependencyResult = this.fetcher.fetchDependencyResult(collectResult.getRoot(), this.dependencySelector);
		} catch (DependencyResolutionException exception) {
			if (!this.catchDependencyResolutionException) {
				throw exception;
			}
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Catched DependencyResolutionException while resolving dependencies for {}", artifact,
						exception);
			} else {
				Throwable cause = exception;
				while (cause.getCause() != null && cause.getCause() != cause) {
					cause = cause.getCause();
				}
				LOGGER.info("Catched DependencyResolutionException while resolving dependencies for {}: {}", artifact,
						cause.getMessage());
			}
			dependencyResult = exception.getResult();
		}
		return this.fetcher.fetchPage(dependencyResult, this.includeUnresolved);
	}

}
