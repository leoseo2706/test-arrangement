package com.fiats.arrangement.service;

import com.fiats.arrangement.payload.TemplateFileDTO;

import java.util.List;

public interface ContractService {

    List<TemplateFileDTO> getContractsBrokerSupport(String customerAccount, Long arrangementId);

    byte[] downloadContractBrokerSupport(String customerAccount, Long arrangementId, String contractName);
}
