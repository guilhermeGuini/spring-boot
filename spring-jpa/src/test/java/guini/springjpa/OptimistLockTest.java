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

    /**
     *
     * Cenário:
     *
     * 1- Thread 1 e Thread 2 obtém o mesmo registro (garantimos isso com o sleep).
     *
     * 2- Thread 1 atualiza o saldo da conta para 3000.
     *
     * 3- Thread 2 que ainda está com o registro antigo, tenta atualizar o saldo para 0,
     *    porém sem sucesso pois o registro está desatualizado.
     *
     * @throws InterruptedException
     */
    @Test()
    public void shouldThrowExceptionWhenHasConcurrency() throws InterruptedException {

        final Account account = createAccount();

        // criando thread para simular processo concorrente
        Thread thread = new Thread(() -> {
            LOGGER.info("Running thread");

            /**
             * Dentro do método updateBalance será realizado os três passos
             * 1- Busca conta usando optimistic lock
             * 2- Dorme 3 segundos
             * 3- Atualiza
             */
            Account newBalance = accountBusiness.updateBalance(account.getId(), BigDecimal.valueOf(3000));
            LOGGER.info("Finish thread");
        });
        thread.setName("Thread 1");
        thread.start();

        // Sleep adicionado para garantir que a thread acima seja executada até o passo 2
        Thread.sleep(1000L);
        Thread.currentThread().setName("Thread 2");

        try {

            /**
             * Dentro do método updateBalance será realizado os três passos
             * 1- Busca conta usando optimistic lock
             * 2- Dorme 3 segundos
             * 3- Atualiza
             */
            LOGGER.info("Update balance to ZERO");
            accountBusiness.updateBalance(account.getId(), BigDecimal.ZERO);

            throw new RuntimeException("fail");
        } catch (ObjectOptimisticLockingFailureException ex) { }
    }

    @Test()
    public void shouldNotThrowExceptionWhenHasNotConcurrency() throws InterruptedException {

        final Account account = createAccount();

        Thread thread = new Thread(() -> {
            LOGGER.info("Running thread");
            Account newBalance = accountBusiness.updateBalance(account.getId(), BigDecimal.valueOf(3000));
            LOGGER.info("Finish thread");
        });
        thread.setName("Thread 1");
        thread.start();
        // com o método join, garantimos que a o código abaixo será executado somente após a execução da thread
        thread.join();

        Thread.currentThread().setName("Thread 2");

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
