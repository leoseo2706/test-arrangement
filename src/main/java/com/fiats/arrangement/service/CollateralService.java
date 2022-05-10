package com.fiats.arrangement.service;

import com.fiats.arrangement.payload.CollateralDTO;
import com.fiats.arrangement.payload.filter.CollateralFilter;
import com.fiats.tmgcoreutils.payload.OrderPlacementDTO;
import com.fiats.tmgjpa.paging.PagingFilterBase;
import com.fiats.tmgjpa.payload.ResponseMessage;

public interface CollateralService {

    ResponseMessage retrieveMortgageRequest(PagingFilterBase<CollateralFilter> pf);

    CollateralDTO insertMortgageRequest(CollateralDTO dto);

    CollateralDTO updateMortgageRequest(CollateralDTO dto);

    CollateralDTO sendApproveMortgageRequest(String id);

    CollateralDTO rejectMortgageRequest(String id);

    Object removeMortgageRequest(String id);

    OrderPlacementDTO approveMortgageRequest(String dto);

}
