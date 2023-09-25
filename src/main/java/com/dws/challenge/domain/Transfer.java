package com.dws.challenge.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transfer {
    private UUID transferId;
    private String accountIdFrom;
    private String accountIdTo;
    private BigDecimal amount;

}
