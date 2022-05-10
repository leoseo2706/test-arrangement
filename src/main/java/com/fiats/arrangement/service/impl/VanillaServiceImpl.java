package com.fiats.arrangement.service.impl;

import com.fiats.arrangement.service.VanillaService;
import com.fiats.tmgcoreutils.payload.ProdVanillaDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class VanillaServiceImpl implements VanillaService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${fiats.internal.api.product.url}")
    String productUrl;

    @Override
    public ProdVanillaDTO findVanillaByCode(String vanillaCode) {
        StringBuilder uri = new StringBuilder(productUrl).append("/prod-vanilla/findByCode");

        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(uri.toString())
                .queryParam("vc", vanillaCode);
        log.info("Request to: {}", uriComponentsBuilder.toUriString());
        ResponseEntity<ProdVanillaDTO> prodVanillaDTO = restTemplate.getForEntity(uriComponentsBuilder.toUriString(), ProdVanillaDTO.class);
        log.debug("Prod vanilla: {}", prodVanillaDTO);
        return prodVanillaDTO.getBody();
    }
}
