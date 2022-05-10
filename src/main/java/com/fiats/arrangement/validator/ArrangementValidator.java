package com.fiats.arrangement.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiats.arrangement.constant.ArrConstant;
import com.fiats.arrangement.constant.ArrangementErrorCode;
import com.fiats.arrangement.constant.AttributeEnum;
import com.fiats.arrangement.jpa.entity.Arrangement;
import com.fiats.arrangement.constant.ArrangementStatusEnum;
import com.fiats.arrangement.constant.ArrangementTypeEnum;
import com.fiats.arrangement.jpa.entity.ArrangementOperation;
import com.fiats.arrangement.jpa.repo.ArrangementRepo;
import com.fiats.arrangement.service.AssetService;
import com.fiats.arrangement.service.AttributeService;
import com.fiats.arrangement.service.IntradayLimitService;
import com.fiats.arrangement.service.ProductDerivativeService;
import com.fiats.exception.NeoFiatsException;
import com.fiats.exception.ValidationException;
import com.fiats.tmgcoreutils.constant.BondType;
import com.fiats.tmgcoreutils.constant.Constant;
import com.fiats.tmgcoreutils.constant.IntradayLimitType;
import com.fiats.tmgcoreutils.payload.*;
import com.fiats.tmgcoreutils.utils.CommonUtils;
import com.fiats.tmgcoreutils.utils.DateHelper;
import com.fiats.tmgcoreutils.utils.ReactiveClientUtils;
import com.fiats.tmgcoreutils.validator.CommonValidator;
import com.fiats.tmgjpa.entity.RecordStatus;
import com.neo.exception.NeoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Component
@Slf4j
public class ArrangementValidator extends CommonValidator {

    @Autowired
    ArrangementRepo arrRepo;

    @Autowired
    RestTemplate restTemplate;

    @Value("${fiats.internal.api.info.url}")
    private String infoUrl;

    @Value("${fiats.internal.api.product.url}")
    private String productUrl;

    @Value("${fiats.internal.api.crm.url}")
    private String crmUrl;

    @Autowired
    @Qualifier("customMapper")
    ObjectMapper mapper;

    @Autowired
    @Qualifier("customWebClient")
    WebClient client;

    @Autowired
    @Qualifier(Constant.DEFAULT_THREAD_POOL)
    TaskExecutor executor;

    @Autowired
    ProductDerivativeService pdService;

    @Autowired
    AssetService assetService;

    @Autowired
    AttributeService attributeService;

    @Autowired
    CustomerValidator customerValidator;

    @Autowired
    IntradayLimitService intradayLimitService;

    public Arrangement validateExistence(Long id, boolean expectation,
                                         boolean fetchParty) throws ValidationException {

        if (id == null || CommonUtils.isInvalidPK(id)) {
            return null;
        }

        Optional<Arrangement> entity = fetchParty
                ? arrRepo.findByIdFetchParty(id)
                : arrRepo.findById(id);
        return validateExistence(entity, expectation);
    }

    public Arrangement validateExistence(Optional<Arrangement> entity, boolean expectation) {

        boolean actual = entity.isPresent();

        if (expectation != actual) {
            throw new ValidationException(CommonUtils.format("Unexpected existence with" +
                    " expectation: {0}, actual: {1}", expectation, actual));
        }

        return actual ? entity.get() : null;
    }


    public void validatePlacementAction(ArrangementTypeEnum type) {

        if (type == null) {
            throw new ValidationException(ArrangementErrorCode.ARRANGEMENT_TYPE_NOT_SUPPORT,
                    CommonUtils.format("Type is not present"));
        }

        if (!type.isBuyOrsell()) {
            throw new ValidationException(ArrangementErrorCode.ARRANGEMENT_TYPE_NOT_SUPPORT,
                    CommonUtils.format("Invalid arrangement type - {0}",
                            type.toString()));
        }
    }

    public void validateSellTradingCode(String sellTradingCode, Long sellTradingId) {

        if (!StringUtils.hasText(sellTradingCode) || CommonUtils.isInvalidPK(sellTradingId)) {
            throw new ValidationException(ArrangementErrorCode.ARRANGEMENT_SELL_ARRANGEMENT_CODE_NOT_FOUND,
                    "Empty sell trading code");
        }
    }


    public Mono<CustomerBrokerWrapper> validateAndIdentifyBroker(String purchaserAccount,
                                                                 Long partyCustomerId) {

        log.info("Calling rest for {} and {} ", purchaserAccount, partyCustomerId);

        return ReactiveClientUtils.getForMono(client, crmUrl,
                "/customer/identify?pa={purchaserAccount}&p={partyCustomerId}",
                executor, CustomerBrokerWrapper.class, log, purchaserAccount, partyCustomerId);
    }

    public Mono<ProductIssuerWrapper> validateProductDerivate(ProdDerivativeDTO dto, Timestamp td) {
        return pdService.findDerivativeByOfferingPeriod(dto, td);
    }

    public AssetDTO validateCustomerAsset(int purchaseVolume, Long customerId, String derivativeCode, String vanillaCode,
                                          Long sellArrangementId, boolean outstanding) {

        log.info("Validating for volume {} based on customerId {}, derivativeCode {}, vanillaCode {}, sellArrangementId {}, outstanding {} ",
                purchaseVolume, customerId, derivativeCode, vanillaCode, sellArrangementId, outstanding);

        if (purchaseVolume < 0 || CommonUtils.isInvalidPK(customerId) || !StringUtils.hasText(derivativeCode)
                || !StringUtils.hasText(vanillaCode) || CommonUtils.isInvalidPK(sellArrangementId)) {
            throw new ValidationException(ArrangementErrorCode.EMPTY_ASSET_PAYLOAD,
                    CommonUtils.format("Empty info while searching for asset of customer id {0}", customerId));
        }

        List<AssetDTO> customerAssets = assetService.findAssetByCustomerId(customerId, vanillaCode, derivativeCode, outstanding, ArrConstant.AssetFilterCondition.ALL.content());
        log.info("Done finding assets for {}", customerId);

        // switch to validate by sellArrangementCode
        AssetDTO assetDTO = customerAssets.stream()
                .filter(tmp -> sellArrangementId.equals(tmp.getId()) && derivativeCode.equals(tmp.getProductDerivativeCode()))
                .findAny().orElse(null);

        if (assetDTO == null || assetDTO.getAvailableVolume() == null || assetDTO.getAvailableVolume() < purchaseVolume) {
            throw new ValidationException(ArrangementErrorCode.ASSET_NOT_ENOUGH,
                    CommonUtils.format("Cannot place {0} volume with insufficient available asset {1} ",
                            purchaseVolume, assetDTO == null ? null : assetDTO.getAvailableVolume()));
        }

        return assetDTO;

    }

    public boolean isTradingDateToday(Timestamp td) {
        ZoneId zoneId = ZoneId.of(Constant.TIMEZONE_ICT);
        LocalDate today = LocalDate.now(zoneId);
        LocalDate tradingDate = DateHelper.parseLocalDate(td.getTime())
                .atStartOfDay(zoneId).toLocalDate();
        return today.isEqual(tradingDate);
    }

    public Map<String, PropsResp> validateAttributesForOrderPlacement(
            Integer volume, Timestamp tradingDate, String prodDerivativeCode,
            String prodVanillaCode, String prodAgreement, ArrangementTypeEnum transactionType,
            String channel, String cusType) {

        Set<String> attributes = new HashSet<>();
        attributes.add(AttributeEnum.MIN_TRADING_AMOUNT.getVariable());
        attributes.add(AttributeEnum.LIMITED_DAY.getVariable());
        attributes.add(AttributeEnum.DEADLINE_PAYMENT.getVariable());
        attributes.add(AttributeEnum.LOCK.getVariable());

        Map<String, PropsResp> attributeMap = attributeService.findSystemAttributes(attributes, tradingDate,
                prodDerivativeCode, prodVanillaCode, prodAgreement, transactionType, channel, cusType);

        log.info("Begin validation for order placement ...");
        validateLockStatus(attributeMap.get(AttributeEnum.LOCK.getVariable()));
        validateLimitedDays(tradingDate, attributeMap.get(AttributeEnum.LIMITED_DAY.getVariable()));
        validateDeadlinePaymentForToday(tradingDate, attributeMap.get(AttributeEnum.DEADLINE_PAYMENT.getVariable()));
        validateMinTradingAmount(volume, attributeMap.get(AttributeEnum.MIN_TRADING_AMOUNT.getVariable()));
        log.info("Done validation for order placement ...");

        return attributeMap;
    }

    public void validateLockStatus(PropsResp lockStatusAttr) {

        try {

            if (Integer.parseInt(lockStatusAttr.getValue().trim()) == ArrConstant.LOCK_STATUS) {
                throw new ValidationException(ArrangementErrorCode.VIOLATED_LOCK_STATUS,
                        CommonUtils.format("This action is locked by system attribute {0}",
                                lockStatusAttr.getValue()));
            }

        } catch (ValidationException e) {
            throw e; // re throw
        } catch (Exception e) {
            throw new NeoFiatsException(CommonUtils.format("Attribute {0} validation failed with {1}",
                    lockStatusAttr.getVariable(), e.getMessage()));
        }
    }

    public void validateLimitedDays(Timestamp td, PropsResp limitedDayAttr) {

        try {

            // sample : 14
            LocalDate tradingDate = Instant.ofEpochMilli(td.getTime())
                    .atZone(ZoneId.of(Constant.TIMEZONE_ICT))
                    .toLocalDate();
            LocalDate limitedDate = LocalDate.now().plusDays(Long.parseLong(limitedDayAttr.getValue()));

            if (tradingDate.isAfter(limitedDate)) {
                throw new ValidationException(ArrangementErrorCode.VIOLATED_LIMITED_DAY,
                        CommonUtils.format("Trading date {0} cannot be later than {1} days of today",
                                DateHelper.formatDateSilently(tradingDate), limitedDayAttr.getValue()));
            }

        } catch (ValidationException e) {
            throw e; // re throw
        } catch (Exception e) {
            throw new NeoFiatsException(CommonUtils.format("Attribute {0} validation failed with {1}",
                    limitedDayAttr.getVariable(), e.getMessage()));
        }
    }

    public void validateDeadlinePaymentForToday(Timestamp td, PropsResp deadlinePaymentAttr) {


        if (!isTradingDateToday(td)) {
            log.info("Trading date {} is in the future. Not gonna validate", td);
            return;// dont validate, still need deadlinePayment for noti
        }

        try {
            // sample: 18h00
            LocalDateTime now = LocalDateTime.now();
            LocalDate dueDate = LocalDate.now();
            int[] time = Arrays.stream(deadlinePaymentAttr.getValue()
                    .split(ArrConstant.NON_NUMMERIC_REGEX))
                    .mapToInt(Integer::parseInt)
                    .toArray();
            int length = time.length;
            LocalDateTime due;

            if (length == 1) {
                due = dueDate.atTime(time[0], 0);
            } else if (length == 2) {
                due = dueDate.atTime(time[0], time[1]);
            } else if (length == 3) {
                due = dueDate.atTime(time[0], time[1], time[2]);
            } else { // only support to seconds
                throw new NeoFiatsException(ArrangementErrorCode.UNSUPPORTED_TIME,
                        CommonUtils.format("Unsupported time {0}", deadlinePaymentAttr.getValue()));
            }

            if (now.isAfter(due)) {
                throw new ValidationException(ArrangementErrorCode.VIOLATED_DEADLINE_PAYMENT,
                        CommonUtils.format("Cannot perform this action after {0}. Now is {1}",
                                deadlinePaymentAttr.getValue(), DateHelper.formatDateTimeSilently(now)));
            }

        } catch (ValidationException e) {
            throw e; // re throw
        } catch (Exception e) {
            throw new NeoFiatsException(CommonUtils.format("Attribute {0} validation failed with {1}",
                    deadlinePaymentAttr.getVariable(), e.getMessage()));
        }
    }

    public void validateMinTradingAmount(Integer volume, PropsResp mtmAttribute) {
        try {

            // sample: 10000
            if (Integer.parseInt(mtmAttribute.getValue()) > volume) {
                throw new ValidationException(ArrangementErrorCode.VIOLATED_MIN_TRADING_AMOUNT,
                        CommonUtils.format("Purchasing volume {0} cannot be below {1}",
                                volume, mtmAttribute.getValue()));
            }

        } catch (ValidationException e) {
            throw e; // re throw
        } catch (Exception e) {
            throw new NeoFiatsException(CommonUtils.format("Attribute {0} validation failed with {1}",
                    mtmAttribute.getVariable(), e.getMessage()));
        }
    }

    public void validateCustomConfirmationForReference(Arrangement arr) {
        if (!arr.getStatus().equals(ArrangementStatusEnum.REFERENCE.getStatus())) {
            throw new ValidationException(ArrangementErrorCode.ARRANGEMENT_STATUS_INVALID,
                    CommonUtils.format("Invalid customer confirmation status for {0}",
                            arr.getId()));
        }
    }

    public void validateCancellation(Arrangement arr, boolean jobAutoExpire) {

        if (ArrConstant.EXCEPTION.equals(arr.getException()) && !jobAutoExpire) {
            throw new ValidationException(ArrangementErrorCode.EXCEPTION_REQUEST_INVALID_STATUS,
                    CommonUtils.format("Internally placed arrangement ID {0} cannot be cancelled",
                            arr.getId()));
        }

        if (arr.getStatus().equals(ArrangementStatusEnum.CANCELLED.getStatus())) {
            throw new ValidationException(ArrangementErrorCode.ARRANGEMENT_STATUS_INVALID,
                    CommonUtils.format("Arrangement ID {0} cannot be cancelled with status {1}",
                            arr.getId(), arr.getStatus()));
        }

        ArrangementTypeEnum type = ArrangementTypeEnum.lookForType(arr.getType());
        if (type == null || !type.isBuyOrsell()) {
            throw new ValidationException(ArrangementErrorCode.ARRANGEMENT_TYPE_NOT_SUPPORT,
                    CommonUtils.format("Arrangement type {0} is not supported", arr.getType()));
        }

        ArrangementOperation buyOrSellOperation;
        if ((buyOrSellOperation = arr.getOperation()) == null
                || RecordStatus.ACTIVE.getStatus() == buyOrSellOperation.getPaymentStatus()) {
            throw new ValidationException(ArrangementErrorCode.ARRANGEMENT_PAYMENT_ALREADY,
                    CommonUtils.format("Arrangement ID {0} cannot be cancelled with payment status {1}",
                            arr.getId(), arr.getStatus()));
        }
    }

    public void validateMatching(Arrangement arr) {

        if (!arr.getStatus().equals(ArrangementStatusEnum.INACTIVE.getStatus())) {
            throw new ValidationException(ArrangementErrorCode.ARRANGEMENT_STATUS_INVALID,
                    CommonUtils.format("The status {0} of the arrangement ID {1} cannot be matched",
                            arr.getStatus(), arr.getId()));
        }

    }

    public void validateSigningContract(Arrangement arr) {
        if (!arr.getStatus().equals(ArrangementStatusEnum.INACTIVE.getStatus())
                || !arr.getOperation().getCustomerStatus().equals(RecordStatus.INACTIVE.getStatus())) {
            throw new ValidationException(ArrangementErrorCode.ARRANGEMENT_STATUS_INVALID,
                    CommonUtils.format("The arr status {0} and operation {1} of the arrangement ID {2} cannot be signed",
                            arr.getStatus(), arr.getOperation().getCustomerStatus(), arr.getId()));
        }

    }

    public void validatePayment(Arrangement arr) {
        if (!arr.getStatus().equals(ArrangementStatusEnum.ACTIVE.getStatus())) {
            throw new ValidationException(ArrangementErrorCode.ARRANGEMENT_STATUS_INVALID,
                    CommonUtils.format("The status {0} of the arrangement ID {1} cannot be matched",
                            arr.getStatus(), arr.getId()));
        }

    }

    public void updateProductModel(OrderPlacementDTO dto, ProductIssuerWrapper piWrapper) {

        ProdDerivativeDTO prodDerivativeDTO = piWrapper.getDerivativeDTO();
        ProdAgreementDTO prodAgreementDTO = prodDerivativeDTO.getProdAgreement();
        ProdVanillaDTO prodVanillaDTO = prodDerivativeDTO.getProdVanilla();

        if (dto.getDerivative().getProdVanilla() == null) {
            dto.getDerivative().setProdVanilla(new ProdVanillaDTO());
        }
        if (dto.getDerivative().getProdAgreement() == null) {
            dto.getDerivative().setProdAgreement(new ProdAgreementDTO());
        }
        // update model
        dto.setIssuerDTO(piWrapper.getIssuerDTO());
        BeanUtils.copyProperties(prodDerivativeDTO, dto.getDerivative(),
                CommonUtils.getNullProperties(prodDerivativeDTO));
        dto.getArrangement().setProductDerivativeCode(prodDerivativeDTO.getCode());
        dto.getArrangement().setProductDerivativeId(prodDerivativeDTO.getId());
        if (prodVanillaDTO != null) {
            dto.getArrangement().setProductVanillaCode(prodVanillaDTO.getCode());
            dto.getArrangement().setProductVanillaId(prodVanillaDTO.getId());
        }
        if (prodAgreementDTO != null) {
            dto.getArrangement().setProductAgreementCode(prodAgreementDTO.getCode());
            dto.getArrangement().setProductAgreementId(prodAgreementDTO.getId());
        }
    }

    public void validateIntradayLimit(Timestamp tradingDate, String vanillaCode, String issuerCode,
                                      Double price) {
        // getting the minimum threshold out of all 3 types
        Double minimumThreshold = intradayLimitService.findIntradayLimitThresholds(
                new ArrayList<IntradayLimitType>() {{
                    add(IntradayLimitType.CAPITAL);
                    add(IntradayLimitType.ISSUER);
                    add(IntradayLimitType.PRODUCT);
                }}, new ArrayList<String>() {{
                    add(vanillaCode);
                }}, new ArrayList<String>() {{
                    add(issuerCode);
                }}, tradingDate)
                .map(IntradayLimitDTO::getPrice)
                .reduce((p1, p2) -> p1 > p2 ? p2 : p1).block();
        log.info("Using intraday limit minimum {} to compare with price {}", minimumThreshold, price);
        if (minimumThreshold != null && price > minimumThreshold) {
            throw new ValidationException(ArrangementErrorCode.INTRADAY_LIMIT_EXCEEDED,
                    CommonUtils.format("Intraday limit {0} has been exceeded {1}",
                            minimumThreshold, price));
        }
    }


    public void validateAndUpdateModelBeforePerformingAction(OrderPlacementDTO dto,
                                                             ArrangementTypeEnum type,
                                                             ArrangementWrapper arrWrapper) {

        CustomerBrokerWrapper cusBroker = arrWrapper.getCusBroker();
        if (dto.getParty() != null) {
            dto.getParty().setCustomer(cusBroker.getCustomer());
        }

        if (dto.getBrokerParty() != null) {
            dto.getBrokerParty().setBroker(cusBroker.getBroker());
        }

        if (dto.getDerivative().getId() == null) {
            ProdDerivativeDTO derivative = pdService.findByCode(dto.getDerivative().getCode());
            if (derivative != null) {
                dto.getDerivative().setId(derivative.getId());
            }
        }

        DateHelper.validateHoliday(restTemplate,
                DateHelper.formatDateSilently(dto.getArrangement().getTradingDate(),
                        Constant.FORMAT_SQLSERVER_SHORT), infoUrl);
        ProductIssuerWrapper piWrapper = validateProductDerivate(dto.getDerivative(),
                dto.getArrangement().getTradingDate()).block();
        arrWrapper.setPiWrapper(piWrapper);

        if (dto.getArrangement().getType().equals(ArrangementTypeEnum.BUY.getType()))
            validateProfessional(cusBroker.getCustomer(), piWrapper.getDerivativeDTO().getProdVanilla());

        ProdDerivativeDTO prodDerivativeDTO = piWrapper.getDerivativeDTO();
        ProdAgreementDTO prodAgreementDTO = prodDerivativeDTO.getProdAgreement();
        ProdVanillaDTO prodVanillaDTO = prodDerivativeDTO.getProdVanilla();
        updateProductModel(dto, piWrapper);

        // now find Listed or OTC or Frozen for further usages
        PropsResp bondStatus = attributeService.findSystemAttribute(AttributeEnum.BOND_STATUS.getVariable(),
                dto.getArrangement().getTradingDate(), prodDerivativeDTO.getCode(), prodVanillaDTO.getCode(),
                prodAgreementDTO.getCode(), type, Constant.NORMAL_CHANNEL, cusBroker.getCustomer().getCustomerType());

        BondType listedStatus = BondType.lookForBondCode(bondStatus.getValue());

        // validate customer stock account
        customerValidator.validateStockAccount(cusBroker.getCustomer(), listedStatus);
        dto.getDerivative().setListedType(listedStatus);
        dto.getArrangement().setListedType(listedStatus.getType());

        // attribute validations
        Map<String, PropsResp> attributes = validateAttributesForOrderPlacement(
                dto.getArrangement().getVolume(),
                dto.getArrangement().getTradingDate(),
                prodDerivativeDTO.getCode(),
                prodVanillaDTO.getCode(),
                prodAgreementDTO.getCode(),
                type, Constant.NORMAL_CHANNEL,
                cusBroker.getCustomer().getCustomerType()
        );
        arrWrapper.setAttributes(attributes);

        // validate & update asset
        if (ArrangementTypeEnum.SELL == type) {
            AssetDTO asset = validateCustomerAsset(dto.getArrangement().getVolume(),
                    dto.getParty().getCustomerId(), prodDerivativeDTO.getCode(),
                    prodVanillaDTO.getCode(), dto.getArrangement().getSellArrangementId(),
                    Constant.INACTIVE);
            arrWrapper.setAssets(new ArrayList<AssetDTO>() {{
                add(asset);
            }});
        }
    }

    public void validateProfessional(CustomerDTO customer, ProdVanillaDTO vanilla) {

        Timestamp requireDate = DateHelper.parseTimestamp("2021-01-01", Constant.FORMAT_SQLSERVER_SHORT);

        if ((vanilla.getIssueDate().equals(requireDate) || vanilla.getIssueDate().after(requireDate))
                && (StringUtils.hasText(vanilla.getIssueType()) && vanilla.getIssueType().equals("0"))
                && (customer.getProfessional() == null || customer.getProfessional() == 0)) {
            throw new NeoException(null, ArrangementErrorCode.PROFESSIONAL_CUSTOMER_REQUIRED,
                    CommonUtils.format("Product vanilla {0} require customer {1} professional",
                            vanilla.getCode(), customer.getAccount()));
        }
    }
}
