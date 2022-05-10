package com.fiats.arrangement.service;

import com.fiats.arrangement.payload.filter.ArrangementExceptionFilter;
import com.fiats.tmgcoreutils.payload.ArrangementExceptionDTO;
import com.fiats.tmgcoreutils.payload.OrderPlacementDTO;
import com.fiats.tmgjpa.paging.PagingFilterBase;
import com.fiats.tmgjpa.payload.ResponseMessage;

public interface ArrangementExceptionService {

    ResponseMessage retrieveExceptionRequest(PagingFilterBase<ArrangementExceptionFilter> pf);

    ArrangementExceptionDTO insertArrangementException(ArrangementExceptionDTO dto);

    ArrangementExceptionDTO updateArrangementException(ArrangementExceptionDTO dto);

    ArrangementExceptionDTO sendApproveArrangementException(String id);

    ArrangementExceptionDTO rejectArrangementException(String id);

    Object removeArrangementException(String id);

    OrderPlacementDTO approveArrangementException(String dto);

}
