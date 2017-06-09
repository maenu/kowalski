package ch.unibe.scg.kowalski.callgraph.batch.function;

public class RelaxedPredicate<I> implements java.util.function.Predicate<I> {

	public static interface Predicate<I> {

		boolean test(I i) throws Throwable;

	}

	public static <I> java.util.function.Predicate<I> newPredicate(RelaxedPredicate.Predicate<I> f) {
		return new RelaxedPredicate<>(f);
	}

	private RelaxedPredicate.Predicate<I> f;

	public RelaxedPredicate(RelaxedPredicate.Predicate<I> f) {
		this.f = f;
	}

	@Override
	public boolean test(I i) {
		try {
			return f.test(i);
		} catch (Throwable throwable) {
			throw new RuntimeException(throwable);
		}
	}

}
