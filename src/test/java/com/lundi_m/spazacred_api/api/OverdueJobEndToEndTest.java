package com.lundi_m.spazacred_api.api;

import com.lundi_m.spazacred_api.credit.job.OverdueAccountJob;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OverdueJobEndToEndTest extends CommittedDataTestBase {

    @Autowired private OverdueAccountJob overdueAccountJob;
    @Autowired private Clock clock;

    @Test
    void onlyDebtsMoreThanTheGracePeriodOverdueSuspendTheAccount() throws Exception {
        long eightDays = customerWithDebtOverdueBy(8);
        long sevenDays = customerWithDebtOverdueBy(7);
        long threeDays = customerWithDebtOverdueBy(3);
        long paidOff = customerWithDebtOverdueBy(20);
        pay(paidOff, "100.00");
        overdueAccountJob.suspendOverdueAccounts();

        assertThat(accountStatus(eightDays)).isEqualTo("SUSPENDED");
        assertThat(accountStatus(sevenDays)).isEqualTo("ACTIVE");
        assertThat(accountStatus(threeDays)).isEqualTo("ACTIVE");
        assertThat(accountStatus(paidOff)).isEqualTo("ACTIVE");
    }

    @Test
    void theSuspensionIsASystemActionWithTheDetailsAndIsNotRepeated() throws Exception {
        long customerId = customerWithDebtOverdueBy(10);

        overdueAccountJob.suspendOverdueAccounts();
        overdueAccountJob.suspendOverdueAccounts();

        long accountId = accountIdOf(customerId);
        assertThat(autoSuspensions(accountId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs WHERE action = 'ACCOUNT_AUTO_SUSPENDED' "
                + "AND entity_id = ? AND user_id IS NULL", Long.class, accountId)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT details ->> 'reason' FROM audit_logs WHERE action = 'ACCOUNT_AUTO_SUSPENDED' "
                + "AND entity_id = ?", String.class, accountId)).isEqualTo("OVERDUE_DEBT");
    }

    @Test
    void anAccountAlreadySuspendedByAPersonIsLeftAlone() throws Exception {
        long customerId = customerWithDebtOverdueBy(10);
        mockMvc.perform(patch("/api/v1/customers/{id}/account/suspend", customerId)
                        .header(AUTH, bearer(owner)).contentType(JSON).content("{\"reason\":\"By hand\"}"))
                .andExpect(status().isOk());

        overdueAccountJob.suspendOverdueAccounts();

        assertThat(autoSuspensions(accountIdOf(customerId))).isZero();
    }

    @Test
    void anAccountReactivatedWhileStillOverdueIsSuspendedAgain() throws Exception {

        long customerId = customerWithDebtOverdueBy(10);
        overdueAccountJob.suspendOverdueAccounts();

        mockMvc.perform(patch("/api/v1/customers/{id}/account/reactivate", customerId)
                        .header(AUTH, bearer(owner)).contentType(JSON).content("{\"reason\":\"Promised to pay\"}"))
                .andExpect(status().isOk());
        assertThat(accountStatus(customerId)).isEqualTo("ACTIVE");

        overdueAccountJob.suspendOverdueAccounts();

        assertThat(accountStatus(customerId)).isEqualTo("SUSPENDED");
        assertThat(autoSuspensions(accountIdOf(customerId))).isEqualTo(2);
    }


    private long customerWithDebtOverdueBy(int days) throws Exception {
        long customerId = createCustomer("1000.00");
        long purchaseId = buy(customerId, "100.00", 5);
        jdbcTemplate.update("UPDATE credit_transactions SET due_date = ? WHERE id = ?",
                LocalDate.now(clock).minusDays(days), purchaseId);
        return customerId;
    }

    private String accountStatus(long customerId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM credit_accounts WHERE customer_id = ?",
                String.class,
                customerId
        );
    }

    private long accountIdOf(long customerId) {
        Long accountId = jdbcTemplate.queryForObject(
                "SELECT id FROM credit_accounts WHERE customer_id = ?",
                Long.class,
                customerId
        );

        return Objects.requireNonNullElse(accountId, 0L);
    }

    private long autoSuspensions(long accountId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs WHERE action = 'ACCOUNT_AUTO_SUSPENDED' AND entity_id = ?",
                Long.class,
                accountId
        );

        return Objects.requireNonNullElse(count, 0L);
    }
}