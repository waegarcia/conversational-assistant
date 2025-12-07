package com.enterprise.assistant.infrastructure.external.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class WeatherResponse {

    private Main main;
    private List<Weather> weather;
    private Wind wind;
    private String name;

    @Data
    public static class Main {
        private Double temp;

        @JsonProperty("feels_like")
        private Double feelsLike;

        private Integer humidity;
    }

    @Data
    public static class Weather {
        private String main;
        private String description;
    }

    @Data
    public static class Wind {
        private Double speed;
    }
}
