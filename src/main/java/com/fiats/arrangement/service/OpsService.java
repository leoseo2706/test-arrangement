package com.fiats.arrangement.service;

import com.fiats.arrangement.payload.filter.BackSupportFilter;
import com.fiats.tmgjpa.paging.PagingFilterBase;

public interface OpsService {
    void expire(PagingFilterBase<BackSupportFilter> pf);
}
