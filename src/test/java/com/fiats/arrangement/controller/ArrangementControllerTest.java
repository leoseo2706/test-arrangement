//package com.fiats.arrangement.controller;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fiats.arrangement.constant.ArrangementTypeEnum;
//import com.fiats.arrangement.service.BackSupportService;
//import com.fiats.tmgcoreutils.payload.*;
//import com.fiats.tmgcoreutils.utils.DateHelper;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.web.client.TestRestTemplate;
//import org.springframework.boot.web.server.LocalServerPort;
//import reactor.core.publisher.Mono;
//
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@Slf4j
//public class ArrangementControllerTest {
//
//    // bind the above RANDOM_PORT
//    @LocalServerPort
//    private int port;
//
//    @Autowired
//    private TestRestTemplate restTemplate;
//
//    @Value("#{servletContext.contextPath}")
//    String contextPath;
//
//    @Autowired
//    @Qualifier("customMapper")
//    ObjectMapper mapper;
//
//    @Autowired
//    BackSupportService backSupportService;
//
//    @Test
//    public void test_listArrangement() throws Exception {
//
////        String historyUrl = "http://localhost:" + port + contextPath + "/order/history"
////                .concat("?cid=101&sd=2021-02-25&ed=2021-03-02&t=1&s=1");
////
////        ResponseMessage response = restTemplate.getForEntity(
////                new URL(historyUrl).toString(),
////                ResponseMessage.class).getBody();
////        log.info("Getting results {}", response);
////
////        List<OrderPlacementDTO> records = mapper.readValue(
////                mapper.writeValueAsString(response.getData()),
////                new TypeReference<List<OrderPlacementDTO>>(){});
////        log.info("Got a list of records {} ", records);
////
////        Assertions.assertNotNull(records);
//    }
//
//    @Test
//    public void test_placeOrder() throws Exception {
//
////        OrderPlacementDTO order = buildDataTest();
////
////        ResponseMessage response = restTemplate.postForEntity(
////                new URL("http://125.212.239.152:" + port + contextPath + "/order/place").toString(),
////                order, ResponseMessage.class).getBody();
////        log.info("Getting results {}", response);
////
////        order = mapper.readValue(mapper.writeValueAsString(response.getData()), OrderPlacementDTO.class);
////        Assertions.assertNotNull(order);
//    }
//
//    @Test
//    public void test() throws JsonProcessingException {
////        log.info("test# {}", mapper.writeValueAsString(buildDataTest()));
//
//
////        List<ProdDerivativeDTO> list = new ArrayList<>();
////        list.add(ProdDerivativeDTO.builder().id(1L).build());
////
////        String test = mapper.writeValueAsString(list);
////        String test = "{\"id\":63,\"code\":\"unique-code-3\",\"name\":\"Bond3M-TMG02\",\"agreementId\":42,\"startDate\":\"2021-02-01\",\"endDate\":\"2021-03-14\",\"status\":1,\"createdDate\":\"2021-02-22\"}\n";
////        list = mapper.readValue(test, new TypeReference<List<ProdDerivativeDTO>>() {});
////
////        list = CommonUtils.castObjectsToDTOs(mapper, test, ProdDerivativeDTO.class);
//
////        log.info("test ## {} ", list.get(0));
//
//    }
//
//    private OrderPlacementDTO buildDataTest() {
//
//        String test = "2021-02-10";
//
//        ArrangementDTO arrDTO =  ArrangementDTO.builder()
//                .type(ArrangementTypeEnum.BUY.getType())
//                .tradingDate(DateHelper.parseTimestamp(test))
//                .volume(1000)
//                .build();
//        ArrangementPartyDTO partyDTO = ArrangementPartyDTO.builder()
//                .customerId(101L)
//                .build();
//        ArrangementPricingDTO priceDTO = ArrangementPricingDTO.builder()
//                .rate(0.2).reinvestmentRate(0.4)
//                .price(200000.0).unitPrice(100000.0)
//                .fee(20000.0).tax(10000.0)
//                .discountedAmount(5000.0)
//                .build();
//        ProdDerivativeDTO derivativeDTO = ProdDerivativeDTO.builder()
//                .id(1L).build();
//        OrderPlacementDTO order = OrderPlacementDTO.builder()
//                .purchaserAccount("test123")
//                .arrangement(arrDTO).party(partyDTO)
//                .pricing(priceDTO).derivative(derivativeDTO).build();
//
//        return order;
//
//    }
//
//    @Test
//    public void testGetDetailArrangement() {
//
//        ArrangementInfoDTO output = backSupportService.retrieveDetailArrangement(Long.valueOf(342), Long.valueOf(361));
//        log.info("output: {}", output);
//
//    }
//
//    @Test
//    public void testMono() {
//
////        Mono.empty().doOnNext(data -> testMonoData()).subscribe(a -> log.info("test#0"));
////        Mono.empty().doOnSuccess(data -> testMonoData()).subscribe(a -> log.info("test#1"));
//        Mono.empty().zipWith(testVoidMonoData(), (data, data1) -> {
//            log.info("test#123");
//            return data1;
//        });
////        Mono.empty().doOnSubscribe(data -> testMonoData()).subscribe(a -> log.info("test#2"));
////        Mono.empty().doOnTerminate(this::testMonoData).subscribe(a -> log.info("test#3"));
////        Mono.empty().doOnRequest(data -> testMonoData()).subscribe(a -> log.info("test#4"));
//    }
//
//
//    private void testMonoData() {
//        log.info("DATA RETURN ....");
//    }
//
//    private Mono<String> testVoidMonoData() {
//        return Mono.just("ABCXYZ").map(data -> {
//            log.info("test### {}", data);
//            return data;
//        });
//    }
//
//    @Test
//    public void testApprove() {
//
//    }
//}
