package com.dws.challenge.repository;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.Transfer;

import java.math.BigDecimal;
import java.util.Optional;

public interface TransferRepository {

    Optional<Transfer> transfer(Account accountIdFrom, Account accountIdTo, BigDecimal amount);

    void clear();
}
