package ch.unibe.scg.kowalski.callgraph.batch.function;

public class RelaxedFunction<I, O> implements java.util.function.Function<I, O> {

	public static interface Function<I, O> {

		O apply(I i) throws Throwable;

	}

	public static <I, O> java.util.function.Function<I, O> newFunction(RelaxedFunction.Function<I, O> f) {
		return new RelaxedFunction<>(f);
	}

	private RelaxedFunction.Function<I, O> f;

	public RelaxedFunction(RelaxedFunction.Function<I, O> f) {
		this.f = f;
	}

	@Override
	public O apply(I i) {
		try {
			return f.apply(i);
		} catch (Throwable throwable) {
			throw new RuntimeException(throwable);
		}
	}

}
