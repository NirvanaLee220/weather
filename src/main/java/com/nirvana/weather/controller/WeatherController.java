package com.nirvana.weather.controller;

import com.nirvana.weather.service.WeatherSerivce;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping(value = "/weather")
public class WeatherController {

    @Autowired
    private WeatherSerivce weatherSerivce;

    @GetMapping(value = "/temperature")
    public Optional<Integer> getTemperature(@RequestParam String province, @RequestParam String city, @RequestParam String county) {
        Optional<Integer> temp = weatherSerivce.doGetTemp(province, city, county);
        return temp;
    }

}
