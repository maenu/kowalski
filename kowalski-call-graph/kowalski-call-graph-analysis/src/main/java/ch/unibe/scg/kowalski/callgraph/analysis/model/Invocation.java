package ch.unibe.scg.kowalski.callgraph.analysis.model;

public class Invocation {

	private Method method;
	private int line;
	private String statement;

	public Invocation(Method method, int line, String statement) {
		this.method = method;
		this.line = line;
		this.statement = statement;
	}

	/**
	 * Required by Kryo.
	 */
	protected Invocation() {

	}

	public Method getMethod() {
		return this.method;
	}

	public int getLine() {
		return this.line;
	}

	public String getStatement() {
		return this.statement;
	}

	@Override
	public String toString() {
		return String.format("[%s %s %s]", this.method.toString(), this.line, this.statement);
	}

}
