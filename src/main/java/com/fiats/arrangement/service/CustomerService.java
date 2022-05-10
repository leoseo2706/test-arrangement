package com.fiats.arrangement.service;

import com.fiats.tmgcoreutils.payload.CustomerBrokerWrapper;
import com.fiats.tmgcoreutils.payload.CustomerDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

public interface CustomerService {

    List<CustomerDTO> retrieveCustomerInfo(Collection<Long> customerIds);

    CustomerDTO retrieveCustomerInfo(Long customerId);

    CustomerDTO findByAccountName(String account, boolean fetchSubAccounts);

    Mono<CustomerDTO> findByAccount(String account, boolean fetchSubAccounts);

    Flux<CustomerBrokerWrapper> findCustomerBrokerByIds(Collection<Long> customerIds);

}
