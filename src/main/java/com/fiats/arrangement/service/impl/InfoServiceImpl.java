package com.fiats.arrangement.service.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiats.arrangement.constant.ArrangementErrorCode;
import com.fiats.arrangement.service.InfoService;
import com.fiats.tmgcoreutils.payload.BrokerDTO;
import com.fiats.tmgcoreutils.payload.IssuerDTO;
import com.neo.exception.NeoException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class InfoServiceImpl implements InfoService {

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    private ObjectMapper mapper;

    @Value("${fiats.internal.api.info.url}")
    private String infoUrl;

    @Override
    public BrokerDTO retrieveBrokerInfo(Long brokerId) {
        try {
            StringBuilder url = new StringBuilder().append(infoUrl).append("/broker/{brokerId}");

            Map<String, Object> params = new HashMap<>();
            params.put("brokerId", brokerId);

            ResponseEntity<String> response = restTemplate.getForEntity(
                    url.toString(), String.class, params);
//            log.debug("Getting results {}", response.getBody());

            JSONObject mapData = new JSONObject(response.getBody());
            JSONObject data = mapData.getJSONObject("data");

            if (data != null) {
                mapper = mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                BrokerDTO result = mapper.readValue(data.toString(), BrokerDTO.class);

                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public IssuerDTO retrieveIssuerInfo(Long issuerId) {
        try {
            StringBuilder url = new StringBuilder().append(infoUrl).append("/issuer/{issuerId}");

            Map<String, Object> params = new HashMap<>();
            params.put("issuerId", issuerId);

            ResponseEntity<String> response = restTemplate.getForEntity(
                    url.toString(), String.class, params);
//            log.debug("Getting results {}", response.getBody());

            JSONObject mapData = new JSONObject(response.getBody());
            if (response.getStatusCode().is2xxSuccessful()) {

                if (mapData != null) {
                    IssuerDTO result = mapper.readValue(mapData.toString(), IssuerDTO.class);

                    return result;
                }
            } else {
                throw new NeoException(null, ArrangementErrorCode.INFORMATION_SERVICE_ERROR, mapData);
            }
        } catch (Exception e) {
            throw new NeoException(e, ArrangementErrorCode.INFORMATION_SERVICE_ERROR, e.getMessage());
        }

        return null;
    }
}