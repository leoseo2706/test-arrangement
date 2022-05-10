package com.fiats.arrangement.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiats.arrangement.constant.ArrangementErrorCode;
import com.fiats.arrangement.service.BrokerService;
import com.fiats.exception.NeoFiatsException;
import com.fiats.exception.ValidationException;
import com.fiats.exception.payload.ErrorResponseMessage;
import com.fiats.tmgcoreutils.constant.Constant;
import com.fiats.tmgcoreutils.payload.BrokerDTO;
import com.fiats.tmgcoreutils.payload.PricingBondResultDTO;
import com.fiats.tmgcoreutils.utils.CommonUtils;
import com.neo.exception.NeoException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BrokerServiceImpl implements BrokerService {

    @Value("${fiats.internal.api.info.url}")
    private String infoUrl;

    @Autowired
    @Qualifier("customWebClient")
    WebClient webClient;

    @Autowired
    @Qualifier(Constant.DEFAULT_THREAD_POOL)
    TaskExecutor executor;

    @Autowired
    @Qualifier("customMapper")
    ObjectMapper customMapper;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Override
    public Flux<BrokerDTO> findBrokerDTOByCustomerID(Collection<Long> customerIds, Boolean active) {

        log.info("Finding broker by {}", customerIds);

        if (CollectionUtils.isEmpty(customerIds)) {
            return Flux.empty();
        }

        String ids = customerIds.stream()
                .map(Object::toString)
                .collect(Collectors.joining(Constant.COMMA));

        return webClient.mutate()
                .baseUrl(infoUrl).build()
                .get()
                .uri("/broker/customer/id?cid={customerIds}&active={active}", ids, active)
                .exchangeToFlux(res -> {

                    if (res.statusCode().equals(HttpStatus.OK)) {
                        return res.bodyToFlux(BrokerDTO.class);
                    } else if (res.statusCode().is4xxClientError()) {
                        log.error("Receiving error {} {}", res.statusCode().value(),
                                res.bodyToMono(String.class));

                        if (res.headers().contentType().isPresent()
                                && res.headers().contentType().get().includes(MediaType.APPLICATION_XML)) {
                            // wso2 errors
                            log.error("Got wso error header {}", res.headers().asHttpHeaders());
                            throw new NeoFiatsException(ArrangementErrorCode.WSO2_ERROR,
                                    CommonUtils.format("Received http code {0} wso2 error", res.statusCode().value()));
                        }

                        // normal invalid errors from services
                        return res.bodyToMono(ErrorResponseMessage.class)
                                .flatMapMany(err -> {
                                    throw new ValidationException(err, res.statusCode());
                                });

                    } else if (res.statusCode().is5xxServerError()) {
                        throw new NeoFiatsException("Server error");
                    }

                    log.warn("Other status codes {}", res.statusCode().toString());
                    return Flux.empty(); //for 1xx & 3xx Http codes
                })
                .publishOn(Schedulers.fromExecutor(executor));
    }

    @Override
    public BrokerDTO getBrokerById(Long brokerId) {
        try {
            StringBuilder url = new StringBuilder().append(infoUrl).append("/broker/{brokerId}");

            Map<String, Object> params = new HashMap<>();
            params.put("brokerId", brokerId);

            ResponseEntity<String> response = restTemplate.getForEntity(
                    url.toString(), String.class, params);

            JSONObject mapData = new JSONObject(response.getBody());
            log.info("Get broker data result: {}", mapData);

            if (response.getStatusCode().is2xxSuccessful()) {
                if (mapData.has("data")) {
                    JSONObject data = mapData.getJSONObject("data");

                    BrokerDTO result = objectMapper.readValue(data.toString(), BrokerDTO.class);

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