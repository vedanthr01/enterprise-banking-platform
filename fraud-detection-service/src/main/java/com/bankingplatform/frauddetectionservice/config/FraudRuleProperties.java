package com.bankingplatform.frauddetectionservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * The full set of tunable fraud-rule thresholds, bound from
 * fraud.rules.* — deliberately grouped into one properties class (rather
 * than individual @Value fields, used elsewhere in this platform) so the
 * whole rule configuration can be exposed as-is through
 * GET /fraud/rules/config. That endpoint matters for the same reason the
 * rules are hand-written if/else logic rather than a trained model: a
 * fraud analyst — or an auditor — can see exactly what thresholds are
 * active on a given environment right now, not just read the source code
 * and hope it matches what's actually deployed.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "fraud.rules")
public class FraudRuleProperties {

    /** A completed transfer above this amount is flagged as high-risk on its own. */
    private BigDecimal largeAmountThreshold = new BigDecimal("200000");

    /** Rolling window, in minutes, that the velocity check counts transfers over. */
    private int velocityWindowMinutes = 10;

    /** More than this many transfers from one account within the window triggers a flag. */
    private int velocityMaxTransfers = 5;
}
