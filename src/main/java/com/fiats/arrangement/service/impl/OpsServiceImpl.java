package com.fiats.arrangement.service.impl;

import com.fiats.arrangement.constant.ArrangementAction;
import com.fiats.arrangement.jpa.entity.Arrangement;
import com.fiats.arrangement.jpa.repo.ArrangementRepo;
import com.fiats.arrangement.payload.filter.BackSupportFilter;
import com.fiats.arrangement.service.*;
import com.fiats.arrangement.utils.ConverterUtils;
import com.fiats.tmgcoreutils.constant.Constant;
import com.fiats.tmgcoreutils.payload.*;
import com.fiats.tmgcoreutils.utils.DateHelper;
import com.fiats.tmgjpa.paging.PagingFilterBase;
import com.neo.exception.LoggingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.ValidationException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class OpsServiceImpl implements OpsService {

    @Autowired
    private BackSupportService backSupportService;

    @Autowired
    private ArrangementRepo arrangementRepo;

    @Autowired
    private NotiService notiService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    ArrangementService arrangementService;

    @Autowired
    ArrangementLogService arrangementLogService;

    @Override
    public void expire(PagingFilterBase<BackSupportFilter> pf) {
        log.info("Find arrangement: {}", pf);
        List<ArrangementInfoDTO> dtoList = backSupportService.findArrangements(pf);
        if (dtoList != null && dtoList.size() > 0) {
            updateExpired(dtoList);
        } else {
            log.info("No job for expire");
        }
    }

    //Update database
    public void updateExpired(List<ArrangementInfoDTO> infoDTOS) {
        log.info("infoDTOS: {}", LoggingUtils.objToStringIgnoreEx(infoDTOS));
        infoDTOS.stream().forEach(a -> {
            try {
                OrderPlacementDTO originalModel = arrangementService.cancelArrangementAndSendSignal(a.getArrangementId(),
                        null, Constant.ACTIVE);
                log.info("Send request to expired, arrangementCode: {}", a.getArrangementCode());
                arrangementLogService.createArrangementLog(originalModel, ArrangementAction.EXPIRED);

                log.info("Update arrangement {} - {} to expired success!", a.getArrangementCode(),
                        a.getArrangementId());
                //Send noti of auto expired separately
                sendNotification(a);
            } catch (ValidationException e) {
                log.error("This record is not valid for cancellation {}", a.getArrangementId());
                log.error(e.getMessage(), e);
            }
        });
    }


    //Send notification
    public void sendNotification(ArrangementInfoDTO arrangementInfoDTO) {
        String emailTemplate = "";
        String smsTemplate = "";
        if (!arrangementInfoDTO.getCustomerStatus()) { //Chua ky hop dong
            emailTemplate = "EB08";
            smsTemplate = "SB08";
        } else if (arrangementInfoDTO.getCustomerStatus() && arrangementInfoDTO.getStatus() == 0) { //Chua xac nhan han muc
            emailTemplate = "EB07";
            smsTemplate = "SB07";
        } else if (arrangementInfoDTO.getCustomerStatus() && !arrangementInfoDTO.getPaymentStatus() && arrangementInfoDTO.getStatus() == 1) { //Chua thanh toan
            emailTemplate = "EB06";
            smsTemplate = "SB06";
        }

        CustomerDTO customerDTO = customerService.findByAccountName(arrangementInfoDTO.getPartyAccount(), Constant.INACTIVE);
        if (customerDTO != null) {
            OrderPlacementDTO orderPlacementDTO = toOrder(arrangementInfoDTO);

            Map<String, Object> params = new HashMap<>();
            params.put("customerDTO", customerDTO);
            params.put("orderPlacementDTO", orderPlacementDTO);

            if (!emailTemplate.equals("")) {
                EmailTemplateDto emailTemplateDto = EmailTemplateDto.builder()
                        .arrangementId(arrangementInfoDTO.getArrangementId())
                        .to(customerDTO.getEmail())
                        .cc("")
                        .bcc("")
                        .templateCode(emailTemplate)
                        .params(params)
                        .build();
                notiService.sendEmail(emailTemplateDto);
            }
            if (!smsTemplate.equals("")) {
                SmsTemplateDto smsTemplateDto = SmsTemplateDto.builder()
                        .arrangementId(arrangementInfoDTO.getArrangementId())
                        .destination(customerDTO.getPhone())
                        .templateCode(smsTemplate)
                        .params(params)
                        .build();
                notiService.sendSms(smsTemplateDto);
            }
        } else {
            log.info("Can't send notification because customer null!!!");
        }


    }

    private OrderPlacementDTO toOrder(ArrangementInfoDTO arrangementInfoDTO) {
        return OrderPlacementDTO.builder()
                .arrangement(ArrangementDTO.builder()
                        .code(arrangementInfoDTO.getArrangementCode())
                        .tradingDate(arrangementInfoDTO.getTradingDate())
                        .volume(arrangementInfoDTO.getVolume().intValue())
                        .build())
                .pricing(ArrangementPricingDTO.builder()
                        .totalInvestAmount(arrangementInfoDTO.getPrincipal())
                        .fee(arrangementInfoDTO.getTransactionFee())
                        .unitPrice(arrangementInfoDTO.getUnitPrice())
                        .build())
                .derivative(ProdDerivativeDTO.builder()
                        .code(arrangementInfoDTO.getDerivativeCode())
                        .build())
                .build();
    }
}
