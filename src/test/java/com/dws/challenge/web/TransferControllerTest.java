package com.dws.challenge.web;

import com.dws.challenge.domain.Account;
import com.dws.challenge.dto.TransferResponse;
import com.dws.challenge.repository.TransferRepository;
import com.dws.challenge.service.AccountsService;
import com.dws.challenge.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static com.dws.challenge.util.JsonUtil.toObject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@WebAppConfiguration
class TransferControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    private AccountsService accountsService;
    @Autowired
    private TransferRepository transferRepository;
    @MockBean
    private NotificationService notificationService;

    private Account account1;
    private Account account2;

    @BeforeEach
    void setUp() {
        this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

        accountsService.getAccountsRepository().clearAccounts();
        transferRepository.clear();

        account1 = new Account("acc-1", new BigDecimal(1000));
        account2 = new Account("acc-2", new BigDecimal(1000));
        this.accountsService.createAccount(account1);
        this.accountsService.createAccount(account2);
    }

    @Test
    void executeTransfer() throws Exception {
        // act
        var mvcResult = this.mockMvc.perform(post("/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account_id_from\":\"acc-1\",\"account_id_to\":\"acc-2\",\"amount\":1000}"))
                .andExpect(status().isCreated())
                .andReturn();

        var contentAsString = mvcResult.getResponse().getContentAsString();
        var transferResponse = toObject(contentAsString, TransferResponse.class);

        // assert
        assertNotNull(transferResponse.getTransferId());
        assertEquals("acc-1", transferResponse.getAccountIdFrom());
        assertEquals("acc-2", transferResponse.getAccountIdTo());
        assertEquals(new BigDecimal(1000), transferResponse.getAmount());

        assertEquals(BigDecimal.ZERO, accountsService.getAccount("acc-1").getBalance());
        assertEquals(new BigDecimal(2000), accountsService.getAccount("acc-2").getBalance());
        verify(notificationService).notifyAboutTransfer(account1, "Successfully transfer 1000 from your account to acc-2");
        verify(notificationService).notifyAboutTransfer(account2, "Successfully received 1000 on your account from acc-1");
    }

    @Test
    void executeTwoTransfer() throws Exception {
        // act
        var mvcResult1 = this.mockMvc.perform(post("/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account_id_from\":\"acc-1\",\"account_id_to\":\"acc-2\",\"amount\":300}"))
                .andExpect(status().isCreated())
                .andReturn();

        var contentAsString1 = mvcResult1.getResponse().getContentAsString();
        var transferResponse1 = toObject(contentAsString1, TransferResponse.class);

        var mvcResult2 = this.mockMvc.perform(post("/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account_id_from\":\"acc-1\",\"account_id_to\":\"acc-2\",\"amount\":400}"))
                .andExpect(status().isCreated())
                .andReturn();

        var contentAsString2 = mvcResult2.getResponse().getContentAsString();
        var transferResponse2 = toObject(contentAsString2, TransferResponse.class);

        // assert transfer1
        assertNotNull(transferResponse1.getTransferId());
        assertEquals("acc-1", transferResponse1.getAccountIdFrom());
        assertEquals("acc-2", transferResponse1.getAccountIdTo());
        assertEquals(new BigDecimal(300), transferResponse1.getAmount());

        // assert transfer2
        assertNotNull(transferResponse2.getTransferId());
        assertEquals("acc-1", transferResponse2.getAccountIdFrom());
        assertEquals("acc-2", transferResponse2.getAccountIdTo());
        assertEquals(new BigDecimal(400), transferResponse2.getAmount());

        assertEquals(new BigDecimal(300), accountsService.getAccount("acc-1").getBalance());
        assertEquals(new BigDecimal(1700), accountsService.getAccount("acc-2").getBalance());

        verify(notificationService).notifyAboutTransfer(account1, "Successfully transfer 300 from your account to acc-2");
        verify(notificationService).notifyAboutTransfer(account1, "Successfully transfer 300 from your account to acc-2");
        verify(notificationService).notifyAboutTransfer(account2, "Successfully received 400 on your account from acc-1");
        verify(notificationService).notifyAboutTransfer(account2, "Successfully received 400 on your account from acc-1");
    }

    @Test
    void executeTransferAmountBiggerThanActual() throws Exception {
        this.mockMvc.perform(post("/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account_id_from\":\"acc-1\",\"account_id_to\":\"acc-2\",\"amount\":999999}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void executeTransferNonExistingAccountFrom() throws Exception {
        this.mockMvc.perform(post("/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account_id_from\":\"non-existing\",\"account_id_to\":\"acc-2\",\"amount\":1000}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void executeTransferNonExistingAccountTo() throws Exception {
        this.mockMvc.perform(post("/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account_id_from\":\"acc-1\",\"account_id_to\":\"non-existing\",\"amount\":1000}"))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100, -1000000})
    void executeTransferInvalidAmount(int amount) throws Exception {
        this.mockMvc.perform(post("/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account_id_from\":\"acc-1\",\"account_id_to\":\"non-existing\",\"" + amount + "\":1000}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void executeTransferTheSameAccounts() throws Exception {
        this.mockMvc.perform(post("/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account_id_from\":\"acc-1\",\"account_id_to\":\"acc-1\",\"amount\":1000}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void executeTransferNoBody() throws Exception {
        this.mockMvc.perform(post("/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void executeTransferNullAccountFrom() throws Exception {
        this.mockMvc.perform(post("/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account_id_to\":\"acc-1\",\"amount\":1000}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void executeTransferNullAccountTo() throws Exception {
        this.mockMvc.perform(post("/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account_id_from\":\"acc-1\",\"amount\":1000}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void executeTransferNullBalance() throws Exception {
        this.mockMvc.perform(post("/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account_id_from\":\"acc-1\",\"account_id_to\":\"acc-2\"}"))
                .andExpect(status().isBadRequest());
    }

}
