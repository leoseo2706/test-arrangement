package com.fiats.arrangement.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiats.arrangement.constant.ArrangementErrorCode;
import com.fiats.arrangement.constant.ArrangementTypeEnum;
import com.fiats.arrangement.payload.PricingParamDTO;
import com.fiats.arrangement.service.PricingService;
import com.fiats.tmgcoreutils.payload.PricingBondResultDTO;
import com.fiats.tmgcoreutils.utils.DateHelper;
import com.neo.exception.NeoException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Slf4j
public class PricingServiceImpl implements PricingService {

    @Value("${fiats.internal.api.pricing.url}")
    String pricingUrl;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    RestTemplate restTemplate;

    @Override
    public List<PricingBondResultDTO> calculatePricingBond(PricingParamDTO paramDTO) {

        try {

            String url = pricingUrl.concat("/bond/normal?");

            List<String> params = new ArrayList<>();
            if (StringUtils.hasText(paramDTO.getProductCode())) {
                params.add("productCode=" + paramDTO.getProductCode());
            }

            String tradingDate = DateHelper.formatDateSilently(paramDTO.getTradingDate());
            if (StringUtils.hasText(tradingDate)) {
                params.add("tradingDate=" + tradingDate);
            }

            if (StringUtils.hasText(paramDTO.getAction())) {
                params.add("action=" + paramDTO.getAction());
            }

            if (paramDTO.getQuantity() != null && paramDTO.getQuantity() > 0) {
                params.add("quantity=" + paramDTO.getQuantity());
            }

            if (StringUtils.hasText(paramDTO.getCustType())) {
                params.add("custType=" + paramDTO.getCustType());
            }

            if (ArrangementTypeEnum.SELL.getTypeStr().equals(paramDTO.getAction())) {

                String buyDate = DateHelper.formatDateSilently(paramDTO.getBuyDate());
                if (StringUtils.hasText(buyDate)) {
                    params.add("buyDate=" + buyDate);
                }

                if (paramDTO.getBuyPrice() != null) {
                    params.add("buyPrice=" + paramDTO.getBuyPrice());
                }

                if (paramDTO.getBuyVolume() != null) {
                    params.add("buyVolume=" + paramDTO.getBuyVolume());
                }

                if (paramDTO.getBuyRate() != null) {
                    params.add("buyRate=" + paramDTO.getBuyRate());
                }
            }

            url = url.concat(String.join("&", params));
            log.info("Using url {} to call pricing", url);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class, params);
            log.debug("Getting results {}", response.getBody());

            JSONObject mapData = new JSONObject(response.getBody());

            if (response.getStatusCode().is2xxSuccessful()) {


                JSONArray data = mapData.getJSONArray("data");

                if (data != null) {
                    List<PricingBondResultDTO> result = mapper.readValue(data.toString(),
                            new TypeReference<List<PricingBondResultDTO>>() {
                            });

                    return result;
                }
            } else {
                throw new NeoException(null, ArrangementErrorCode.PRICING_SERVICE_ERROR, mapData.get("message"));
            }
        } catch (Exception e) {
            throw new NeoException(e, ArrangementErrorCode.PRICING_SERVICE_ERROR, e.getMessage());
        }

        return null;
    }
}