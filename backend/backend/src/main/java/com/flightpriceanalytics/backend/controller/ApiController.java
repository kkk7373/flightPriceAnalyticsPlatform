package com.flightpriceanalytics.backend.controller;

import org.springframework.web.bind.annotation.RestController;

import com.flightpriceanalytics.backend.dto.InitResponse;
import com.flightpriceanalytics.backend.service.init.InitDashboardService;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.web.bind.annotation.GetMapping;


@RestController 
public class ApiController {
    private final InitDashboardService initDashboardService;

    public ApiController(InitDashboardService initDashboardService){
        this.initDashboardService = initDashboardService;
    }
    
    @GetMapping("api/init")
    public InitResponse init() throws IOException, InterruptedException{
        return initDashboardService.initDashboard(LocalDate.now(ZoneId.of("Asia/Tokyo")));
    }
    

}
