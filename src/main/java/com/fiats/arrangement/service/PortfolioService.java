package com.fiats.arrangement.service;

import com.fiats.tmgcoreutils.constant.PortfolioRetailAction;
import com.fiats.tmgcoreutils.event.PortfolioWrapperEvent;
import com.fiats.tmgcoreutils.payload.*;
import org.springframework.kafka.annotation.KafkaListener;

import java.util.List;

public interface PortfolioService {

    List<PortViewArrangementDTO> getPortfolioByAccountAndAsset(String customerCode, String account, String assetCode);

    PortTransactionDTO makePortfolioTransaction(PortTransactionDTO dto);

    List<PortViewDerivativeDTO> getPortfolioByDerivativeCode(String derivativeCode);

    OrderPlacementDTO getLatestAfc(String derivativeCode);

    OrderPlacementDTO getLatestAft(String vanillaCode);

    void sendEventToPortfolioQueue(OrderPlacementDTO order, PortfolioRetailAction action);

    // queue tam de ban sang portfolio
    @KafkaListener(topics = "bond.arrangement-core.portfolio.confirming.action",
            groupId = "bond.arrangement-core.portfolio.confirming.action")
    void handlePortfolioEvent(PortfolioWrapperEvent event);

    @KafkaListener(topics = "bond.portfolio.normal.buy.pay.transaction.response",
            groupId = "bond.portfolio.normal.pay.transaction.response")
    void handlePortfolioResponseEventForPay(PortfolioResponseMessageDTO event);

    @KafkaListener(topics = "bond.portfolio.normal.sell.hold.transaction.response",
            groupId = "bond.portfolio.normal.hold.transaction.response")
    void handlePortfolioResponseEventForHold(PortfolioResponseMessageDTO event);

    @KafkaListener(topics = "bond.portfolio.normal.sell.paid.transaction.response",
            groupId = "bond.portfolio.normal.paid.transaction.response")
    void handlePortfolioResponseEventForPaid(PortfolioResponseMessageDTO event);

    @KafkaListener(topics = "bond.portfolio.normal.sell.delivery.transaction.response",
            groupId = "bond.portfolio.normal.delivery.transaction.response")
    void handlePortfolioResponseEventForDelivery(PortfolioResponseMessageDTO event);

}
