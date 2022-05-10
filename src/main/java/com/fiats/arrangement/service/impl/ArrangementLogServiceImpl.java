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
    ArrangementAction: action tương ứng từng step khi đặt lệnh
     */
    public String buildContentLog(OrderPlacementDTO dto, ArrangementAction action) {
        StringBuilder contents = new StringBuilder();
        switch (action) {
            case BROKER: //Cho trường hợp giới thiệu lệnh
                contents.append("Môi giới ")
                        .append(dto.getPurchaserAccount())
                        .append(" thực hiện giới thiệu lệnh ")
                        .append(dto.getArrangement().getType().equals(1) ? "mua" : "bán")
                        .append(".");
                break;
            case CUSTOMER_CONFIRM:
                contents.append("Khách hàng ")
                        .append(dto.getParty().getAccount())
                        .append(" đã thực hiện xác nhận lệnh.");
                break;
            case PLACE_ORDER:
                contents.append("Khách hàng ")
                        .append(dto.getParty().getAccount())
                        .append(" đã thực hiện đặt lệnh ")
                        .append(dto.getArrangement().getType().equals(1) ? "mua" : "bán")
                        .append(" trái phiếu.");
                break;
            case SIGN_CONTRACT:
                contents.append("Khách hàng ")
                        .append(dto.getParty().getAccount())
                        .append(" đã thực hiện ký hợp đồng thành công.");
                break;
            case CONFIRM:
                contents.append("Hệ thống đã xác nhận hạn mức thành công.");
                break;
            case PAYMENT:
                contents.append("User ")
                        .append(dto.getPurchaserAccount()) // ticked user actually
                .append(" đã thực hiện xác nhận thanh toán.");
                ;
                break;
            case DELIVERY:
                contents.append("Hệ thống đã thực hiện chuyển nhượng thành công.");
                break;
            case HOLD:
                contents.append("Hệ thống đã thực hiện hold ")
                .append(dto.getArrangement().getVolume())
                .append(" trái phiếu thành công.");
                break;
            case CANCEL:
                contents.append("Khách hàng ")
                .append(dto.getParty().getAccount())
                .append(" đã thực hiện hủy lệnh thành công.");
                break;
            case UNHOLD:
                contents.append("Hệ thống đã thực hiện unhold ")
                        .append(dto.getArrangement().getVolume())
                        .append(" trái phiếu thành công.");
                break;
            case EXPIRED:
                contents.append("Hệ thống đã thực hiện expired thành công.");
                break;
            case UNCONFIRM:
                contents.append("Hệ thống đã thực hiện unconfirm thành công.");
                break;
            case CANCEL_AFC:
                contents.append("Hệ thống đã gửi tín hiệu huỷ bỏ xác nhận lệnh thành công.");
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
