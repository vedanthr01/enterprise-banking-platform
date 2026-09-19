package com.bankingplatform.common.event;

/** Rule-based fraud checks run by Fraud Detection Service against each
 *  transaction — deliberately simple, explainable rules rather than a
 *  black-box ML model (see Fraud Detection Service's README notes). */
public enum FraudRuleType {
    VELOCITY_CHECK,
    LARGE_AMOUNT,
    NEW_BENEFICIARY
}
