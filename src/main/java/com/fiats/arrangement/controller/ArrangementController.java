package com.fiats.arrangement.controller;

import com.fiats.arrangement.payload.filter.ArrangementFilter;
import com.fiats.arrangement.service.ArrangementCodeService;
import com.fiats.arrangement.service.ArrangementService;
import com.fiats.arrangement.validator.annotation.ArrangementFilterStatus;
import com.fiats.tmgcoreutils.constant.Constant;
import com.fiats.tmgcoreutils.jwt.JWTHelper;
import com.fiats.tmgcoreutils.payload.OrderPlacementDTO;
import com.fiats.tmgcoreutils.utils.DateHelper;
import com.fiats.tmgcoreutils.validator.annotation.ArrangementType;
import com.fiats.tmgjpa.paging.PageRequestDTO;
import com.fiats.tmgjpa.paging.PagingFilterBase;
import com.fiats.tmgjpa.payload.ResponseMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Positive;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping(path = "/order")
@Validated
@Slf4j
public class ArrangementController {

    @Autowired
    ArrangementService service;

    @Autowired
    ArrangementCodeService arrCodeService;

    @PostMapping("/place")
    public ResponseMessage placeOrder(
            @RequestBody @Validated(OrderPlacementDTO.PlaceOrder.class) OrderPlacementDTO dto,
            @RequestHeader(JWTHelper.Authorization)
            @NotEmpty(message = "Authorization cannot be empty") String authHeader) {
        return new ResponseMessage<>(service.placeOrderAndSendNoti(dto, new JWTHelper(authHeader), false));
    }

    @PostMapping("/broker/place")
    public ResponseMessage placeOrderForBrokerReference(
            @RequestBody @Validated(OrderPlacementDTO.PlaceOrder.class) OrderPlacementDTO dto,
            @RequestHeader(JWTHelper.Authorization)
            @NotEmpty(message = "Authorization cannot be empty") String authHeader) {
        return new ResponseMessage<>(service.placeOrderAndSendNoti(dto, new JWTHelper(authHeader), true));
    }

    @PatchMapping("/customer-confirm/{arrangementId}") // for RM Reference
    public ResponseMessage customConfirmForReference(
            @RequestHeader(JWTHelper.Authorization)
            @NotEmpty(message = "Authorization cannot be empty") String authToken,
            @PathVariable
            @Positive(message = "Arrangement ID must be positive") Long arrangementId) {
        return new ResponseMessage<>(service.customerConfirmForReference(arrangementId, new JWTHelper(authToken)));
    }

    @PatchMapping("/cancel/{arrangementId}")
    public ResponseMessage cancelArrangement(
            @RequestHeader(JWTHelper.Authorization)
            @NotEmpty(message = "Authorization cannot be empty") String authToken,
            @PathVariable
            @Positive(message = "Arrangement ID must be positive") Long arrangementId) {
        return new ResponseMessage<>(service.cancelArrangementAndSendSignal(arrangementId,
                new JWTHelper(authToken), Constant.INACTIVE));
    }

    @GetMapping("/history/broker")
    public ResponseMessage listBrokerArrangement(
            @RequestParam(value = "sd", required = false)
            @DateTimeFormat(pattern = Constant.FORMAT_SQLSERVER_SHORT) Date startDate,
            @RequestParam(value = "ed", required = false)
            @DateTimeFormat(pattern = Constant.FORMAT_SQLSERVER_SHORT) Date endDate,
            @RequestParam(value = "s", required = false)
                    List<@ArrangementFilterStatus(optional = true) Integer> statuses,
            @RequestParam(value = "t", required = false)
                    List<@ArrangementType(optional = true) Integer> types,
            @RequestParam(value = "pn", required = false)
            @Min(value = 1, message = "Page number must be larger than 0") final Integer pageNum,
            @RequestParam(value = "ps", required = false)
            @Min(value = 1, message = "Page size must be larger than 0") final Integer pageSize,
            @RequestHeader(JWTHelper.Authorization)
            @NotEmpty(message = "Authorization cannot be empty") String authHeader) {
        return service.listBrokerArrangement(buildPagingFilter(startDate, endDate, statuses,
                types, pageNum, pageSize), new JWTHelper(authHeader));
    }

    @GetMapping("/history")
    public ResponseMessage listCustomerArrangement(
            @RequestParam(value = "sd", required = false)
            @DateTimeFormat(pattern = Constant.FORMAT_SQLSERVER_SHORT) Date startDate,
            @RequestParam(value = "ed", required = false)
            @DateTimeFormat(pattern = Constant.FORMAT_SQLSERVER_SHORT) Date endDate,
            @RequestParam(value = "s", required = false)
                    List<@ArrangementFilterStatus(optional = true) Integer> statuses,
            @RequestParam(value = "t", required = false)
                    List<@ArrangementType(optional = true) Integer> types,
            @RequestParam(value = "pn", required = false)
            @Min(value = 1, message = "Page number must be larger than 0") final Integer pageNum,
            @RequestParam(value = "ps", required = false)
            @Min(value = 1, message = "Page size must be larger than 0") final Integer pageSize,
            @RequestHeader(JWTHelper.Authorization)
            @NotEmpty(message = "Authorization cannot be empty") String authHeader) {
        return service.listCustomerArrangement(buildPagingFilter(startDate, endDate, statuses,
                types, pageNum, pageSize), new JWTHelper(authHeader));
    }

    @PostMapping("/sign-contract/{arrangementId}")
    public ResponseMessage signContract(
            @RequestHeader(JWTHelper.CUSTOM_AUTH_HEADER)
            @NotEmpty(message = "Authorization cannot be empty") String authHeader,
            @PathVariable
            @Positive(message = "Arrangement ID must be positive") Long arrangementId) {
        return new ResponseMessage<>(service.signContractAndSendSignal(arrangementId, new JWTHelper(authHeader)));
    }

    @GetMapping("/trading-code/generate")
    public ResponseMessage generateTradingCode(
            @RequestParam(value = "ac") String agencyCode,
            @RequestParam(value = "dc") String derivativeCode) {
        return new ResponseMessage<>(arrCodeService.generateTradingCode(agencyCode, derivativeCode));
    }

    @GetMapping("/broker/contract/{arrangementId}")
    public ResponseMessage getContractsBrokerSupport(
            @RequestHeader(JWTHelper.Authorization)
            @NotEmpty(message = "Authorization cannot be empty") String authToken,
            @PathVariable(value = "arrangementId") Long arrangementId) {
        return new ResponseMessage<>(service.getContractsBrokerSupport(new JWTHelper(authToken), arrangementId));
    }

    @GetMapping("/broker/contract/{arrangementId}/{contractName}")
    public ResponseMessage downloadContractBrokerSupport(
            @RequestHeader(JWTHelper.Authorization)
            @NotEmpty(message = "Authorization cannot be empty") String authToken,
            @PathVariable(value = "arrangementId") Long arrangementId,
            @PathVariable(value = "contractName") String contractName) {
        return new ResponseMessage<>(service.downloadContractBrokerSupport(new JWTHelper(authToken), arrangementId, contractName));
    }

    private PagingFilterBase<ArrangementFilter> buildPagingFilter(Date startDate,
                                                                  Date endDate,
                                                                  List<Integer> statuses,
                                                                  List<Integer> types,
                                                                  Integer pageNum,
                                                                  Integer pageSize) {
        ArrangementFilter filter = ArrangementFilter.builder()
                .startDate(DateHelper.dateToTimestamp(startDate))
                .endDate(DateHelper.dateToTimestamp(endDate))
                .filterStatuses(statuses)
                .types(types).build();
        PageRequestDTO paging = PageRequestDTO.builder()
                .pageNum(pageNum).pageSize(pageSize).build();
        return new PagingFilterBase<>(filter, paging);
    }

}