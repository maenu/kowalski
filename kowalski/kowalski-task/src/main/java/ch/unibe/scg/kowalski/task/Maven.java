package ch.unibe.scg.kowalski.task;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;

import com.google.common.collect.Lists;

/**
 * User settings can be given by <code>maven.settings</code> property. If that
 * property is not set, <code>${user.home}/.m2/settings.xml</code> is used.
 * 
 * Global settings can be given by specifying Maven home by
 * <code>maven.home</code> property. If that property is not given,
 * <code>M2_HOME</code> environment variable is used.
 * 
 * @see <a
 *      href="http://stackoverflow.com/questions/27818659/loading-mavens-settings-xml-for-jcabi-aether-to-use"<http://stackoverflow.com/questions/27818659/loading-mavens-settings-xml-for-jcabi-aether-to-use</a>
 * @see <a href=
 *      "http://wiki.eclipse.org/Aether/Setting_Aether_Up">http://wiki.eclipse.org/Aether/Setting_Aether_Up</a>
 * @see <a href=
 *      "http://wiki.eclipse.org/Aether/Creating_a_Repository_System_Session">http://wiki.eclipse.org/Aether/Creating_a_Repository_System_Session</a>
 * @see <a href=
 *      "http://wiki.eclipse.org/Aether/Resolving_Dependencies">http://wiki.eclipse.org/Aether/Resolving_Dependencies</a>
 * @see <a href=
 *      "http://stackoverflow.com/questions/15094751/how-to-get-all-maven-dependencies-using-aether">http://stackoverflow.com/questions/15094751/how-to-get-all-maven-dependencies-using-aether</a>
 */
public class Maven {

	protected static final String MAVEN_SETTINGS = System.getProperty("maven.settings");
	protected static final String USER_HOME = System.getProperty("user.home");
	protected static final String MAVEN_HOME = System.getProperty("maven.home");
	protected static final String ENV_MAVEN_HOME = System.getenv("M2_HOME");
	protected static final File GLOBAL_SETTINGS = Paths
			.get((MAVEN_HOME != null) ? MAVEN_HOME : (ENV_MAVEN_HOME != null) ? ENV_MAVEN_HOME : ".").resolve("conf")
			.resolve("settings.xml").toFile();
	protected static final File USER_SETTINGS = (MAVEN_SETTINGS != null) ? Paths.get(MAVEN_SETTINGS).toFile()
			: Paths.get(USER_HOME).resolve(".m2").resolve("settings.xml").toFile();
	protected static final LocalRepository DEFAULT_LOCAL_REPOSITORY = new LocalRepository(
			Paths.get(USER_HOME).resolve(".m2").resolve("repository").toFile());
	protected static final RemoteRepository DEFAULT_REMOTE_REPOSITORY = new RemoteRepository.Builder("central",
			"default", "https://repo1.maven.org/maven2/").build();

	protected static Settings newSettings() throws SettingsBuildingException {
		SettingsBuildingRequest settingsBuildingRequest = new DefaultSettingsBuildingRequest();
		settingsBuildingRequest.setSystemProperties(System.getProperties());
		settingsBuildingRequest.setGlobalSettingsFile(GLOBAL_SETTINGS);
		settingsBuildingRequest.setUserSettingsFile(USER_SETTINGS);
		return (new DefaultSettingsBuilderFactory()).newInstance().build(settingsBuildingRequest)
				.getEffectiveSettings();
	}

	protected static DefaultServiceLocator newServiceLocator() {
		DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
		locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
		locator.addService(TransporterFactory.class, FileTransporterFactory.class);
		locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
		return locator;
	}

	protected static LocalRepository newLocalRepository(Settings settings) {
		if (settings.getLocalRepository() != null) {
			return new LocalRepository(settings.getLocalRepository());
		}
		return DEFAULT_LOCAL_REPOSITORY;
	}

	protected static List<RemoteRepository> newRemoteRepositories(Settings settings) {
		Map<String, Profile> profiles = settings.getProfilesAsMap();
		List<RemoteRepository> remoteRepositories = settings.getActiveProfiles().stream().map(activeProfile -> {
			if (!profiles.containsKey(activeProfile)) {
				throw new RuntimeException("Activate profile " + activeProfile + " is not defined");
			}
			return profiles.get(activeProfile);
		}).map(Profile::getRepositories).flatMap(Collection::stream).map(
				repository -> new RemoteRepository.Builder(repository.getId(), "default", repository.getUrl()).build())
				.collect(Collectors.toList());
		if (!remoteRepositories.isEmpty()) {
			return remoteRepositories;
		}
		return Lists.newArrayList(DEFAULT_REMOTE_REPOSITORY);
	}

	protected Settings settings;
	protected DefaultServiceLocator serviceLocator;
	protected LocalRepository localRepository;
	protected List<RemoteRepository> remoteRepositories;

	public Maven() throws SettingsBuildingException {
		this.settings = newSettings();
		this.serviceLocator = newServiceLocator();
		this.localRepository = newLocalRepository(this.settings);
		this.remoteRepositories = newRemoteRepositories(this.settings);
	}

	public DefaultServiceLocator getServiceLocator() {
		return this.serviceLocator;
	}

	public RepositorySystem getRepositorySystem() {
		return this.serviceLocator.getService(RepositorySystem.class);
	}

	public DefaultRepositorySystemSession newRepositorySystemSession() {
		DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
		session.setLocalRepositoryManager(this.serviceLocator.getService(RepositorySystem.class)
				.newLocalRepositoryManager(session, this.localRepository));
		return session;
	}

	/**
	 * 
	 * @param artifact
	 * @param scope
	 *            {@link org.eclipse.aether.util.artifact.JavaScopes}
	 * @return
	 */
	public CollectRequest newCollectRequest(Artifact artifact, String scope) {
		Dependency dependency = new Dependency(artifact, scope);
		CollectRequest collectRequest = new CollectRequest();
		collectRequest.setRoot(dependency);
		this.remoteRepositories.stream().forEach(collectRequest::addRepository);
		return collectRequest;
	}

	public DependencyRequest newDependencyRequest(DependencyNode root) {
		DependencyRequest dependencyRequest = new DependencyRequest();
		dependencyRequest.setRoot(root);
		return dependencyRequest;
	}

}
