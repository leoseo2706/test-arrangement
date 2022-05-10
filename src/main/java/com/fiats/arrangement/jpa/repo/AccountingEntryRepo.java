package com.fiats.arrangement.jpa.repo;

import com.fiats.arrangement.jpa.entity.AccountingEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface AccountingEntryRepo extends JpaRepository<AccountingEntry, Long>, JpaSpecificationExecutor<AccountingEntry> {

    // to convert to unparse string = ''
    @Query(value = "select * " +
            "from ACCOUNTING_ENTRY a " +
            "where NVL(a.ENTRY_NO, '') || ',' || TO_CHAR(a.ENTRY_TRANSACTION_DATE, 'YYYY-MM-DD') || ',' || " +
            "      TO_CHAR(a.ENTRY_EFFECTIVE_DATE, 'YYYY-MM-DD') || ',' || NVL(a.ENTRY_DESCRIPTION, '') || ',' || " +
            "      TRIM(TO_CHAR(a.ENTRY_AMOUNT, '999999999999999999.0000')) || ',' || " +
            "      NVL(a.ENTRY_TYPE, '') || ',' || " +
            "      TRIM(TO_CHAR(a.ENTRY_REMAINING_AMOUNT, '999999999999999999.0000')) in :keys", nativeQuery = true)
    List<AccountingEntry> findByUniqueKey(Collection<String> keys);

    List<AccountingEntry> findByArrangementIdIn(Collection<Long> arrangementIds);

}