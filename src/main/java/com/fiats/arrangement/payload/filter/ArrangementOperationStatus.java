package com.fiats.arrangement.payload.filter;

import com.fiats.arrangement.jpa.entity.Arrangement;
import com.fiats.arrangement.jpa.entity.ArrangementOperation;

import javax.persistence.metamodel.SingularAttribute;
import java.util.Map;

public interface ArrangementOperationStatus {

    Map<Integer, SingularAttribute<ArrangementOperation, Integer>> getCustomerStatusMetaModel();
    Map<Integer, SingularAttribute<ArrangementOperation, Integer>> getContractStatusMetaModel();
    Map<Integer, SingularAttribute<ArrangementOperation, Integer>> getPaymentStatusMetaModel();
    Map<Integer, SingularAttribute<ArrangementOperation, Integer>> getDeliveryStatusMetaModel();
    Map<Integer, SingularAttribute<ArrangementOperation, Integer>> getCollateralStatusMetaModel();
    Map<Integer, SingularAttribute<ArrangementOperation, Integer>> getReleaseStatusMetaModel();
    Map<Integer, SingularAttribute<Arrangement, Integer>> getArrangementStatusMetaModel();
}
