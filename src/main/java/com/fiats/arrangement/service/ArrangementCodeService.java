package com.fiats.arrangement.service;

import com.fiats.tmgcoreutils.payload.BrokerDTO;
import com.fiats.tmgcoreutils.payload.ProdDerivativeDTO;

public interface ArrangementCodeService {

    String generateTradingCode(BrokerDTO brokerDTO, ProdDerivativeDTO derivativeDTO);

    String generateTradingCode(String agencyCode, String derivativeCode);
}
