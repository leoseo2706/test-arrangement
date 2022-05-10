package com.fiats.arrangement.controller;

import com.fiats.arrangement.payload.filter.ArrangementExceptionFilter;
import com.fiats.arrangement.service.ArrangementExceptionService;
import com.fiats.tmgcoreutils.payload.ArrangementExceptionDTO;
import com.fiats.tmgjpa.paging.PageRequestDTO;
import com.fiats.tmgjpa.paging.PagingFilterBase;
import com.fiats.tmgjpa.payload.ResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Min;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(path = "/ex")
public class ArrangementExceptionController {

    @Autowired
    ArrangementExceptionService arrangementExceptionService;

    @GetMapping(path = "/wait")
    public ResponseMessage retrieveArrangementException(@RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                                        @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                                                        @RequestParam(value = "at", required = false) Integer arrangementType,
                                                        @RequestParam(value = "ac", required = false) String arrangementCode,
                                                        @RequestParam(value = "did", required = false) List<Long> derivativeId,
                                                        @RequestParam(value = "pid", required = false) Long partyId,
                                                        @RequestParam(value = "status", required = false) Integer status,
                                                        @RequestParam(value = "cn", required = false) String channel,
                                                        @RequestParam(value = "pageNum", required = false)
                                                        @Min(value = 1, message = "Page number must be larger than 0") Integer pageNum,
                                                        @RequestParam(value = "pageSize", required = false)
                                                        @Min(value = 1, message = "Page number must be larger than 0") Integer pageSize) {
        ArrangementExceptionFilter filter = ArrangementExceptionFilter.builder()
                .fromDate(fromDate != null ? Timestamp.valueOf(fromDate.atStartOfDay()) : null)
                .toDate(toDate != null ? Timestamp.valueOf(toDate.atStartOfDay()) : null)
                .type(arrangementType)
                .partyId(partyId)
                .arrangementCode(arrangementCode)
                .derivativeId(derivativeId)
                .status(status).build();
        PageRequestDTO page = PageRequestDTO.builder().pageNum(pageNum)
                .pageSize(pageSize).build();
        return arrangementExceptionService.retrieveExceptionRequest(new PagingFilterBase<>(filter, page));
    }

    @PostMapping("/draft")
    public ResponseMessage insertArrangementException(@Validated(ArrangementExceptionDTO.Insert.class) @RequestBody ArrangementExceptionDTO dto) {
        return new ResponseMessage<>(arrangementExceptionService.insertArrangementException(dto));
    }

    @PatchMapping("/draft")
    public ResponseMessage updateArrangementException(@Validated(ArrangementExceptionDTO.Update.class) @RequestBody ArrangementExceptionDTO dto) {
        return new ResponseMessage<>(arrangementExceptionService.updateArrangementException(dto));
    }

    @PatchMapping("/send-approve/{rqId}")
    public ResponseMessage sendApproveArrangementException(@PathVariable("rqId") String rdId) {
        return new ResponseMessage<>(arrangementExceptionService.sendApproveArrangementException(rdId));
    }

    @PatchMapping("/reject/{rqId}")
    public ResponseMessage rejectArrangementException(@PathVariable("rqId") String rdId) {
        return new ResponseMessage<>(arrangementExceptionService.rejectArrangementException(rdId));
    }

    @DeleteMapping("/{rqId}")
    public ResponseMessage removeArrangementException(@PathVariable("rqId") String rdId) {
        return new ResponseMessage<>(arrangementExceptionService.removeArrangementException(rdId));
    }

    @PatchMapping("/approve/{rqId}")
    public ResponseMessage approveArrangementException(@PathVariable("rqId") String rdId) {
        return new ResponseMessage<>(arrangementExceptionService.approveArrangementException(rdId));
    }
}