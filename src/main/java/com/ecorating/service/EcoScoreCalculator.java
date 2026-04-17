package com.ecorating.service;

import org.springframework.stereotype.Component;

@Component
public class EcoScoreCalculator {

    public double calculate(double airIndex, double greenIndex, double hazardIndex) {
        return (airIndex * 0.40) + (greenIndex * 0.35) + (hazardIndex * 0.25);
    }
}
