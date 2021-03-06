package com.fiats.arrangement.service.impl;

import com.fiats.arrangement.constant.ArrangementAction;
import com.fiats.arrangement.constant.ArrangementTypeEnum;
import com.fiats.arrangement.jpa.entity.ArrangementLog;
import com.fiats.arrangement.jpa.repo.ArrangementLogRepo;
import com.fiats.arrangement.payload.ArrangementLogDTO;
import com.fiats.arrangement.service.ArrangementLogService;
import com.fiats.tmgcoreutils.payload.OrderPlacementDTO;
import com.fiats.tmgjpa.entity.RecordStatus;
import com.neo.exception.LoggingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ArrangementLogServiceImpl implements ArrangementLogService {

    @Autowired
    private ArrangementLogRepo arrangementLogRepo;

    @Override
    @Transactional
    public void createArrangementLog(ArrangementLogDTO arrangementLogDTO) {
        log.info("Create arrangement log: {}", LoggingUtils.objToStringIgnoreEx(arrangementLogDTO));

        ArrangementLog arrangementLog = new ArrangementLog();
        BeanUtils.copyProperties(arrangementLogDTO, arrangementLog);
        arrangementLog = arrangementLogRepo.save(arrangementLog);
        log.info("Create arrrangement log success, id: {}", arrangementLog.getId());
    }

    @Override
    public void createArrangementLog(OrderPlacementDTO order, ArrangementAction action) {
        try {
            log.info("Create arrangement log: {} - {} - {}", action.name(), order.getArrangement().getCode(), order.getArrangement().getId());
            String content = buildContentLog(order, action);

            ArrangementLogDTO dto = ArrangementLogDTO.builder()
                    .arrangementId(order.getArrangement().getId())
                    .arrangementCode(order.getArrangement().getCode())
                    .arrangementType(order.getArrangement().getType())
                    .contents(content)
                    .createdDate(new Timestamp(System.currentTimeMillis()))
                    .customerId(order.getParty().getCustomerId())
                    .active(RecordStatus.ACTIVE.getStatus())
                    .logType(action.name())
                    .build();

            createArrangementLog(dto);
        } catch (Exception e) {
            log.info("Error create arrangement log: {}", e.getMessage());
            log.error("Error: {}", e);
        }
    }

    @Override
    /*
    ArrangementAction: action t????ng ???ng t???ng step khi ?????t l???nh
     */
    public String buildContentLog(OrderPlacementDTO dto, ArrangementAction action) {
        StringBuilder contents = new StringBuilder();
        switch (action) {
            case BROKER: //Cho tr?????ng h???p gi???i thi???u l???nh
                contents.append("M??i gi???i ")
                        .append(dto.getPurchaserAccount())
                        .append(" th???c hi???n gi???i thi???u l???nh ")
                        .append(dto.getArrangement().getType().equals(1) ? "mua" : "b??n")
                        .append(".");
                break;
            case CUSTOMER_CONFIRM:
                contents.append("Kh??ch h??ng ")
                        .append(dto.getParty().getAccount())
                        .append(" ???? th???c hi???n x??c nh???n l???nh.");
                break;
            case PLACE_ORDER:
                contents.append("Kh??ch h??ng ")
                        .append(dto.getParty().getAccount())
                        .append(" ???? th???c hi???n ?????t l???nh ")
                        .append(dto.getArrangement().getType().equals(1) ? "mua" : "b??n")
                        .append(" tr??i phi???u.");
                break;
            case SIGN_CONTRACT:
                contents.append("Kh??ch h??ng ")
                        .append(dto.getParty().getAccount())
                        .append(" ???? th???c hi???n k?? h???p ?????ng th??nh c??ng.");
                break;
            case CONFIRM:
                contents.append("H??? th???ng ???? x??c nh???n h???n m???c th??nh c??ng.");
                break;
            case PAYMENT:
                contents.append("User ")
                        .append(dto.getPurchaserAccount()) // ticked user actually
                .append(" ???? th???c hi???n x??c nh???n thanh to??n.");
                ;
                break;
            case DELIVERY:
                contents.append("H??? th???ng ???? th???c hi???n chuy???n nh?????ng th??nh c??ng.");
                break;
            case HOLD:
                contents.append("H??? th???ng ???? th???c hi???n hold ")
                .append(dto.getArrangement().getVolume())
                .append(" tr??i phi???u th??nh c??ng.");
                break;
            case CANCEL:
                contents.append("Kh??ch h??ng ")
                .append(dto.getParty().getAccount())
                .append(" ???? th???c hi???n h???y l???nh th??nh c??ng.");
                break;
            case UNHOLD:
                contents.append("H??? th???ng ???? th???c hi???n unhold ")
                        .append(dto.getArrangement().getVolume())
                        .append(" tr??i phi???u th??nh c??ng.");
                break;
            case EXPIRED:
                contents.append("H??? th???ng ???? th???c hi???n expired th??nh c??ng.");
                break;
            case UNCONFIRM:
                contents.append("H??? th???ng ???? th???c hi???n unconfirm th??nh c??ng.");
                break;
            case CANCEL_AFC:
                contents.append("H??? th???ng ???? g???i t??n hi???u hu??? b??? x??c nh???n l???nh th??nh c??ng.");
                break;
        }
        return contents.toString();
    }

    @Override
    public List<ArrangementLogDTO> findAllLogByArrangementId(Long arrangementId) {
        log.info("Find all arrangement log by arrangementId: {}", arrangementId);
        Optional<List<ArrangementLog>> arrangementLogs = arrangementLogRepo.findAllByArrangementIdOrderByCreatedDateDesc(arrangementId);
        if(arrangementLogs.isPresent()) {
            List<ArrangementLogDTO> logDTOS = new ArrayList<>();
            arrangementLogs.get().forEach(a -> {
                ArrangementLogDTO dto = new ArrangementLogDTO();
                BeanUtils.copyProperties(a, dto);
                logDTOS.add(dto);
            });
            log.debug("Return arrangement log: {}", logDTOS);
            return logDTOS;
        }
        return null;
    }

    @Override
    public List<ArrangementLogDTO> findAllLogByArrangementCode(String arrangementCode) {
        log.info("Find all arrangement log by arrangementCode: {}", arrangementCode);
        Optional<List<ArrangementLog>> arrangementLogs = arrangementLogRepo.findAllByArrangementCodeOrderByCreatedDateDesc(arrangementCode);
        if(arrangementLogs.isPresent()) {
            List<ArrangementLogDTO> logDTOS = new ArrayList<>();
            arrangementLogs.get().forEach(a -> {
                ArrangementLogDTO dto = new ArrangementLogDTO();
                BeanUtils.copyProperties(a, dto);
                logDTOS.add(dto);
            });
            log.debug("Return arrangement log: {}", logDTOS);
            return logDTOS;
        }
        return null;
    }
}
