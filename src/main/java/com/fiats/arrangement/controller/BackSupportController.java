package com.fiats.arrangement.controller;

import com.fiats.arrangement.payload.filter.BackSupportFilter;
import com.fiats.arrangement.service.BackSupportService;
import com.fiats.tmgjpa.paging.PageRequestDTO;
import com.fiats.tmgjpa.paging.PagingFilterBase;
import com.fiats.tmgjpa.payload.ResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Min;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "/back")
public class BackSupportController {

    @Autowired
    BackSupportService backSupportService;

    @GetMapping("")
    public ResponseMessage retrieveArrangements(@RequestParam(value = "dateType", required = false) String dateType,
                                                @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                                @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                                                @RequestParam(value = "at", required = false) List<Integer> arrangementType,
                                                @RequestParam(value = "pid", required = false) Long partyId,
                                                @RequestParam(value = "cid", required = false) Long counterId,
                                                @RequestParam(value = "ac", required = false) String arrangementCode,
                                                @RequestParam(value = "ls", required = false) Integer listedStatus,
                                                @RequestParam(value = "ex", required = false) Boolean exception,
                                                @RequestParam(value = "did", required = false) List<Long> derivativeId,
                                                @RequestParam(value = "as", required = false) List<Integer> arrangementStatus,
                                                @RequestParam(value = "os", required = false) List<String> operationStatus,
                                                @RequestParam(value = "pageNum", required = false)
                                                @Min(value = 1, message = "Page number must be larger than 0") final Integer pageNum,
                                                @RequestParam(value = "pageSize", required = false)
                                                @Min(value = 1, message = "Page size must be larger than 0") final Integer pageSize) {
        Map<String, List<Integer>> operationStatusMap = backSupportService.prepareOperationStatus(operationStatus);

        BackSupportFilter filter = BackSupportFilter.builder()
                .tradingDateFrom(StringUtils.hasText(dateType) && dateType.equals("TRD") && fromDate != null ? Timestamp.valueOf(fromDate.atStartOfDay()) : null)
                .tradingDateTo(StringUtils.hasText(dateType) && dateType.equals("TRD") && toDate != null ? Timestamp.valueOf(toDate.atStartOfDay()) : null)
                .paymentDateFrom(StringUtils.hasText(dateType) && dateType.equals("MTD") && fromDate != null ? Timestamp.valueOf(fromDate.atStartOfDay()) : null)
                .paymentDateTo(StringUtils.hasText(dateType) && dateType.equals("MTD") && toDate != null ? Timestamp.valueOf(toDate.atStartOfDay()) : null)
                .deliveryDateFrom(StringUtils.hasText(dateType) && dateType.equals("TFD") && fromDate != null ? Timestamp.valueOf(fromDate.atStartOfDay()) : null)
                .deliveryDateTo(StringUtils.hasText(dateType) && dateType.equals("TFD") && toDate != null ? Timestamp.valueOf(toDate.atStartOfDay()) : null)
                .partyId(partyId)
                .counterId(counterId)
                .arrangementCode(arrangementCode)
                .arrangementType(arrangementType)
                .listedStatus(listedStatus)
                .exception(exception)
                .derivativeId(derivativeId)
                .status(arrangementStatus)
                .operationStatus(operationStatusMap).build();

        PageRequestDTO paging = PageRequestDTO.builder().pageNum(pageNum).pageSize(pageSize).build();

        return backSupportService.retrieveArrangements(new PagingFilterBase<>(filter, paging));
    }

    @GetMapping("/detail/{arrangementId}")
    public ResponseMessage retrieveDetailArrangement(@PathVariable Long arrangementId,
                                                     @RequestParam(name = "mid", required = false) Long matchedId) {
        return new ResponseMessage<>(backSupportService.retrieveDetailArrangement(arrangementId, matchedId));
    }
}