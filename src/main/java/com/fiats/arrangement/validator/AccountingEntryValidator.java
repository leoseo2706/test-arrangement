package com.fiats.arrangement.validator;

import com.fiats.arrangement.constant.AccountingEntryStatusEnum;
import com.fiats.arrangement.constant.ArrangementErrorCode;
import com.fiats.arrangement.jpa.entity.AccountingEntry;
import com.fiats.arrangement.jpa.repo.AccountingEntryRepo;
import com.fiats.arrangement.payload.AccountingEntryMapper;
import com.fiats.exception.ValidationException;
import com.fiats.tmgcoreutils.constant.Constant;
import com.fiats.tmgcoreutils.utils.CommonUtils;
import com.fiats.tmgcoreutils.validator.CommonValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Slf4j
public class AccountingEntryValidator extends CommonValidator {

    @Autowired
    AccountingEntryRepo accountingEntryRepo;

    public void validateBasicAttributes(AccountingEntryMapper dto) {
        // validate dto
        if (dto == null || CollectionUtils.isEmpty(dto.getIds())
                || CommonUtils.isInvalidPK(dto.getArrangementId())) {
            throw new ValidationException(ArrangementErrorCode.T24_INVALID_INPUT,
                    "Empty payload while mapping manually");
        }
    }

    public List<AccountingEntry> validateEntryExistences(Collection<Long> ids) {
        // validate existence of entry
        List<AccountingEntry> entries = accountingEntryRepo.findAllById(ids);
        if (CollectionUtils.isEmpty(entries) || entries.size() != ids.size()) {
            throw new ValidationException(ArrangementErrorCode.T24_ENTRY_NOT_AVAILABLE,
                    CommonUtils.format("Some or all of the accounting entries ID {0} does not exist",
                            ids));
        }

        return entries;
    }

    public void validateManualMapping(List<AccountingEntry> entries) {

        if (CollectionUtils.isEmpty(entries)) {
            throw new ValidationException(ArrangementErrorCode.T24_ENTRY_NOT_AVAILABLE,
                    CommonUtils.format("Entries list not available while validating for manual mapping!"));
        }

        // validate the status of entry
        AtomicReference<String> invalidStatus = new AtomicReference<>();
        AtomicReference<Long> invalidId = new AtomicReference<>();
        boolean invalid = entries.stream()
                .anyMatch(e -> {

                    if (!AccountingEntryStatusEnum.UNMAPPED.toString().equals(e.getStatus())) {
                        invalidStatus.set(e.getStatus());
                        invalidId.set(e.getId());
                        return Constant.ACTIVE;
                    }

                    return Constant.INACTIVE;
                });
        if (invalid) {
            throw new ValidationException(ArrangementErrorCode.T24_INVALID_MAPPING_STATUS,
                    CommonUtils.format("Only status {0} is allowable while accounting entry {1} has status {2}",
                            AccountingEntryStatusEnum.UNMAPPED, invalidId.get(), invalidStatus.get()));
        }
    }

}
