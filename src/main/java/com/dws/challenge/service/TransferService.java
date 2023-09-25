package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.Transfer;
import com.dws.challenge.dto.TransferResponse;
import com.dws.challenge.exception.CannotExecuteTransferException;
import com.dws.challenge.repository.AccountsRepository;
import com.dws.challenge.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TransferService {
    private static final String TRANSFER_MESSAGE_FROM = "Successfully transfer %s from your account to %s";
    private static final String TRANSFER_MESSAGE_TO = "Successfully received %s on your account from %s";

    private final TransferRepository transferRepository;
    private final AccountsRepository accountsRepository;
    private final NotificationService notificationService;

    private static void validateTransferInput(String accountIdFrom, String accountIdTo, BigDecimal amount) {
        if (accountIdFrom == null || accountIdTo == null || amount == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid input");
        }
        if (Objects.equals(accountIdFrom, accountIdTo)) {
            throw new ResponseStatusException(BAD_REQUEST, "Account ids cannot be the same");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid amount");
        }
    }

    private static TransferResponse toTransferDto(Transfer transfer) {
        return TransferResponse.builder()
                .transferId(transfer.getTransferId())
                .accountIdFrom(transfer.getAccountIdFrom())
                .accountIdTo(transfer.getAccountIdTo())
                .amount(transfer.getAmount())
                .build();
    }

    public TransferResponse transfer(String accountIdFrom, String accountIdTo, BigDecimal amount) {
        validateTransferInput(accountIdFrom, accountIdTo, amount);

        Account fromAccount = getAccountOrThrow(accountIdFrom);
        Account toAccount = getAccountOrThrow(accountIdTo);
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(BAD_REQUEST, "Insufficient balance in the fromAccount");
        }

        var transfer = transferRepository.transfer(fromAccount, toAccount, amount);

        if (transfer.isPresent()) {
            log.info("Successfully executed transfer {}", transfer.get());
            notify(fromAccount, toAccount, transfer.get().getAmount());
            return toTransferDto(transfer.get());
        } else {
            throw new CannotExecuteTransferException("Cannot execute transfer. Please try again later");
        }
    }

    private Account getAccountOrThrow(String accountIdTo) {
        return Optional.ofNullable(accountsRepository.getAccount(accountIdTo))
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Cannot find an account"));
    }

    private void notify(Account fromAccount, Account toAccount, BigDecimal amount) {
        notificationService.notifyAboutTransfer(
                fromAccount, format(TRANSFER_MESSAGE_FROM, amount, toAccount.getAccountId())
        );
        notificationService.notifyAboutTransfer(
                toAccount, format(TRANSFER_MESSAGE_TO, amount, fromAccount.getAccountId())
        );
    }

}
