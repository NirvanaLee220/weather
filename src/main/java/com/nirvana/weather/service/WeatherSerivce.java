package com.nirvana.weather.service;

import java.util.Optional;

public interface WeatherSerivce {
    Optional<Integer> doGetTemp(String province, String city, String county);
}
