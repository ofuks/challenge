package com.dws.challenge.mapper;

import com.dws.challenge.domain.Transfer;
import com.dws.challenge.dto.TransferResponse;
import lombok.NonNull;
import org.springframework.stereotype.Component;

@Component
public class TransferMapper {

    public TransferResponse toTransferDto(@NonNull Transfer transfer) {
        return TransferResponse.builder()
                .transferId(transfer.getTransferId())
                .accountIdFrom(transfer.getAccountIdFrom())
                .accountIdTo(transfer.getAccountIdTo())
                .amount(transfer.getAmount())
                .build();
    }
}
