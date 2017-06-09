package ch.unibe.scg.kowalski.callgraph.batch.function;

public class RelaxedConsumer<I> implements java.util.function.Consumer<I> {

	public static interface Consumer<I> {

		void accept(I i) throws Throwable;

	}

	public static <I, O> java.util.function.Consumer<I> newConsumer(RelaxedConsumer.Consumer<I> f) {
		return new RelaxedConsumer<>(f);
	}

	private RelaxedConsumer.Consumer<I> f;

	public RelaxedConsumer(RelaxedConsumer.Consumer<I> f) {
		this.f = f;
	}

	@Override
	public void accept(I i) {
		try {
			f.accept(i);
		} catch (Throwable throwable) {
			throw new RuntimeException(throwable);
		}
	}

}
