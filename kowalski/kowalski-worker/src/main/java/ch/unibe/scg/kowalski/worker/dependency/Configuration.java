package ch.unibe.scg.kowalski.worker.dependency;

import java.util.function.Supplier;

import org.apache.maven.settings.building.SettingsBuildingException;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.graph.selector.ScopeDependencySelector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import ch.unibe.scg.kowalski.task.Dependency;
import ch.unibe.scg.kowalski.task.Maven;

@org.springframework.context.annotation.Configuration(Configuration.PACKAGE + ".configuration")
public class Configuration {

	public static final String PACKAGE = "ch.unibe.scg.kowalski.worker.dependency";

	@Value("${" + PACKAGE + ".catchDependencyCollectionException:#{true}}")
	protected boolean catchDependencyCollectionException;
	@Value("${" + PACKAGE + ".catchDependencyResolutionException:#{true}}")
	protected boolean catchDependencyResolutionException;
	@Value("${" + PACKAGE + ".scope:#{\"" + JavaScopes.COMPILE + "\"}}")
	protected String scope;
	@Value("${" + PACKAGE + ".excludedDependencySelectorScopes:#{new String[] { \"" + JavaScopes.TEST + "\", \""
			+ JavaScopes.PROVIDED + "\", \"" + JavaScopes.SYSTEM + "\" }}}")
	protected String[] excludedDependencySelectorScopes;
	@Value("${" + PACKAGE + ".includeUnresolved:#{false}}")
	protected boolean includeUnresolved;

	@Bean(PACKAGE + ".processorFactory")
	public Supplier<Processor> processorFactory() throws SettingsBuildingException {
		Maven maven = new Maven();
		return () -> new Processor(new Dependency(maven), this.catchDependencyCollectionException,
				this.catchDependencyResolutionException, this.scope,
				new ScopeDependencySelector(this.excludedDependencySelectorScopes), this.includeUnresolved);
	}

}
