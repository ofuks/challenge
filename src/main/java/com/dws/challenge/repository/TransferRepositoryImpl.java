package com.dws.challenge.repository;

import com.dws.challenge.domain.Transfer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class TransferRepositoryImpl implements TransferRepository {

    @Override
    public Transfer save(Transfer transfer) {
        return transfer;
    }

}
