package com.fiats.arrangement.service;

import com.fiats.arrangement.constant.ArrangementTypeEnum;
import com.fiats.tmgcoreutils.payload.PropsResp;

import java.sql.Timestamp;
import java.util.Map;
import java.util.Set;

public interface AttributeService {

    PropsResp findAttribute(String attributeName, Timestamp tradingDate,  Map<String, Object> filter);

    PropsResp findSystemAttribute(String attribute, Timestamp tradingDate,
                                  String prodDerivativeCode,
                                  String prodVanillaCode, String prodAgreement,
                                  ArrangementTypeEnum transactionType,
                                  String channel, String cusType);


    Map<String, PropsResp> findSystemAttributes(Set<String> attributes, Timestamp tradingDate,
                                                String prodDerivativeCode,
                                                String prodVanillaCode, String prodAgreement,
                                                ArrangementTypeEnum transactionType,
                                                String channel, String cusType);


}
