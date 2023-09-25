package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.of;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class TransferServiceTest {

    @Autowired
    private AccountsService accountsService;
    @Autowired
    private TransferService underTest;
    @MockBean
    private NotificationService notificationService;

    private Account account1;
    private Account account2;

    public static Stream<Arguments> transferInvalidInputSource() {
        return Stream.of(
                of(null, "acc-2", 100), // null input
                of(null, null, 100), // null input
                of("acc-1", null, 100), // null input
                of("acc-1", null, null), // null input
                of(null, null, null), // null input
                of("acc-1", "non-existing", 100), // non exising account
                of("non-existing", "acc-2", 100), // non exising account
                of("acc-1", "acc-2", 0), // invalid amount
                of("acc-1", "acc-2", -1), // invalid amount
                of("acc-1", "acc-1", -1) // the same from and to accounts
        );
    }

    @BeforeEach
    void setUp() {
        accountsService.getAccountsRepository().clearAccounts();

        account1 = new Account("acc-1", new BigDecimal(1000));
        account2 = new Account("acc-2", new BigDecimal(1000));
        this.accountsService.createAccount(account1);
        this.accountsService.createAccount(account2);
    }

    @Test
    void transfer() {
        // act
        var result = underTest.transfer("acc-1", "acc-2", new BigDecimal(100));

        // assert
        assertNotNull(result.getTransferId());
        assertEquals("acc-1", result.getAccountIdFrom());
        assertEquals("acc-2", result.getAccountIdTo());
        assertEquals(new BigDecimal(100), result.getAmount());

        verify(notificationService).notifyAboutTransfer(account1, "Successfully transfer 100 from your account to acc-2");
        verify(notificationService).notifyAboutTransfer(account2, "Successfully received 100 on your account from acc-1");
    }

    @ParameterizedTest
    @MethodSource("transferInvalidInputSource")
    void transferInvalidInput(String accountIdFrom, String accountIdTo, Integer amount) {
        assertThrows(ResponseStatusException.class,
                () -> underTest.transfer(accountIdFrom, accountIdTo, amount == null ? null : new BigDecimal(amount)));
    }

}
