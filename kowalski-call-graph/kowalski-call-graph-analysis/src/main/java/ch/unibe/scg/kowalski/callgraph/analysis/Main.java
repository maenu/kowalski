package ch.unibe.scg.kowalski.callgraph.analysis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.unibe.scg.kowalski.callgraph.analysis.model.Factory;
import ch.unibe.scg.kowalski.callgraph.analysis.model.Invocation;
import ch.unibe.scg.kowalski.callgraph.analysis.model.Method;
import soot.Body;
import soot.BodyTransformer;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.Transform;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;

public class Main {

	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

	/**
	 * Last argument is output.
	 *
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		Map<Method, Set<Invocation>> data = run(Arrays.copyOfRange(args, 0, args.length - 1));
		Path output = Paths.get(args[args.length - 1]);
		try (Output out = new Output(Files.newOutputStream(output))) {
			(new Kryo()).writeObject(out, data);
		}
	}

	public static Map<Method, Set<Invocation>> run(String[] args) {
		// transformers must be thread safe, as it is called from multiple
		// threads
		Factory factory = new Factory();
		Map<Method, Set<Invocation>> data = new ConcurrentHashMap<>();
		PackManager.v().getPack("jtp").add(new Transform("jtp.analyze", new BodyTransformer() {
			@Override
			protected void internalTransform(Body body, String phase, @SuppressWarnings("rawtypes") Map options) {
				try {
					Method method = factory.toMethod(body.getMethod());
					Set<Invocation> invocations = body.getUnits().stream().filter(unit -> unit instanceof Stmt)
							.map(unit -> (Stmt) unit).map(stmt -> {
								if (!stmt.containsInvokeExpr()) {
									return Optional.<Invocation>empty();
								}
								InvokeExpr invokeExpr = stmt.getInvokeExpr();
								Invocation invocation = factory.toInvocation(stmt, invokeExpr.getMethod());
								return Optional.of(invocation);
							}).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toSet());
					data.put(method, invocations);
				} catch (Throwable throwable) {
					LOGGER.warn("Analyzing failed for {}", body.getMethod().getSignature(), throwable);
				}
			}
		}));
		Scene.v().addBasicClass("java.lang.invoke.LambdaMetafactory", SootClass.SIGNATURES);
		Scene.v().addBasicClass("scala.runtime.java8.JFunction0$mcV$sp", SootClass.HIERARCHY);
		Scene.v().addBasicClass("scala.runtime.java8.JFunction0$mcZ$sp", SootClass.HIERARCHY);
		Scene.v().addBasicClass("scala.runtime.java8.JFunction1$mcDD$sp", SootClass.HIERARCHY);
		Scene.v().addBasicClass("scala.runtime.java8.JFunction1$mcFD$sp", SootClass.HIERARCHY);
		Scene.v().addBasicClass("scala.runtime.java8.JFunction1$mcVI$sp", SootClass.HIERARCHY);
		soot.Main.v().run(args);
		if (LOGGER.isDebugEnabled()) {
			exportJson(data);
		}
		return data;
	}

	public static void exportJson(Map<Method, Set<Invocation>> data) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.setVisibilityChecker(mapper.getVisibilityChecker().withFieldVisibility(Visibility.ANY));
			String formatted = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
			LOGGER.debug("Dereferences: {}", formatted);
		} catch (JsonProcessingException exception) {
			LOGGER.warn("Export of dereferences failed", exception);
		}
	}

}
