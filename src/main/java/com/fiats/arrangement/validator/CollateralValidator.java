package com.fiats.arrangement.validator;

import com.fiats.arrangement.constant.ArrConstant;
import com.fiats.arrangement.constant.ArrangementErrorCode;
import com.fiats.arrangement.constant.ArrangementTypeEnum;
import com.fiats.arrangement.payload.CollateralDTO;
import com.fiats.arrangement.payload.filter.CollateralFilter;
import com.fiats.arrangement.redis.entity.Collateral;
import com.fiats.arrangement.redis.repo.CollateralRepo;
import com.fiats.arrangement.redis.specs.CollateralSpecs;
import com.fiats.arrangement.service.AssetService;
import com.fiats.arrangement.service.CustomerService;
import com.fiats.arrangement.service.PortfolioService;
import com.fiats.exception.NeoFiatsException;
import com.fiats.exception.ValidationException;
import com.fiats.tmgcoreutils.payload.AssetDTO;
import com.fiats.tmgcoreutils.payload.CustomerDTO;
import com.fiats.tmgcoreutils.payload.PortViewArrangementDTO;
import com.fiats.tmgcoreutils.utils.CommonUtils;
import com.neo.exception.NeoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class CollateralValidator {

    @Autowired
    CollateralRepo collateralRepo;

    @Autowired
    CollateralSpecs collateralSpecs;

    @Autowired
    CustomerService customerService;

    @Autowired
    AssetService assetService;

    @Autowired
    PortfolioService portfolioService;

    public Collateral validateExistence(CollateralDTO dto) throws ValidationException {

        CollateralFilter filter = new CollateralFilter();
        BeanUtils.copyProperties(dto, filter);

        Optional<Collateral> trans = dto != null && dto.getId() != null
                ? collateralRepo.findById(dto.getId())
                : collateralRepo.findOne(collateralSpecs.buildSpecs(filter));

        if (trans.isPresent()) {
            return trans.get();
        }

        return null;
    }

    public Collateral validateExistence(String id) throws NeoFiatsException {

        if (!StringUtils.hasText(id)) {
            return null;
        }

        return validateExistence(CollateralDTO.builder().id(id).build());
    }

    public void validateUpdateStatus(Collateral rt) {

        if (rt == null || !StringUtils.hasText(rt.getStatus())) {
            throw new NeoException(null, ArrangementErrorCode.COLLATERAL_REQUEST_INVALID_STATUS, "Invalid request status");
        }

        if (!ArrConstant.MakeCheckStatus.DRAFT.toString().equals(rt.getStatus())
                && !ArrConstant.MakeCheckStatus.REJECTED.toString().equals(rt.getStatus())) {
            throw new NeoException(null, ArrangementErrorCode.COLLATERAL_REQUEST_INVALID_STATUS,
                    CommonUtils.format("Cannot update the record {0}" +
                    " with this invalid status {1}", rt.getId(), rt.getStatus()));
        }

    }

    public void validateSendApproveStatus(Collateral rt) {

        if (rt == null || !StringUtils.hasText(rt.getStatus())) {
            throw new NeoException(null, ArrangementErrorCode.COLLATERAL_REQUEST_INVALID_STATUS, "Invalid request status");
        }

        if (!ArrConstant.MakeCheckStatus.DRAFT.toString().equals(rt.getStatus())
                && !ArrConstant.MakeCheckStatus.REJECTED.toString().equals(rt.getStatus())) {
            throw new NeoException(null, ArrangementErrorCode.COLLATERAL_REQUEST_INVALID_STATUS,
                    CommonUtils.format("Cannot reject the record {0}" +
                            " with this invalid status {1}", rt.getId(), rt.getStatus()));
        }

    }

    public void validateRejectStatus(Collateral rt) {

        if (rt == null || !StringUtils.hasText(rt.getStatus())) {
            throw new NeoException(null, ArrangementErrorCode.COLLATERAL_REQUEST_INVALID_STATUS, "Invalid request status");
        }

        if (!ArrConstant.MakeCheckStatus.WAITING.toString().equals(rt.getStatus())) {
            throw new NeoException(null, ArrangementErrorCode.COLLATERAL_REQUEST_INVALID_STATUS,
                    CommonUtils.format("Cannot reject the record {0}" +
                    " with this invalid status {1}", rt.getId(), rt.getStatus()));
        }

    }

    public void validateRemoveStatus(Collateral rt) {

        if (rt == null || !StringUtils.hasText(rt.getStatus())) {
            throw new NeoException(null, ArrangementErrorCode.COLLATERAL_REQUEST_INVALID_STATUS, "Invalid request status");
        }

        if (ArrConstant.MakeCheckStatus.APPROVED.toString().equals(rt.getStatus())
                || ArrConstant.MakeCheckStatus.OUTDATED.toString().equals(rt.getStatus())) {
            throw new NeoException(null, ArrangementErrorCode.COLLATERAL_REQUEST_INVALID_STATUS,
                    CommonUtils.format("Cannot delete an {0} record {1}",
                    rt.getStatus(), rt.getId()));
        }

    }

    public void validateApproveStatus(Collateral rt) {

        if (rt == null || !StringUtils.hasText(rt.getStatus())) {
            throw new NeoException(null, ArrangementErrorCode.COLLATERAL_REQUEST_INVALID_STATUS, "Invalid request status");
        }

        if (!ArrConstant.MakeCheckStatus.WAITING.toString().equals(rt.getStatus())) {
            throw new NeoException(null, ArrangementErrorCode.COLLATERAL_REQUEST_INVALID_STATUS,
                    CommonUtils.format("Cannot approve an {0} record {1}", rt.getStatus(), rt.getId()));
        }

    }

    public void validateType(Integer type) {

        if (type == null) {
            throw new NeoException(null, ArrangementErrorCode.INFORMATION_REQUIRED, new StringBuilder("Arrangement type is required"));
        }

        if (ArrangementTypeEnum.COLLATERAL.getType() != type && ArrangementTypeEnum.RELEASE.getType() != type) {
            throw new NeoException(null, ArrangementErrorCode.ARRANGEMENT_TYPE_NOT_SUPPORT, new StringBuilder("Arrangement type is not supported"));
        }

    }

    public CustomerDTO validateCustomer(Long partyId) {
        if (partyId == null) {
            throw new NeoException(null, ArrangementErrorCode.INFORMATION_REQUIRED, new StringBuilder("Customer info is required"));
        }

        List<CustomerDTO> party = customerService.retrieveCustomerInfo(Arrays.asList(partyId));
        if (!CollectionUtils.isEmpty(party)) {
            return party.get(0);
        } else {
            throw new NeoException(null, ArrangementErrorCode.CUSTOMER_NOT_FOUND, new StringBuilder("Customer not found"));
        }
    }

    public AssetDTO validateAsset(CollateralDTO dto) {

        log.info("Start verify assert for request collateral: {}", dto);

        if (dto.getAssetId() == null) {
            throw new NeoException(null, ArrangementErrorCode.INFORMATION_REQUIRED, new StringBuilder("Asset info is required"));
        }

        List<AssetDTO> assets = assetService.findAssetByCustomerId(dto.getPartyId(), null, null, true, ArrConstant.AssetFilterCondition.ALL.content());
        Optional<AssetDTO> asset = assets.stream().filter(a -> a.getId().equals(dto.getAssetId())).findAny();
        if (!asset.isPresent()) {
            throw new NeoException(null, ArrangementErrorCode.ASSET_UNAVAILABLE, "Asset not available for sell");
        } else {
            log.info("Assert verify: {}", asset.get());

            if (dto.getType().equals(ArrangementTypeEnum.COLLATERAL.getType()) &&
                    asset.get().getAvailableVolume().intValue() < dto.getMortgageVolume()) {
                throw new NeoException(null, ArrangementErrorCode.ASSET_NOT_ENOUGH, "Asset not enough");
            }

            if (dto.getType().equals(ArrangementTypeEnum.RELEASE.getType()) &&
                    asset.get().getMortgageVolume().intValue() < dto.getMortgageVolume()) {
                throw new NeoException(null, ArrangementErrorCode.ASSET_NOT_ENOUGH, "Asset not enough");
            }

            return asset.get();
        }
    }

}
