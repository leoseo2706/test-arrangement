package com.fiats.arrangement.service;

import com.fiats.arrangement.constant.ArrangementAction;
import com.fiats.arrangement.constant.ArrangementLogType;
import com.fiats.arrangement.payload.ArrangementLogDTO;
import com.fiats.tmgcoreutils.payload.OrderPlacementDTO;

import java.util.List;

public interface ArrangementLogService {
    void createArrangementLog(ArrangementLogDTO arrangementLogDTO);

    void createArrangementLog(OrderPlacementDTO order, ArrangementAction action);

    String buildContentLog(OrderPlacementDTO dto, ArrangementAction action);

    List<ArrangementLogDTO> findAllLogByArrangementId(Long arrangementId);

    List<ArrangementLogDTO> findAllLogByArrangementCode(String arrangementCode);
}
