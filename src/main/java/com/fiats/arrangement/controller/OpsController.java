package com.fiats.arrangement.controller;

import com.fiats.arrangement.payload.filter.BackSupportFilter;
import com.fiats.arrangement.service.BackSupportService;
import com.fiats.arrangement.service.OpsService;
import com.fiats.tmgjpa.paging.PageRequestDTO;
import com.fiats.tmgjpa.paging.PagingFilterBase;
import com.neo.exception.LoggingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "/ops")
@Slf4j
public class OpsController {

    @Autowired
    private BackSupportService backSupportService;

    @Autowired
    private OpsService opsService;

    @GetMapping("/auto-expire")
    public void expireArrangement(@RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                  @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                                  @RequestParam(value = "at", required = false) List<Integer> arrangementType,
                                  @RequestParam(value = "pid", required = false) Long partyId,
                                  @RequestParam(value = "cid", required = false) Long counterId,
                                  @RequestParam(value = "ac", required = false) String arrangementCode,
                                  @RequestParam(value = "ls", required = false) Integer listedStatus,
                                  @RequestParam(value = "ex", required = false) Boolean exception,
                                  @RequestParam(value = "did", required = false) List<Long> derivativeId,
                                  @RequestParam(value = "as", required = false) List<Integer> arrangementStatus,
                                  @RequestParam(value = "os", required = false) List<String> operationStatus) {
        Map<String, List<Integer>> operationStatusMap = backSupportService.prepareOperationStatus(operationStatus);

        LocalDate now = LocalDate.now();

        BackSupportFilter filter = BackSupportFilter.builder()
                .tradingDateFrom(fromDate != null ? Timestamp.valueOf(fromDate.atStartOfDay()) : Timestamp.valueOf(now.atStartOfDay()))
                .tradingDateTo(toDate != null ? Timestamp.valueOf(toDate.atStartOfDay()) : Timestamp.valueOf(now.atStartOfDay()))
                .partyId(partyId)
                .counterId(counterId)
                .arrangementCode(arrangementCode)
                .arrangementType(arrangementType)
                .listedStatus(listedStatus)
                .exception(exception)
                .derivativeId(derivativeId)
                .status(arrangementStatus)
                .operationStatus(operationStatusMap).build();

        log.info("Request expire: {}", LoggingUtils.objToStringIgnoreEx(filter));

        PageRequestDTO paging = PageRequestDTO.builder().pageNum(1).pageSize(99999).build();

        opsService.expire(new PagingFilterBase<>(filter, paging));
    }
}
