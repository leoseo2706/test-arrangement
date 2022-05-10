package com.fiats.arrangement.service;

import com.fiats.tmgcoreutils.payload.BrokerDTO;
import reactor.core.publisher.Flux;

import java.util.Collection;

public interface BrokerService {

    Flux<BrokerDTO> findBrokerDTOByCustomerID(Collection<Long> customerIds, Boolean active);

    BrokerDTO getBrokerById(Long brokerId);
}
