package ch.unibe.scg.kowalski.callgraph.analysis.utility;

public class Triplet<U, V, W> {

	private U u;
	private V v;
	private W w;

	public Triplet(U u, V v, W w) {
		this.u = u;
		this.v = v;
		this.w = w;
	}

	/**
	 * needed by Kryo
	 */
	@SuppressWarnings("unused")
	private Triplet() {

	}

	public U getU() {
		return this.u;
	}

	public V getV() {
		return this.v;
	}

	public W getW() {
		return this.w;
	}

	@Override
	public String toString() {
		return String.format("[%s, %s, %s]", this.u, this.v, this.w);
	}

}
