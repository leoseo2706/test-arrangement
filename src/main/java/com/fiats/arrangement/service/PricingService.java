package com.fiats.arrangement.service;

import com.fiats.arrangement.payload.PricingParamDTO;
import com.fiats.tmgcoreutils.payload.PricingBondResultDTO;

import java.util.List;

public interface PricingService {

    List<PricingBondResultDTO> calculatePricingBond(PricingParamDTO paramDTO);

}
