package com.fiats.arrangement.controller;

import com.fiats.arrangement.payload.filter.ReportingFilter;
import com.fiats.arrangement.service.ReportingService;
import com.fiats.tmgjpa.paging.PageRequestDTO;
import com.fiats.tmgjpa.paging.PagingFilterBase;
import com.fiats.tmgjpa.payload.ResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Min;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(path = "/report")
public class ReportingController {

    @Autowired
    ReportingService reportingService;

    @GetMapping("/matched")
    public ResponseMessage retrieveMatchedTransaction(@RequestParam(name = "at", required = false) Integer type,
                                                      @RequestParam(name = "td", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tradingDate,
                                                      @RequestParam(name = "did", required = false) List<Long> derivativeId,
                                                      @RequestParam(name = "ac", required = false) String arrangementCode,
                                                      @RequestParam(name = "cid", required = false) Long customerId,
                                                      @RequestParam(name = "bid", required = false) Long brokerId,
                                                      @RequestParam(name = "aid", required = false) Long agencyId,
                                                      @RequestParam(value = "pn", required = false)
                                                      @Min(value = 1, message = "Page number must be larger than 0") final Integer pageNum,
                                                      @RequestParam(value = "ps", required = false)
                                                      @Min(value = 1, message = "Page size must be larger than 0") final Integer pageSize) {

        ReportingFilter filter = ReportingFilter.builder()
                .tradingDate(tradingDate != null ? Timestamp.valueOf(tradingDate.atStartOfDay()) : Timestamp.valueOf(LocalDate.now().atStartOfDay()))
                .arrangementCode(arrangementCode)
                .arrangementType(type)
                .derivativeId(derivativeId)
                .customerId(customerId)
                .brokerId(brokerId)
                .agencyId(agencyId).build();

        PageRequestDTO paging = PageRequestDTO.builder().pageNum(pageNum).pageSize(pageSize).build();

        return reportingService.retrieveMatchedArrangement(new PagingFilterBase<>(filter, paging));
    }

    @GetMapping("/payment")
    public ResponseMessage retrievePaymentTransaction(@RequestParam(name = "td", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tradingDate,
                                                      @RequestParam(name = "ac", required = false) String arrangementCode,
                                                      @RequestParam(name = "cid", required = false) Long customerId,
                                                      @RequestParam(value = "pn", required = false)
                                                      @Min(value = 1, message = "Page number must be larger than 0") final Integer pageNum,
                                                      @RequestParam(value = "ps", required = false)
                                                      @Min(value = 1, message = "Page size must be larger than 0") final Integer pageSize) {

        ReportingFilter filter = ReportingFilter.builder()
                .tradingDate(tradingDate != null ? Timestamp.valueOf(tradingDate.atStartOfDay()) : Timestamp.valueOf(LocalDate.now().atStartOfDay()))
                .arrangementCode(arrangementCode)
                .customerId(customerId).build();

        PageRequestDTO paging = PageRequestDTO.builder().pageNum(pageNum).pageSize(pageSize).build();

        return reportingService.retrievePaymentArrangement(new PagingFilterBase<>(filter, paging));
    }

    @GetMapping("/summary")
    public ResponseMessage retrieveSummaryTransaction(@RequestParam(name = "at", required = false) Integer type,
                                                      @RequestParam(name = "td", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tradingDate,
                                                      @RequestParam(name = "did", required = false) List<Long> derivativeId,
                                                      @RequestParam(name = "ac", required = false) String arrangementCode,
                                                      @RequestParam(name = "cid", required = false) Long customerId,
                                                      @RequestParam(name = "bid", required = false) Long brokerId,
                                                      @RequestParam(name = "aid", required = false) Long agencyId,
                                                      @RequestParam(name = "as", required = false) Integer arrangementStatus,
                                                      @RequestParam(value = "pn", required = false)
                                                      @Min(value = 1, message = "Page number must be larger than 0") final Integer pageNum,
                                                      @RequestParam(value = "ps", required = false)
                                                      @Min(value = 1, message = "Page size must be larger than 0") final Integer pageSize) {

        ReportingFilter filter = ReportingFilter.builder()
                .tradingDate(tradingDate != null ? Timestamp.valueOf(tradingDate.atStartOfDay()) : Timestamp.valueOf(LocalDate.now().atStartOfDay()))
                .arrangementCode(arrangementCode)
                .arrangementType(type)
                .derivativeId(derivativeId)
                .customerId(customerId)
                .brokerId(brokerId)
                .agencyId(agencyId)
                .arrangementStatus(arrangementStatus).build();

        PageRequestDTO paging = PageRequestDTO.builder().pageNum(pageNum).pageSize(pageSize).build();

        return reportingService.retrieveSummaryArrangement(new PagingFilterBase<>(filter, paging));
    }
}