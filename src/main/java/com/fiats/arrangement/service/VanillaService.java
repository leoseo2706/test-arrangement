package com.fiats.arrangement.service;

import com.fiats.tmgcoreutils.payload.ProdVanillaDTO;

public interface VanillaService {
    ProdVanillaDTO findVanillaByCode(String vanillaCode);
}
