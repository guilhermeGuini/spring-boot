package guini.springjpa;

import static org.assertj.core.api.Assertions.assertThat;

import guini.springjpa.business.AccountBusiness;
import guini.springjpa.entity.domain.entity.Account;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;

@SpringBootTest
public class OptimistLockTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(OptimistLockTest.class);

    @Autowired
    private AccountBusiness accountBusiness;

    @Test
    public void shouldIncrementVersion() {
        Account account = createAccount();
        assertThat(account.getVersion()).isEqualTo(0);

        Account newBalance = accountBusiness.updateBalance(account.getId(), BigDecimal.valueOf(3000));
        assertThat(newBalance.getVersion()).isEqualTo(2);
    }

    @Test()
    public void shouldThrowExceptionWhenHasConcurrency() throws InterruptedException {

        final Account account = createAccount();

        // create thread for simulate concurrency
        Thread thread = new Thread(() -> {
            LOGGER.info("Running thread");

            /**
             * 1- First will selected account
             * 2- Sleep 3 seconds
             * 3- Update
             */
            Account newBalance = accountBusiness.updateBalance(account.getId(), BigDecimal.valueOf(3000));
            LOGGER.info("Finish thread");
        });
        thread.start();

        // Sleeping to wait the start thread execution
        Thread.sleep(1000L);

        try {

            /**
             * 1- First will selected account
             * 2- Sleep 3 seconds
             * 3- Update
             */
            // update balance to zero while thread not finish
            // when this code to execute, the code above yet not finished
            // then this code will find old account
            LOGGER.info("Update balance to ZERO");
            accountBusiness.updateBalance(account.getId(), BigDecimal.ZERO);

            throw new RuntimeException("fail");
        } catch (ObjectOptimisticLockingFailureException ex) { }
    }

    @Test()
    public void shouldNotThrowExceptionWhenHasNotConcurrency() throws InterruptedException {

        final Account account = createAccount();

        // create thread for simulate concurrency
        Thread thread = new Thread(() -> {
            LOGGER.info("Running thread");
            Account newBalance = accountBusiness.updateBalance(account.getId(), BigDecimal.valueOf(3000));
            LOGGER.info("Finish thread");
        });
        thread.start();
        // with join method, the code bellow will execute only when thread execution to finish
        thread.join();

        Account newAccount = accountBusiness.findById(account.getId()).get();

        // update balance to zero while thread not finish
        LOGGER.info("Update balance to ZERO");
        newAccount = accountBusiness.updateBalance(newAccount.getId(), BigDecimal.ZERO);
        assertThat(newAccount.getBalance().compareTo(BigDecimal.ZERO));
    }

    private Account createAccount() {
        Account account = accountBusiness.createAccount("GUINI", BigDecimal.valueOf(50000.91));
        LOGGER.info("Created account: {}", account);

        assertThat(account).isNotNull();
        assertThat(account.getName()).isEqualTo("GUINI");
        assertThat(account.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(50000.91));
        assertThat(account.getVersion()).isEqualTo(0);
        return account;
    }
}
