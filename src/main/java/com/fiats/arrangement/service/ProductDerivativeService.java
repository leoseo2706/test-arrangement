package com.fiats.arrangement.service;

import com.fiats.tmgcoreutils.payload.ProdDerivativeDTO;
import com.fiats.tmgcoreutils.payload.ProductIssuerWrapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.sql.Timestamp;
import java.util.Collection;

public interface ProductDerivativeService {

    Mono<ProdDerivativeDTO> findByID(Long id);
    Flux<ProdDerivativeDTO> findFullInfoByID(Collection<Long> ids);
    Mono<ProductIssuerWrapper> findDerivativeByOfferingPeriod(ProdDerivativeDTO dto, Timestamp td);
    ProdDerivativeDTO findByCode(String code);

}
