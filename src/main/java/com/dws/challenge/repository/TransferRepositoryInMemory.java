package com.dws.challenge.repository;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.Transfer;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TransferRepositoryInMemory implements TransferRepository {

    private final Map<UUID, Transfer> transfers = new ConcurrentHashMap<>();
    private final Map<String, Lock> accountLocks = new ConcurrentHashMap<>();

    private static Transfer buildTransfer(String accountIdFrom, String accountIdTo, BigDecimal amount) {
        return new Transfer(UUID.randomUUID(), accountIdFrom, accountIdTo, amount);
    }

    @Override
    public Optional<Transfer> transfer(@NonNull Account fromAccount, @NonNull Account toAccount, @NonNull BigDecimal amount) {
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

    @Override
    public void clear() {
        transfers.clear();
        accountLocks.clear();
    }

    private Optional<Transfer> executeTransfer(BigDecimal amount, Account fromAccount, Account toAccount) {
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance in the fromAccount");
        }
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));

        Transfer transfer = buildTransfer(fromAccount.getAccountId(), toAccount.getAccountId(), amount);
        transfers.put(transfer.getTransferId(), transfer);
        return Optional.of(transfer);
    }

}
