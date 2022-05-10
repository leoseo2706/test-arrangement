package com.fiats.arrangement.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiats.arrangement.constant.ArrangementErrorCode;
import com.fiats.arrangement.constant.ArrangementTypeEnum;
import com.fiats.arrangement.service.AttributeService;
import com.fiats.exception.NeoFiatsException;
import com.fiats.tmgcoreutils.constant.Constant;
import com.fiats.tmgcoreutils.constant.PropsReqFilterEnum;
import com.fiats.tmgcoreutils.payload.PropsReq;
import com.fiats.tmgcoreutils.payload.PropsResp;
import com.fiats.tmgcoreutils.utils.CommonUtils;
import com.fiats.tmgcoreutils.utils.DateHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AttributeServiceImpl implements AttributeService {

    @Value("${fiats.internal.api.prop.url}")
    private String propUrl;

    @Autowired
    @Qualifier("customWebClient")
    WebClient webClient;

    @Autowired
    @Qualifier(Constant.DEFAULT_THREAD_POOL)
    TaskExecutor executor;

    @Autowired
    @Qualifier("customMapper")
    ObjectMapper customMapper;

    public final static String CONTRACT_VARIABLE_CODE = "contract";

    @Override
    public PropsResp findAttribute(String attributeName, Timestamp tradingDate,  Map<String, Object> filter) {
        log.info("Finding attribute value: {} - {}", attributeName, filter);

        return callAttributeService(attributeName, tradingDate, filter);
    }

    @Override
    public PropsResp findSystemAttribute(String attribute, Timestamp tradingDate,
                                         String prodDerivativeCode,
                                         String prodVanillaCode, String prodAgreement,
                                         ArrangementTypeEnum transactionType,
                                         String channel, String cusType) {

        log.info("Finding a single system attribute based on {}, {}, {}, {}, {}, {}, {}, {}",
                attribute, tradingDate, prodDerivativeCode, prodVanillaCode, prodAgreement,
                transactionType, channel, cusType);

        Map<String, Object> filterMap = buildFilter(prodDerivativeCode, prodVanillaCode, prodAgreement,
                transactionType, channel, cusType) ;

        return callAttributeService(attribute, tradingDate, filterMap);
    }

    private Map<String, Object> buildFilter(String prodDerivativeCode,
                                            String prodVanillaCode, String prodAgreement,
                                            ArrangementTypeEnum transactionType,
                                            String channel, String cusType) {
        Map<String, Object> filters = new HashMap<>();

        if (StringUtils.hasText(channel)) {
            filters.put(PropsReqFilterEnum.CHANNEL.getFilterAttr(), channel);
        }

        if (StringUtils.hasText(cusType)) {
            filters.put(PropsReqFilterEnum.CUSTOMER_TYPE.getFilterAttr(), cusType);
        }

        if (StringUtils.hasText(prodAgreement)) {
            filters.put(PropsReqFilterEnum.PROD_AGREEMENT.getFilterAttr(), prodAgreement);
        }
        if (StringUtils.hasText(prodVanillaCode)) {
            filters.put(PropsReqFilterEnum.PROD_VANILLA.getFilterAttr(), prodVanillaCode);
        }
        if (StringUtils.hasText(prodDerivativeCode)) {
            filters.put(PropsReqFilterEnum.PROD_DERIVATIVE.getFilterAttr(), prodDerivativeCode);
        }
        if (transactionType != null) {
            filters.put(PropsReqFilterEnum.TRANSACTION_TYPE.getFilterAttr(), transactionType.getTypeStr());
        }
        return filters;
    }

    private PropsResp callAttributeService(String attribute, Timestamp tradingDate, Map<String, Object> filters) {

        log.info("Finding system attribute based on {}, {}, {}", attribute, tradingDate, filters);

        PropsReq propsReq = PropsReq.builder()
                .requestDate(DateHelper.formatDateSilently(tradingDate))
                .variableCode(attribute)
                .filter(filters)
                .build();

        PropsResp propsResp = CommonUtils.callRestGetProp(propsReq, webClient, propUrl, executor)
                .onErrorMap(t -> {
                    log.error("Getting error while finding attributes: {}", t.getMessage());
                    log.error("Stack trace: ", t);
                    throw new NeoFiatsException(ArrangementErrorCode.ATTRIBUTE_NOT_FOUND,
                            CommonUtils.format("Failed to retrieve prop attribute based on {0}, {1} and {2}",
                                    propsReq, filters));
                }).blockFirst();

        if (propsResp == null || !StringUtils.hasText(propsResp.getValue())) {
            throw new NeoFiatsException("Empty or invalid attribute request for "
                    .concat(attribute));
        }

        return propsResp;

    }

    @Override
    public Map<String, PropsResp> findSystemAttributes(Set<String> attributes, Timestamp tradingDate,
                                                       String prodDerivativeCode, String prodVanillaCode,
                                                       String prodAgreement, ArrangementTypeEnum transactionType,
                                                       String channel, String cusType) {

        log.info("Finding a list of system attributes based on {}, {}, {}, {}, {}, {}, {}, {}",
                attributes, tradingDate, prodDerivativeCode, prodVanillaCode, prodAgreement,
                transactionType, channel, cusType);

        Map<String, Object> filterMap = buildFilter(prodDerivativeCode, prodVanillaCode, prodAgreement,
                transactionType, channel, cusType);

        return attributes.stream()
                .map(attribute -> callAttributeService(attribute, tradingDate, filterMap))
                .collect(Collectors.toMap(PropsResp::getVariable, Function.identity()));
    }

}