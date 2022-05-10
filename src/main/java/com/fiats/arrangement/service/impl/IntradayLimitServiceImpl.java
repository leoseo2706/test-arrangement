package com.fiats.arrangement.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiats.arrangement.constant.ArrangementErrorCode;
import com.fiats.arrangement.service.IntradayLimitService;
import com.fiats.exception.NeoFiatsException;
import com.fiats.tmgcoreutils.constant.Constant;
import com.fiats.tmgcoreutils.constant.IntradayLimitType;
import com.fiats.tmgcoreutils.payload.IntradayLimitDTO;
import com.fiats.tmgcoreutils.utils.CommonUtils;
import com.fiats.tmgcoreutils.utils.DateHelper;
import com.fiats.tmgcoreutils.utils.ReactiveClientUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class IntradayLimitServiceImpl implements IntradayLimitService {

    @Autowired
    @Qualifier("customWebClient")
    WebClient client;

    @Value("${fiats.internal.api.info.url}")
    String infoUrl;

    @Autowired
    @Qualifier(Constant.DEFAULT_THREAD_POOL)
    TaskExecutor executor;


    @Override
    public Flux<IntradayLimitDTO> findIntradayLimitThresholds(List<IntradayLimitType> types,
                                                              List<String> vanillaCodes,
                                                              List<String> issuerCodes,
                                                              Timestamp tradingDate) {

        if (tradingDate == null || CollectionUtils.isEmpty(types) || CollectionUtils.isEmpty(vanillaCodes)
                || CollectionUtils.isEmpty(issuerCodes)
                || vanillaCodes.stream().anyMatch(Objects::isNull)
                || issuerCodes.stream().anyMatch(Objects::isNull)) {
            throw new NeoFiatsException(ArrangementErrorCode.INTRADAY_LIMIT_EMPTY_PAYLOAD,
                    CommonUtils.format("Empty payload while finding intraday limit {0}, {1}, {2}, {3}",
                            types, vanillaCodes, issuerCodes, tradingDate));
        }

        String typeStr = types.stream().map(Enum::toString)
                .collect(Collectors.joining(Constant.COMMA));

        return ReactiveClientUtils.getForFlux(client, infoUrl,
                "/intraday/info?productCode={productCode}&issuerCode={issuerCode}&type={type}&appliedDate={appliedDate}",
                executor, IntradayLimitDTO.class, log, String.join(Constant.COMMA, vanillaCodes), String.join(Constant.COMMA, issuerCodes),
                typeStr, DateHelper.formatDateSilently(tradingDate));
    }
}