package com.fiats.arrangement.service;

import com.fiats.arrangement.payload.filter.BackSupportFilter;
import com.fiats.tmgcoreutils.payload.ArrangementInfoDTO;
import com.fiats.tmgjpa.paging.PagingFilterBase;
import com.fiats.tmgjpa.payload.ResponseMessage;

import java.util.List;
import java.util.Map;

public interface BackSupportService {

    ResponseMessage retrieveArrangements(PagingFilterBase<BackSupportFilter> pf);

    List<ArrangementInfoDTO> findArrangements(PagingFilterBase<BackSupportFilter> pf);

    ArrangementInfoDTO retrieveDetailArrangement(Long arrangementId, Long matchedId);

    Map<String, List<Integer>> prepareOperationStatus (List<String> operationStatus);
}
