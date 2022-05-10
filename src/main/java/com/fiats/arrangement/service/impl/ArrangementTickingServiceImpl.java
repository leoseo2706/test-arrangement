package com.fiats.arrangement.service.impl;

import com.fiats.arrangement.constant.ArrangementErrorCode;
import com.fiats.arrangement.jpa.entity.Arrangement;
import com.fiats.arrangement.jpa.entity.ArrangementOperation;
import com.fiats.arrangement.jpa.repo.ArrangementOperationRepo;
import com.fiats.arrangement.jpa.repo.ArrangementRepo;
import com.fiats.arrangement.service.ArrangementTickingService;
import com.fiats.arrangement.validator.ArrangementValidator;
import com.fiats.exception.NeoFiatsException;
import com.fiats.tmgcoreutils.utils.CommonUtils;
import com.fiats.tmgcoreutils.utils.DateHelper;
import com.fiats.tmgjpa.entity.RecordStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ArrangementTickingServiceImpl implements ArrangementTickingService {

    @Autowired
    ArrangementRepo arrRepo;

    @Autowired
    ArrangementValidator arrValidator;

    @Autowired
    ArrangementOperationRepo arrOpRepo;

    @Override
    @Transactional // -> open a new one or use the existing one
    public void tickingPaymentStatus(Collection<Arrangement> originalArrs) {

        if (!CollectionUtils.isEmpty(originalArrs)) {

            Timestamp now = DateHelper.nowInTimestamp();

            List<ArrangementOperation> ops = originalArrs.stream()
                    .peek(arr -> arr.setUpdatedDate(now))
                    .flatMap(tmpArr -> tmpArr.getOperations().stream())
                    .filter(op -> op.getPaymentStatus() == null
                            || op.getPaymentStatus().equals(RecordStatus.INACTIVE.getStatus()))
                    .peek(op -> {
                        op.setPaymentStatus(RecordStatus.ACTIVE.getStatus());
                        op.setPaymentStatusDate(now);
                    })
                    .collect(Collectors.toList());

            if (!CollectionUtils.isEmpty(ops)) {
                arrOpRepo.saveAll(ops);
                arrRepo.saveAll(originalArrs);
            }
        }
    }

    @Override
    @Transactional
    public List<Arrangement> tickingPaymentStatusByOperations(Collection<ArrangementOperation> operations) {

        if (!CollectionUtils.isEmpty(operations)) {
            Timestamp now = DateHelper.nowInTimestamp();

            List<Arrangement> arrs = operations.stream()
                    .filter(op -> op.getPaymentStatus() == null
                            || op.getPaymentStatus().equals(RecordStatus.INACTIVE.getStatus()))
                    .peek(op -> {
                        op.setPaymentStatus(RecordStatus.ACTIVE.getStatus());
                        op.setPaymentStatusDate(now);
                    }).map(ArrangementOperation::getArrangement)
                    .peek(arr -> arr.setUpdatedDate(now))
                    .collect(Collectors.toList());


            if (!CollectionUtils.isEmpty(arrs)) {
                operations = arrOpRepo.saveAll(operations);
                arrs = arrRepo.saveAll(arrs);
                return arrs;
            }
        }

        return Collections.emptyList();

    }

    @Override
    @Transactional
    public List<Arrangement> tickingDeliveryStatusByOperations(Collection<ArrangementOperation> operations) {

        if (!CollectionUtils.isEmpty(operations)) {
            Timestamp now = DateHelper.nowInTimestamp();

            List<Arrangement> arrs = operations.stream()
                    .filter(op -> op.getDeliveryStatus() == null
                            || op.getDeliveryStatus().equals(RecordStatus.INACTIVE.getStatus()))
                    .peek(op -> {
                        op.setDeliveryStatus(RecordStatus.ACTIVE.getStatus());
                        op.setDeliveryStatusDate(now);
                    }).map(ArrangementOperation::getArrangement)
                    .peek(arr -> arr.setUpdatedDate(now))
                    .collect(Collectors.toList());

            if (!CollectionUtils.isEmpty(arrs)) {
                operations = arrOpRepo.saveAll(operations);
                arrs = arrRepo.saveAll(arrs);
                return arrs;
            }
        }

        return Collections.emptyList();
    }

    @Override
    @Transactional // -> open a new one or use the existing one
    public void tickingDeliveryStatus(Collection<Arrangement> arrangements) {

        if (!CollectionUtils.isEmpty(arrangements)) {

            Timestamp now = DateHelper.nowInTimestamp();

            List<ArrangementOperation> ops = arrangements.stream()
                    .peek(arr -> arr.setUpdatedDate(now))
                    .flatMap(tmpArr -> tmpArr.getOperations().stream())
                    .filter(op -> op.getDeliveryStatus() == null
                            || op.getDeliveryStatus().equals(RecordStatus.INACTIVE.getStatus()))
                    .peek(op -> {
                        op.setDeliveryStatus(RecordStatus.ACTIVE.getStatus());
                        op.setDeliveryStatusDate(now);
                    })
                    .collect(Collectors.toList());

            if (!CollectionUtils.isEmpty(ops)) {
                arrOpRepo.saveAll(ops);
                arrRepo.saveAll(arrangements);
            }
        }
    }
}