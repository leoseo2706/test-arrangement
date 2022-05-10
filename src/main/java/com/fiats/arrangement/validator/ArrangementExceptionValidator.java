package com.fiats.arrangement.validator;

import com.fiats.arrangement.constant.*;
import com.fiats.arrangement.payload.PricingParamDTO;
import com.fiats.arrangement.payload.filter.ArrangementExceptionFilter;
import com.fiats.arrangement.redis.entity.ArrangementException;
import com.fiats.arrangement.redis.repo.ArrangementExceptionRepo;
import com.fiats.arrangement.redis.specs.ArrangementExceptionSpecs;
import com.fiats.arrangement.service.AttributeService;
import com.fiats.exception.NeoFiatsException;
import com.fiats.exception.ValidationException;
import com.fiats.tmgcoreutils.constant.BondType;
import com.fiats.tmgcoreutils.constant.Constant;
import com.fiats.tmgcoreutils.payload.*;
import com.fiats.tmgcoreutils.utils.CommonUtils;
import com.fiats.tmgcoreutils.utils.DateHelper;
import com.neo.exception.NeoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
@Slf4j
public class ArrangementExceptionValidator {

    @Autowired
    ArrangementExceptionRepo arrangementExceptionRepo;

    @Autowired
    ArrangementExceptionSpecs arrangementExceptionSpecs;

    @Autowired
    ArrangementValidator arrangementValidator;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    CustomerValidator customerValidator;

    @Autowired
    AttributeService attributeService;

    @Value("${fiats.internal.api.info.url}")
    private String infoUrl;

    public ArrangementException validateExistence(ArrangementExceptionDTO dto) throws ValidationException {

        ArrangementExceptionFilter filter = new ArrangementExceptionFilter();
        BeanUtils.copyProperties(dto, filter);

        Optional<ArrangementException> trans = dto != null && dto.getId() != null
                ? arrangementExceptionRepo.findById(dto.getId())
                : arrangementExceptionRepo.findOne(arrangementExceptionSpecs.buildArrangementExceptionSpecs(filter));

        if (trans.isPresent()) {
            return trans.get();
        }

        return null;
    }

    public ArrangementException validateExistence(String id) throws NeoFiatsException {

        if (!StringUtils.hasText(id)) {
            return null;
        }

        return validateExistence(ArrangementExceptionDTO.builder().id(id).build());
    }

    public void validateUpdateStatus(ArrangementException rt) {

        if (rt == null || !StringUtils.hasText(rt.getStatus())) {
            throw new NeoException(null, ArrangementErrorCode.EXCEPTION_REQUEST_INVALID_STATUS, "Invalid request status");
        }

        if (!ArrConstant.MakeCheckStatus.DRAFT.toString().equals(rt.getStatus())
                && !ArrConstant.MakeCheckStatus.REJECTED.toString().equals(rt.getStatus())) {
            throw new NeoException(null, ArrangementErrorCode.EXCEPTION_REQUEST_INVALID_STATUS,
                    CommonUtils.format("Cannot update the record {0}" +
                    " with this invalid status {1}", rt.getId(), rt.getStatus()));
        }

    }

    public void validateSendApproveStatus(ArrangementException rt) {

        if (rt == null || !StringUtils.hasText(rt.getStatus())) {
            throw new NeoException(null, ArrangementErrorCode.EXCEPTION_REQUEST_INVALID_STATUS, "Invalid request status");
        }

        if (!ArrConstant.MakeCheckStatus.DRAFT.toString().equals(rt.getStatus())
                && !ArrConstant.MakeCheckStatus.REJECTED.toString().equals(rt.getStatus())) {
            throw new NeoException(null, ArrangementErrorCode.EXCEPTION_REQUEST_INVALID_STATUS,
                    CommonUtils.format("Cannot reject the record {0}" +
                            " with this invalid status {1}", rt.getId(), rt.getStatus()));
        }

    }

    public void validateRejectStatus(ArrangementException rt) {

        if (rt == null || !StringUtils.hasText(rt.getStatus())) {
            throw new NeoException(null, ArrangementErrorCode.EXCEPTION_REQUEST_INVALID_STATUS, "Invalid request status");
        }

        if (!ArrConstant.MakeCheckStatus.WAITING.toString().equals(rt.getStatus())) {
            throw new NeoException(null, ArrangementErrorCode.EXCEPTION_REQUEST_INVALID_STATUS,
                    CommonUtils.format("Cannot reject the record {0}" +
                    " with this invalid status {1}", rt.getId(), rt.getStatus()));
        }

    }

    public void validateRemoveStatus(ArrangementException rt) {

        if (rt == null || !StringUtils.hasText(rt.getStatus())) {
            throw new NeoException(null, ArrangementErrorCode.EXCEPTION_REQUEST_INVALID_STATUS, "Invalid request status");
        }

        if (ArrConstant.MakeCheckStatus.APPROVED.toString().equals(rt.getStatus())
                || ArrConstant.MakeCheckStatus.OUTDATED.toString().equals(rt.getStatus())) {
            throw new NeoException(null, ArrangementErrorCode.EXCEPTION_REQUEST_INVALID_STATUS,
                    CommonUtils.format("Cannot delete an {0} record {1}",
                    rt.getStatus(), rt.getId()));
        }

    }

    public void validateApproveStatus(ArrangementException rt) {

        if (rt == null || !StringUtils.hasText(rt.getStatus())) {
            throw new NeoException(null, ArrangementErrorCode.EXCEPTION_REQUEST_INVALID_STATUS, "Invalid request status");
        }

        if (!ArrConstant.MakeCheckStatus.WAITING.toString().equals(rt.getStatus())) {
            throw new NeoException(null, ArrangementErrorCode.EXCEPTION_REQUEST_INVALID_STATUS,
                    CommonUtils.format("Cannot approve an {0} record {1}",
                    rt.getStatus(), rt.getId()));
        }

    }

    public void validateArrangementException(OrderPlacementDTO dto, CustomerDTO customer) {
        ArrangementTypeEnum type = ArrangementTypeEnum.lookForType(dto.getArrangement().getType());
        log.info("Placing order for dto {}", dto);

        // common validations
        arrangementValidator.validatePlacementAction(type);

        if (ArrangementTypeEnum.SELL == type) {
            // create new relations between sell and original sell order
            arrangementValidator.validateSellTradingCode(dto.getArrangement().getSellArrangementCode(),
                    dto.getArrangement().getSellArrangementId());
        }

        // decide role first inside ArrangementRoleDTO.role
        // validate
        DateHelper.validateHoliday(restTemplate,
                DateHelper.formatDateSilently(dto.getArrangement().getTradingDate(),
                        Constant.FORMAT_SQLSERVER_SHORT), infoUrl);
        ProductIssuerWrapper piWrapper = arrangementValidator.validateProductDerivate(dto.getDerivative(),
                dto.getArrangement().getTradingDate()).block();

        if (dto.getArrangement().getType().equals(ArrangementTypeEnum.BUY.getType()))
            arrangementValidator.validateProfessional(customer, piWrapper.getDerivativeDTO().getProdVanilla());

        ProdDerivativeDTO prodDerivativeDTO = piWrapper.getDerivativeDTO();
        ProdAgreementDTO prodAgreementDTO = prodDerivativeDTO.getProdAgreement();
        ProdVanillaDTO prodVanillaDTO = prodDerivativeDTO.getProdVanilla();
        arrangementValidator.updateProductModel(dto, piWrapper);

        if (ArrangementTypeEnum.SELL == type) {
            arrangementValidator.validateIntradayLimit(dto.getArrangement().getTradingDate(),
                    dto.getDerivative().getProdVanilla().getCode(),
                    dto.getIssuerDTO().getCode(), dto.getPricing().getTotalReceivedAmount());
        }

        // now find Listed or OTC or Frozen for further usages
        PropsResp bondStatus = attributeService.findSystemAttribute(AttributeEnum.BOND_STATUS.getVariable(),
                dto.getArrangement().getTradingDate(), prodDerivativeDTO.getCode(), prodVanillaDTO.getCode(),
                prodAgreementDTO.getCode(), type, Constant.NORMAL_CHANNEL, customer.getCustomerType());

        BondType listedStatus = BondType.lookForBondCode(bondStatus.getValue());

        // validate customer stock account
        customerValidator.validateStockAccount(customer, listedStatus);

        Set<String> attributes = new HashSet<>();
        attributes.add(AttributeEnum.MIN_TRADING_AMOUNT.getVariable());
        attributes.add(AttributeEnum.LIMITED_DAY.getVariable());
        attributes.add(AttributeEnum.DEADLINE_PAYMENT.getVariable());
        attributes.add(AttributeEnum.LOCK.getVariable());

        Map<String, PropsResp> attributeMap = attributeService.findSystemAttributes(attributes, dto.getArrangement().getTradingDate(),
                prodDerivativeDTO.getCode(), prodVanillaDTO.getCode(), prodAgreementDTO.getCode(), type, Constant.NORMAL_CHANNEL, customer.getCustomerType());

        arrangementValidator.validateLockStatus(attributeMap.get(AttributeEnum.LOCK.getVariable()));
        arrangementValidator.validateLimitedDays(dto.getArrangement().getTradingDate(), attributeMap.get(AttributeEnum.LIMITED_DAY.getVariable()));
//        arrangementValidator.validateDeadlinePaymentForToday(dto.getArrangement().getTradingDate(), attributeMap.get(AttributeEnum.DEADLINE_PAYMENT.getVariable()));
        arrangementValidator.validateMinTradingAmount(dto.getArrangement().getVolume(), attributeMap.get(AttributeEnum.MIN_TRADING_AMOUNT.getVariable()));

    }
}
