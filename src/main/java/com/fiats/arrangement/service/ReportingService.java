package com.fiats.arrangement.service;

import com.fiats.arrangement.payload.filter.ReportingFilter;
import com.fiats.tmgjpa.paging.PagingFilterBase;
import com.fiats.tmgjpa.payload.ResponseMessage;

import java.util.List;
import java.util.Map;

public interface ReportingService {

    Map<String, List<Integer>> prepareOperationStatus (List<String> operationStatus);

    ResponseMessage retrieveMatchedArrangement(PagingFilterBase<ReportingFilter> pf);

    ResponseMessage retrievePaymentArrangement(PagingFilterBase<ReportingFilter> pf);

    ResponseMessage retrieveSummaryArrangement(PagingFilterBase<ReportingFilter> pf);
}
