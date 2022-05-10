package com.fiats.arrangement.controller;

import com.fiats.arrangement.payload.ArrangementLogDTO;
import com.fiats.arrangement.service.ArrangementLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "/arrangement-log")
public class ArrangementLogController {

    @Autowired
    private ArrangementLogService arrangementLogService;

    @GetMapping("/findByArrangementId/{arrangementId}")
    public List<ArrangementLogDTO> findByArrangementId(@PathVariable("arrangementId") Long arrangementId){

        return arrangementLogService.findAllLogByArrangementId(arrangementId);
    }
}
