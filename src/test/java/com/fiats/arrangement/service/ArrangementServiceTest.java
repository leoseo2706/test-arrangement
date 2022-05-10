package com.fiats.arrangement.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiats.tmgcoreutils.event.MatchingBaseEvent;
import com.fiats.tmgcoreutils.payload.CustomerBrokerWrapper;
import com.fiats.tmgcoreutils.payload.PortfolioResponseMessageDTO;
import com.neo.exception.LoggingUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@Slf4j
public class ArrangementServiceTest {

    @Autowired
    ObjectMapper mapper;

    @Autowired
    MatchingService matchingService;

    @Autowired
    CustomerService customerService;

    @Autowired
    PortfolioService portfolioService;

    @Test
    @DisplayName("Test handle AFC")
    void test_handleAFC() throws JsonProcessingException {
        String json = "{\"mechanism\": \"AFC\",\"orderPlacementDto\": {\"arrangement\":{\"id\":2456}}}";
        MatchingBaseEvent event = mapper.readValue(json, MatchingBaseEvent.class);
        matchingService.handleConfirmedRecord(event);
    }

    @Test
    @DisplayName("Test portfolio PAID")
    void test_PorfolioPaid() throws JsonProcessingException {
        String json = "{\"transactionId\":999,\"arrangementId\":\"2114\",\"vanillaCode\":\"DRC03202301\",\"derivativeCode\":\"Term9M-DRC202103\",\"assetCode\":\"000.Term9M-DRC202103.000002272\",\"status\":\"MATCHED\",\"action\":\"PAID\",\"description\":null}";
        PortfolioResponseMessageDTO event = mapper.readValue(json, PortfolioResponseMessageDTO.class);
        portfolioService.handlePortfolioResponseEventForPaid(event);
    }

    @Test
    @DisplayName("Test portfolio Pay")
    void test_PorfolioPay() throws JsonProcessingException {
        String json = "{\"transactionId\":999,\"arrangementId\":\"2456\",\"vanillaCode\":\"DRC03202301\",\"derivativeCode\":\"Term9M-DRC202103\",\"assetCode\":\"000.Term9M-DRC202103.000002270\",\"status\":\"MATCHED\",\"action\":\"PAY\",\"description\":null}";
        PortfolioResponseMessageDTO event = mapper.readValue(json, PortfolioResponseMessageDTO.class);
        portfolioService.handlePortfolioResponseEventForPay(event);
    }

    @Test
    @DisplayName("Test handle AFT")
    void test_handleAFT() throws JsonProcessingException {
        String json = "{\"mechanism\": \"AFT\",\"orderPlacementDto\": {\"arrangement\":{\"id\":2456}, \"party\" : {\"customer\": null}}}";
        MatchingBaseEvent event = mapper.readValue(json, MatchingBaseEvent.class);
        matchingService.handleConfirmedRecord(event);
    }

    @Test
    @DisplayName("Test portfolio DELIVERY")
    void test_PorfolioDelivery() throws JsonProcessingException {
        String json = "{\"transactionId\":999,\"arrangementId\":\"2114\",\"vanillaCode\":\"DRC03202301\",\"derivativeCode\":\"Term9M-DRC202103\",\"assetCode\":\"000.Term9M-DRC202103.000002272\",\"status\":\"MATCHED\",\"action\":\"DELIVERY\",\"description\":null}";
        PortfolioResponseMessageDTO event = mapper.readValue(json, PortfolioResponseMessageDTO.class);
        portfolioService.handlePortfolioResponseEventForDelivery(event);
    }

    @Test
    @DisplayName("Test callCustomerBroker")
    void test_callCustomerBroker() throws JsonProcessingException {
        List<CustomerBrokerWrapper> cusBrokers = customerService.findCustomerBrokerByIds(new ArrayList<Long>() {{
            add(346L);
        }}).collectList().block();
        log.info(LoggingUtils.objToStringIgnoreEx(cusBrokers));
    }
}
