package ch.unibe.scg.kowalski.callgraph.analysis.model;

public class Method {

	private Class clazz;
	private String signature;

	public Method(Class clazz, String signature) {
		this.clazz = clazz;
		this.signature = signature;
	}

	/**
	 * Required by Kryo.
	 */
	protected Method() {

	}

	public Class getClazz() {
		return this.clazz;
	}

	public String getSignature() {
		return this.signature;
	}

	@Override
	public String toString() {
		return String.format("[%s %s]", this.clazz.getName(), this.signature);
	}

}
