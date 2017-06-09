package ch.unibe.scg.kowalski.task;

import java.util.List;
import java.util.Optional;

public class Page<K, E> {

	protected Optional<K> key;
	protected List<E> elements;

	public Page(Optional<K> key, List<E> elements) {
		this.key = key;
		this.elements = elements;
	}

	public Optional<K> getKey() {
		return this.key;
	}

	public List<E> getElements() {
		return this.elements;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.key == null) ? 0 : this.key.hashCode());
		result = prime * result + ((this.elements == null) ? 0 : this.elements.hashCode());
		return result;
	}

	@Override
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
		@SuppressWarnings("unchecked")
		Page<K, E> other = (Page<K, E>) obj;
		if (this.key == null) {
			if (other.key != null) {
				return false;
			}
		} else if (!this.key.equals(other.key)) {
			return false;
		}
		if (this.elements == null) {
			if (other.elements != null) {
				return false;
			}
		} else if (!this.elements.equals(other.elements)) {
			return false;
		}
		return true;
	}

}
