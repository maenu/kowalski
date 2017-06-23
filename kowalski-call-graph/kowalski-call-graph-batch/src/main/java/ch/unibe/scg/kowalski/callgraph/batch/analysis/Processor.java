package ch.unibe.scg.kowalski.callgraph.batch.analysis;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.aether.artifact.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

import ch.unibe.scg.kowalski.callgraph.analysis.model.Class;
import ch.unibe.scg.kowalski.callgraph.analysis.model.Invocation;
import ch.unibe.scg.kowalski.callgraph.analysis.model.Method;
import ch.unibe.scg.kowalski.callgraph.batch.function.RelaxedFunction;
import ch.unibe.scg.kowalski.task.Page;

public class Processor implements ItemProcessor<Page<Artifact, Artifact>, Result> {

	protected static final Logger LOGGER = LoggerFactory.getLogger(Processor.class);

	private AnalysisRunner analysisRunner;
	private Artifact analysisJreArtifact;
	private LastModified lastModified;
	private String groupIdFilter;

	public Processor(AnalysisRunner analysisRunner, Artifact analysisJreArtifact, File lastModifiedCache,
			String groupIdFilter) {
		this.analysisRunner = analysisRunner;
		this.analysisJreArtifact = analysisJreArtifact;
		this.lastModified = new LastModified(lastModifiedCache);
		this.groupIdFilter = groupIdFilter;
	}

	@Override
	public Result process(Page<Artifact, Artifact> page) throws Exception {
		Artifact artifact = this.attributeWithTimestamp(page.getKey().get());
		List<Artifact> dependencies = page.getElements().stream().map(this::attributeWithTimestamp)
				.collect(Collectors.toList());
		List<Artifact> classPathArtifacts = this.newClassPathArtifacts(artifact, dependencies);
		Map<Method, Set<Invocation>> data = this.runAnalysis(artifact, classPathArtifacts);
		Set<Method> methods = this.extractAllMethods(data);
		Set<Class> classes = this.extractAllClasses(methods);
		Map<Class, Artifact> classArtifacts = this.mapArtifactToClasses(classPathArtifacts, classes);
		// filter artifacts
		Map<Class, Artifact> invokedClassArtifacts = classArtifacts.entrySet().stream()
				.filter(entry -> entry.getValue().getGroupId().matches(this.groupIdFilter))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		data = data.entrySet().stream().map(entry -> {
			entry.setValue(entry.getValue().stream()
					.filter(invocation -> invokedClassArtifacts.containsKey(invocation.getMethod().getClazz()))
					.collect(Collectors.toSet()));
			return entry;
		}).filter(entry -> !entry.getValue().isEmpty())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		methods = this.extractAllMethods(data);
		classes = this.extractAllClasses(methods);
		classArtifacts = this.mapArtifactToClasses(classPathArtifacts, classes);
		return new Result(artifact, data, classArtifacts, classes, methods);
	}

	private Artifact attributeWithTimestamp(Artifact artifact) {
		try {
			Date date = this.lastModified.fetch(LastModified.newUri(artifact));
			Map<String, String> properties = new HashMap<>();
			properties.put("timestamp", "" + date.getTime());
			return artifact.setProperties(properties);
		} catch (Exception exception) {
			LOGGER.info("Could not get timestamp for {}: {}", artifact.toString(), exception.toString());
			return artifact;
		}
	}

	private Set<Method> extractAllMethods(Map<Method, Set<Invocation>> data) {
		Set<Method> methods = new HashSet<>();
		methods.addAll(data.keySet());
		methods.addAll(
				data.values().stream().flatMap(Set::stream).map(Invocation::getMethod).collect(Collectors.toSet()));
		return methods;
	}

	private Set<Class> extractAllClasses(Set<Method> methods) {
		return methods.stream().map(Method::getClazz).collect(Collectors.toSet());
	}

	private List<Artifact> newClassPathArtifacts(Artifact artifact, List<Artifact> dependencies) {
		List<Artifact> artifacts = new ArrayList<>(dependencies);
		artifacts.add(0, artifact);
		artifacts.add(this.analysisJreArtifact);
		return artifacts;
	}

	private Map<Method, Set<Invocation>> runAnalysis(Artifact patientArtifact, List<Artifact> classPathArtifacts)
			throws ClassNotFoundException, IOException, InterruptedException,
			AnalysisRunner.AnalysisRunFailedException {
		String patientClassPath = String.join(System.getProperty("path.separator"), classPathArtifacts.stream()
				.map(artifact -> artifact.getFile().getAbsolutePath()).collect(Collectors.toList()));
		return this.analysisRunner.run(patientClassPath, patientArtifact);
	}

	private Map<Class, Artifact> mapArtifactToClasses(List<Artifact> classPathArtifacts, Set<Class> classes)
			throws Exception {
		try (ClassResolver classResolver = newClassResolver(classPathArtifacts)) {
			return classes.stream().collect(Collectors.toMap(clazz -> clazz,
					RelaxedFunction.newFunction(clazz -> classResolver.resolve(clazz))));
		}
	}

	private ClassResolver newClassResolver(List<Artifact> classPathArtifacts) {
		return new ClassResolver(classPathArtifacts.stream()
				.map(RelaxedFunction.newFunction(artifact -> artifact.getFile().toURI().toURL())).toArray(URL[]::new),
				classPathArtifacts);
	}

}
