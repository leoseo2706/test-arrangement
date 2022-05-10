package com.fiats.arrangement.service;

import com.fiats.arrangement.jpa.entity.Arrangement;
import com.fiats.arrangement.jpa.entity.ArrangementOperation;
import com.fiats.arrangement.payload.TemplateFileDTO;
import com.fiats.tmgcoreutils.jwt.JWTHelper;
import com.fiats.tmgcoreutils.payload.ContTemplateDocVersionDTO;
import com.fiats.tmgcoreutils.payload.CustomerBrokerWrapper;
import com.fiats.tmgcoreutils.payload.OrderPlacementDTO;
import com.fiats.tmgcoreutils.payload.SnapshotDTO;
import reactor.core.publisher.Flux;

import java.util.Map;

public interface SnapshotService {

    void snapshotContract(CustomerBrokerWrapper cusBroker, Arrangement arrangement,
                          ArrangementOperation operation, OrderPlacementDTO dto,
                          JWTHelper jwtHelper);

    Flux<ContTemplateDocVersionDTO> callContractGenerator(SnapshotDTO dto, String userName);

    Flux<TemplateFileDTO> listAvailableFiles(Long arrangementId, String userName);

    Map<String, Object> buildSnapshotModel(CustomerBrokerWrapper cusBroker,
                                           OrderPlacementDTO dto);
}
