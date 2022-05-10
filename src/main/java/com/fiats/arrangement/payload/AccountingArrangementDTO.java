package com.fiats.arrangement.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fiats.tmgcoreutils.payload.ArrangementDTO;
import com.fiats.tmgcoreutils.payload.CustomerDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountingArrangementDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private ArrangementDTO arrangement;

    private CustomerDTO customer;

}