package com.dws.challenge.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {

    @NotNull
    @NotEmpty
    @JsonProperty("account_id_from")
    private String accountIdFrom;

    @NotNull
    @NotEmpty
    @JsonProperty("account_id_to")
    private String accountIdTo;

    @NotNull
    @Min(value = 0, message = "Balance must be positive.")
    @JsonProperty("amount")
    private BigDecimal amount;

}
