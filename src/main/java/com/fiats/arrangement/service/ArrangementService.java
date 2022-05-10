package com.fiats.arrangement.service;

import com.fiats.arrangement.jpa.entity.Arrangement;
import com.fiats.arrangement.jpa.entity.ArrangementOperation;
import com.fiats.arrangement.payload.CollateralDTO;
import com.fiats.arrangement.payload.TemplateFileDTO;
import com.fiats.arrangement.payload.filter.ArrangementFilter;
import com.fiats.tmgcoreutils.jwt.JWTHelper;
import com.fiats.tmgcoreutils.payload.*;
import com.fiats.tmgjpa.paging.PagingFilterBase;
import com.fiats.tmgjpa.payload.ResponseMessage;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ArrangementService {

    ResponseMessage listCustomerArrangement(PagingFilterBase<ArrangementFilter> pf, JWTHelper jwt);

    ResponseMessage listBrokerArrangement(PagingFilterBase<ArrangementFilter> pf, JWTHelper jwt);

    OrderPlacementDTO customerConfirmForReference(Long arrangementId, JWTHelper jwtHelper);

    List<Arrangement> cancelArrangement(Long arrangementId, CustomerDTO customer, JWTHelper jwt, boolean jobAutoExpire);

    OrderPlacementDTO cancelArrangementAndSendSignal(Long arrangementId, JWTHelper jwt, boolean jobAutoExpire);

    ArrangementNotificationDTO placeOrder(OrderPlacementDTO dto, JWTHelper jwtHelper, Boolean isBrokerReferred);

    OrderPlacementDTO placeOrderAndSendNoti(OrderPlacementDTO dto, JWTHelper jwtHelper, Boolean isBrokerReferred);

    OrderPlacementDTO placeOrderException(OrderPlacementDTO dto);

    OrderPlacementDTO placeCollateral(CollateralDTO dto);

//    OrderPlacementDTO createMatching(Long arrangementId);

    OrderPlacementDTO createMatchingRecords(Arrangement arrangement);

    OrderPlacementDTO signContract(CustomerBrokerWrapper cusBrokerWrapper, Long arrangementId, JWTHelper jwt);

    Object signContractAndSendSignal(Long arrangementId, JWTHelper jwt);

    List<ArrangementOperation> findEquivalentMatchingOperations(List<Arrangement> originalArrs);

    Collection<ArrangementOperation> findAllRelatedOperations(List<Arrangement> originalArrs);

    List<Arrangement> findMatchingArrangements(List<Arrangement> originalArrs);

    List<Arrangement> findOriginalAndCounterArrangements(List<Arrangement> originalArrs);

    Map<Long, List<Arrangement>> findOriginalAndCounterArrangementsGroupingByMatchingID(List<Arrangement> originalArrs);

    Map<Long, Arrangement> findOriginalSellOrders(List<Arrangement> arrangements);

    List<TemplateFileDTO> getContractsBrokerSupport(JWTHelper jwt, Long arrangementId);

    Object downloadContractBrokerSupport(JWTHelper jwt, Long arrangementId, String contractName);
}
