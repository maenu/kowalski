package ch.unibe.scg.kowalski.configurationlanguage;

import org.junit.Assert;
import org.junit.Test;

import ch.unibe.scg.kowalski.configurationlanguage.Facade;
import ch.unibe.scg.kowalski.configurationlanguage.Parser.InputNode;
import ch.unibe.scg.kowalski.configurationlanguage.Parser.MappedOutputNode;
import ch.unibe.scg.kowalski.configurationlanguage.Parser.PageOutputNode;
import ch.unibe.scg.kowalski.configurationlanguage.Parser.QueueOutput;
import ch.unibe.scg.kowalski.configurationlanguage.Parser.Visitor;
import ch.unibe.scg.kowalski.configurationlanguage.Parser.WorkerPoolNode;

public class ParserTest {

	public class PrintingVisitor extends Visitor {

		private StringBuilder printed;

		public PrintingVisitor() {
			super();
			this.printed = new StringBuilder();
		}

		@Override
		public void visitWorkerPoolNode(WorkerPoolNode node) {
			this.printed = new StringBuilder();
			this.printed.append(node.getSize());
			this.printed.append("x ");
			node.getInputNode().accept(this);
			this.printed.append(" -");
			this.printed.append(node.getTaskName());
			this.printed.append("-> ");
			node.getOutputNode().accept(this);
		}

		@Override
		public void visitInputNode(InputNode node) {
			this.printed.append(node.getName());
			node.getReadLimit().ifPresent(readLimit -> {
				this.printed.append(":");
				this.printed.append(readLimit);
			});
		}

		@Override
		public void visitQueueOutput(QueueOutput node) {
			this.printed.append(node.getName());
		}

		@Override
		public void visitPageOutputNode(PageOutputNode node) {
			this.printed.append("page(");
			node.getKeyOutputNode().ifPresent(outputNode -> outputNode.accept(this));
			this.printed.append(", ");
			node.getElementOutputNode().ifPresent(outputNode -> outputNode.accept(this));
			this.printed.append(")");
		}

		@Override
		public void visitMappedOutputNode(MappedOutputNode node) {
			this.printed.append(node.getName());
			this.printed.append(" > ");
			node.getOutputNode().accept(this);
		}

		public String getPrinted() {
			return this.printed.toString();
		}

	}

	private Facade facade;

	public ParserTest() {
		this.facade = new Facade();
	}

	@Test
	public void testParseSimple() {
		String original = "4x input -match-> dependent";
		WorkerPoolNode workerPoolNode = this.facade.parse(original);
		PrintingVisitor visitor = new PrintingVisitor();
		workerPoolNode.accept(visitor);
		String printed = visitor.getPrinted();
		Assert.assertEquals(original, printed);
	}

	@Test
	public void testParseComplex() {
		String original = "4x input:1 -match-> page(match, dependent)";
		WorkerPoolNode workerPoolNode = this.facade.parse(original);
		PrintingVisitor visitor = new PrintingVisitor();
		workerPoolNode.accept(visitor);
		String printed = visitor.getPrinted();
		Assert.assertEquals(original, printed);
	}

	@Test
	public void testParseVeryComplex() {
		String original = "4x input:1 -match-> page(, newArtifactWithLatestVersion > newUri > dependent)";
		WorkerPoolNode workerPoolNode = this.facade.parse(original);
		PrintingVisitor visitor = new PrintingVisitor();
		workerPoolNode.accept(visitor);
		String printed = visitor.getPrinted();
		Assert.assertEquals(original, printed);
	}

}
