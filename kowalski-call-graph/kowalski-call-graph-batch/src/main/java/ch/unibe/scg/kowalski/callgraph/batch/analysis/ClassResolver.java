package ch.unibe.scg.kowalski.callgraph.batch.analysis;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.unibe.scg.kowalski.callgraph.analysis.model.Class;
import ch.unibe.scg.kowalski.callgraph.analysis.utility.Cache;
import ch.unibe.scg.kowalski.callgraph.batch.function.RelaxedConsumer;
import ch.unibe.scg.kowalski.callgraph.batch.function.RelaxedFunction;
import ch.unibe.scg.kowalski.callgraph.batch.function.RelaxedPredicate;

public class ClassResolver implements AutoCloseable {

	public static class NullClassLoader extends ClassLoader {

		@Override
		public URL getResource(String name) {
			return null;
		}

		@Override
		public Enumeration<URL> getResources(String name) throws IOException {
			return new Enumeration<URL>() {

				@Override
				public boolean hasMoreElements() {
					return false;
				}

				@Override
				public URL nextElement() {
					return null;
				}

			};
		}

	}

	public static final Artifact NO_ARTIFACT_FOUND = new DefaultArtifact(
			"no-artifact-found:no-artifact-found:no-artifact-found");
	private static final Logger LOGGER = LoggerFactory.getLogger(ClassResolver.class);

	private URLClassLoader classLoader;
	private List<Artifact> artifacts;
	private List<FileSystem> fileSystemsUp;
	private Cache<Class, Artifact> artifactCache;
	private List<Function<Class, Optional<Artifact>>> strategies;
	private URL[] urls;

	public ClassResolver(URL[] urls, List<Artifact> artifacts) {
		this.urls = urls;
		// class loader without parent, only the class path of the analysis
		this.classLoader = new URLClassLoader(this.urls, new NullClassLoader());
		this.artifacts = artifacts;
		this.artifactCache = new Cache<>();
		this.fileSystemsUp = new ArrayList<>();
		this.strategies = new ArrayList<>();
		this.strategies.add(RelaxedFunction.newFunction(this::resolveByCodeSource));
		this.strategies.add(RelaxedFunction.newFunction(this::resolveByResource));
	}

	public Artifact resolve(Class clazz) {
		return this.artifactCache.getOrPut(clazz, () -> {
			List<Exception> exceptions = new ArrayList<>();
			Artifact artifact = this.strategies.stream().sequential().map(strategy -> {
				try {
					return strategy.apply(clazz);
				} catch (Exception exception) {
					exceptions.add(exception);
					return Optional.<Artifact>empty();
				}
			}).filter(Optional::isPresent).map(Optional::get).findFirst().orElse(NO_ARTIFACT_FOUND);
			if (!clazz.getName().equals("soot.dummy.InvokeDynamic")) {
				// TODO can we do anything with invoke dynamic?
				if (artifact == NO_ARTIFACT_FOUND) {
					if (exceptions.size() > 0 && LOGGER.isDebugEnabled()) {
						exceptions.stream().forEach(
								exception -> LOGGER.warn("No artifact found for {}", clazz.getName(), exception));
					} else {
						LOGGER.debug("No artifact found for {}", clazz.getName());
					}
				}
			}
			LOGGER.trace("Found {} in {}", clazz.getName(), artifact.toString());
			return artifact;
		});
	}

	private Optional<Artifact> resolveByCodeSource(Class clazz) throws ClassNotFoundException, URISyntaxException {
		CodeSource codeSource = this.classLoader.loadClass(clazz.getName()).getProtectionDomain().getCodeSource();
		if (codeSource == null) {
			return Optional.empty();
		}
		return this.find(Paths.get(codeSource.getLocation().toURI()));
	}

	/**
	 * @see <a
	 *      href="http://docs.oracle.com/javase/7/docs/technotes/guides/io/fsp/zipfilesystemprovider.html"http://docs.oracle.com/javase/7/docs/technotes/guides/io/fsp/zipfilesystemprovider.html</a>
	 * @see <a href=
	 *      "http://stackoverflow.com/questions/8014099/how-do-i-convert-a-jarfile-uri-to-the-path-of-jar-file">http://stackoverflow.com/questions/8014099/how-do-i-convert-a-jarfile-uri-to-the-path-of-jar-file</a>
	 * @param clazz
	 * @return
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	private Optional<Artifact> resolveByResource(Class clazz) throws URISyntaxException, IOException {
		URL url = this.classLoader.getResource(clazz.getName().replaceAll("\\.", "/") + ".class");
		if (url == null) {
			return Optional.empty();
		}
		URI uri = url.toURI();
		this.fileSystemsUp.add(FileSystemSynchronizer.INSTANCE.up(uri));
		if (uri.getScheme().equals("jar")) {
			JarURLConnection connection = (JarURLConnection) uri.toURL().openConnection();
			uri = connection.getJarFileURL().toURI();
		}
		return this.find(Paths.get(uri).toAbsolutePath());
	}

	private Optional<Artifact> find(Path path) {
		return this.artifacts.stream().filter(
				RelaxedPredicate.newPredicate(artifact -> path.startsWith(artifact.getFile().getAbsolutePath())))
				.findAny();
	}

	@Override
	public void close() throws Exception {
		this.fileSystemsUp.stream().forEach(RelaxedConsumer.newConsumer(FileSystemSynchronizer.INSTANCE::down));
		this.fileSystemsUp.clear();
		this.classLoader.close();
	}

}
