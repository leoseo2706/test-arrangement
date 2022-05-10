package com.fiats.arrangement.payload.filter;

import com.fiats.arrangement.constant.ArrConstant;
import com.fiats.arrangement.jpa.entity.ArrangementOperation_;
import com.fiats.arrangement.constant.ArrangementStatusEnum;
import com.fiats.arrangement.jpa.entity.Arrangement_;
import com.fiats.arrangement.payload.metamodel.ArrangementFilterMetaModel;
import com.fiats.arrangement.payload.metamodel.ArrangementOperationMetaModel;
import com.fiats.arrangement.payload.metamodel.ArrangementStatusMetaModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Getter
@AllArgsConstructor
@Slf4j
public enum ArrangementFilterStatusEnum {

    WAIT_FOR_CUSTOMER_CONFIRMATION(1, "Chờ KH xác nhận lệnh", ArrConstant.ARR_STATUS.INACTIVE, null, null, null, null, null, ArrangementStatusEnum.REFERENCE.getStatus()),

    WAIT_FOR_SIGNING_CONTRACT(2, "Chờ ký hợp đồng", ArrConstant.ARR_STATUS.INACTIVE, null, null, null, null, null, ArrangementStatusEnum.INACTIVE.getStatus()),

    WAIT_FOR_ORDER_CONFRIMATION(3, "Chờ xác nhận lệnh", ArrConstant.ARR_STATUS.ACTIVE, null, null, null, null, null, ArrangementStatusEnum.INACTIVE.getStatus()),

    WAIT_FOR_PAYMENT(4, "Chờ thanh toán", ArrConstant.ARR_STATUS.ACTIVE, null, ArrConstant.ARR_STATUS.INACTIVE, null, null, null, ArrangementStatusEnum.ACTIVE.getStatus()),

    WAIT_FOR_DELIVERY(5, "Chờ chuyển nhượng", ArrConstant.ARR_STATUS.ACTIVE, null, ArrConstant.ARR_STATUS.ACTIVE, ArrConstant.ARR_STATUS.INACTIVE, null, null, ArrangementStatusEnum.ACTIVE.getStatus()),

    DONE(6, "Giao dịch hoàn tất", ArrConstant.ARR_STATUS.ACTIVE, null, ArrConstant.ARR_STATUS.ACTIVE, ArrConstant.ARR_STATUS.ACTIVE, null, null, ArrangementStatusEnum.ACTIVE.getStatus()),

    CANCELLED(7, "Đã hủy", null, null, null, null, null, null, ArrangementStatusEnum.CANCELLED.getStatus());

    private Integer filterStatus;

    private String description;

    private Integer customerStatus;

    private Integer contractStatus;

    private Integer paymentStatus;

    private Integer deliveryStatus;

    private Integer collateralStatus;

    private Integer releaseStatus;

    private Integer arrangementStatus;

    // lookup hashMap
    private static Map<Integer, ArrangementFilterMetaModel> keyLookup = new LinkedHashMap<>();
    private static Map<Integer, ArrangementFilterStatusEnum> valueLookup = new LinkedHashMap<>();

    static {
        EnumSet.allOf(ArrangementFilterStatusEnum.class)
                .forEach(e -> {
                    keyLookup.put(e.getFilterStatus(), e.buildMetaModel());
                    valueLookup.put(e.getFilterStatus(), e);
                });
    }

    public static ArrangementFilterStatusEnum lookByValue(
            Integer customerStatus, Integer contractStatus,
            Integer paymentStatus, Integer deliveryStatus,
            Integer collateralStatus, Integer releaseStatus,
            Integer arrangementStatus) {

        return valueLookup.values().stream().filter(e -> {

            if (e.getCustomerStatus() != null) {
                boolean equal = e.getCustomerStatus().equals(customerStatus);
                if (!equal) {
                    return false;
                }
            }
            if (e.getContractStatus() != null) {
                boolean equal = e.getContractStatus().equals(contractStatus);
                if (!equal) {
                    return false;
                }
            }
            if (e.getPaymentStatus() != null) {
                boolean equal = e.getPaymentStatus().equals(paymentStatus);
                if (!equal) {
                    return false;
                }
            }
            if (e.getDeliveryStatus() != null) {
                boolean equal = e.getDeliveryStatus().equals(deliveryStatus);
                if (!equal) {
                    return false;
                }
            }
            if (e.getCollateralStatus() != null) {
                boolean equal = e.getCollateralStatus().equals(collateralStatus);
                if (!equal) {
                    return false;
                }
            }
            if (e.getCollateralStatus() != null) {
                boolean equal = e.getCollateralStatus().equals(releaseStatus);
                if (!equal) {
                    return false;
                }
            }

            if (e.getArrangementStatus() != null) {
                boolean equal = e.getArrangementStatus().equals(arrangementStatus);
                if (!equal) {
                    return false;
                }
            }

            return true; // can only be all equals or all null (which is still true btw)
        }).findAny().orElse(null);
    }

    public static ArrangementFilterMetaModel lookForMetaModel(Integer val) {
        return keyLookup.get(val);
    }

    private ArrangementFilterMetaModel buildMetaModel() {

        List<ArrangementOperationMetaModel> operations = new ArrayList<>();

        if (customerStatus != null) {
            operations.add(new ArrangementOperationMetaModel(
                    ArrangementOperation_.customerStatus,
                    customerStatus));
        }
        if (contractStatus != null) {
            operations.add(new ArrangementOperationMetaModel(
                    ArrangementOperation_.contractStatus,
                    contractStatus));
        }
        if (paymentStatus != null) {
            operations.add(new ArrangementOperationMetaModel(
                    ArrangementOperation_.paymentStatus,
                    paymentStatus));
        }
        if (deliveryStatus != null) {
            operations.add(new ArrangementOperationMetaModel(
                    ArrangementOperation_.deliveryStatus,
                    deliveryStatus));
        }
        if (collateralStatus != null) {
            operations.add(new ArrangementOperationMetaModel(
                    ArrangementOperation_.collateralStatus,
                    collateralStatus));
        }
        if (releaseStatus != null) {
            operations.add(new ArrangementOperationMetaModel(
                    ArrangementOperation_.releaseStatus,
                    releaseStatus));
        }

        ArrangementStatusMetaModel status = arrangementStatus == null
                ? null : new ArrangementStatusMetaModel(Arrangement_.status,
                arrangementStatus);

        return ArrangementFilterMetaModel.builder()
                .arrOperations(operations).arrStatus(status)
                .build();
    }


}
