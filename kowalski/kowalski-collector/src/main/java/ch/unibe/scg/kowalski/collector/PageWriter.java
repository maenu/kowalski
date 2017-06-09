package ch.unibe.scg.kowalski.collector;

import java.util.List;
import java.util.Optional;

import org.springframework.batch.item.ItemWriter;

import ch.unibe.scg.kowalski.task.Page;

public class PageWriter<K, E> implements ItemWriter<Page<K, E>> {

	protected Optional<Sender> mappingSenderKey;
	protected Optional<Sender> mappingSenderElement;

	public PageWriter(Optional<Sender> mappingSenderKey, Optional<Sender> mappingSenderElement) {
		this.mappingSenderKey = mappingSenderKey;
		this.mappingSenderElement = mappingSenderElement;
	}

	@Override
	public void write(List<? extends Page<K, E>> pages) throws Exception {
		pages.stream().forEach(page -> {
			page.getKey().ifPresent(key -> this.mappingSenderKey.ifPresent(mappingSender -> mappingSender.send(key)));
			page.getElements().stream().forEach(
					element -> this.mappingSenderElement.ifPresent(mappingSender -> mappingSender.send(element)));
		});
	}

}
