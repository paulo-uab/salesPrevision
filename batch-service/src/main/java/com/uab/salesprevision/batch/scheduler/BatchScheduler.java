package com.uab.salesprevision.batch.scheduler;

import com.uab.salesprevision.batch.model.BatchScheduleConfig;
import com.uab.salesprevision.batch.repository.BatchScheduleConfigRepository;
import com.uab.salesprevision.batch.service.BatchScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BatchScheduler {

    private final BatchScheduleConfigRepository scheduleConfigRepository;
    private final BatchScheduleService batchScheduleService;

    @Value("${scheduling.enabled:true}")
    private boolean schedulingEnabled;

    @Scheduled(fixedDelay = 60_000)
    public void checkSchedules() {
        if (!schedulingEnabled) return;

        List<BatchScheduleConfig> active = scheduleConfigRepository.findByActiveTrue();
        LocalDateTime now = LocalDateTime.now();

        for (BatchScheduleConfig config : active) {
            try {
                if (isDue(config, now)) {
                    log.info("Triggering schedule id={}, cron='{}'", config.getId(), config.getCronExpression());
                    config.setLastRunAt(now);
                    scheduleConfigRepository.save(config);
                    batchScheduleService.runJob(config);
                }
            } catch (Exception e) {
                log.error("Failed to run schedule id={}: {}", config.getId(), e.getMessage());
            }
        }
    }

    private boolean isDue(BatchScheduleConfig config, LocalDateTime now) {
        try {
            CronExpression cron = CronExpression.parse(config.getCronExpression());
            LocalDateTime lastRun = config.getLastRunAt() != null ? config.getLastRunAt() : now.minusMinutes(2);
            LocalDateTime nextAfterLastRun = cron.next(lastRun);
            return nextAfterLastRun != null && !nextAfterLastRun.isAfter(now);
        } catch (Exception e) {
            log.warn("Invalid cron expression for schedule id={}: '{}'", config.getId(), config.getCronExpression());
            return false;
        }
    }
}
