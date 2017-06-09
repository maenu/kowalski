package ch.unibe.scg.kowalski.callgraph.batch.analysis;

import java.util.Map;
import java.util.Set;

import org.eclipse.aether.artifact.Artifact;

import ch.unibe.scg.kowalski.callgraph.analysis.model.Class;
import ch.unibe.scg.kowalski.callgraph.analysis.model.Invocation;
import ch.unibe.scg.kowalski.callgraph.analysis.model.Method;

public class Result {

	private Artifact artifact;
	private Map<Method, Set<Invocation>> data;
	private Map<Class, Artifact> classArtifacts;
	private Set<Class> classes;
	private Set<Method> methods;

	public Result(Artifact artifact, Map<Method, Set<Invocation>> data, Map<Class, Artifact> classArtifacts,
			Set<Class> classes, Set<Method> methods) {
		this.artifact = artifact;
		this.data = data;
		this.classArtifacts = classArtifacts;
		this.classes = classes;
		this.methods = methods;
	}

	public Artifact getArtifact() {
		return this.artifact;
	}

	public Map<Method, Set<Invocation>> getData() {
		return this.data;
	}

	public Map<Class, Artifact> getClassArtifacts() {
		return this.classArtifacts;
	}

	public Set<Class> getClasses() {
		return this.classes;
	}

	public Set<Method> getMethods() {
		return this.methods;
	}

}
