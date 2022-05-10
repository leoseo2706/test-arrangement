package com.fiats.arrangement.service;

import com.fiats.tmgcoreutils.payload.AssetDTO;

import java.util.List;

public interface AssetService {

    List<AssetDTO> findAssetByCustomerId(Long customerId, String vanillaCode, String derivativeCode, Boolean outstanding, String condition);
}
