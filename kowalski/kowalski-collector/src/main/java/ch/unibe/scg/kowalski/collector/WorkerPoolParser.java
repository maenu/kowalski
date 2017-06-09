package ch.unibe.scg.kowalski.collector;

import java.net.URISyntaxException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.activemq.artemis.jms.client.ActiveMQQueue;
import org.apache.solr.common.SolrDocument;
import org.eclipse.aether.artifact.Artifact;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jms.core.JmsTemplate;

import ch.unibe.scg.kowalski.configurationlanguage.Facade;
import ch.unibe.scg.kowalski.configurationlanguage.Parser.IOutputNode;
import ch.unibe.scg.kowalski.configurationlanguage.Parser.InputNode;
import ch.unibe.scg.kowalski.configurationlanguage.Parser.MappedOutputNode;
import ch.unibe.scg.kowalski.configurationlanguage.Parser.PageOutputNode;
import ch.unibe.scg.kowalski.configurationlanguage.Parser.QueueOutput;
import ch.unibe.scg.kowalski.configurationlanguage.Parser.Visitor;
import ch.unibe.scg.kowalski.configurationlanguage.Parser.WorkerPoolNode;
import ch.unibe.scg.kowalski.task.Dependent;
import ch.unibe.scg.kowalski.task.Match;
import ch.unibe.scg.kowalski.worker.Reader;

public class WorkerPoolParser {

	public static class WorkerPoolVisitor extends Visitor {

		public static final String TASK_DEPENDENCY = "dependency";
		public static final String TASK_DEPENDENT = "dependent";
		public static final String TASK_MATCH = "match";

		private int size;
		private ActiveMQQueue input;
		private Optional<Integer> readLimit;
		private String task;
		private OutputVisitor outputVisitor;

		@Override
		public void visitWorkerPoolNode(WorkerPoolNode node) {
			this.size = node.getSize();
			node.getInputNode().accept(this);
			this.task = this.newTask(node.getTaskName());
			node.getOutputNode().accept(this);
		}

		public WorkerPool toWorkerPool(JmsTemplate jmsTemplate,
				Supplier<ch.unibe.scg.kowalski.worker.match.Processor> processorFactoryMatch,
				Supplier<ch.unibe.scg.kowalski.worker.dependent.Processor> processorFactoryDependent,
				Supplier<ch.unibe.scg.kowalski.worker.dependency.Processor> processorFactoryDependency) {
			ItemReader<?> reader = this.toReader(jmsTemplate);
			ItemProcessor<?, ?> processor = this.toProcessor(processorFactoryMatch, processorFactoryDependent,
					processorFactoryDependency);
			ItemWriter<?> writer = this.toWriter(jmsTemplate);
			return new WorkerPool(this.size, reader, processor, writer);
		}

		@Override
		public void visitInputNode(InputNode node) {
			this.input = new ActiveMQQueue(node.getName());
			this.readLimit = node.getReadLimit();
		}

		@Override
		public void visitPageOutputNode(PageOutputNode node) {
			this.outputVisitor = new PageOutputVisitor();
			node.accept(this.outputVisitor);
		}

		@Override
		public void visitMappedOutputNode(MappedOutputNode node) {
			this.outputVisitor = new MappedOutputVisitor();
			node.accept(this.outputVisitor);
		}

		@Override
		public void visitQueueOutput(QueueOutput node) {
			this.outputVisitor = new QueueOutputVisitor();
			node.accept(this.outputVisitor);
		}

		private ItemReader<?> toReader(JmsTemplate jmsTemplate) {
			if (this.readLimit.isPresent()) {
				return new LimitedReader<>(jmsTemplate, this.input, this.readLimit.get());
			} else {
				return new Reader<>(jmsTemplate, this.input);
			}
		}

		private ItemProcessor<?, ?> toProcessor(
				Supplier<ch.unibe.scg.kowalski.worker.match.Processor> processorFactoryMatch,
				Supplier<ch.unibe.scg.kowalski.worker.dependent.Processor> processorFactoryDependent,
				Supplier<ch.unibe.scg.kowalski.worker.dependency.Processor> processorFactoryDependency) {
			if (this.task.equals(TASK_MATCH)) {
				return processorFactoryMatch.get();
			}
			if (this.task.equals(TASK_DEPENDENT)) {
				return processorFactoryDependent.get();
			}
			if (this.task.equals(TASK_DEPENDENCY)) {
				return processorFactoryDependency.get();
			}
			throw new IllegalArgumentException("Processor must be one of match, dependent or dependency");
		}

		private ItemWriter<?> toWriter(JmsTemplate jmsTemplate) {
			return this.outputVisitor.toWriter(jmsTemplate);
		}

		private String newTask(String task) {
			if (task.equals(TASK_MATCH)) {
				return TASK_MATCH;
			}
			if (task.equals(TASK_DEPENDENT)) {
				return TASK_DEPENDENT;
			}
			if (task.equals(TASK_DEPENDENCY)) {
				return TASK_DEPENDENCY;
			}
			throw new IllegalArgumentException("Processor must be one of match, dependent or dependency");
		}

	}

	public static class OutputVisitor extends Visitor {

		public ItemWriter<?> toWriter(JmsTemplate jmsTemplate) {
			throw new UnsupportedOperationException();
		}

	}

	public static class QueueOutputVisitor extends OutputVisitor {

		protected ActiveMQQueue queue;
		protected Function<Object, Object> mapper;

		public QueueOutputVisitor() {
			super();
			this.queue = null;
			this.mapper = Function.identity();
		}

		public Sender toSender(JmsTemplate jmsTemplate) {
			return new Sender(jmsTemplate, this.queue, this.mapper);
		}

		@Override
		public void visitQueueOutput(QueueOutput node) {
			this.queue = new ActiveMQQueue(node.getName());
		}

		@Override
		public ItemWriter<?> toWriter(JmsTemplate jmsTemplate) {
			return new Writer<>(this.toSender(jmsTemplate));
		}

	}

	public static class MappedOutputVisitor extends QueueOutputVisitor {

		public static final String MAPPER_NEW_SOLR_QUERY_FOR_ALL_VERSIONS = "newSolrQueryForAllVersions";
		public static final String MAPPER_NEW_SOLR_QUERY_FOR_LATEST_VERSION = "newSolrQueryForLatestVersion";
		public static final String MAPPER_NEW_ARTIFACT_WITH_LATEST_VERSION = "newArtifactWithLatestVersion";
		public static final String MAPPER_NEW_ARTIFACT_WITH_VERSION = "newArtifactWithVersion";
		public static final String MAPPER_NEW_URI = "newUri";

		@Override
		public void visitMappedOutputNode(MappedOutputNode node) {
			super.visitMappedOutputNode(node);
			this.mapper = this.mapper.compose(this.newMapper(node.getName()));
		}

		private Function<Object, Object> newMapper(String mapper) {
			if (mapper.equals(MAPPER_NEW_URI)) {
				return object -> {
					try {
						return Dependent.newUri((Artifact) object);
					} catch (URISyntaxException exception) {
						throw new RuntimeException(exception);
					}
				};
			}
			if (mapper.equals(MAPPER_NEW_ARTIFACT_WITH_VERSION)) {
				return object -> Match.newArtifactWithVersion((SolrDocument) object);
			}
			if (mapper.equals(MAPPER_NEW_ARTIFACT_WITH_LATEST_VERSION)) {
				return object -> Match.newArtifactWithLatestVersion((SolrDocument) object);
			}
			if (mapper.equals(MAPPER_NEW_SOLR_QUERY_FOR_ALL_VERSIONS)) {
				return object -> Match.newSolrQueryForAllVersions((Artifact) object);
			}
			if (mapper.equals(MAPPER_NEW_SOLR_QUERY_FOR_LATEST_VERSION)) {
				return object -> Match.newSolrQueryForLatestVersion((Artifact) object);
			}
			throw new IllegalArgumentException(
					"Mapper must be one of newUri, newArtifactWithVersion, newArtifactWithLatestVersion or newSolrQuery");
		}

	}

	public static class PageOutputVisitor extends OutputVisitor {

		private Optional<MappedOutputVisitor> keyVisitor;
		private Optional<MappedOutputVisitor> elementVisitor;

		public PageOutputVisitor() {
			super();
			this.keyVisitor = Optional.empty();
			this.elementVisitor = Optional.empty();
		}

		@Override
		public void visitPageOutputNode(PageOutputNode node) {
			this.keyVisitor = node.getKeyOutputNode().map(this::newMappedOutputVisitor);
			this.elementVisitor = node.getElementOutputNode().map(this::newMappedOutputVisitor);
		}

		@Override
		public ItemWriter<?> toWriter(JmsTemplate jmsTemplate) {
			Optional<Sender> keySender = this.keyVisitor.map(visitor -> visitor.toSender(jmsTemplate));
			Optional<Sender> elementSender = this.elementVisitor.map(visitor -> visitor.toSender(jmsTemplate));
			return new PageWriter<>(keySender, elementSender);
		}

		private MappedOutputVisitor newMappedOutputVisitor(IOutputNode node) {
			MappedOutputVisitor visitor = new MappedOutputVisitor();
			node.accept(visitor);
			return visitor;
		}

	}

	private JmsTemplate jmsTemplate;
	private Supplier<ch.unibe.scg.kowalski.worker.match.Processor> processorFactoryMatch;
	private Supplier<ch.unibe.scg.kowalski.worker.dependent.Processor> processorFactoryDependent;
	private Supplier<ch.unibe.scg.kowalski.worker.dependency.Processor> processorFactoryDependency;
	private Facade facade;

	public WorkerPoolParser(JmsTemplate jmsTemplate,
			Supplier<ch.unibe.scg.kowalski.worker.match.Processor> processorFactoryMatch,
			Supplier<ch.unibe.scg.kowalski.worker.dependent.Processor> processorFactoryDependent,
			Supplier<ch.unibe.scg.kowalski.worker.dependency.Processor> processorFactoryDependency) {
		this.jmsTemplate = jmsTemplate;
		this.processorFactoryMatch = processorFactoryMatch;
		this.processorFactoryDependent = processorFactoryDependent;
		this.processorFactoryDependency = processorFactoryDependency;
		this.facade = new Facade();
	}

	public WorkerPool parse(String source) {
		WorkerPoolNode node = this.facade.parse(source);
		WorkerPoolVisitor visitor = new WorkerPoolVisitor();
		node.accept(visitor);
		return visitor.toWorkerPool(this.jmsTemplate, this.processorFactoryMatch, this.processorFactoryDependent,
				this.processorFactoryDependency);
	}

}
