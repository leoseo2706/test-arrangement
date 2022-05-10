package com.fiats.arrangement.service.impl;

import com.fiats.arrangement.constant.ArrConstant;
import com.fiats.arrangement.constant.ArrangementErrorCode;
import com.fiats.arrangement.constant.ArrangementTypeEnum;
import com.fiats.arrangement.payload.filter.ArrangementExceptionFilter;
import com.fiats.arrangement.redis.entity.ArrangementException;
import com.fiats.arrangement.redis.repo.ArrangementExceptionRepo;
import com.fiats.arrangement.redis.specs.ArrangementExceptionSpecs;
import com.fiats.arrangement.service.*;
import com.fiats.arrangement.validator.ArrangementExceptionValidator;
import com.fiats.arrangement.validator.ArrangementValidator;
import com.fiats.tmgcoreutils.constant.Constant;
import com.fiats.tmgcoreutils.payload.*;
import com.fiats.tmgcoreutils.utils.CommonUtils;
import com.fiats.tmgcoreutils.utils.DateHelper;
import com.fiats.tmgjpa.entity.RecordStatus;
import com.fiats.tmgjpa.paging.PagingFilterBase;
import com.fiats.tmgjpa.payload.ResponseMessage;
import com.neo.exception.NeoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ArrangementExceptionServiceImpl implements ArrangementExceptionService {

    @Autowired
    ArrangementExceptionRepo arrangementExceptionRepo;

    @Autowired
    ArrangementExceptionSpecs arrangementExceptionSpecs;

    @Autowired
    ArrangementExceptionValidator arrangementExceptionValidator;

    @Autowired
    ArrangementCodeService arrangementCodeService;

    @Autowired
    ProductDerivativeService productDerivativeService;

    @Autowired
    AssetService assetService;

    @Autowired
    InfoService infoService;

    @Autowired
    CustomerService customerService;

    @Autowired
    ArrangementService arrangementService;

    @Autowired
    BrokerService brokerService;

    @Autowired
    ArrangementValidator arrangementValidator;

    @Value("${organization.prefix}")
    private String organizePrefix;

    @Override
    public ResponseMessage retrieveExceptionRequest(PagingFilterBase<ArrangementExceptionFilter> pf) {

        List<ArrangementException> data = arrangementExceptionRepo.findAll(arrangementExceptionSpecs.buildArrangementExceptionSpecs(pf.getFilter()));
        if (!CollectionUtils.isEmpty(data)) {
            List<ArrangementExceptionDTO> convertData = data.stream().map(a -> {
                ArrangementExceptionDTO item = new ArrangementExceptionDTO();
                BeanUtils.copyProperties(a, item);
                item.setTradingDate(StringUtils.hasText(a.getTradingDate()) ? DateHelper.parseTimestamp(a.getTradingDate()) : null);
                item.setMatchingDate(StringUtils.hasText(a.getMatchingDate()) ? DateHelper.parseTimestamp(a.getMatchingDate()) : null);
                item.setDeliveryDate(StringUtils.hasText(a.getDeliveryDate()) ? DateHelper.parseTimestamp(a.getDeliveryDate()) : null);
                return item;
            }).filter(a -> (pf.getFilter().getFromDate() == null ||
                    a.getTradingDate().after(pf.getFilter().getFromDate()) ||
                    a.getTradingDate().equals(pf.getFilter().getFromDate()))
                    && (pf.getFilter().getToDate() == null ||
                    a.getTradingDate().before(pf.getFilter().getToDate()) ||
                    a.getTradingDate().equals(pf.getFilter().getToDate()))
            ).filter(a -> !StringUtils.hasText(pf.getFilter().getArrangementCode()) ||
                    a.getCode().toUpperCase().contains(pf.getFilter().getArrangementCode().toUpperCase())
            ).filter(a -> CollectionUtils.isEmpty(pf.getFilter().getDerivativeId()) ||
                    pf.getFilter().getDerivativeId().contains(a.getProductDerivativeId())).collect(Collectors.toList());

            if (!pf.isPageable()) {
                return new ResponseMessage<>(convertData, pf.getPaging());
            } else {
//                Pageable pageable = PageRequest.of(pf.getPageNum(), pf.getPageSize());
//                Page<ArrangementExceptionDTO> result =
//                        new PageImpl<>(convertData, pageable, convertData.size());
                int numPages = (int) Math.ceil((double)convertData.size() / (double)pf.getPageSize());

                pf.getPaging().setTotalRecords((long) convertData.size());
                pf.getPaging().setTotalPages(numPages);

                int fromIndex = pf.getPageNum() * pf.getPageSize();

                List<ArrangementExceptionDTO> result = convertData.subList(fromIndex, Math.min(fromIndex + pf.getPageSize(), convertData.size()));

                return new ResponseMessage<>(result, pf.getPaging());
            }
        }

        return new ResponseMessage<>(Collections.emptyList(), pf.getPaging());
    }

    @Override
    public ArrangementExceptionDTO insertArrangementException(ArrangementExceptionDTO dto) {

        log.info("Start create new request arrangement exception: {}", dto);

        ArrangementException data = new ArrangementException();
        BeanUtils.copyProperties(dto, data, CommonUtils.buildIgnoreAndNullPropsArray(dto, "id", "status"));

        buildArrangementException(data, dto);

        data.setStatus(ArrConstant.MakeCheckStatus.DRAFT.content());
        data = arrangementExceptionRepo.save(data);

        ArrangementExceptionDTO result = new ArrangementExceptionDTO();
        BeanUtils.copyProperties(data, result);

        if (data.getTradingDate() != null)
            result.setTradingDate(DateHelper.parseTimestamp(data.getTradingDate()));
        if (data.getMatchingDate() != null)
            result.setMatchingDate(DateHelper.parseTimestamp(data.getMatchingDate()));
        if (data.getDeliveryDate() != null)
            result.setDeliveryDate(DateHelper.parseTimestamp(data.getDeliveryDate()));

        log.info("Finish save arrangement exception data: {}", result);

        return result;
    }

    @Override
    public ArrangementExceptionDTO updateArrangementException(ArrangementExceptionDTO dto) {

        ArrangementException data = arrangementExceptionValidator.validateExistence(dto);
        arrangementExceptionValidator.validateUpdateStatus(data);
        BeanUtils.copyProperties(dto, data, CommonUtils.buildIgnoreAndNullPropsArray(dto, "id", "status"));
        buildArrangementException(data, dto);

        data.setStatus(ArrConstant.MakeCheckStatus.DRAFT.content());
        data = arrangementExceptionRepo.save(data);

        ArrangementExceptionDTO result = new ArrangementExceptionDTO();
        BeanUtils.copyProperties(data, result);

        if (data.getTradingDate() != null)
            result.setTradingDate(DateHelper.parseTimestamp(data.getTradingDate()));
        if (data.getMatchingDate() != null)
            result.setMatchingDate(DateHelper.parseTimestamp(data.getMatchingDate()));
        if (data.getDeliveryDate() != null)
            result.setDeliveryDate(DateHelper.parseTimestamp(data.getDeliveryDate()));

        return result;
    }

    @Override
    public ArrangementExceptionDTO sendApproveArrangementException(String id) {

        ArrangementException rt = arrangementExceptionValidator.validateExistence(id);
        arrangementExceptionValidator.validateSendApproveStatus(rt);
        rt.setStatus(ArrConstant.MakeCheckStatus.WAITING.content());
        rt = arrangementExceptionRepo.save(rt);

        ArrangementExceptionDTO result = new ArrangementExceptionDTO();
        BeanUtils.copyProperties(rt, result);

        if (rt.getTradingDate() != null)
            result.setTradingDate(DateHelper.parseTimestamp(rt.getTradingDate()));
        if (rt.getMatchingDate() != null)
            result.setMatchingDate(DateHelper.parseTimestamp(rt.getMatchingDate()));
        if (rt.getDeliveryDate() != null)
            result.setDeliveryDate(DateHelper.parseTimestamp(rt.getDeliveryDate()));

        return result;
    }

    @Override
    public ArrangementExceptionDTO rejectArrangementException(String id) {

        ArrangementException rt = arrangementExceptionValidator.validateExistence(id);
        arrangementExceptionValidator.validateRejectStatus(rt);
        rt.setStatus(ArrConstant.MakeCheckStatus.REJECTED.content());
        rt = arrangementExceptionRepo.save(rt);

        ArrangementExceptionDTO result = new ArrangementExceptionDTO();
        BeanUtils.copyProperties(rt, result);

        if (rt.getTradingDate() != null)
            result.setTradingDate(DateHelper.parseTimestamp(rt.getTradingDate()));
        if (rt.getMatchingDate() != null)
            result.setMatchingDate(DateHelper.parseTimestamp(rt.getMatchingDate()));
        if (rt.getDeliveryDate() != null)
            result.setDeliveryDate(DateHelper.parseTimestamp(rt.getDeliveryDate()));

        return result;
    }

    @Override
    public Object removeArrangementException(String id) {

        ArrangementException rt = arrangementExceptionValidator.validateExistence(id);
        arrangementExceptionValidator.validateRemoveStatus(rt);
        arrangementExceptionRepo.delete(rt);

        return Constant.SUCCESS;
    }

    @Override
    public OrderPlacementDTO approveArrangementException(String id) {

        ArrangementException rt = arrangementExceptionValidator.validateExistence(id);
        arrangementExceptionValidator.validateApproveStatus(rt);

        OrderPlacementDTO result = buildOrderPlacementDto(rt);

        result = arrangementService.placeOrderException(result);

        arrangementExceptionRepo.delete(rt);

        return result;
    }

    private void buildArrangementException(ArrangementException data, ArrangementExceptionDTO dto) {
        CustomerDTO cus = null;
        if (dto.getCustomerId() != null) {
            List<CustomerDTO> customer = customerService.retrieveCustomerInfo(Arrays.asList(dto.getCustomerId()));
            if (!CollectionUtils.isEmpty(customer)) {
                cus = customer.get(0);
                data.setCustomerId(customer.get(0).getId());
                data.setPartyName(customer.get(0).getName());
                data.setPartyIdentity(customer.get(0).getIdCard());
                data.setPartyStockAccount(customer.get(0).getStockAccount());
            } else {
                throw new NeoException(null, ArrangementErrorCode.CUSTOMER_NOT_FOUND, "Customer not available");
            }
        }

        String agencyCode = organizePrefix;
        if (dto.getBrokerId() != null) {
            BrokerDTO broker = infoService.retrieveBrokerInfo(dto.getBrokerId());
            if (broker != null) {
                if (data.getCustomerId().intValue() == broker.getCustomerId().intValue()) {
                    throw new NeoException(null, ArrangementErrorCode.SELF_REFER_VIOLATED, "Can't self-refer");
                }

                data.setBrokerName(broker.getName());
                data.setBrokerId(broker.getId());
                agencyCode = broker.getAgency() != null ? broker.getAgency().getCode() : organizePrefix;
            } else {
                throw new NeoException(null, ArrangementErrorCode.BROKER_NOT_FOUND, "Broker not available");
            }
        }

        ProdDerivativeDTO product = productDerivativeService.findByID(dto.getProductDerivativeId()).block();
        if (product == null) {
            throw new NeoException(null, ArrangementErrorCode.PRODUCT_UNAVAILABLE, "Product not available");
        }

        data.setProductDerivativeId(product.getId());
        data.setProductDerivativeCode(product.getCode());

        if (dto.getAssetId() != null) {
            List<AssetDTO> assets = assetService.findAssetByCustomerId(dto.getCustomerId(), null, product.getCode(), false, ArrConstant.AssetFilterCondition.ALL.content());
            Optional<AssetDTO> asset = assets.stream().filter(a -> a.getId().equals(dto.getAssetId())).findAny();
            if (!asset.isPresent()) {
                throw new NeoException(null, ArrangementErrorCode.ASSET_PORTFOLIO_UNAVAILABLE, "Asset not available for sell");
            }
            data.setAssetId(asset.get().getId());
            data.setAssetCode(asset.get().getCode());
        }

        data.setCode(arrangementCodeService.generateTradingCode(agencyCode, product.getCode()));
        if (dto.getTradingDate() != null)
            data.setTradingDate(DateHelper.formatDateSilently(dto.getTradingDate().toLocalDateTime().toLocalDate(), Constant.FORMAT_SQLSERVER_SHORT));
        if (dto.getMatchingDate() != null)
            data.setMatchingDate(DateHelper.formatDateSilently(dto.getMatchingDate().toLocalDateTime().toLocalDate(), Constant.FORMAT_SQLSERVER_SHORT));
        if (dto.getDeliveryDate() != null)
            data.setDeliveryDate(DateHelper.formatDateSilently(dto.getDeliveryDate().toLocalDateTime().toLocalDate(), Constant.FORMAT_SQLSERVER_SHORT));

        OrderPlacementDTO orderDto = buildOrderPlacementDto(data);
        arrangementExceptionValidator.validateArrangementException(orderDto, cus);
    }

    private OrderPlacementDTO buildOrderPlacementDto(ArrangementException rt) {
        OrderPlacementDTO result = new OrderPlacementDTO();

        ArrangementDTO arrDTO = new ArrangementDTO();
        arrDTO.setException(1);
        arrDTO.setCode(rt.getCode());
        arrDTO.setType(rt.getType());
        arrDTO.setTradingDate(DateHelper.parseTimestamp(rt.getTradingDate()));
        arrDTO.setVolume(rt.getVolume());
        result.setArrangement(arrDTO);
        if (rt.getAssetId() != null && StringUtils.hasText(rt.getAssetCode())) {
            arrDTO.setSellArrangementCode(rt.getAssetCode());
            arrDTO.setSellArrangementId(rt.getAssetId());
        }

        ArrangementPartyDTO partyDTO = new ArrangementPartyDTO();
        partyDTO.setCustomerId(rt.getCustomerId());

        if (rt.getBrokerId() != null) {
            BrokerDTO broker = brokerService.getBrokerById(rt.getBrokerId());
            if (broker != null) {
                partyDTO.setBroker(broker);
                arrDTO.setAgencyId(broker.getAgencyId());
                arrDTO.setAgencyCode(broker.getAgency().getCode());
            }
        }
        result.setParty(partyDTO);

        ProdDerivativeDTO derivativeDTO = new ProdDerivativeDTO();
        derivativeDTO.setCode(rt.getProductDerivativeCode());
        derivativeDTO.setId(rt.getProductDerivativeId());
        result.setDerivative(derivativeDTO);

        ArrangementPricingDTO pricingDTO = new ArrangementPricingDTO();
        pricingDTO.setPrice(rt.getPrice());
        pricingDTO.setUnitPrice(rt.getUnitPrice());
        if (rt.getType().equals(ArrangementTypeEnum.BUY.getType()))
            pricingDTO.setTotalInvestAmount(rt.getPrincipal());
        else if (rt.getType().equals(ArrangementTypeEnum.SELL.getType()))
            pricingDTO.setTotalReceivedAmount(rt.getPrincipal());
        pricingDTO.setRate(rt.getRate());
        pricingDTO.setFee(rt.getTransactionFee());
        result.setPricing(pricingDTO);

        ArrangementOperationDTO operationDTO = new ArrangementOperationDTO();
        operationDTO.setCustomerStatus(RecordStatus.ACTIVE.getStatus());
        operationDTO.setCustomerStatusDate(DateHelper.parseTimestamp(rt.getTradingDate()));
        result.setOperation(operationDTO);

        return result;
    }
}