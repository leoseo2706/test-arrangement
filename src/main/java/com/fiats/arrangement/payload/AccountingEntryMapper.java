package com.fiats.arrangement.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountingEntryMapper implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "Accounting entry ID list cannot be empty")
    private List<@Positive(message = "Id must be positive") Long> ids;

    @NotNull(message = "Arrangement ID cannot be null")
    @Positive(message = "Arrangement ID must be positive")
    private Long arrangementId;

}