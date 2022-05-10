package com.fiats.arrangement.service;

import com.fiats.arrangement.jpa.entity.AccountingEntry;
import com.fiats.arrangement.jpa.entity.Arrangement;
import com.fiats.arrangement.payload.AccountingArrangementDTO;
import com.fiats.arrangement.payload.AccountingEntryDTO;
import com.fiats.arrangement.payload.AccountingEntryMapper;
import com.fiats.arrangement.payload.filter.AccountingEntryFilter;
import com.fiats.tmgcoreutils.jwt.JWTHelper;
import com.fiats.tmgjpa.paging.PagingFilterBase;
import com.fiats.tmgjpa.payload.ResponseMessage;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AccountingEntryService {

    AccountingArrangementDTO findArrangementInfo(String code);

    ResponseMessage<List<AccountingEntryDTO>> listAccountingEntries(PagingFilterBase<AccountingEntryFilter> pf);

    Object manuallyMapEntry(AccountingEntryMapper dto, JWTHelper jwt);

    Object importAndMapEntries(MultipartFile file, JWTHelper jwt);

    void mapEntriesAndSave(List<AccountingEntryDTO> entries, String fileName,
                           List<AccountingEntry> allEntries,
                           List<Arrangement> updatedArrangements);

    void saveAccountingEntries(List<AccountingEntry> allEntries);

}
