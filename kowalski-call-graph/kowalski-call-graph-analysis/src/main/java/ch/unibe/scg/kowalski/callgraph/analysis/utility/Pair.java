package ch.unibe.scg.kowalski.callgraph.analysis.utility;

public class Pair<U, V> {

	private U u;
	private V v;

	public Pair(U u, V v) {
		this.u = u;
		this.v = v;
	}

	/**
	 * needed by Kryo
	 */
	@SuppressWarnings("unused")
	private Pair() {

	}

	public U getU() {
		return this.u;
	}

	public V getV() {
		return this.v;
	}

	@Override
	public String toString() {
		return String.format("[%s, %s]", this.u, this.v);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.u == null) ? 0 : this.u.hashCode());
		result = prime * result + ((this.v == null) ? 0 : this.v.hashCode());
		return result;
	}

	@Override
	@SuppressWarnings("rawtypes")
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Pair other = (Pair) obj;
		if (this.u == null) {
			if (other.u != null) {
				return false;
			}
		} else if (!this.u.equals(other.u)) {
			return false;
		}
		if (this.v == null) {
			if (other.v != null) {
				return false;
			}
		} else if (!this.v.equals(other.v)) {
			return false;
		}
		return true;
	}

}
