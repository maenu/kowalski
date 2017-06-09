package ch.unibe.scg.kowalski.callgraph.batch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

@org.springframework.context.annotation.Configuration
@Import(Configuration.class)
@PropertySource(value = "classpath:application.properties", ignoreResourceNotFound = true)
public class Application {

	public static void main(String[] args) throws IOException, InterruptedException {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Application.class)) {
			Application application = context.getBean(Application.class);
			application.start();
		}
	}

	@Autowired
	private JobLauncher jobLauncher;
	@Value("${" + Configuration.PACKAGE + ".cardinality}")
	private int cardinality;
	@Autowired
	public Supplier<Job> jobFactory;

	public void start() throws IOException, InterruptedException {
		List<JobExecution> jobExecutions = new ArrayList<>();
		// launch jobs
		jobExecutions.addAll(IntStream.range(0, this.cardinality).mapToObj(i -> {
			Job analysisJob = this.jobFactory.get();
			JobParametersBuilder jobParametersBuilder = new JobParametersBuilder();
			jobParametersBuilder.addString("id", analysisJob.getName() + "-" + i, true);
			try {
				return this.jobLauncher.run(analysisJob, jobParametersBuilder.toJobParameters());
			} catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
					| JobParametersInvalidException exception) {
				throw new RuntimeException(exception);
			}
		}).collect(Collectors.toList()));
		// wait for termination
		while (jobExecutions.stream().anyMatch(jobExecution -> jobExecution.getStatus().isRunning())) {
			Thread.sleep(1000);
		}
	}

}
