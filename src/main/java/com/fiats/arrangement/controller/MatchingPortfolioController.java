package com.fiats.arrangement.controller;

import com.fiats.arrangement.service.PortfolioService;
import com.fiats.tmgcoreutils.payload.OrderPlacementDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author hungd
 * @project NEOBOND SYSTEM
 * @date 4/19/2021 11:20
 * @company TMG Solution
 */

@RestController
@RequestMapping(path = "/portfolio")
@Slf4j
public class MatchingPortfolioController {

    @Autowired
    private PortfolioService portfolioService;

    @GetMapping("/afc/{derivativeCode}")
    public OrderPlacementDTO getLatestAFC(@PathVariable(value = "derivativeCode") String derivativeCode) {
        try {
            return portfolioService.getLatestAfc(derivativeCode);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    @GetMapping("/aft/{vanillaCode}")
    public OrderPlacementDTO getLatestAFT(@PathVariable(value = "vanillaCode") String vanillaCode) {
        try {
            return portfolioService.getLatestAft(vanillaCode);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }
}
