package com.fiats.arrangement.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiats.arrangement.constant.ArrangementErrorCode;
import com.fiats.arrangement.service.ProductDerivativeService;
import com.fiats.exception.NeoFiatsException;
import com.fiats.exception.ValidationException;
import com.fiats.exception.payload.ErrorResponseMessage;
import com.fiats.tmgcoreutils.common.ErrorCode;
import com.fiats.tmgcoreutils.constant.Constant;
import com.fiats.tmgcoreutils.payload.ProdDerivativeDTO;
import com.fiats.tmgcoreutils.payload.ProductIssuerWrapper;
import com.fiats.tmgcoreutils.utils.CommonUtils;
import com.fiats.tmgcoreutils.utils.DateHelper;
import com.fiats.tmgcoreutils.utils.ReactiveClientUtils;
import com.fiats.tmgjpa.payload.ResponseMessage;
import com.neo.exception.NeoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@Slf4j
public class ProductDerivativeServiceImpl implements ProductDerivativeService {

    @Autowired
    @Qualifier("customWebClient")
    WebClient client;

    @Value("${fiats.internal.api.product.url}")
    String productUrl;

    @Autowired
    @Qualifier("customMapper")
    ObjectMapper mapper;

    @Autowired
    @Qualifier(Constant.DEFAULT_THREAD_POOL)
    TaskExecutor executor;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public Flux<ProdDerivativeDTO> findFullInfoByID(Collection<Long> ids) {

        if (CollectionUtils.isEmpty(ids)) {
            return Flux.empty();
        }
        String concatenateID = ids.stream().map(Object::toString).collect(Collectors.joining(Constant.COMMA));
        return ReactiveClientUtils.getForFlux(client, productUrl, "/prod-derivative/full-info?id={id}",
                executor, ProdDerivativeDTO.class, log, concatenateID);
    }

    @Override
    public Mono<ProdDerivativeDTO> findByID(Long id) {

        if (CommonUtils.isInvalidPK(id)) {
            throw new ValidationException(CommonUtils.format("Invalid derivative id {0}", id));
        }

        return client.mutate()
                .baseUrl(productUrl).build()
                .get()
                .uri("/prod-derivative/{id}", id)
                .exchangeToMono(res -> {
                    if (res.statusCode().equals(HttpStatus.OK)) {
                        return res.bodyToMono(ResponseMessage.class)
                                .map(r -> {
                                    if (r.getData() == null) {
                                        return new ProdDerivativeDTO();
                                    }

                                    return CommonUtils.castObjectToDTO(mapper, r.getData(),
                                            ProdDerivativeDTO.class);
                                });
                    } else if (res.statusCode().is4xxClientError()) {

                        if (res.headers().contentType().isPresent()
                                && res.headers().contentType().get().includes(MediaType.APPLICATION_XML)) {
                            // wso2 errors
                            log.error("Got wso error header {}", res.headers().asHttpHeaders());
                            throw new NeoFiatsException(ArrangementErrorCode.WSO2_ERROR,
                                    CommonUtils.format("Received http code {0} wso2 error", res.statusCode().value()));
                        }

                        return res.bodyToMono(ErrorResponseMessage.class)
                                .flatMap(err -> {
                                    throw new ValidationException(err, res.statusCode());
                                });
                    } else if (res.statusCode().is5xxServerError()) {
                        throw new NeoFiatsException("Server error");
                    }

                    log.warn("Other status codes {}", res.statusCode().toString());
                    return Mono.empty(); //for 1xx & 3xx Http codes
                })
                .publishOn(Schedulers.fromExecutor(executor));
    }

    @Override
    public Mono<ProductIssuerWrapper> findDerivativeByOfferingPeriod(
            ProdDerivativeDTO derivativeDTO, Timestamp td) {

        String id = derivativeDTO.getId().toString();
        String tradingDate = DateHelper.formatDateSilently(td);

        return client.mutate()
                .baseUrl(productUrl).build()
                .get()
                .uri("/prod-derivative/offering-period?id={derivativeId}&td={tradingDate}",
                        id, tradingDate)
                .exchangeToMono(res -> {
                    if (res.statusCode().equals(HttpStatus.OK)) {
                        return res.bodyToMono(ResponseMessage.class)
                                .map(r -> {
                                    if (r.getData() == null) {
                                        return new ProductIssuerWrapper();
                                    }

                                    return CommonUtils.castObjectToDTO(mapper, r.getData(),
                                            ProductIssuerWrapper.class);
                                });
                    } else if (res.statusCode().is4xxClientError()) {

                        if (res.headers().contentType().isPresent()
                                && res.headers().contentType().get().includes(MediaType.APPLICATION_XML)) {
                            // wso2 errors
                            log.error("Got wso error header {}", res.headers().asHttpHeaders());
                            throw new NeoFiatsException(ArrangementErrorCode.WSO2_ERROR,
                                    CommonUtils.format("Received http code {0} wso2 error", res.statusCode().value()));
                        }

                        return res.bodyToMono(ErrorResponseMessage.class)
                                .flatMap(err -> {
                                    throw new ValidationException(err, res.statusCode());
                                });
                    } else if (res.statusCode().is5xxServerError()) {
                        throw new NeoFiatsException("Server error");
                    }

                    log.warn("Other status codes {}", res.statusCode().toString());
                    return Mono.empty(); //for 1xx & 3xx Http codes
                })
                .doOnSuccess(piWrapper -> {
                    if (piWrapper == null || piWrapper.getDerivativeDTO() == null
                            || piWrapper.getDerivativeDTO().getProdVanilla() == null
                            || piWrapper.getDerivativeDTO().getProdAgreement() == null) {
                        throw new NeoFiatsException(
                                CommonUtils.format("Empty attributes of product derivative {0}", id));
                    }
                })
                .publishOn(Schedulers.fromExecutor(executor));
    }

    @Override
    public ProdDerivativeDTO findByCode(String code) {
        log.info("Find prodDerivative by code: {}", code);
        StringBuilder url = new StringBuilder(productUrl).append("/prod-derivative/code/{code}");
        UriComponentsBuilder urlBuilder = UriComponentsBuilder.fromUriString(url.toString());
        Map<String, Object> params = new HashMap<>();
        params.put("code", code);

        try {
            log.info("Find prodDerivative by code, url: {}", urlBuilder.buildAndExpand(params).toUriString());
            ProdDerivativeDTO prodDerivativeDTO = restTemplate.getForObject(urlBuilder.buildAndExpand(params).toUriString(), ProdDerivativeDTO.class);

            return prodDerivativeDTO;
        } catch (Exception e) {
            throw new NeoException(e, ErrorCode.SERVER_ERROR, new StringBuilder("Error prodDerivative by code: ").append(code));
        }
    }
}