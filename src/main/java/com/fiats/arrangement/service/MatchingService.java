package com.fiats.arrangement.service;

import com.fiats.tmgcoreutils.event.MatchingBaseEvent;
import com.fiats.tmgcoreutils.event.PaymentBuySuccessEvent;
import com.fiats.tmgcoreutils.event.SignBuyContractEvent;
import com.fiats.tmgcoreutils.payload.OrderPlacementDTO;
import org.springframework.kafka.annotation.KafkaListener;
import reactor.core.publisher.Mono;

public interface MatchingService {

    @KafkaListener(topics = "bond.matching-service.matched",
            groupId = "bond.matching-service.matched-test")
    void handleConfirmedRecord(MatchingBaseEvent event);

    void handleAFCMechanismResultAndSendSignal(OrderPlacementDTO order);
    OrderPlacementDTO handleAFCMechanismResult(OrderPlacementDTO order);

    void handleAFTMechanismResultAndSendSignal(OrderPlacementDTO order);
    OrderPlacementDTO handleAFTMechanismResult(OrderPlacementDTO order);

    // queue tạm để bắn post trước khi sang AFC
    @KafkaListener(topics = "bond.arrangement-core.afc.confirming",
            groupId = "bond.arrangement-core.afc.confirming-test")
    void confirmingAFC(SignBuyContractEvent event);

    // queue tạm để bắn post trước khi sang AFT
    @KafkaListener(topics = "bond.arrangement-core.aft.confirming",
            groupId = "bond.matching-service.aft.confirming-test")
    void confirmingAFT(PaymentBuySuccessEvent event);

    void sendEventToAFCQueue(OrderPlacementDTO order);

    void sendEventToAFTQueue(OrderPlacementDTO order);

}
