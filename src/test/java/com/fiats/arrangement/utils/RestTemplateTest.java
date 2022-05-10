package com.fiats.arrangement.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiats.arrangement.service.CustomerService;
import com.fiats.arrangement.service.SnapshotService;
import com.fiats.arrangement.validator.ArrangementValidator;
import com.fiats.exception.NeoFiatsException;
import com.fiats.exception.ValidationException;
import com.fiats.tmgcoreutils.payload.*;
import com.neo.exception.LoggingUtils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootTest
@Slf4j
public class RestTemplateTest {

    @Autowired
    ArrangementValidator validator;

    private static String couponModelJSON = "[{\"totalDayOfCoupon\":31,\"couponStartDate\":\"2021-05-11\",\"couponEndDate\":\"2021-06-11\",\"couponPaymentDate\":\"2021-06-11\",\"couponAmount\":246575.3424657534,\"couponRate\":0.03},{\"totalDayOfCoupon\":30,\"couponStartDate\":\"2021-06-11\",\"couponEndDate\":\"2021-07-11\",\"couponPaymentDate\":\"2021-07-11\",\"couponAmount\":246575.3424657534,\"couponRate\":0.03},{\"totalDayOfCoupon\":31,\"couponStartDate\":\"2021-07-11\",\"couponEndDate\":\"2021-08-11\",\"couponPaymentDate\":\"2021-08-11\",\"couponAmount\":246575.3424657534,\"couponRate\":0.03},{\"totalDayOfCoupon\":31,\"couponStartDate\":\"2021-08-11\",\"couponEndDate\":\"2021-09-11\",\"couponPaymentDate\":\"2021-09-11\",\"couponAmount\":246575.3424657534,\"couponRate\":0.03},{\"totalDayOfCoupon\":30,\"couponStartDate\":\"2021-09-11\",\"couponEndDate\":\"2021-10-11\",\"couponPaymentDate\":\"2021-10-11\",\"couponAmount\":246575.3424657534,\"couponRate\":0.03},{\"totalDayOfCoupon\":31,\"couponStartDate\":\"2021-10-11\",\"couponEndDate\":\"2021-11-11\",\"couponPaymentDate\":\"2021-11-11\",\"couponAmount\":246575.3424657534,\"couponRate\":0.03},{\"totalDayOfCoupon\":30,\"couponStartDate\":\"2021-11-11\",\"couponEndDate\":\"2021-12-11\",\"couponPaymentDate\":\"2021-12-11\",\"couponAmount\":246575.3424657534,\"couponRate\":0.03},{\"totalDayOfCoupon\":31,\"couponStartDate\":\"2021-12-11\",\"couponEndDate\":\"2022-01-11\",\"couponPaymentDate\":\"2022-01-11\",\"couponAmount\":246575.3424657534,\"couponRate\":0.03},{\"totalDayOfCoupon\":31,\"couponStartDate\":\"2022-01-11\",\"couponEndDate\":\"2022-02-11\",\"couponPaymentDate\":\"2022-02-11\",\"couponAmount\":246575.3424657534,\"couponRate\":0.03},{\"totalDayOfCoupon\":28,\"couponStartDate\":\"2022-02-11\",\"couponEndDate\":\"2022-03-11\",\"couponPaymentDate\":\"2022-03-11\",\"couponAmount\":246575.3424657534,\"couponRate\":0.03},{\"totalDayOfCoupon\":28,\"couponStartDate\":\"2022-02-16\",\"couponEndDate\":\"2022-03-16\",\"couponPaymentDate\":\"2022-03-16\",\"couponAmount\":246575.3424657534,\"couponRate\":0.03}]";

    private static String testModelJSON = "{\"purchaserAccount\":\"sham511\",\"arrangement\":{\"id\":2394,\"createdDate\":\"2021-05-11 05:12:47\",\"updatedDate\":\"2021-05-11 05:12:47\",\"status\":0,\"code\":\"000.Term3M-DRC20210301.000002209\",\"type\":1,\"tradingDate\":\"2021-05-11\",\"volume\":1000,\"channel\":\"normal\",\"productDerivativeId\":264,\"productDerivativeCode\":\"Term3M-DRC20210301\",\"productVanillaId\":270,\"productVanillaCode\":\"DRC202201\",\"productAgreementId\":146,\"productAgreementCode\":\"Term3M\",\"listedType\":0},\"party\":{\"id\":3749,\"role\":\"OWNER\",\"customerId\":346,\"account\":\"sham511\",\"customer\":{\"id\":346,\"name\":\"Lê Ngọc Nhung\",\"idCard\":\"013522400\",\"dob\":1501459200000,\"phone\":\"0378568696\",\"email\":\"nhungln.511@gmail.com\",\"gender\":\"0\",\"address\":\"118 Ngô Xuân Quảng, Trâu Quỳ, Gia Lâm, Hà Nội\",\"status\":1,\"account\":\"sham511\",\"nationality\":\"Việt Nam\",\"customerType\":\"PERSONAL\",\"accountNumber\":\"19033718869011\",\"statusAccount\":1,\"stockAccount\":\"HDBS651197\",\"vsdActivateStatus\":1,\"cusBankAccount\":{}}},\"purchaserParty\":{\"id\":3750,\"role\":\"PURCHASER\",\"customerId\":346},\"pricing\":{\"id\":2114,\"rate\":0.03,\"reinvestmentRate\":0.076,\"price\":1.00846E8,\"unitPrice\":100846.0,\"totalInvestAmount\":1.00896423E8,\"fee\":50423.0,\"investmentTimeByMonth\":3.0,\"totalMoneyRtm\":1.073926E8,\"coupons\":[{\"totalDayOfCoupon\":31,\"couponStartDate\":\"2021-05-11\",\"couponEndDate\":\"2021-06-11\",\"couponPaymentDate\":\"2021-06-11\",\"couponAmount\":246575.3424657534,\"couponRate\":0.03},{\"totalDayOfCoupon\":30,\"couponStartDate\":\"2021-06-11\",\"couponEndDate\":\"2021-07-11\",\"couponPaymentDate\":\"2021-07-11\",\"couponAmount\":246575.3424657534,\"couponRate\":0.03},{\"totalDayOfCoupon\":31,\"couponStartDate\":\"2021-07-11\",\"couponEndDate\":\"2021-08-11\",\"couponPaymentDate\":\"2021-08-11\",\"couponAmount\":246575.3424657534,\"couponRate\":0.03},{\"totalDayOfCoupon\":31,\"couponStartDate\":\"2021-08-11\",\"couponEndDate\":\"2021-09-11\",\"couponPaymentDate\":\"2021-09-11\",\"couponAmount\":246575.3424657534,\"couponRate\":0.03},{\"totalDayOfCoupon\":30,\"couponStartDate\":\"2021-09-11\",\"couponEndDate\":\"2021-10-11\",\"couponPaymentDate\":\"2021-10-11\",\"couponAmount\":246575.3424657534,\"couponRate\":0.03},{\"totalDayOfCoupon\":31,\"couponStartDate\":\"2021-10-11\",\"couponEndDate\":\"2021-11-11\",\"couponPaymentDate\":\"2021-11-11\",\"couponAmount\":246575.3424657534,\"couponRate\":0.03},{\"totalDayOfCoupon\":30,\"couponStartDate\":\"2021-11-11\",\"couponEndDate\":\"2021-12-11\",\"couponPaymentDate\":\"2021-12-11\",\"couponAmount\":246575.3424657534,\"couponRate\":0.03},{\"totalDayOfCoupon\":31,\"couponStartDate\":\"2021-12-11\",\"couponEndDate\":\"2022-01-11\",\"couponPaymentDate\":\"2022-01-11\",\"couponAmount\":246575.3424657534,\"couponRate\":0.03},{\"totalDayOfCoupon\":31,\"couponStartDate\":\"2022-01-11\",\"couponEndDate\":\"2022-02-11\",\"couponPaymentDate\":\"2022-02-11\",\"couponAmount\":246575.3424657534,\"couponRate\":0.03},{\"totalDayOfCoupon\":28,\"couponStartDate\":\"2022-02-11\",\"couponEndDate\":\"2022-03-11\",\"couponPaymentDate\":\"2022-03-11\",\"couponAmount\":246575.3424657534,\"couponRate\":0.03},{\"totalDayOfCoupon\":28,\"couponStartDate\":\"2022-02-16\",\"couponEndDate\":\"2022-03-16\",\"couponPaymentDate\":\"2022-03-16\",\"couponAmount\":246575.3424657534,\"couponRate\":0.03}],\"pricingBondResultDTO\":{\"unitPrice\":100846.0,\"totalPar\":1.0E8,\"investmentTimeByMonth\":3.0,\"cashFlows\":[{\"totalDayOfCoupon\":92,\"totalDayFromCouponDateToActualDate\":273,\"couponStartDate\":\"2021-03-16\",\"couponEndDate\":\"2021-06-16\",\"couponPaymentDate\":\"2021-06-16\",\"couponExDate\":\"2021-06-07\",\"couponAmount\":1863340.0,\"reinvestAmount\":0.0,\"reinvestmentRate\":0.0075,\"reinvestmentAmountActual\":0.0,\"couponRate\":0.074},{\"totalDayOfCoupon\":92,\"totalDayFromCouponDateToActualDate\":181,\"couponStartDate\":\"2021-06-16\",\"couponEndDate\":\"2021-09-16\",\"couponPaymentDate\":\"2021-09-16\",\"couponExDate\":\"2021-09-07\",\"couponAmount\":1863340.0,\"reinvestAmount\":0.0,\"reinvestmentRate\":0.0075,\"reinvestmentAmountActual\":0.0,\"couponRate\":0.074},{\"totalDayOfCoupon\":91,\"totalDayFromCouponDateToActualDate\":90,\"couponStartDate\":\"2021-09-16\",\"couponEndDate\":\"2021-12-16\",\"couponPaymentDate\":\"2021-12-16\",\"couponExDate\":\"2021-12-07\",\"couponAmount\":1843087.0,\"reinvestAmount\":0.0,\"reinvestmentRate\":0.0075,\"reinvestmentAmountActual\":0.0,\"couponRate\":0.074},{\"totalDayOfCoupon\":90,\"totalDayFromCouponDateToActualDate\":0,\"couponStartDate\":\"2021-12-16\",\"couponEndDate\":\"2022-03-16\",\"couponPaymentDate\":\"2022-03-16\",\"couponExDate\":\"2022-03-07\",\"couponAmount\":1822833.0,\"reinvestAmount\":0.0,\"reinvestmentRate\":0.0075,\"reinvestmentAmountActual\":0.0,\"couponRate\":0.074}],\"coupons\":[{\"totalDayOfCoupon\":31,\"couponStartDate\":\"2021-05-11\",\"couponEndDate\":\"2021-06-11\",\"couponPaymentDate\":\"2021-06-11\",\"couponAmount\":246575.3424657534,\"couponRate\":0.03},{\"totalDayOfCoupon\":30,\"couponStartDate\":\"2021-06-11\",\"couponEndDate\":\"2021-07-11\",\"couponPaymentDate\":\"2021-07-11\",\"couponAmount\":246575.3424657534,\"couponRate\":0.03},{\"totalDayOfCoupon\":31,\"couponStartDate\":\"2021-07-11\",\"couponEndDate\":\"2021-08-11\",\"couponPaymentDate\":\"2021-08-11\",\"couponAmount\":246575.3424657534,\"couponRate\":0.03},{\"totalDayOfCoupon\":31,\"couponStartDate\":\"2021-08-11\",\"couponEndDate\":\"2021-09-11\",\"couponPaymentDate\":\"2021-09-11\",\"couponAmount\":246575.3424657534,\"couponRate\":0.03},{\"totalDayOfCoupon\":30,\"couponStartDate\":\"2021-09-11\",\"couponEndDate\":\"2021-10-11\",\"couponPaymentDate\":\"2021-10-11\",\"couponAmount\":246575.3424657534,\"couponRate\":0.03},{\"totalDayOfCoupon\":31,\"couponStartDate\":\"2021-10-11\",\"couponEndDate\":\"2021-11-11\",\"couponPaymentDate\":\"2021-11-11\",\"couponAmount\":246575.3424657534,\"couponRate\":0.03},{\"totalDayOfCoupon\":30,\"couponStartDate\":\"2021-11-11\",\"couponEndDate\":\"2021-12-11\",\"couponPaymentDate\":\"2021-12-11\",\"couponAmount\":246575.3424657534,\"couponRate\":0.03},{\"totalDayOfCoupon\":31,\"couponStartDate\":\"2021-12-11\",\"couponEndDate\":\"2022-01-11\",\"couponPaymentDate\":\"2022-01-11\",\"couponAmount\":246575.3424657534,\"couponRate\":0.03},{\"totalDayOfCoupon\":31,\"couponStartDate\":\"2022-01-11\",\"couponEndDate\":\"2022-02-11\",\"couponPaymentDate\":\"2022-02-11\",\"couponAmount\":246575.3424657534,\"couponRate\":0.03},{\"totalDayOfCoupon\":28,\"couponStartDate\":\"2022-02-11\",\"couponEndDate\":\"2022-03-11\",\"couponPaymentDate\":\"2022-03-11\",\"couponAmount\":246575.3424657534,\"couponRate\":0.03},{\"totalDayOfCoupon\":28,\"couponStartDate\":\"2022-02-16\",\"couponEndDate\":\"2022-03-16\",\"couponPaymentDate\":\"2022-03-16\",\"couponAmount\":246575.3424657534,\"couponRate\":0.03}],\"pricing\":{\"transactionFee\":50423.0,\"totalInvestAmount\":1.00896423E8,\"yieldToMaturity\":0.076,\"ytmReinvested\":0.076,\"rate\":0.03,\"totalMoneyRtm\":1.073926E8,\"totalMoneyRtmReinvested\":1.073926E8,\"interestAmount\":6496177.0,\"interestAmountReinvested\":6496177.0,\"price\":1.00846E8,\"reinvestmentAmountToMaturity\":0.0}}},\"operation\":{\"id\":2424,\"customerStatus\":0,\"contractStatus\":0,\"paymentStatus\":0,\"deliveryStatus\":0,\"collateralStatus\":0,\"releaseStatus\":0},\"derivative\":{\"id\":264,\"prodVanilla\":{\"id\":270,\"createdDate\":1615593600000,\"updatedDate\":1615593600000,\"issuerId\":191,\"code\":\"DRC202201\",\"name\":\"BondDRC202103\",\"issueDate\":1615852800000,\"maturityDate\":1647388800000,\"par\":100000,\"listedCode\":\"DRC1603\",\"issueType\":\"0\",\"issuePurpose\":\"đầu tư\",\"status\":1,\"prodVanillaBond\":{}},\"vanillaId\":270,\"code\":\"Term3M-DRC20210301\",\"name\":\"Term3M-DRC20210301\",\"prodAgreement\":{\"id\":146,\"instrumentId\":1,\"code\":\"Term3M\",\"name\":\"Term3M\",\"holdingPeriod\":3,\"repurchase\":1,\"repurchaseMonth\":3,\"holdMoney\":1,\"holdAsset\":1,\"discount\":1,\"discountRate\":0.15,\"mortgage\":1,\"status\":1,\"transfer\":0},\"agreementId\":146,\"startDate\":\"2021-03-17\",\"endDate\":\"2022-12-17\",\"createdDate\":\"2021-03-17\",\"status\":\"2\",\"listedType\":\"OTC\"},\"issuerDTO\":{\"id\":191,\"name\":\"Công ty Cổ phần Cao su Đà Nẵng\",\"code\":\"DRC\",\"businessNo\":\"0400101531\",\"email\":\"hanhchinh@drc.com.vn\",\"address\":\"Tạ Quang Bửu - KCN Liên Chiểu - P. Hòa Hiệp Bắc - Q. Liên Chiểu - TP. Đà Nẵng\",\"phone\":\"02363771405\",\"active\":true}}";

    private static String cusBrokerModelJSON = "{\"customer\":{\"id\":346,\"name\":\"Lê Ngọc Nhung\",\"idCard\":\"013522400\",\"dob\":1501459200000,\"phone\":\"0378568696\",\"email\":\"nhungln.511@gmail.com\",\"gender\":\"0\",\"address\":\"118 Ngô Xuân Quảng, Trâu Quỳ, Gia Lâm, Hà Nội\",\"status\":1,\"account\":\"sham511\",\"nationality\":\"Việt Nam\",\"customerType\":\"PERSONAL\",\"accountNumber\":\"19033718869011\",\"statusAccount\":1,\"stockAccount\":\"HDBS651197\",\"vsdActivateStatus\":1,\"cusBankAccount\":{}},\"broker\":null}";

    @Autowired
    private RestTemplate restTemplate;

    @Value("${fiats.internal.api.product.url}")
    private String productUrl;

    @Autowired
    CustomerService customerService;

    @Autowired
    SnapshotService snapshotService;

    @Autowired
    ObjectMapper mapper;

    @DisplayName("Test Rest Template")
    @Test
    void test_restTemplate() {
//        String url = productUrl + "/prod-derivative" + "/search/approval";
//        log.info("Calling patch to {}", url);
//
//        ResponseEntity<ResponseMessage> res = restTemplate.getForEntity(url, ResponseMessage.class);
//
//        List<ProdDerivativeDTO> results = (List<ProdDerivativeDTO>) res.getBody().getData();
//        log.info("Got : {}", results);


    }

    @DisplayName("Test web client utils")
    @Test
    void test_callWebClient() throws JsonProcessingException {
//        customerService.findByAccount("test123", Constant.INACTIVE)
//                .subscribe(data -> log.info("Got: {}", LoggingUtils.objToStringIgnoreEx(data)));
//
//        customerService.findCustomerBrokerByIds(new ArrayList<Long>() {{
//            add(121L);
//            add(142L);
//        }}).subscribe(data -> log.info("Got: {}", LoggingUtils.objToStringIgnoreEx(data)));
//
//        SnapshotDTO model = mapper.readValue(testSnapshotData, SnapshotDTO.class);
//        snapshotService.callContractGenerator(model, new JWTHelper(sampleJWT))
//                .subscribe(data -> log.info("Got: {}", LoggingUtils.objToStringIgnoreEx(data)));
    }

//    @DisplayName("test rest error")
//    @Test
//    void test_callRestError() {
//        try {
//            validator.validateAndIdentifyBroker("sham511", 183L).block();
//        } catch (NeoFiatsException e) {
//            log.error("another test {}", LoggingUtils.objToStringIgnoreEx(e));
//            log.error("another test {}", LoggingUtils.objToStringIgnoreEx(e.getErr()));
//            log.error("another test {}", LoggingUtils.objToStringIgnoreEx(e.getErrorCode()));
//        } catch (Exception e) {
//            log.error("test# {} {}", e.getMessage(), e.getClass().getName());
//        }
//    }

    @DisplayName("test flatten")
    @Test
    void test_flattenMap() throws JsonProcessingException {

        OrderPlacementDTO testModel = mapper.readValue(testModelJSON, OrderPlacementDTO.class);
        CustomerBrokerWrapper cusbroker = mapper.readValue(cusBrokerModelJSON, CustomerBrokerWrapper.class);

        Map<String, Object> map = snapshotService.buildSnapshotModel(cusbroker, testModel);
        log.info("Got: {}", mapper.writeValueAsString(map));
    }

    @DisplayName("To collect all unique variables")
    @Test
    void findUniqueVariables() throws IOException {

        String[] varCodeGroups = new String[]{"CONTRACT_UAT"};

        for (String varCodeGroup : varCodeGroups) {

            String absPath = "/Users/leoseo/Desktop/noti";

            Pattern pattern = Pattern.compile(".*?(\\$[0-9A-Za-z]+).*");

            File dir = new File(absPath.concat(File.separator).concat(varCodeGroup));

            Set<String> variables = new HashSet<>();
            if (dir.isDirectory()) {
                String[] files = dir.list();

                for (String file : files) {

                    if (file.startsWith(".")) {
                        continue;
                    }

                    log.info("reading file {}", file);

                    Path path = Paths.get(absPath.concat(File.separator)
                            .concat(varCodeGroup).concat(File.separator).concat(file));
                    List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

                    lines.forEach(line -> {

                        String[] tmpParts = line.split("\\$");

                        int countPart = 0;
                        for (String tmp : tmpParts) {

                            if (countPart > 0) {
                                Matcher matcher = pattern.matcher("$" + tmp);
                                if (matcher.matches()) {
                                    log.info("matched {}", matcher.group(1));
                                    variables.add(matcher.group(1));
                                }
                            }

                            countPart++;
                        }

                    });
                    log.info("\n");
                }
            }


            log.info("Getting {}", variables);


            try (FileWriter writer = new FileWriter(
                    absPath.concat(File.separator).concat("variables")
                            .concat(File.separator).concat(varCodeGroup)
                            .concat(".txt"));
                 BufferedWriter bw = new BufferedWriter(writer)) {

                bw.write(String.join(", ", variables));

            } catch (IOException e) {

            }
        }


    }


}
