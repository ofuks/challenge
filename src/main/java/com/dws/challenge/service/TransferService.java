package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.Transfer;
import com.dws.challenge.dto.TransferResponse;
import com.dws.challenge.exception.CannotExecuteTransferException;
import com.dws.challenge.mapper.TransferMapper;
import com.dws.challenge.repository.AccountsRepository;
import com.dws.challenge.repository.TransferRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TransferService {
    private static final String TRANSFER_MESSAGE_FROM = "Successfully transfer %s from your account to %s";
    private static final String TRANSFER_MESSAGE_TO = "Successfully received %s on your account from %s";

    private final Map<String, Lock> accountLocks = new ConcurrentHashMap<>();

    private final TransferRepository transferRepository;
    private final AccountsRepository accountsRepository;
    private final NotificationService notificationService;
    private final TransferMapper transferMapper;

    private static Transfer buildTransfer(String accountIdFrom, String accountIdTo, BigDecimal amount) {
        return new Transfer(UUID.randomUUID(), accountIdFrom, accountIdTo, amount);
    }

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

    public TransferResponse transfer(String accountIdFrom, String accountIdTo, BigDecimal amount) {
        validateTransferInput(accountIdFrom, accountIdTo, amount);

        Account fromAccount = getAccountOrThrow(accountIdFrom);
        Account toAccount = getAccountOrThrow(accountIdTo);
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(BAD_REQUEST, "Insufficient balance in the fromAccount");
        }

        var transfer = this.transfer(fromAccount, toAccount, amount);

        if (transfer.isPresent()) {
            log.info("Successfully executed transfer {}", transfer.get());
            notify(fromAccount, toAccount, transfer.get().getAmount());
            return transferMapper.toTransferDto(transfer.get());
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

    private Optional<Transfer> transfer(@NonNull Account fromAccount, @NonNull Account toAccount, @NonNull BigDecimal amount) {
        Lock fromAccountLock = accountLocks.computeIfAbsent(fromAccount.getAccountId(), accountId -> new ReentrantLock());
        Lock toAccountLock = accountLocks.computeIfAbsent(toAccount.getAccountId(), accountId -> new ReentrantLock());

        try {
            fromAccountLock.lock();
            try {
                toAccountLock.lock();
                return executeTransfer(amount, fromAccount, toAccount);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(e);
            } catch (Exception e) {
                log.warn("Cannot acquire lock for toAccount {}", toAccount, e);
            } finally {
                toAccountLock.unlock();
            }
        } catch (Exception e) {
            log.warn("Cannot acquire lock for fromAccount {}", fromAccount, e);
        } finally {
            fromAccountLock.unlock();
        }
        return Optional.empty();
    }

    private Optional<Transfer> executeTransfer(BigDecimal amount, Account fromAccount, Account toAccount) {
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance in the fromAccount");
        }
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        Transfer transfer = buildTransfer(fromAccount.getAccountId(), toAccount.getAccountId(), amount);
        Transfer savedTransfer = transferRepository.save(transfer);
        log.info("Successfully saved transfer {}", savedTransfer);
        return Optional.of(savedTransfer);
    }

}
