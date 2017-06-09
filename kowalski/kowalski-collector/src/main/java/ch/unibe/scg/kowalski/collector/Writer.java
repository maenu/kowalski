package ch.unibe.scg.kowalski.collector;

import java.util.List;

import org.springframework.batch.item.ItemWriter;

public class Writer<O> implements ItemWriter<O> {

	protected Sender mappingSender;

	public Writer(Sender mappingSender) {
		this.mappingSender = mappingSender;
	}

	@Override
	public void write(List<? extends O> items) throws Exception {
		items.stream().forEach(item -> this.mappingSender.send(item));
	}

}
