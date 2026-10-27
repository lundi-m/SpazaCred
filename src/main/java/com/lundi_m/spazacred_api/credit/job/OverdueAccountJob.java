package com.lundi_m.spazacred_api.credit.job;

import com.lundi_m.spazacred_api.credit.service.OverdueAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OverdueAccountJob {
    private final OverdueAccountService overdueAccountService;

    @Scheduled(
            cron = "${credit.overdue-job-cron}",
            zone = "${credit.overdue-job-zone}"
    )
    public void suspendOverdueAccounts() {
        List<Long> accountIds = overdueAccountService.findAccountIdsToSuspend();
        log.info("Overdue job started: {} candidate account(s)", accountIds.size());

        int suspended = 0;
        int failed = 0;

        for (Long accountId : accountIds) {
            try {
                if (overdueAccountService.suspendIfStillOverdue(accountId)) {
                    suspended++;
                }
            } catch (RuntimeException exception) {
                failed++;
                log.error("Overdue job failed for account {}", accountId, exception);
            }
        }

        log.info("Overdue job finished: {} suspended, {} failed", suspended, failed);
    }
}
