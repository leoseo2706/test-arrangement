package com.fiats.arrangement.controller;

import com.fiats.arrangement.payload.CollateralDTO;
import com.fiats.arrangement.payload.filter.CollateralFilter;
import com.fiats.arrangement.service.CollateralService;
import com.fiats.tmgjpa.paging.PageRequestDTO;
import com.fiats.tmgjpa.paging.PagingFilterBase;
import com.fiats.tmgjpa.payload.ResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Min;

@RestController
@RequestMapping(path = "/collateral")
public class CollateralController {

    @Autowired
    CollateralService collateralService;

    @GetMapping(path = "/draft")
    public ResponseMessage retrieveArrangementException(@RequestParam(value = "ac", required = false) String arrangementCode,
                                                        @RequestParam(value = "pid", required = false) Long partyId,
                                                        @RequestParam(value = "pageNum", required = false)
                                                        @Min(value = 1, message = "Page number must be larger than 0") Integer pageNum,
                                                        @RequestParam(value = "pageSize", required = false)
                                                        @Min(value = 1, message = "Page number must be larger than 0") Integer pageSize) {
        CollateralFilter filter = CollateralFilter.builder()
                .arrangementCode(arrangementCode)
                .partyId(partyId).build();
        PageRequestDTO page = PageRequestDTO.builder().pageNum(pageNum)
                .pageSize(pageSize).build();
        return collateralService.retrieveMortgageRequest(new PagingFilterBase<>(filter, page));
    }

    @PostMapping("/draft")
    public ResponseMessage insertMortgageRequest(@RequestBody CollateralDTO dto) {
        return new ResponseMessage<>(collateralService.insertMortgageRequest(dto));
    }

    @PatchMapping("/draft")
    public ResponseMessage updateMortgageRequest(@RequestBody CollateralDTO dto) {
        return new ResponseMessage<>(collateralService.updateMortgageRequest(dto));
    }

    @PatchMapping("/send-approve/{rqId}")
    public ResponseMessage sendApproveMortgageRequest(@PathVariable("rqId") String rdId) {
        return new ResponseMessage<>(collateralService.sendApproveMortgageRequest(rdId));
    }

    @PatchMapping("/reject/{rqId}")
    public ResponseMessage rejectMortgageRequest(@PathVariable("rqId") String rdId) {
        return new ResponseMessage<>(collateralService.rejectMortgageRequest(rdId));
    }

    @DeleteMapping("/{rqId}")
    public ResponseMessage removeMortgageRequest(@PathVariable("rqId") String rdId) {
        return new ResponseMessage<>(collateralService.removeMortgageRequest(rdId));
    }

    @PatchMapping("/approve/{rqId}")
    public ResponseMessage approveMortgageRequest(@PathVariable("rqId") String rdId) {
        return new ResponseMessage<>(collateralService.approveMortgageRequest(rdId));
    }
}