package guini.springjpa;

import static org.assertj.core.api.Assertions.assertThat;

import guini.springjpa.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class SpringBootApplicationTest {

    @Autowired
    private AccountRepository accountRepository;

    @Test
    public void verifyIfAccountIsNotNull() {
        assertThat(accountRepository).isNotNull();
    }
}
