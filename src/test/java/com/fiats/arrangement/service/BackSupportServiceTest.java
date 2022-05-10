package com.fiats.arrangement.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiats.arrangement.jpa.entity.Arrangement;
import com.fiats.arrangement.jpa.entity.ArrangementOperation;
import com.fiats.arrangement.jpa.repo.ArrangementOperationRepo;
import com.fiats.arrangement.jpa.repo.ArrangementRepo;
import com.fiats.arrangement.payload.filter.BackSupportFilter;
import com.fiats.arrangement.payload.filter.ReportingFilter;
import com.fiats.tmgcoreutils.constant.MechanismType;
import com.fiats.tmgcoreutils.event.MatchingBaseEvent;
import com.fiats.tmgcoreutils.payload.ArrangementDTO;
import com.fiats.tmgcoreutils.payload.ContGroupDocDTO;
import com.fiats.tmgcoreutils.payload.OrderPlacementDTO;
import com.fiats.tmgcoreutils.utils.CommonUtils;
import com.fiats.tmgcoreutils.utils.DateHelper;
import com.fiats.tmgjpa.paging.PageRequestDTO;
import com.fiats.tmgjpa.paging.PagingFilterBase;
import com.neo.exception.LoggingUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
@Slf4j
public class BackSupportServiceTest {

    @Autowired
    BackSupportService service;

    @Autowired
    ReportingService reportingService;

    @Test
    void test_BackSupportPaging() {
//        BackSupportFilter filter = BackSupportFilter.builder()
////                .tradingDateFrom(fromDate != null ? Timestamp.valueOf(fromDate.atStartOfDay()) : null)
////                .tradingDateTo(toDate != null ? Timestamp.valueOf(toDate.atStartOfDay()) : null)
////                .partyId(partyId)
////                .counterId(counterId)
////                .arrangementCode(arrangementCode)
//                .arrangementType(Arrays.asList(1, 2))
////                .listedStatus(listedStatus)
////                .exception(exception)
////                .derivativeId(derivativeId)
////                .status(arrangementStatus)
////                .operationStatus(operationStatusMap)
//                .build();
//
//        PageRequestDTO paging = PageRequestDTO.builder().pageNum(1).pageSize(10).build();
//
//        service.retrieveArrangements(new PagingFilterBase<>(filter, paging));
    }

    @Test
    void test_RerportSummary() {
//        ReportingFilter filter = ReportingFilter.builder()
//                .tradingDate(DateHelper.parseTimestamp("2021-04-29"))
////                .arrangementCode(arrangementCode)
//                .arrangementType(1)
////                .derivativeId(derivativeId)
////                .customerId(customerId)
////                .brokerId(brokerId)
////                .agencyId(agencyId)
////                .arrangementStatus(arrangementStatus)
//                .build();
//
//        PageRequestDTO paging = PageRequestDTO.builder().pageNum(1).pageSize(10).build();
//
//        reportingService.retrieveSummaryArrangement(new PagingFilterBase<>(filter, paging));
    }
}
