package com.fiats.arrangement.service.impl;

import com.fiats.arrangement.constant.ArrConstant;
import com.fiats.arrangement.constant.ArrangementErrorCode;
import com.fiats.arrangement.jpa.entity.Arrangement;
import com.fiats.arrangement.jpa.entity.ArrangementPricing;
import com.fiats.arrangement.jpa.repo.ArrangementRepo;
import com.fiats.arrangement.service.AssetService;
import com.fiats.arrangement.service.CustomerService;
import com.fiats.arrangement.service.PortfolioService;
import com.fiats.tmgcoreutils.payload.AssetDTO;
import com.fiats.tmgcoreutils.payload.CustomerDTO;
import com.fiats.tmgcoreutils.payload.OrderPlacementDTO;
import com.fiats.tmgcoreutils.payload.PortViewArrangementDTO;
import com.fiats.tmgcoreutils.utils.CommonUtils;
import com.neo.exception.NeoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AssetServiceImpl implements AssetService {

    @Autowired
    ArrangementRepo arrangementRepo;

    @Autowired
    PortfolioService portfolioService;

    @Autowired
    CustomerService customerService;

    @Override
    public List<AssetDTO> findAssetByCustomerId(Long customerId, String vanillaCode, String derivativeCode, Boolean outstanding, String condition) {
        CustomerDTO customer = customerService.retrieveCustomerInfo(customerId);

        if (customer != null) {
            List<Arrangement> data = arrangementRepo.findAssetByCustomerId(customerId, vanillaCode, derivativeCode);
            List<PortViewArrangementDTO> dataPortfolio = portfolioService.getPortfolioByAccountAndAsset(customer.getAccount(), customer.getAccount(), "");

            List<AssetDTO> result = new ArrayList<>();

            Map<String, PortViewArrangementDTO> asset = dataPortfolio.stream().collect(Collectors.toMap(a -> a.getAssetCode(), Function.identity(), (o, n) -> n));

            Comparator<AssetDTO> sortByTradingDate = Comparator.comparing((AssetDTO p) -> p.getTradingDate())
                    .thenComparing(p -> p.getCreatedDate());

            result.addAll(data.stream().filter(a -> asset.get(a.getCode()) != null
                    && (outstanding || asset.get(a.getCode()).getAvailable().intValue() > 0)
                    && (condition.equalsIgnoreCase(ArrConstant.AssetFilterCondition.ALL.content()) ||
                        (condition.equalsIgnoreCase(ArrConstant.AssetFilterCondition.AVAILABLE.content()) && asset.get(a.getCode()).getAvailable().intValue() > 0) ||
                        (condition.equalsIgnoreCase(ArrConstant.AssetFilterCondition.HOLD.content()) && asset.get(a.getCode()).getHold().intValue() > 0) ||
                        (condition.equalsIgnoreCase(ArrConstant.AssetFilterCondition.BLOCK.content()) && asset.get(a.getCode()).getBlock().intValue() > 0))
            ).map(a -> {
                AssetDTO item = new AssetDTO();
                BeanUtils.copyProperties(a, item);

                item.setVolume(asset.get(a.getCode()).getTotal().intValue());
                item.setAvailableVolume(asset.get(a.getCode()).getAvailable().intValue());
                item.setHoldVolume(asset.get(a.getCode()).getHold().intValue());
                item.setMortgageVolume(asset.get(a.getCode()).getBlock().intValue());

                ArrangementPricing arrPricing = a.getPricing();

                item.setPrice(arrPricing.getPrice());
                item.setPrincipal(arrPricing.getPrincipal());
                item.setRate(arrPricing.getRate());
                item.setUnitPrice(arrPricing.getUnitPrice());

                return item;
            }).sorted(sortByTradingDate.reversed()).collect(Collectors.toList()));

            return result;
        } else {
            throw new NeoException(null, ArrangementErrorCode.CUSTOMER_NOT_FOUND,
                    CommonUtils.format("Customer id {0} is not found", customerId));
        }
    }
}