package com.fiats.arrangement.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiats.arrangement.constant.ArrConstant;
import com.fiats.arrangement.constant.ArrangementErrorCode;
import com.fiats.arrangement.service.CustomerService;
import com.fiats.arrangement.service.MatchingService;
import com.fiats.exception.NeoFiatsException;
import com.fiats.exception.ValidationException;
import com.fiats.exception.payload.ErrorResponseMessage;
import com.fiats.tmgcoreutils.constant.Constant;
import com.fiats.tmgcoreutils.payload.ContTemplateDocVersionDTO;
import com.fiats.tmgcoreutils.payload.CustomerBrokerWrapper;
import com.fiats.tmgcoreutils.payload.CustomerDTO;
import com.fiats.tmgcoreutils.utils.CommonUtils;
import com.fiats.tmgcoreutils.utils.ReactiveClientUtils;
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
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    private ObjectMapper mapper;

    @Value("${fiats.internal.api.crm.url}")
    private String crmUrl;

    @Autowired
    @Qualifier("customWebClient")
    WebClient webClient;

    @Autowired
    @Qualifier(Constant.DEFAULT_THREAD_POOL)
    TaskExecutor executor;

    @Autowired
    @Qualifier("customMapper")
    ObjectMapper customMapper;

    @Override
    public List<CustomerDTO> retrieveCustomerInfo(Collection<Long> customerIds) {


        if (CollectionUtils.isEmpty(customerIds)) {
            return null;
        }

        try {
            StringBuilder url = new StringBuilder().append(crmUrl).append("/customer?id={customerIds}");

            Map<String, Object> params = new HashMap<>();
            params.put("customerIds", customerIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")));

            ResponseEntity<String> response = restTemplate.getForEntity(
                    url.toString(), String.class, params);
//            log.debug("Getting results {}", response.getBody());

            JSONObject mapData = new JSONObject(response.getBody());
            JSONArray data = mapData.getJSONArray("data");

            if (data != null) {
                mapper = mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                List<CustomerDTO> result = mapper.readValue(data.toString(), new TypeReference<List<CustomerDTO>>() {
                });

                return result;
            }
        } catch (Exception e) {
            log.error("Error while finding customer info {} with err {}", customerIds, e.getMessage());
            log.error("Stack trace: ", e);
        }

        return null;
    }

    @Override
    public CustomerDTO retrieveCustomerInfo(Long customerId) {

        List<CustomerDTO> customerDTOS = retrieveCustomerInfo(new ArrayList<Long>() {{
            add(customerId);
        }});

        if (CollectionUtils.isEmpty(customerDTOS)) {
            throw new NeoFiatsException(ArrangementErrorCode.CUSTOMER_NOT_FOUND,
                    CommonUtils.format("Customer ID {0} is N/A", customerId));
        }

        return customerDTOS.get(0);
    }

    @Override
    public CustomerDTO findByAccountName(String account, boolean fetchSubAccounts) {
        log.info("Finding customer by account {}", account);
        if (!StringUtils.hasText(account)) {
            return null;
        }

        StringBuilder uri = new StringBuilder(crmUrl).append("/customer/account/").append(account);
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("f", fetchSubAccounts ? "1" : "0");
        log.info("Request to uri: {}", uri.toString());
        CustomerDTO customerDTO = restTemplate.getForObject(uri.toString(), CustomerDTO.class, queryParams, account);

        return customerDTO;
    }

    @Override
    public Mono<CustomerDTO> findByAccount(String account, boolean fetchSubAccounts) {

        log.info("Finding customer by account {}", account);

        if (!StringUtils.hasText(account)) {
            throw new ValidationException("Empty customer account");
        }

        String uri = "/customer/account/{account}";
        uri = fetchSubAccounts ? uri.concat("?f=1") : uri; // fetch all bank, stock, vsd accounts
        log.info("Using uri {} for fetchSubAccounts {}", uri, fetchSubAccounts);

        return ReactiveClientUtils.getForMono(webClient, crmUrl, uri,
                executor, CustomerDTO.class, log, account);
    }

    @Override
    public Flux<CustomerBrokerWrapper> findCustomerBrokerByIds(Collection<Long> customerIds) {


        if (CollectionUtils.isEmpty(customerIds)) {
            throw new ValidationException(ArrangementErrorCode.CUSTOMER_NOT_FOUND,
                    "Empty customer ids to search!");
        }

        String ids = customerIds.stream().map(Object::toString)
                .collect(Collectors.joining(Constant.COMMA));

        return ReactiveClientUtils.getForFlux(webClient, crmUrl,
                "/customer/customer-broker?cid={ids}", executor,
                CustomerBrokerWrapper.class, log, ids);
    }
}