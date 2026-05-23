package com.trestle.reconciler.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class MatchingConfig {

    @Value("${matching.weight.phone}")
    private double phoneWeight;

    @Value("${matching.weight.name}")
    private double nameWeight;

    @Value("${matching.weight.dob}")
    private double dobWeight;

    @Value("${matching.weight.email}")
    private double emailWeight;

    @Value("${matching.weight.address}")
    private double addressWeight;

    @Value("${matching.threshold.high-confidence}")
    private double highConfidenceThreshold;

    @Value("${matching.threshold.review-required}")
    private double reviewRequiredThreshold;
}
