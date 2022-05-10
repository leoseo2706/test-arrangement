package com.fiats.arrangement.controller;

import com.fiats.arrangement.payload.AccountingEntryMapper;
import com.fiats.arrangement.payload.filter.AccountingEntryFilter;
import com.fiats.arrangement.service.AccountingEntryService;
import com.fiats.arrangement.validator.annotation.AccountingEntryStatus;
import com.fiats.tmgcoreutils.constant.Constant;
import com.fiats.tmgcoreutils.jwt.JWTHelper;
import com.fiats.tmgcoreutils.utils.DateHelper;
import com.fiats.tmgcoreutils.validator.annotation.ArrangementType;
import com.fiats.tmgjpa.paging.PageRequestDTO;
import com.fiats.tmgjpa.paging.PagingFilterBase;
import com.fiats.tmgjpa.payload.ResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping(path = "/accounting")
@Validated
public class AccountingEntryController {

    @Autowired
    AccountingEntryService accountingEntryService;

    @GetMapping
    public ResponseMessage listEntries(
            @RequestParam(value = "sd", required = false)
            @DateTimeFormat(pattern = Constant.FORMAT_SQLSERVER_SHORT) Date startDate,
            @RequestParam(value = "ed", required = false)
            @DateTimeFormat(pattern = Constant.FORMAT_SQLSERVER_SHORT) Date endDate,
            @RequestParam(value = "s", required = false)
                    List<@AccountingEntryStatus(optional = true) String> statuses,
            @RequestParam(value = "t", required = false)
            @ArrangementType(optional = true) Integer type,
            @RequestParam(value = "c", required = false) String arrCode,
            @RequestParam(value = "d", required = false) String description,
            @RequestParam(value = "pn", required = false)
            @Min(value = 1, message = "Page number must be larger than 0") final Integer pageNum,
            @RequestParam(value = "ps", required = false)
            @Min(value = 1, message = "Page size must be larger than 0") final Integer pageSize) {
        AccountingEntryFilter filter = AccountingEntryFilter.builder()
                .arrangementCode(arrCode)
                .startDate(DateHelper.dateToTimestamp(startDate))
                .endDate(DateHelper.dateToTimestamp(endDate))
                .statuses(statuses)
                .description(description)
                .type(type).build();
        PageRequestDTO paging = PageRequestDTO.builder().pageNum(pageNum)
                .pageSize(pageSize).build();
        return accountingEntryService.listAccountingEntries(new PagingFilterBase<>(filter, paging));
    }

    @GetMapping("/arrangement/code/{code}")
    public ResponseMessage findArrangementInfo(@PathVariable String code) {
        return new ResponseMessage<>(accountingEntryService.findArrangementInfo(code));
    }

    @PostMapping("/upload")
    public ResponseMessage importAndMap(@RequestParam(value = "excelFile") MultipartFile file,
                                        @RequestHeader(JWTHelper.Authorization)
                                        @NotEmpty(message = "Authorization cannot be empty") String authHeader) {
        return new ResponseMessage<>(accountingEntryService.importAndMapEntries(file, new JWTHelper(authHeader)));
    }

    @PostMapping("/map")
    public ResponseMessage mapManually(@RequestBody @Valid AccountingEntryMapper model,
                                       @RequestHeader(JWTHelper.Authorization)
                                       @NotEmpty(message = "Authorization cannot be empty") String authHeader) {
        return new ResponseMessage<>(accountingEntryService.manuallyMapEntry(model, new JWTHelper(authHeader)));
    }
}