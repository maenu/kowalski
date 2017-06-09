package ch.unibe.scg.kowalski.callgraph.batch.function;

public class RelaxedSupplier<O> implements java.util.function.Supplier<O> {

	public static interface Supplier<O> {

		O get() throws Throwable;

	}

	public static <O> java.util.function.Supplier<O> newSupplier(RelaxedSupplier.Supplier<O> f) {
		return new RelaxedSupplier<>(f);
	}

	private RelaxedSupplier.Supplier<O> f;

	public RelaxedSupplier(RelaxedSupplier.Supplier<O> f) {
		this.f = f;
	}

	@Override
	public O get() {
		try {
			return f.get();
		} catch (Throwable throwable) {
			throw new RuntimeException(throwable);
		}
	}

}
