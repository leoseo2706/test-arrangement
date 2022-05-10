package com.fiats.arrangement.service;

import com.fiats.tmgcoreutils.constant.IntradayLimitType;
import com.fiats.tmgcoreutils.payload.IntradayLimitDTO;
import reactor.core.publisher.Flux;

import java.sql.Timestamp;
import java.util.List;

public interface IntradayLimitService {

    Flux<IntradayLimitDTO> findIntradayLimitThresholds(List<IntradayLimitType> types,
                                                       List<String> vanillaCodes,
                                                       List<String> issuerCodes,
                                                       Timestamp tradingDate);
}
