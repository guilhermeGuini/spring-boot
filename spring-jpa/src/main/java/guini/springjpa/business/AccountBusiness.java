package guini.springjpa.business;

import guini.springjpa.entity.domain.entity.Account;
import guini.springjpa.repository.AccountRepository;
import org.hibernate.StaleObjectStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.Optional;

@Service
public class AccountBusiness {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountBusiness.class);

    private final AccountRepository accountRepository;

    @Autowired
    public AccountBusiness(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * Create account in a new Transaction
     * @param name
     * @param openingBalance
     * @return
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public Account createAccount(String name, BigDecimal openingBalance) {
        Account account = new Account();
        account.setName(name);
        account.setBalance(openingBalance);

        Account newAccount = accountRepository.save(account);
        LOGGER.info("New Account: {}", newAccount);
        return newAccount;
    }

    /**
     * Update account in a new Transaction
     *
     * @param id
     * @param newBalance
     * @return
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public Account updateBalance(Long id, BigDecimal newBalance) {

        Account account = accountRepository.findByIdWithLock(id).get();
        LOGGER.info("Account found: {}", account);

        try {
            /**
             * Sleep para simular processo concorrente
             */
            Thread.sleep(3000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        account.setBalance(newBalance);

        try {
            // save and flush para enviar as alterações para o banco instantaneamente
            Account newAccount = accountRepository.saveAndFlush(account);
            LOGGER.info("Account updated: {}", newAccount);
            return newAccount;

        } catch (StaleObjectStateException | ObjectOptimisticLockingFailureException ex) {
            LOGGER.error("Error to updated: {}", account);
            throw ex;
        }
    }

    public Optional<Account> findAccount(Long id) {
        return accountRepository.findByIdWithLock(id);
    }

    public Optional<Account> findById(Long id) {
        return accountRepository.findById(id);
    }
}
