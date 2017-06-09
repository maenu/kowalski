package ch.unibe.scg.kowalski.callgraph.analysis.model;

public class Class {

	private String name;

	public Class(String name) {
		this.name = name;
	}

	/**
	 * Required by Kryo.
	 */
	@SuppressWarnings("unused")
	private Class() {

	}

	public String getName() {
		return this.name;
	}

	@Override
	public String toString() {
		return String.format("[%s]", this.name);
	}

}
