package com.fiats.arrangement.service.impl;

import com.fiats.arrangement.constant.ArrConstant;
import com.fiats.arrangement.payload.CollateralDTO;
import com.fiats.arrangement.payload.filter.CollateralFilter;
import com.fiats.arrangement.redis.entity.Collateral;
import com.fiats.arrangement.redis.repo.CollateralRepo;
import com.fiats.arrangement.redis.specs.CollateralSpecs;
import com.fiats.arrangement.service.*;
import com.fiats.arrangement.validator.CollateralValidator;
import com.fiats.tmgcoreutils.constant.Constant;
import com.fiats.tmgcoreutils.payload.AssetDTO;
import com.fiats.tmgcoreutils.payload.CustomerDTO;
import com.fiats.tmgcoreutils.payload.OrderPlacementDTO;
import com.fiats.tmgcoreutils.utils.CommonUtils;
import com.fiats.tmgcoreutils.utils.DateHelper;
import com.fiats.tmgjpa.paging.PagingFilterBase;
import com.fiats.tmgjpa.payload.ResponseMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CollateralServiceImpl implements CollateralService {

    @Autowired
    CollateralSpecs collateralSpecs;

    @Autowired
    CollateralService collateralService;

    @Autowired
    CollateralRepo collateralRepo;

    @Autowired
    CustomerService customerService;

    @Autowired
    AssetService assetService;

    @Autowired
    ArrangementService arrangementService;

    @Autowired
    PortfolioService portfolioService;

    @Autowired
    CollateralValidator collateralValidator;

    @Override
    public ResponseMessage retrieveMortgageRequest(PagingFilterBase<CollateralFilter> pf) {

        List<Collateral> result = null;
        if (!pf.isPageable()) {
            result = collateralRepo.findAll(collateralSpecs.buildSpecs(pf.getFilter()));
        } else {
            Page<Collateral> page = collateralRepo.findAll(collateralSpecs.buildSpecs(pf.getFilter()),
                    PageRequest.of(pf.getPageNum(), pf.getPageSize()));
            result = page.getContent();
            pf.getPaging().setTotalPages(page.getTotalPages());
            pf.getPaging().setTotalRecords(page.getTotalElements());
        }

        if (!CollectionUtils.isEmpty(result)) {
            List<CollateralDTO> convertData = result.stream().map(a -> {
                CollateralDTO item = new CollateralDTO();
                BeanUtils.copyProperties(a, item);
                item.setCollateralDate(StringUtils.hasText(a.getCollateralDate()) ? DateHelper.parseTimestamp(a.getCollateralDate()) : null);
                return item;
            }).collect(Collectors.toList());

            return new ResponseMessage<>(convertData, pf.getPaging());
        }

        return new ResponseMessage<>(Collections.emptyList(), pf.getPaging());
    }

    @Override
    public CollateralDTO insertMortgageRequest(CollateralDTO dto) {

        collateralValidator.validateType(dto.getType());

        Collateral data = new Collateral();
        BeanUtils.copyProperties(dto, data, CommonUtils.buildIgnoreAndNullPropsArray(dto, "id", "status"));
        data.setCollateralDate(DateHelper.formatDateSilently(dto.getCollateralDate()));
        buildCollateralData(data, dto);

        data.setStatus(ArrConstant.MakeCheckStatus.DRAFT.content());
        data = collateralRepo.save(data);

        CollateralDTO result = new CollateralDTO();
        BeanUtils.copyProperties(data, result);
        result.setCollateralDate(StringUtils.hasText(data.getCollateralDate()) ? DateHelper.parseTimestamp(data.getCollateralDate()) : null);

        return result;
    }

    @Override
    public CollateralDTO updateMortgageRequest(CollateralDTO dto) {

        Collateral rt = collateralValidator.validateExistence(dto);
        collateralValidator.validateUpdateStatus(rt);
        BeanUtils.copyProperties(dto, rt, CommonUtils.buildIgnoreAndNullPropsArray(dto, "id", "status"));
        rt.setCollateralDate(DateHelper.formatDateSilently(dto.getCollateralDate()));
        buildCollateralData(rt, dto);

        rt.setStatus(ArrConstant.MakeCheckStatus.DRAFT.content());
        rt = collateralRepo.save(rt);

        CollateralDTO result = new CollateralDTO();
        BeanUtils.copyProperties(rt, result);
        result.setCollateralDate(StringUtils.hasText(rt.getCollateralDate()) ? DateHelper.parseTimestamp(rt.getCollateralDate()) : null);

        return result;
    }

    @Override
    public CollateralDTO sendApproveMortgageRequest(String id) {

        Collateral rt = collateralValidator.validateExistence(id);
        collateralValidator.validateSendApproveStatus(rt);
        rt.setStatus(ArrConstant.MakeCheckStatus.WAITING.content());
        rt = collateralRepo.save(rt);

        CollateralDTO result = new CollateralDTO();
        BeanUtils.copyProperties(rt, result);
        result.setCollateralDate(StringUtils.hasText(rt.getCollateralDate()) ? DateHelper.parseTimestamp(rt.getCollateralDate()) : null);

        return result;
    }

    @Override
    public CollateralDTO rejectMortgageRequest(String id) {

        Collateral rt = collateralValidator.validateExistence(id);
        collateralValidator.validateRejectStatus(rt);
        rt.setStatus(ArrConstant.MakeCheckStatus.REJECTED.content());
        rt = collateralRepo.save(rt);

        CollateralDTO result = new CollateralDTO();
        BeanUtils.copyProperties(rt, result);
        result.setCollateralDate(StringUtils.hasText(rt.getCollateralDate()) ? DateHelper.parseTimestamp(rt.getCollateralDate()) : null);

        return result;
    }

    @Override
    public Object removeMortgageRequest(String id) {

        Collateral rt = collateralValidator.validateExistence(id);
        collateralValidator.validateRemoveStatus(rt);
        collateralRepo.delete(rt);

        return Constant.SUCCESS;
    }

    @Override
    public OrderPlacementDTO approveMortgageRequest(String id) {

        Collateral rt = collateralValidator.validateExistence(id);
        collateralValidator.validateApproveStatus(rt);

        CollateralDTO dto = new CollateralDTO();
        BeanUtils.copyProperties(rt, dto, CommonUtils.buildIgnoreAndNullPropsArray("id"));
        dto.setCollateralDate(StringUtils.hasText(rt.getCollateralDate()) ? DateHelper.parseTimestamp(rt.getCollateralDate()) : null);

        collateralValidator.validateCustomer(dto.getPartyId());
        collateralValidator.validateAsset(dto);

        //Integrate service create arrangement
        OrderPlacementDTO result = arrangementService.placeCollateral(dto);

        collateralRepo.delete(rt);

        return result;
    }

    private void buildCollateralData(Collateral data, CollateralDTO dto) {
        CustomerDTO customer = collateralValidator.validateCustomer(dto.getPartyId());
        data.setPartyAccount(customer.getAccount());
        data.setPartyName(customer.getName());
        data.setPartyIdentity(customer.getIdCard());
        data.setPartyStockAccount(customer.getStockAccount());

        dto.setPartyAccount(customer.getAccount());

        AssetDTO asset = collateralValidator.validateAsset(dto);
        data.setAssetCode(asset.getCode());
        data.setVolume(asset.getVolume());
        data.setPrice(asset.getPrice());
        data.setDerivativeId(asset.getProductDerivativeId());
        data.setDerivativeCode(asset.getProductDerivativeCode());
        data.setVanillaId(asset.getProductVanillaId());
        data.setVanillaCode(asset.getProductVanillaCode());
    }
}