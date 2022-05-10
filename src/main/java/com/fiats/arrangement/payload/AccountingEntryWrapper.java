package com.fiats.arrangement.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccountingEntryWrapper implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<AccountingEntryDTO> unrecognizableEntries;



}