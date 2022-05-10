package com.fiats.arrangement.service.impl;

import com.fiats.arrangement.service.HolidayService;
import com.fiats.tmgcoreutils.common.ErrorCode;
import com.fiats.tmgcoreutils.utils.DateHelper;
import com.fiats.tmgjpa.payload.ResponseMessage;
import com.neo.exception.NeoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class HolidayServiceImpl implements HolidayService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${fiats.internal.api.info.url}")
    private String infoUrl;

    @Override
    public String getListWorkingDays(Date fromDate, Date toDate) {
        log.info("Get list holiday: fromDate {} - toDate {}", fromDate, toDate);
        StringBuilder builder = new StringBuilder(infoUrl).append("/holiday/workingdays");
        UriComponentsBuilder urlBuilder = UriComponentsBuilder.fromUriString(builder.toString())
                .queryParam("fromDate", DateHelper.formatDateSilently(fromDate))
                .queryParam("toDate", DateHelper.formatDateSilently(toDate));

        try {
            log.info("URL: {}", urlBuilder.toUriString());

            ResponseMessage<List<String>> responseMessage = restTemplate.getForObject(urlBuilder.toUriString(), ResponseMessage.class);

            log.info("Reponse: {}", responseMessage.getData());

            String data = "";
            if(Optional.ofNullable(responseMessage.getData()).isPresent()) {
                data = String.join(";", responseMessage.getData());
            }

            return data;
        } catch (Exception e) {
            throw new NeoException(e, ErrorCode.SERVER_ERROR, new StringBuilder("Error call get list holiday"));
        }
    }
}
