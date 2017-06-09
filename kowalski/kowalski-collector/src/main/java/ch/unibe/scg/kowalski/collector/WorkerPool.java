package ch.unibe.scg.kowalski.collector;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;

public class WorkerPool {

	private int size;
	private ItemReader<?> reader;
	private ItemProcessor<?, ?> processor;
	private ItemWriter<?> writer;

	public WorkerPool(int size, ItemReader<?> reader, ItemProcessor<?, ?> processor, ItemWriter<?> writer) {
		this.size = size;
		this.reader = reader;
		this.processor = processor;
		this.writer = writer;
	}

	public int getSize() {
		return this.size;
	}

	public ItemReader<?> getReader() {
		return this.reader;
	}

	public ItemProcessor<?, ?> getProcessor() {
		return this.processor;
	}

	public ItemWriter<?> getWriter() {
		return this.writer;
	}

}
