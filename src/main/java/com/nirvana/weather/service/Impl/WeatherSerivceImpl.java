package com.nirvana.weather.service.Impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.nirvana.weather.retry.MyRetryTemplate;
import com.nirvana.weather.service.WeatherSerivce;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class WeatherSerivceImpl implements WeatherSerivce {

    //重试次数
    @Value("${weather.retry.time:5}")
    private int retryTime;
    //重试时间
    @Value("${weather.retry.sleep.time:5000}")
    private int sleepTime;

    /**
     * TODO  待完善
     * 每秒事务数超过100，块调用
     *
     * @param province
     * @param city
     * @param county
     */
    private void batchGetTemp(String province, String city, String county) {
        int num = 0;// 并发数 如何获取程序并发数？？？？
        List<Object> reqList = new ArrayList<>(0);
        int batch = (int) Math.ceil((double) num / 100);

        CountDownLatch countDownLatch = new CountDownLatch(batch);

        Lists.partition(reqList, 100).forEach(list -> {
            new Thread() {
                @Override
                public void run() {
                    try {
                        Optional<Integer> temp = doGetTemp(province, city, county);
                    } catch (Exception e) {
                        log.error("get temperature failure...");
                        e.printStackTrace();
                    } finally {
                        countDownLatch.countDown();
                    }
                }
            };
        });

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            log.error("interrupt main thread: {} failure...", "doGetTemp method");
        }
    }

    @Override
    public Optional<Integer> doGetTemp(String province, String city, String county) {
        Object ans = null;
        try {
            ans = new MyRetryTemplate() {
                @Override
                public Optional<Integer> doBiz() throws Exception {
                    Optional<Integer> result = null;
                    try {
                        result = getWeather(province, city, county);
                    } catch (Exception e) {
                        log.error(e.getMessage());
                        throw e;
                    }
                    return result;
                }
            }.setRetryTime(retryTime).setSleepTime(sleepTime).execute();
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }
        return (Optional<Integer>) ans;
    }

    public Optional<Integer> getWeather(String province, String city, String county) {

        String provinceCode = "", cityCode = "", countryCode = "";

        Map<String, Object> provinceMap = getMap("http://www.weather.com.cn/data/city3jdata/china.html");
        if (StringUtils.hasLength(province)) {
            provinceMap = rebuildMap(provinceMap);
            provinceCode = null == provinceMap.get(province) ? provinceCode : provinceMap.get(province).toString();
        }
        if (!StringUtils.hasLength(provinceCode)) {
            return Optional.empty();
        }
        if (StringUtils.hasLength(city) && StringUtils.hasLength(provinceCode)) {
            Map<String, Object> cityMap = getMap("http://www.weather.com.cn/data/city3jdata/provshi/" + provinceCode + ".html");
            cityMap = rebuildMap(cityMap);
            cityCode = null == cityMap.get(city) ? cityCode : cityMap.get(city).toString();
        }
        if (!StringUtils.hasLength(cityCode)) {
            return Optional.empty();
        }
        if (StringUtils.hasLength(county) && StringUtils.hasLength(provinceCode) && StringUtils.hasLength(cityCode)) {
            Map<String, Object> countryMap = getMap("http://www.weather.com.cn/data/city3jdata/station/" + provinceCode + cityCode + ".html");
            countryMap = rebuildMap(countryMap);
            countryCode = null == countryMap.get(county) ? countryCode : countryMap.get(county).toString();
        }
        if (!StringUtils.hasLength(countryCode)) {
            return Optional.empty();
        }
        if (StringUtils.hasLength(provinceCode) && StringUtils.hasLength(cityCode) && StringUtils.hasLength(countryCode)) {
            Map<String, Object> map = getMap("http://www.weather.com.cn/data/sk/" + provinceCode + cityCode + countryCode + ".html");
            JSONObject weatherinfo = JSONObject.parseObject(map.get("weatherinfo").toString());
            float temp = Float.valueOf(weatherinfo.get("temp").toString());
            return Optional.of(Math.round(temp));
        }
        return Optional.empty();
    }

    /**
     * 根据url，获取信息
     *
     * @param urlStr 输入的url
     * @return 具体信息Map<String, String>
     */
    private Map<String, Object> getMap(String urlStr) {
        Map<String, Object> result = new HashMap<>();
        try {
            URL url = new URL(urlStr);
            URLConnection urlConnection = url.openConnection();
            HttpURLConnection connection = (HttpURLConnection) urlConnection;

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String urlString = "";
            String current;
            while ((current = in.readLine()) != null) {
                urlString += current;
            }
            result = (Map) JSON.parse(urlString);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 将map中的k，v位置转换
     *
     * @param source 源数据
     * @return 目标数据
     */
    private Map<String, Object> rebuildMap(Map<String, Object> source) {
        final Map<String, Object> target = new HashMap<>();
        if (CollectionUtils.isEmpty(source)) {
            return target;
        }
        source.forEach((k, v) -> {
            target.put(v.toString(), k.toString());
        });
        return target;
    }
}
