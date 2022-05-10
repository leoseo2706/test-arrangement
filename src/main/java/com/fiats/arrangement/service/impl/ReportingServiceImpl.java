package com.fiats.arrangement.service.impl;

import com.fiats.arrangement.constant.ArrangementStatusEnum;
import com.fiats.arrangement.constant.ArrangementTypeEnum;
import com.fiats.arrangement.mapper.ReportingMapper;
import com.fiats.arrangement.payload.filter.ArrangementFilterStatusEnum;
import com.fiats.arrangement.payload.filter.ReportingFilter;
import com.fiats.arrangement.service.BrokerService;
import com.fiats.arrangement.service.CustomerService;
import com.fiats.arrangement.service.ReportingService;
import com.fiats.tmgcoreutils.payload.ArrangementInfoDTO;
import com.fiats.tmgcoreutils.payload.BrokerDTO;
import com.fiats.tmgcoreutils.payload.CustomerDTO;
import com.fiats.tmgcoreutils.payload.ReportingMatchedArrangementDTO;
import com.fiats.tmgjpa.paging.PagingFilterBase;
import com.fiats.tmgjpa.payload.ResponseMessage;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
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
public class ReportingServiceImpl implements ReportingService {

    @Autowired
    CustomerService customerService;

    @Autowired
    BrokerService brokerService;

    @Autowired
    private ReportingMapper reportingMapper;

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

    @Override
    public ResponseMessage retrieveMatchedArrangement(PagingFilterBase<ReportingFilter> pf) {

        List<ArrangementInfoDTO> result = null;

        ReportingMatchedArrangementDTO report = reportingMapper.retrieveMatchedArrangementCount(pf.getFilter());

        if (report != null) {
            if (!pf.isPageable()) {
                result = reportingMapper.retrieveMatchedArrangement(pf.getFilter()).getResult();
            } else {
                Long totalRecord = report.getTotalRecord();
                if (totalRecord != null && totalRecord > 0) {
                    Long totalPage = totalRecord / pf.getPageSize() + (totalRecord % pf.getPageSize() == 0 ? 0 : 1);

                    Integer offset = pf.getPageNum() * pf.getPageSize();
                    RowBounds rb = new RowBounds(offset, pf.getPageSize());

                    Page<ArrangementInfoDTO> page = reportingMapper.retrieveMatchedArrangement(pf.getFilter(), rb);

                    result = page.getResult();
                    pf.getPaging().setTotalRecords(totalRecord);
                    pf.getPaging().setTotalPages(totalPage.intValue());
                }
            }

            if (!CollectionUtils.isEmpty(result)) {
                List<Long> customerIds = result.stream().filter(a -> a.getPartyId() != null).map(a -> a.getPartyId()).collect(Collectors.toList());
                List<Long> brokerIds = result.stream().filter(a -> a.getBrokerId() != null).map(a -> a.getBrokerId()).collect(Collectors.toList());
                List<Long> counterIds = result.stream().filter(a -> a.getCounterId() != null).map(a -> a.getCounterId()).collect(Collectors.toList());

                Set<Long> set = new LinkedHashSet<>(customerIds);
                set.addAll(brokerIds);
                set.addAll(counterIds);
                List<Long> filters = new ArrayList<>(set);

                Map<Long, CustomerDTO> mapCus = new HashMap<>();
                List<CustomerDTO> customerData = customerService.retrieveCustomerInfo(filters);
                if (!CollectionUtils.isEmpty(customerData)) {
                    mapCus = customerData.stream().collect(Collectors.toMap(CustomerDTO::getId, data -> data));
                }
                final Map<Long, CustomerDTO> mapCustomer = mapCus;

                Map<Long, BrokerDTO> mapBrk = new HashMap<>();
                List<BrokerDTO> brokerData = brokerService.findBrokerDTOByCustomerID(brokerIds, null).collectList().block();
//            log.info("broker data: {}", brokerData);
                if (!CollectionUtils.isEmpty(brokerData)) {
                    mapBrk = brokerData.stream().collect(Collectors.toMap(BrokerDTO::getCustomerId, data -> data));
                }
                final Map<Long, BrokerDTO> mapBroker = mapBrk;

                result = result.stream().map(a -> {
                    if (mapCustomer.containsKey(a.getPartyId())) {
                        CustomerDTO item = mapCustomer.get(a.getPartyId());
                        a.setPartyName(StringUtils.hasText(item.getName()) ? item.getName() : null);
                        a.setPartyIdentity(StringUtils.hasText(item.getIdCard()) ? item.getIdCard() : null);
                        a.setPartyCustodyCode(StringUtils.hasText(item.getStockAccount()) ? item.getStockAccount() : null);
                    }

                    if (mapCustomer.containsKey(a.getBrokerId())) {
                        CustomerDTO item = mapCustomer.get(a.getBrokerId());
                        a.setBrokerName(StringUtils.hasText(item.getName()) ? item.getName() : null);
                        a.setBrokerIdentity(StringUtils.hasText(item.getIdCard()) ? item.getIdCard() : null);
                        a.setBrokerCustodyCode(StringUtils.hasText(item.getStockAccount()) ? item.getStockAccount() : null);

                        if (mapBroker.containsKey(a.getBrokerId())) {
                            BrokerDTO broker = mapBroker.get(a.getBrokerId());
                            a.setAgencyId(broker.getAgencyId());
                            a.setAgencyCode(broker.getAgency().getCode());
                            a.setAgencyName(broker.getAgency().getName());
                        }
                    }

                    if (mapCustomer.containsKey(a.getCounterId())) {
                        CustomerDTO item = mapCustomer.get(a.getCounterId());
                        a.setCounterName(StringUtils.hasText(item.getName()) ? item.getName() : null);
                        a.setCounterIdentity(StringUtils.hasText(item.getIdCard()) ? item.getIdCard() : null);
                        a.setCounterCustodyCode(StringUtils.hasText(item.getStockAccount()) ? item.getStockAccount() : null);
                    }

                    return a;
                }).collect(Collectors.toList());
            }

            report.setMatchedArrangement(result);
        }
        return new ResponseMessage(report, pf.getPaging());
    }

    @Override
    public ResponseMessage retrievePaymentArrangement(PagingFilterBase<ReportingFilter> pf) {

        List<ArrangementInfoDTO> result = null;

        ReportingMatchedArrangementDTO report = reportingMapper.retrievePaymentArrangementCount(pf.getFilter());

        if (report != null) {
            if (!pf.isPageable()) {
                result = reportingMapper.retrievePaymentArrangement(pf.getFilter()).getResult();
            } else {
                Long totalRecord = report.getTotalRecord();
                if (totalRecord != null && totalRecord > 0) {
                    Long totalPage = totalRecord / pf.getPageSize() + (totalRecord % pf.getPageSize() == 0 ? 0 : 1);

                    Integer offset = pf.getPageNum() * pf.getPageSize();
                    RowBounds rb = new RowBounds(offset, pf.getPageSize());

                    Page<ArrangementInfoDTO> page = reportingMapper.retrievePaymentArrangement(pf.getFilter(), rb);

                    result = page.getResult();
                    pf.getPaging().setTotalRecords(totalRecord);
                    pf.getPaging().setTotalPages(totalPage.intValue());
                }
            }

            if (!CollectionUtils.isEmpty(result)) {
                List<Long> customerIds = result.stream().filter(a -> a.getPartyId() != null).map(a -> a.getPartyId()).collect(Collectors.toList());

                List<CustomerDTO> customerData = customerService.retrieveCustomerInfo(customerIds);
                if (!CollectionUtils.isEmpty(customerData)) {
                    Map<Long, CustomerDTO> map = customerData.stream()
                            .collect(Collectors.toMap(CustomerDTO::getId, data -> data));

                    result = result.stream().map(a -> {
                        if (map.containsKey(a.getPartyId())) {
                            CustomerDTO item = map.get(a.getPartyId());
                            a.setPartyName(StringUtils.hasText(item.getName()) ? item.getName() : null);
                            a.setPartyIdentity(StringUtils.hasText(item.getIdCard()) ? item.getIdCard() : null);
                            a.setPartyCustodyCode(StringUtils.hasText(item.getStockAccount()) ? item.getStockAccount() : null);

                            if (item.getCusBankAccount() != null) {
                                a.setPartyBankAccount(StringUtils.hasText(item.getCusBankAccount().getAccountNumber()) ? item.getCusBankAccount().getAccountNumber() : null);
                                a.setPartyBankAccountName(StringUtils.hasText(item.getCusBankAccount().getAccountName()) ? item.getCusBankAccount().getAccountName() : null);
                                a.setPartyBank(StringUtils.hasText(item.getCusBankAccount().getBankName()) ? item.getCusBankAccount().getBankName() : null);
                                a.setPartyBankBranch(StringUtils.hasText(item.getCusBankAccount().getBranchName()) ? item.getCusBankAccount().getBranchName() : null);
                            }
                        }

                        return a;
                    }).collect(Collectors.toList());
                }
            }

            report.setMatchedArrangement(result);
        }
        return new ResponseMessage(report, pf.getPaging());
    }

    @Override
    public ResponseMessage retrieveSummaryArrangement(PagingFilterBase<ReportingFilter> pf) {
        List<ArrangementInfoDTO> result = null;

        ReportingMatchedArrangementDTO report = reportingMapper.retrieveSummaryArrangementCount(pf.getFilter());

        if (report != null) {
            if (!pf.isPageable()) {
                result = reportingMapper.retrieveSummaryArrangement(pf.getFilter()).getResult();
            } else {
                Long totalRecord = report.getTotalRecord();
                if (totalRecord != null && totalRecord > 0) {
                    Long totalPage = totalRecord / pf.getPageSize() + (totalRecord % pf.getPageSize() == 0 ? 0 : 1);

                    Integer offset = pf.getPageNum() * pf.getPageSize();
                    RowBounds rb = new RowBounds(offset, pf.getPageSize());

                    Page<ArrangementInfoDTO> page = reportingMapper.retrieveSummaryArrangement(pf.getFilter(), rb);

                    result = page.getResult();
                    pf.getPaging().setTotalRecords(totalRecord);
                    pf.getPaging().setTotalPages(totalPage.intValue());
                }
            }

            if (!CollectionUtils.isEmpty(result)) {
                List<Long> customerIds = result.stream().filter(a -> a.getPartyId() != null).map(a -> a.getPartyId()).collect(Collectors.toList());
                List<Long> brokerIds = result.stream().filter(a -> a.getBrokerId() != null).map(a -> a.getBrokerId()).collect(Collectors.toList());
                List<Long> counterIds = result.stream().filter(a -> a.getCounterId() != null).map(a -> a.getCounterId()).collect(Collectors.toList());

                Set<Long> set = new LinkedHashSet<>(customerIds);
                set.addAll(brokerIds);
                set.addAll(counterIds);
                List<Long> filters = new ArrayList<>(set);

                Map<Long, CustomerDTO> mapCus = new HashMap<>();
                List<CustomerDTO> customerData = customerService.retrieveCustomerInfo(filters);
                if (!CollectionUtils.isEmpty(customerData)) {
                    mapCus = customerData.stream().collect(Collectors.toMap(CustomerDTO::getId, data -> data));
                }
                final Map<Long, CustomerDTO> mapCustomer = mapCus;

                Map<Long, BrokerDTO> mapBrk = new HashMap<>();
                List<BrokerDTO> brokerData = brokerService.findBrokerDTOByCustomerID(brokerIds, null).collectList().block();
                if (!CollectionUtils.isEmpty(brokerData)) {
                    mapBrk = brokerData.stream().collect(Collectors.toMap(BrokerDTO::getCustomerId, data -> data));
                }
                final Map<Long, BrokerDTO> mapBroker = mapBrk;

                result = result.stream().map(a -> {
                    if (mapCustomer.containsKey(a.getPartyId())) {
                        CustomerDTO item = mapCustomer.get(a.getPartyId());
                        a.setPartyName(StringUtils.hasText(item.getName()) ? item.getName() : null);
                        a.setPartyIdentity(StringUtils.hasText(item.getIdCard()) ? item.getIdCard() : null);
                        a.setPartyCustodyCode(StringUtils.hasText(item.getStockAccount()) ? item.getStockAccount() : null);
                    }

                    if (mapCustomer.containsKey(a.getBrokerId())) {
                        CustomerDTO item = mapCustomer.get(a.getBrokerId());
                        a.setBrokerName(StringUtils.hasText(item.getName()) ? item.getName() : null);
                        a.setBrokerIdentity(StringUtils.hasText(item.getIdCard()) ? item.getIdCard() : null);
                        a.setBrokerCustodyCode(StringUtils.hasText(item.getStockAccount()) ? item.getStockAccount() : null);

                        if (mapBroker.containsKey(a.getBrokerId())) {
                            BrokerDTO broker = mapBroker.get(a.getBrokerId());
                            a.setAgencyId(broker.getAgencyId());
                            a.setAgencyCode(broker.getAgency().getCode());
                            a.setAgencyName(broker.getAgency().getName());
                        }
                    }

                    if (mapCustomer.containsKey(a.getCounterId())) {
                        CustomerDTO item = mapCustomer.get(a.getCounterId());
                        a.setCounterName(StringUtils.hasText(item.getName()) ? item.getName() : null);
                        a.setCounterIdentity(StringUtils.hasText(item.getIdCard()) ? item.getIdCard() : null);
                        a.setCounterCustodyCode(StringUtils.hasText(item.getStockAccount()) ? item.getStockAccount() : null);
                    }

                    ArrangementFilterStatusEnum arrStatus = ArrangementFilterStatusEnum
                            .lookByValue(a.getCustomerStatus() != null && a.getCustomerStatus() ? 1 : 0,
                                    a.getContractStatus() != null && a.getContractStatus() ? 1 : 0,
                                    a.getPaymentStatus() != null && a.getPaymentStatus() ? 1 : 0,
                                    a.getDeliveryStatus() != null && a.getDeliveryStatus() ? 1 : 0,
                                    0, 0,
                                    a.getStatus());
                    log.debug("Using status {} for {}", arrStatus, a.getArrangementId());

                    a.setStatus(arrStatus != null ? arrStatus.getFilterStatus() : null);

                    return a;
                }).collect(Collectors.toList());
            }

            report.setMatchedArrangement(result);
        }
        return new ResponseMessage<>(report, pf.getPaging());
    }


}