package com.lundi_m.spazacred_api.api;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
public abstract class CommittedDataTestBase extends ApiFlowTestBase {

    @Autowired protected JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanDatabase() {

        jdbcTemplate.execute("DELETE FROM payment_allocations");
        jdbcTemplate.execute("DELETE FROM payments");
        jdbcTemplate.execute("DELETE FROM credit_transactions");
        jdbcTemplate.execute("DELETE FROM credit_accounts");
        jdbcTemplate.execute("DELETE FROM audit_logs");
        jdbcTemplate.execute("DELETE FROM customers");
        jdbcTemplate.execute("DELETE FROM users WHERE email LIKE '%@test.local'");
    }

    protected ResultActions voidPaymentRequest(long paymentId) throws Exception {
        return mockMvc.perform(post("/api/v1/payments/{id}/void", paymentId)
                .header(AUTH, bearer(owner)).contentType(JSON).content("{\"reason\":\"test\"}"));
    }

    protected ResultActions voidPurchaseRequest(long transactionId) throws Exception {
        return mockMvc.perform(post("/api/v1/credit-transactions/{id}/void", transactionId)
                .header(AUTH, bearer(owner)).contentType(JSON).content("{\"reason\":\"test\"}"));
    }

    protected int statusOf(ResultActions actions) throws Exception {
        return actions.andReturn().getResponse().getStatus();
    }

    protected long count(List<Integer> statuses, int status) {
        return statuses.stream().filter(s -> s == status).count();
    }

    protected List<Integer> inParallel(int copies, Callable<Integer> task) throws Exception {
        return inParallel(Collections.nCopies(copies, task));
    }


    protected List<Integer> inParallel(List<Callable<Integer>> tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        try {
            CountDownLatch ready = new CountDownLatch(tasks.size());
            CountDownLatch go = new CountDownLatch(1);

            List<Future<Integer>> futures = new ArrayList<>();
            for (Callable<Integer> task : tasks) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    go.await();
                    return task.call();
                }));
            }

            ready.await();
            go.countDown();

            List<Integer> results = new ArrayList<>();
            for (Future<Integer> future : futures) {
                results.add(future.get(60, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }
}