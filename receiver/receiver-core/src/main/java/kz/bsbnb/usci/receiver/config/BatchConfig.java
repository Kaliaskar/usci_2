package kz.bsbnb.usci.receiver.config;

import kz.bsbnb.usci.receiver.listener.BatchJobListener;
import kz.bsbnb.usci.receiver.reader.CrEntityReader;
import kz.bsbnb.usci.receiver.reader.EavEntityReader;
import kz.bsbnb.usci.receiver.reader.MaintenanceEntityReader;
import kz.bsbnb.usci.receiver.writer.RmiEventEntityWriter;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import javax.sql.DataSource;

/**
 * @author Jandos Iskakov
 */

@Configuration
@EnableBatchProcessing
public class BatchConfig extends DefaultBatchConfigurer {
    private static final int BATCH_CONCURRENCY_LIMIT = 100;

    private JobBuilderFactory jobs;
    private EavEntityReader eavEntityReader;
    private CrEntityReader crEntityReader;
    private RmiEventEntityWriter rmiEventEntityWriter;
    private StepBuilderFactory stepBuilderFactory;
    private BatchJobListener batchJobListener;
    private MaintenanceEntityReader maintenanceEntityReader;

    public BatchConfig(JobBuilderFactory jobs,
                       EavEntityReader eavEntityReader,
                       CrEntityReader crEntityReader,
                       RmiEventEntityWriter rmiEventEntityWriter,
                       StepBuilderFactory stepBuilderFactory,
                       BatchJobListener batchJobListener,
                       MaintenanceEntityReader maintenanceEntityReader) {
        this.jobs = jobs;
        this.eavEntityReader = eavEntityReader;
        this.crEntityReader = crEntityReader;
        this.rmiEventEntityWriter = rmiEventEntityWriter;
        this.stepBuilderFactory = stepBuilderFactory;
        this.batchJobListener = batchJobListener;
        this.maintenanceEntityReader = maintenanceEntityReader;
    }

    @Override
    public void setDataSource(DataSource customerDataSource) {
        // переопределяю метод чтобы он не работал с БД а был inmemory
        // иначе spring batch по умолчанию будет весь этап работы джобов писать в БД что только будет тормозит работу
        // общая практика согласно в github, посему ничего здесь необычного
    }

    @Bean
    public Job batchJobEav() {
        return jobs.get("batchJobEav")
                .preventRestart()
                .listener(batchJobListener)
                .start(stepBatchEav())
                .build();
    }

    /**
     * джоб для работы с батчами формата КР
     */
    @Bean
    public Job batchJobCr() {
        return jobs.get("batchJobCr")
                .preventRestart()
                .listener(batchJobListener)
                .start(stepBatchCr())
                .build();
    }

    @Bean
    public Job batchJobMaintenance() {
        return jobs.get("batchJobMaintenance")
                .preventRestart()
                .listener(batchJobListener)
                .start(stepBatchMaintenance())
                .build();
    }

    @Bean
    public TaskExecutor taskExecutor() {
        SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
        taskExecutor.setConcurrencyLimit(BATCH_CONCURRENCY_LIMIT);
        return taskExecutor;
    }

    @Bean
    public JobLauncher jobLauncher() {
        SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
        jobLauncher.setTaskExecutor(taskExecutor());
        jobLauncher.setJobRepository(getJobRepository());
        return jobLauncher;
    }

    @Bean
    public Step stepBatchEav() {
        return stepBuilderFactory.get("workflowEav")
                .chunk(50)
                .reader(eavEntityReader)
                .writer(rmiEventEntityWriter)
                .build();
    }

    @Bean
    public Step stepBatchMaintenance() {
        return stepBuilderFactory.get("workflowMaintenance")
                .chunk(50)
                .reader(maintenanceEntityReader)
                .writer(rmiEventEntityWriter)
                .build();
    }

    @Bean
    public Step stepBatchCr() {
        return stepBuilderFactory.get("workflowCr")
                .chunk(100)
                .reader(crEntityReader)
                .writer(rmiEventEntityWriter)
                .build();
    }

    @Override
    public JobLauncher createJobLauncher() {
        return jobLauncher();
    }

}
