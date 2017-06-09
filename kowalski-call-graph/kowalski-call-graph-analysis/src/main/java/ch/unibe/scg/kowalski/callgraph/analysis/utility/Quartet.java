package ch.unibe.scg.kowalski.callgraph.analysis.utility;

public class Quartet<U, V, W, X> {

	private U u;
	private V v;
	private W w;
	private X x;

	public Quartet(U u, V v, W w, X x) {
		this.u = u;
		this.v = v;
		this.w = w;
		this.x = x;
	}

	/**
	 * needed by Kryo
	 */
	@SuppressWarnings("unused")
	private Quartet() {

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

	public X getX() {
		return this.x;
	}

	@Override
	public String toString() {
		return String.format("[%s, %s, %s, %s]", this.u, this.v, this.w, this.x);
	}

}
