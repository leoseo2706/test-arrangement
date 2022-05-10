package com.fiats.arrangement.service;

import com.fiats.tmgcoreutils.payload.BrokerDTO;
import com.fiats.tmgcoreutils.payload.IssuerDTO;

public interface InfoService {

    BrokerDTO retrieveBrokerInfo(Long brokerId);

    IssuerDTO retrieveIssuerInfo(Long issuerId);

}
