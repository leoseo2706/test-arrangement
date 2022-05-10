package com.fiats.arrangement.service;

import com.fiats.arrangement.jpa.entity.Arrangement;
import com.fiats.arrangement.jpa.entity.ArrangementOperation;

import java.util.Collection;
import java.util.List;

public interface ArrangementTickingService {

    void tickingPaymentStatus(Collection<Arrangement> arrangements);

    List<Arrangement> tickingPaymentStatusByOperations(Collection<ArrangementOperation> operations);

    void tickingDeliveryStatus(Collection<Arrangement> arrangements);

    List<Arrangement> tickingDeliveryStatusByOperations(Collection<ArrangementOperation> operations);
}
