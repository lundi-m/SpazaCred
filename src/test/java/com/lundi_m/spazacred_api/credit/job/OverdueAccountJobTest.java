package com.lundi_m.spazacred_api.credit.job;

import com.lundi_m.spazacred_api.credit.service.OverdueAccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OverdueAccountJobTest {

    @Mock private OverdueAccountService overdueAccountService;

    @InjectMocks private OverdueAccountJob job;

    @Test
    void triesEveryCandidateAccount() {
        when(overdueAccountService.findAccountIdsToSuspend()).thenReturn(List.of(1L, 2L, 3L));
        when(overdueAccountService.suspendIfStillOverdue(anyLong())).thenReturn(true);

        job.suspendOverdueAccounts();

        verify(overdueAccountService).suspendIfStillOverdue(1L);
        verify(overdueAccountService).suspendIfStillOverdue(2L);
        verify(overdueAccountService).suspendIfStillOverdue(3L);
    }

    @Test
    void oneFailingAccountDoesNotStopTheOthers() {
        when(overdueAccountService.findAccountIdsToSuspend()).thenReturn(List.of(1L, 2L, 3L));
        when(overdueAccountService.suspendIfStillOverdue(1L)).thenReturn(true);
        when(overdueAccountService.suspendIfStillOverdue(2L)).thenThrow(new IllegalStateException("boom"));
        when(overdueAccountService.suspendIfStillOverdue(3L)).thenReturn(true);

        job.suspendOverdueAccounts();   // must not throw

        verify(overdueAccountService).suspendIfStillOverdue(3L);
    }

    @Test
    void doesNothingWhenThereAreNoCandidates() {
        when(overdueAccountService.findAccountIdsToSuspend()).thenReturn(List.of());

        job.suspendOverdueAccounts();

        verify(overdueAccountService, never()).suspendIfStillOverdue(anyLong());
    }
}
