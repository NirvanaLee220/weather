package com.nirvana.weather.service.Impl;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class WeatherSerivceImplTest {

    @Test
    public void getWeather() {
        WeatherSerivceImpl weatherSerivce = new WeatherSerivceImpl();
        try {
            Optional<Integer> integer = weatherSerivce.doGetTemp("江苏", "苏州", "苏州");
            System.out.println(integer.get());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}