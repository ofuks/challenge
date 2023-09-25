package com.dws.challenge.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponse {

    @JsonProperty("transfer_id")
    private UUID transferId;
    @JsonProperty("account_id_from")
    private String accountIdFrom;
    @JsonProperty("account_id_to")
    private String accountIdTo;
    @JsonProperty("amount")
    private BigDecimal amount;

}
