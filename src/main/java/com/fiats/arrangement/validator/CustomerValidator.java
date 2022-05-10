package com.fiats.arrangement.validator;

import com.fiats.arrangement.constant.ArrangementErrorCode;
import com.fiats.exception.ValidationException;
import com.fiats.tmgcoreutils.common.ErrorCode;
import com.fiats.tmgcoreutils.constant.BondType;
import com.fiats.tmgcoreutils.payload.CustomerDTO;
import com.fiats.tmgcoreutils.utils.CommonUtils;
import com.fiats.tmgcoreutils.validator.CommonValidator;
import com.fiats.tmgjpa.entity.RecordStatus;
import com.neo.exception.NeoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;


@Slf4j
@Component
public class CustomerValidator extends CommonValidator {

    public void validateStockAccount(CustomerDTO customer, BondType listedStatus) {

        if (BondType.LISTED == listedStatus) {

            if (customer.getStatusAccount() == null || RecordStatus.ACTIVE.getStatus() != customer.getStatusAccount()
                    || !StringUtils.hasText(customer.getStockAccount())
                    || customer.getVsdActivateStatus() == null || RecordStatus.ACTIVE.getStatus() != customer.getVsdActivateStatus()
                    || customer.getVsdActivateAccount() == null) {
                throw new NeoException(null, ArrangementErrorCode.STOCK_ACCOUNT_STATUS_INVALID,
                        CommonUtils.format("Invalid status account, stock account or VSD account status for {0}",
                                customer.getId()));
            }
        }

    }

    public void validateCustomerAccountIdEquality(CustomerDTO customerDTO, List<Long> customerIds, String account) {
        log.info("Comparing customer: {} arrangement cus IDs {} with account {}",
                customerDTO, customerIds, account);
        if (CollectionUtils.isEmpty(customerIds) || customerDTO == null
                || CommonUtils.isInvalidPK(customerDTO.getId())
                || !customerIds.contains(customerDTO.getId())) {
            throw new ValidationException(ErrorCode.INVALID_JWT_TOKEN,
                    CommonUtils.format("Account {0} does not have this permission", account));
        }
    }
}
