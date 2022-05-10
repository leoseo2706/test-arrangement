package com.fiats.arrangement.service.impl;

import com.fiats.arrangement.jpa.repo.ArrangementRepo;
import com.fiats.arrangement.service.ArrangementCodeService;
import com.fiats.exception.ValidationException;
import com.fiats.tmgcoreutils.constant.Constant;
import com.fiats.tmgcoreutils.payload.BrokerDTO;
import com.fiats.tmgcoreutils.payload.ProdDerivativeDTO;
import com.fiats.tmgcoreutils.utils.CommonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class ArrangementCodeServiceImpl implements ArrangementCodeService {

    @Value("${organization.prefix}")
    String organizationPrefix;

    @Autowired
    ArrangementRepo arrRepo;

    @Override
    public String generateTradingCode(BrokerDTO brokerDTO, ProdDerivativeDTO derivativeDTO) {

        log.info("Using {} and {} to generate trading code", brokerDTO, derivativeDTO);

        if (derivativeDTO == null || CommonUtils.isInvalidPK(derivativeDTO.getId())
                || !StringUtils.hasText(derivativeDTO.getCode())) {
            throw new ValidationException("Invalid derivative product while generating trading code");
        }

        String agencyCode = brokerDTO != null && brokerDTO.getAgency() != null
                && StringUtils.hasText(brokerDTO.getAgency().getCode())
                ? brokerDTO.getAgency().getCode() : organizationPrefix;

        String tradingCode = generateTradingCode(agencyCode, derivativeDTO.getCode());

        log.info("Generated trading code {}", tradingCode);

        return tradingCode;
    }

    @Override
    public String generateTradingCode(String agencyCode, String derivativeCode) {

        String sequenceNext = arrRepo.getNextTradingCodeVal().toString();
        String uniqueNumbers = Constant.UNIQUE_CODE_NINE_DIGITS.substring(0,
                Constant.UNIQUE_CODE_NINE_DIGITS.length() - sequenceNext.length()).concat(sequenceNext);
        log.info("Getting sequence {}, uniqueNumbers {}", sequenceNext, uniqueNumbers);

        return CommonUtils.format(Constant.ARRANGEMENT_CODE_FORMAT, agencyCode.trim(),
                derivativeCode.trim(), uniqueNumbers);
    }


}