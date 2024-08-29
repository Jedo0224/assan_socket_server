package org.asansocketserver.global.config;

import org.asansocketserver.batch.cdc.entity.SensorData;
import org.asansocketserver.batch.cdc.repository.SensorDataRepository;
import org.asansocketserver.batch.cdc.service.SensorDataService;
import org.asansocketserver.domain.watch.entity.Watch;
import org.asansocketserver.domain.watch.repository.WatchRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

@Configuration
public class BatchConfig extends DefaultBatchConfiguration {


    @Bean
    public Job sensorDataJob(JobRepository jobRepository,Step sensorDataStep) {
        return new JobBuilder("sensorDataJob", jobRepository)
                .start(sensorDataStep)
                .build();
    }

    @Bean
    public Step sensorDataStep(JobRepository jobRepository,
                               ItemReader<SensorData> sensorDataReader,
                               ItemProcessor<SensorData, SensorData> sensorDataProcessor,
                               ItemWriter<SensorData> sensorDataWriter,
                               PlatformTransactionManager transactionManager) {

        return new StepBuilder("sensorDataStep", jobRepository)
                .<SensorData, SensorData>chunk(new SimpleCompletionPolicy(10), transactionManager)
                .reader(sensorDataReader)
                .processor(sensorDataProcessor)
                .writer(sensorDataWriter)
                .build();
    }

    @Bean
    public ListItemReader<SensorData> sensorDataReader(SensorDataRepository sensorDataRepository) {
        List<SensorData> sensorData = sensorDataRepository.findAll();
        return new ListItemReader<>(sensorData);
    }

    @Bean
    public ItemProcessor<SensorData, SensorData> sensorDataProcessor(SensorDataService sensorDataService) {
        return sensorData -> sensorDataService.processSensorDataForWatch(sensorData);
    }

    @Bean
    public ItemWriter<SensorData> sensorDataWriter(SensorDataRepository sensorDataRepository) {
        return items -> sensorDataRepository.saveAll(items);
    }

    @Bean
    public PlatformTransactionManager transactionManager() {
        return new ResourcelessTransactionManager();
    }
}
