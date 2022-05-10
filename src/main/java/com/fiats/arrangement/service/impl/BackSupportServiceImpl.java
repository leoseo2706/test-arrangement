package com.fiats.arrangement.service.impl;

import com.fiats.arrangement.constant.ArrangementRoleEnum;
import com.fiats.arrangement.jpa.entity.*;
import com.fiats.arrangement.jpa.repo.ArrangementRelationRepo;
import com.fiats.arrangement.jpa.repo.ArrangementRepo;
import com.fiats.arrangement.mapper.BackSupportMapper;
import com.fiats.arrangement.payload.filter.ArrangementFilterStatusEnum;
import com.fiats.arrangement.payload.filter.BackSupportFilter;
import com.fiats.arrangement.service.BackSupportService;
import com.fiats.arrangement.service.CustomerService;
import com.fiats.arrangement.service.InfoService;
import com.fiats.arrangement.service.ProductDerivativeService;
import com.fiats.exception.ErrorCode;
import com.fiats.tmgcoreutils.payload.ArrangementInfoDTO;
import com.fiats.tmgcoreutils.payload.CustomerDTO;
import com.fiats.tmgcoreutils.payload.IssuerDTO;
import com.fiats.tmgcoreutils.payload.ProdDerivativeDTO;
import com.fiats.tmgcoreutils.utils.CommonUtils;
import com.fiats.tmgjpa.paging.PagingFilterBase;
import com.fiats.tmgjpa.payload.ResponseMessage;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.neo.exception.NeoException;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.RowBounds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BackSupportServiceImpl implements BackSupportService {

    @Autowired
    ArrangementRepo arrangementRepo;

    @Autowired
    ArrangementRelationRepo arrangementRelationRepo;

    @Autowired
    CustomerService customerService;
    @Autowired
    ProductDerivativeService productDerivativeService;
    @Autowired
    InfoService infoService;
    @Autowired
    private BackSupportMapper backSupportMapper;

    @Override
    public ResponseMessage retrieveArrangements(PagingFilterBase<BackSupportFilter> pf) {

        return new ResponseMessage(findArrangements(pf), pf.getPaging());
    }

    @Override
    public List<ArrangementInfoDTO> findArrangements(PagingFilterBase<BackSupportFilter> pf) {
        log.info("Find arrangement: {}", pf);
        List<ArrangementInfoDTO> result = null;

        if (!pf.isPageable()) {
            result = backSupportMapper.retrieveArrangements(pf.getFilter()).getResult();
        } else {
            Long totalRecord = backSupportMapper.retrieveArrangementsCount(pf.getFilter());
            if (totalRecord != null && totalRecord > 0) {
                Long totalPage = totalRecord / pf.getPageSize() + (totalRecord % pf.getPageSize() == 0 ? 0 : 1);

                Integer offset = pf.getPageNum() * pf.getPageSize();
                RowBounds rb = new RowBounds(offset, pf.getPageSize());

                Page<ArrangementInfoDTO> page = backSupportMapper.retrieveArrangements(pf.getFilter(), rb);

                result = page.getResult();
                pf.getPaging().setTotalRecords(totalRecord);
                pf.getPaging().setTotalPages(totalPage.intValue());
            }
        }

        if (!CollectionUtils.isEmpty(result)) {
            List<Long> customerIds = result.stream().map(a -> a.getPartyId()).collect(Collectors.toList());
            List<CustomerDTO> customerData = customerService.retrieveCustomerInfo(customerIds);
            if (!CollectionUtils.isEmpty(customerData)) {
                Map<Long, CustomerDTO> map = customerData.stream()
                        .collect(Collectors.toMap(CustomerDTO::getId, data -> data));

                result = result.stream().map(a -> {
                    if (map.containsKey(a.getPartyId())) {
                        CustomerDTO item = map.get(a.getPartyId());
                        a.setPartyName(StringUtils.hasText(item.getName()) ? item.getName() : null);
                        a.setPartyAccount(StringUtils.hasText(item.getAccount()) ? item.getAccount() : null);
                        a.setPartyIdentity(StringUtils.hasText(item.getIdCard()) ? item.getIdCard() : null);
                        a.setPartyCustodyCode(StringUtils.hasText(item.getStockAccount()) ? item.getStockAccount() : null);
                    }

                    return a;
                }).collect(Collectors.toList());
            }
        }

        return result;
    }

    @Override
    public ArrangementInfoDTO retrieveDetailArrangement(Long arrangementId, Long matchedId) {
        List<Long> listPartyId = new ArrayList<>();

        Optional<Arrangement> arrangement = arrangementRepo.findByIdFetchAllRelation(arrangementId);
        if (arrangement.isPresent()) {
            ArrangementInfoDTO result = new ArrangementInfoDTO();

            result.setArrangementId(arrangement.get().getId());
            result.setArrangementCode(arrangement.get().getCode());
            result.setDerivativeCode(arrangement.get().getProductDerivativeCode());
            result.setTradingDate(arrangement.get().getTradingDate());

            ProdDerivativeDTO prodDerivativeDTO = productDerivativeService.findByCode(arrangement.get().getProductDerivativeCode());
            if (prodDerivativeDTO != null) {
                IssuerDTO issuerDTO = infoService.retrieveIssuerInfo(prodDerivativeDTO.getProdVanilla().getIssuerId());
                if (issuerDTO != null) {
                    result.setIssuerId(issuerDTO.getId());
                    result.setIsserCode(issuerDTO.getCode());
                    result.setIssuerName(issuerDTO.getName());
                }
            }

            ArrangementParty owner = arrangement.get().getParties().stream().filter(a -> a.getRole().equals(ArrangementRoleEnum.OWNER.toString())).findAny().orElse(null);
            if (owner != null) {
                result.setPartyId(owner.getCustomerId());
                listPartyId.add(owner.getCustomerId());
            }

            ArrangementParty broker = arrangement.get().getParties().stream().filter(a -> a.getRole().equals(ArrangementRoleEnum.BROKER.toString())).findAny().orElse(null);
            if (broker != null) {
                result.setBrokerId(broker.getCustomerId());
                listPartyId.add(broker.getCustomerId());
            }

            if (matchedId != null) {
                Optional<ArrangementRelation> matchedArrangementRelation = arrangement.get().getRelatedArrs().stream()
                        .filter(a -> a.getArrangement().getId().equals(matchedId)).findFirst();

                if (matchedArrangementRelation.isPresent()) {
                    Arrangement matchedArrangement = matchedArrangementRelation.get().getArrangement();
                    //Get thong tin counter party
                    ArrangementParty counterParty = matchedArrangement.getParties().stream().filter(a -> owner == null || !a.getCustomerId().equals(owner.getCustomerId())).findAny().orElse(null);
                    if (counterParty != null) {
                        result.setCounterId(counterParty.getCustomerId());
                        listPartyId.add(counterParty.getCustomerId());
                    }

                    Optional<ArrangementOperation> matchedArrOperation = matchedArrangement.getOperations().stream()
                            .filter(a -> a.getArrangementRelationId().equals(matchedArrangementRelation.get().getId())).findFirst();

                    if (matchedArrOperation.isPresent()) {
                        result.setPaymentStatus((matchedArrOperation.get().getPaymentStatus() != null && matchedArrOperation.get().getPaymentStatus() == 1) ? true : false);
                        result.setPaymentDate(matchedArrOperation.get().getPaymentStatusDate() != null ? matchedArrOperation.get().getPaymentStatusDate() : null);
                        result.setDeliveryStatus((matchedArrOperation.get().getDeliveryStatus() != null && matchedArrOperation.get().getDeliveryStatus() == 1) ? true : false);
                        result.setDeliveryDate(matchedArrOperation.get().getDeliveryStatusDate() != null ? matchedArrOperation.get().getDeliveryStatusDate() : null);
                    }

                    result.setVolume(matchedArrangement.getVolume().doubleValue());

                    Optional<ArrangementPricing> pricing = matchedArrangement.getPrices().stream().findFirst();
                    if (pricing.isPresent()) {
                        result.setRate(pricing.get().getRate() != null ? pricing.get().getRate() : null);
                        result.setInvestmentTimeByMonth(pricing.get().getInvestmentTimeByMonth() != null ? pricing.get().getInvestmentTimeByMonth() : null);
                        result.setUnitPrice(pricing.get().getUnitPrice() != null ? pricing.get().getUnitPrice().doubleValue() : null);
                        result.setPrice(pricing.get().getPrice() != null ? pricing.get().getPrice().doubleValue() : null);
                        result.setPrincipal(pricing.get().getPrincipal() != null ? pricing.get().getPrincipal().doubleValue() : null);
                        result.setTransactionFee(pricing.get().getFee() != null ? pricing.get().getFee().doubleValue() : null);
                        result.setTax(pricing.get().getTax() != null ? CommonUtils.bigDecimalToDoubleSilently(pricing.get().getTax()) : null);
                    }
                }
            } else {
                result.setVolume(arrangement.get().getVolume().doubleValue());
                Optional<ArrangementPricing> pricing = arrangement.get().getPrices().stream().findFirst();
                if (pricing.isPresent()) {
//                    result.setRate(pricing.get().getRate());
//                    result.setInvestmentTimeByMonth(pricing.get().getInvestmentTimeByMonth());
//                    result.setUnitPrice(pricing.get().getUnitPrice().doubleValue());
//                    result.setPrice(pricing.get().getPrice().doubleValue());
//                    result.setPrincipal(pricing.get().getPrincipal().doubleValue());
//                    result.setTransactionFee(pricing.get().getFee().doubleValue());
//                    result.setTax(CommonUtils.bigDecimalToDoubleSilently(pricing.get().getTax()));

                    result.setRate(pricing.get().getRate() != null ? pricing.get().getRate() : null);
                    result.setInvestmentTimeByMonth(pricing.get().getInvestmentTimeByMonth() != null ? pricing.get().getInvestmentTimeByMonth() : null);
                    result.setUnitPrice(pricing.get().getUnitPrice() != null ? pricing.get().getUnitPrice().doubleValue() : null);
                    result.setPrice(pricing.get().getPrice() != null ? pricing.get().getPrice().doubleValue() : null);
                    result.setPrincipal(pricing.get().getPrincipal() != null ? pricing.get().getPrincipal().doubleValue() : null);
                    result.setTransactionFee(pricing.get().getFee() != null ? pricing.get().getFee().doubleValue() : null);
                    result.setTax(pricing.get().getTax() != null ? CommonUtils.bigDecimalToDoubleSilently(pricing.get().getTax()) : null);
                }
            }

            if (!listPartyId.isEmpty()) {
                List<CustomerDTO> customerData = customerService.retrieveCustomerInfo(listPartyId);
                if (!CollectionUtils.isEmpty(customerData)) {
                    for (CustomerDTO a : customerData) {
                        if (a.getId().equals(result.getPartyId())) {
                            result.setPartyName(StringUtils.hasText(a.getName()) ? a.getName() : null);
                            result.setPartyIdentity(StringUtils.hasText(a.getIdCard()) ? a.getIdCard() : null);
                            result.setPartyCustodyCode(StringUtils.hasText(a.getStockAccount()) ? a.getStockAccount() : null);
                        }

                        if (a.getId().equals(result.getCounterId())) {
                            result.setCounterName(StringUtils.hasText(a.getName()) ? a.getName() : null);
                            result.setCounterIdentity(StringUtils.hasText(a.getIdCard()) ? a.getIdCard() : null);
                            result.setCounterCustodyCode(StringUtils.hasText(a.getStockAccount()) ? a.getStockAccount() : null);
                        }

                        if (a.getId().equals(result.getBrokerId())) {
                            result.setBrokerName(StringUtils.hasText(a.getName()) ? a.getName() : null);
                        }
                    }
                }
            }

            result.setException((arrangement.get().getException() != null && arrangement.get().getException() == 1) ? true : false);

            return result;
        } else {
            throw new NeoException(null, ErrorCode.UNAVAILABLE_ITEM, new StringBuilder("ABC DEF"));
        }
    }

    @Override
    public Map<String, List<Integer>> prepareOperationStatus(List<String> operationStatus) {
        Map<String, List<Integer>> result = null;

        if (!CollectionUtils.isEmpty(operationStatus)) {
            result = new HashMap<>();

            for (String item : operationStatus) {
                switch (item) {
                    case "SIGNED":
                        if (result.containsKey("SIGN_STATUS")) {
                            result.get("SIGN_STATUS").add(1);
                        } else {
                            List<Integer> data = new ArrayList<>();
                            data.add(1);
                            result.put("SIGN_STATUS", data);
                        }
                        break;
                    case "NOTSIGNED":
                        if (result.containsKey("SIGN_STATUS")) {
                            result.get("SIGN_STATUS").add(0);
                        } else {
                            List<Integer> data = new ArrayList<>();
                            data.add(0);
                            result.put("SIGN_STATUS", data);
                        }
                        break;
                    case "CONTRACTED":
                        if (result.containsKey("CONTRACT_STATUS")) {
                            result.get("CONTRACT_STATUS").add(1);
                        } else {
                            List<Integer> data = new ArrayList<>();
                            data.add(1);
                            result.put("CONTRACT_STATUS", data);
                        }
                        break;
                    case "NOTCONTRACTED":
                        if (result.containsKey("CONTRACT_STATUS")) {
                            result.get("CONTRACT_STATUS").add(0);
                        } else {
                            List<Integer> data = new ArrayList<>();
                            data.add(0);
                            result.put("CONTRACT_STATUS", data);
                        }
                        break;
                    case "PAYMENTED":
                        if (result.containsKey("PAYMENT_STATUS")) {
                            result.get("PAYMENT_STATUS").add(1);
                        } else {
                            List<Integer> data = new ArrayList<>();
                            data.add(1);
                            result.put("PAYMENT_STATUS", data);
                        }
                        break;
                    case "NOTPAYMENTED":
                        if (result.containsKey("PAYMENT_STATUS")) {
                            result.get("PAYMENT_STATUS").add(0);
                        } else {
                            List<Integer> data = new ArrayList<>();
                            data.add(0);
                            result.put("PAYMENT_STATUS", data);
                        }
                        break;
                    case "DELIVERED":
                        if (result.containsKey("DELIVERY_STATUS")) {
                            result.get("DELIVERY_STATUS").add(1);
                        } else {
                            List<Integer> data = new ArrayList<>();
                            data.add(1);
                            result.put("DELIVERY_STATUS", data);
                        }
                        break;
                    case "NOTDELIVERED":
                        if (result.containsKey("DELIVERY_STATUS")) {
                            result.get("DELIVERY_STATUS").add(0);
                        } else {
                            List<Integer> data = new ArrayList<>();
                            data.add(0);
                            result.put("DELIVERY_STATUS", data);
                        }
                        break;
                    case "MORTGAGED":
                        if (result.containsKey("COLLATERAL_STATUS")) {
                            result.get("COLLATERAL_STATUS").add(1);
                        } else {
                            List<Integer> data = new ArrayList<>();
                            data.add(1);
                            result.put("COLLATERAL_STATUS", data);
                        }
                        break;
                    case "NOTMORTGAGED":
                        if (result.containsKey("COLLATERAL_STATUS")) {
                            result.get("COLLATERAL_STATUS").add(0);
                        } else {
                            List<Integer> data = new ArrayList<>();
                            data.add(0);
                            result.put("COLLATERAL_STATUS", data);
                        }
                        break;
                }
            }
        }

        return result;
    }
}