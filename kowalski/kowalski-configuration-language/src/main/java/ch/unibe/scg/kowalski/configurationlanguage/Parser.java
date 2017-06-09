package ch.unibe.scg.kowalski.configurationlanguage;

import org.parboiled.BaseParser;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.annotations.SuppressSubnodes;
import org.parboiled.support.Var;

@BuildParseTree
public class Parser extends BaseParser<Object> {

	public static class Visitor {

		public void visitWorkerPoolNode(WorkerPoolNode node) {
			node.getInputNode().accept(this);
			node.getOutputNode().accept(this);
		}

		public void visitInputNode(InputNode node) {
			// nop
		}

		public void visitQueueOutput(QueueOutput node) {
			// nop
		}

		public void visitPageOutputNode(PageOutputNode node) {
			node.getKeyOutputNode().ifPresent(outputNode -> outputNode.accept(this));
			node.getElementOutputNode().ifPresent(outputNode -> outputNode.accept(this));
		}

		public void visitMappedOutputNode(MappedOutputNode node) {
			node.getOutputNode().accept(this);
		}

	}

	public static interface IVisitable {

		void accept(Visitor visitor);

	}

	public static interface IOutputNode extends IVisitable {

	}

	public static class InputNode implements IVisitable {

		private String name;
		private java.util.Optional<Integer> readLimit;

		public InputNode(String name, java.util.Optional<Integer> readLimit) {
			this.name = name;
			this.readLimit = readLimit;
		}

		public String getName() {
			return this.name;
		}

		public java.util.Optional<Integer> getReadLimit() {
			return this.readLimit;
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.visitInputNode(this);
		}

	}

	public static class QueueOutput implements IOutputNode {

		private String name;

		public QueueOutput(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.visitQueueOutput(this);
		}

	}

	public static class MappedOutputNode implements IOutputNode {

		private IOutputNode outputNode;
		private String name;

		public MappedOutputNode(IOutputNode outputNode, String name) {
			this.outputNode = outputNode;
			this.name = name;
		}

		public IOutputNode getOutputNode() {
			return this.outputNode;
		}

		public String getName() {
			return this.name;
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.visitMappedOutputNode(this);
		}

	}

	public static class PageOutputNode implements IOutputNode {

		private java.util.Optional<IOutputNode> keyOutputNode;
		private java.util.Optional<IOutputNode> elementOutputNode;

		public PageOutputNode(java.util.Optional<IOutputNode> keyOutputNode,
				java.util.Optional<IOutputNode> elementOutputNode) {
			this.keyOutputNode = keyOutputNode;
			this.elementOutputNode = elementOutputNode;
		}

		public java.util.Optional<IOutputNode> getKeyOutputNode() {
			return this.keyOutputNode;
		}

		public java.util.Optional<IOutputNode> getElementOutputNode() {
			return this.elementOutputNode;
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.visitPageOutputNode(this);
		}

	}

	public static class WorkerPoolNode implements IVisitable {

		private Integer size;
		private InputNode inputNode;
		private String taskName;
		private IOutputNode outputNode;

		public WorkerPoolNode(IOutputNode outputNode, String taskName, InputNode inputNode, Integer size) {
			this.size = size;
			this.inputNode = inputNode;
			this.taskName = taskName;
			this.outputNode = outputNode;
		}

		public Integer getSize() {
			return this.size;
		}

		public InputNode getInputNode() {
			return this.inputNode;
		}

		public String getTaskName() {
			return this.taskName;
		}

		public IOutputNode getOutputNode() {
			return this.outputNode;
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.visitWorkerPoolNode(this);
		}

	}

	Rule WorkerPool() {
		return Sequence(Integer(), "x ", Input(), " -", Identifier(), "-> ", Output(),
				push(new WorkerPoolNode((IOutputNode) pop(), (String) pop(), (InputNode) pop(), (Integer) pop())));
	}

	Rule Input() {
		Var<java.util.Optional<Integer>> limit = new Var<>(java.util.Optional.empty());
		return Sequence(Identifier(),
				Optional(Sequence(':', Integer(), limit.set(java.util.Optional.of((Integer) pop())))),
				push(new InputNode((String) pop(), limit.get())));
	}

	Rule Output() {
		return FirstOf(PageOutput(), MappedOutput(), QueueOutput());
	}

	Rule QueueOutput() {
		return Sequence(Identifier(), push(new QueueOutput((String) pop())));
	}

	Rule PageOutput() {
		Var<java.util.Optional<IOutputNode>> key = new Var<>(java.util.Optional.empty());
		Var<java.util.Optional<IOutputNode>> element = new Var<>(java.util.Optional.empty());
		return Sequence("page(",
				Optional(Sequence(FirstOf(MappedOutput(), QueueOutput()),
						key.set(java.util.Optional.of((IOutputNode) pop())))),
				", ",
				Optional(Sequence(FirstOf(MappedOutput(), QueueOutput()),
						element.set(java.util.Optional.of((IOutputNode) pop())))),
				")", push(new PageOutputNode(key.get(), element.get())));
	}

	Rule MappedOutput() {
		return Sequence(Identifier(), " > ", FirstOf(MappedOutput(), QueueOutput()),
				push(new MappedOutputNode((IOutputNode) pop(), (String) pop())));
	}

	@SuppressSubnodes
	Rule Identifier() {
		return Sequence(Sequence(Letter(), ZeroOrMore(LetterOrDigit())), push(match()));
	}

	Rule LetterOrDigit() {
		return FirstOf(Letter(), Digit());
	}

	@SuppressSubnodes
	Rule Integer() {
		return Sequence(FirstOf('0', Sequence(CharRange('1', '9'), ZeroOrMore(Digit()))),
				push(Integer.parseInt(match())));
	}

	Rule Letter() {
		return FirstOf(CharRange('a', 'z'), CharRange('A', 'Z'), '_', '$');
	}

	Rule Digit() {
		return CharRange('0', '9');
	}

}