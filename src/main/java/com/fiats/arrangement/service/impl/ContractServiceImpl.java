package com.fiats.arrangement.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiats.arrangement.constant.ArrangementErrorCode;
import com.fiats.arrangement.payload.TemplateFileDTO;
import com.fiats.arrangement.service.ContractService;
import com.fiats.tmgcoreutils.payload.BrokerDTO;
import com.fiats.tmgcoreutils.payload.PortViewArrangementDTO;
import com.neo.exception.NeoException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ContractServiceImpl implements ContractService {

    @Value("${fiats.internal.api.contract.url}")
    private String contractUrl;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    private ObjectMapper mapper;

    @Override
    public List<TemplateFileDTO> getContractsBrokerSupport(String customerAccount, Long arrangementId) {
        try {
            StringBuilder url = new StringBuilder().append(contractUrl).append("/internal/snapshot/list/{arrangementId}?u={customerAccount}");

            Map<String, Object> params = new HashMap<>();
            params.put("customerAccount", customerAccount);
            params.put("arrangementId", arrangementId);

            ResponseEntity<String> response = restTemplate.getForEntity(
                    url.toString(), String.class, params);
//            log.debug("Getting results {}", response.getBody());

            if (response.getStatusCode().is2xxSuccessful()) {
                JSONArray mapData = new JSONArray(response.getBody());

                if (mapData != null) {
                    mapper = mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                    List<TemplateFileDTO> data = mapper.readValue(mapData.toString(), new TypeReference<List<TemplateFileDTO>>() {
                    });

                    return data;
                }
            } else {
                throw new NeoException(null, ArrangementErrorCode.CONTRACT_SERVICE_ERROR, response.getBody());
            }
        } catch (Exception e) {
            throw new NeoException(e, ArrangementErrorCode.CONTRACT_SERVICE_ERROR, e.getMessage());
        }

        return null;
    }

    @Override
    public byte[] downloadContractBrokerSupport(String customerAccount, Long arrangementId, String contractName) {
        try {
            StringBuilder url = new StringBuilder().append(contractUrl).append("/internal/snapshot/download/{fileName}?aid={arrangementId}&u={customerAccount}");

            ByteArrayHttpMessageConverter byteArrayHttpMessageConverter = new ByteArrayHttpMessageConverter();

            List<MediaType> supportedApplicationTypes = new ArrayList<>();
            MediaType pdfApplication = new MediaType("application", "pdf");
            supportedApplicationTypes.add(pdfApplication);

            byteArrayHttpMessageConverter.setSupportedMediaTypes(supportedApplicationTypes);
            List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
            messageConverters.add(byteArrayHttpMessageConverter);
            restTemplate = new RestTemplate();
            restTemplate.setMessageConverters(messageConverters);

            Map<String, Object> params = new HashMap<>();
            params.put("fileName", contractName);
            params.put("arrangementId", arrangementId);
            params.put("customerAccount", customerAccount);

            byte[] result = restTemplate.getForObject(url.toString(), byte[].class, params);
            return result;
        } catch (Exception e) {
            throw new NeoException(e, ArrangementErrorCode.CONTRACT_SERVICE_ERROR, e.getMessage());
        }
    }
}