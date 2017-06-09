package ch.unibe.scg.kowalski.callgraph.batch.analysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.eclipse.aether.artifact.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.google.common.reflect.TypeToken;

import ch.unibe.scg.kowalski.callgraph.analysis.Main;
import ch.unibe.scg.kowalski.callgraph.analysis.model.Invocation;
import ch.unibe.scg.kowalski.callgraph.analysis.model.Method;

public class AnalysisRunner {

	public static class AnalysisRunFailedException extends Exception {

		private static final long serialVersionUID = 1L;

		public AnalysisRunFailedException(int exitValue, Artifact patient) {
			super(String.format("Command exited with %d, see log of %s", exitValue, patient.toString()));
		}

	}

	private static final Logger LOGGER = LoggerFactory.getLogger(AnalysisRunner.class);

	private long timeout;
	private String java;
	private String classPath;
	private String mainClass;

	public AnalysisRunner(long timeout) {
		this.timeout = timeout;
		this.java = Paths.get(System.getProperty("java.home")).resolve("bin").resolve("java").toAbsolutePath()
				.toString();
		this.classPath = System.getProperty("java.class.path");
		this.mainClass = Main.class.getName();
	}

	@SuppressWarnings("unchecked")
	public Map<Method, Set<Invocation>> run(String patientClassPath, Artifact patient)
			throws IOException, InterruptedException, AnalysisRunFailedException, ClassNotFoundException {
		// create temporary file for output
		Path analysisOutput = Files.createTempFile("kowalski-call-graph-batch-analysis-runner-", ".dat");
		// TODO use G.reset() instead of fork and serialize, make sure only one
		// job runs, start multiple job-vms
		try {
			// TODO jmx, arguments should be escaped
			// -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=1044
			// run command
			String command = String.format(
					"%s -Xmx2g -classpath %s %s -output-format none -allow-phantom-refs -p jb use-original-names --keep-line-number --process-dir %s --soot-class-path %s %s",
					this.java, this.classPath, this.mainClass, patient.getFile().getAbsolutePath(), patientClassPath,
					analysisOutput.toAbsolutePath().toString());
			Process process = Runtime.getRuntime().exec(command);
			String mdcPatient = MDC.get(Reader.PATIENT);
			this.pipe(mdcPatient, process.getInputStream(), LOGGER::debug);
			this.pipe(mdcPatient, process.getErrorStream(), LOGGER::warn);
			if (!process.waitFor(this.timeout, TimeUnit.MILLISECONDS)) {
				LOGGER.error("Killing analysis, took too long");
				process.destroyForcibly();
			}
			// wait and check exit value
			int exitValue = process.waitFor();
			if (exitValue != 0) {
				throw new AnalysisRunFailedException(exitValue, patient);
			}
			// deserialize output
			try (Input input = new Input(Files.newInputStream(analysisOutput))) {
				return (Map<Method, Set<Invocation>>) (new Kryo()).readObject(input,
						(new TypeToken<HashMap<Method, HashSet<Invocation>>>() {

							private static final long serialVersionUID = 1L;

						}).getRawType());
			}
		} finally {
			// delete temporary file
			analysisOutput.toFile().delete();
		}
	}

	private void pipe(final String mdcPatient, final InputStream in, final Consumer<String> consumer) {
		Thread thread = new Thread(new Runnable() {

			@Override
			public void run() {
				MDC.put(Reader.PATIENT, mdcPatient);
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
				try {
					do {
						String line = reader.readLine();
						if (line == null) {
							break;
						}
						consumer.accept(line);
					} while (true);
				} catch (IOException exception) {
					LOGGER.error("While reading analysis stream", exception);
				}
			}

		});
		thread.start();
	}

}
