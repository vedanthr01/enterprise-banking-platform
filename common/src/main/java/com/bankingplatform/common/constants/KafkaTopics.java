package com.bankingplatform.common.constants;

public final class KafkaTopics {
    private KafkaTopics() {}

    public static final String TRANSACTION_EVENTS = "transaction-events";
    public static final String ACCOUNT_EVENTS = "account-events";
    public static final String USER_EVENTS = "user-events";
    public static final String LOAN_EVENTS = "loan-events";
    public static final String FRAUD_ALERTS = "fraud-alerts";

    public static final String TRANSACTION_EVENTS_DLT = "transaction-events.DLT";
    public static final String ACCOUNT_EVENTS_DLT = "account-events.DLT";
    public static final String USER_EVENTS_DLT = "user-events.DLT";
    public static final String LOAN_EVENTS_DLT = "loan-events.DLT";

    public static final String TRANSACTION_EVENTS_GROUP_NOTIFICATION = "notification-service-transaction";
    public static final String ACCOUNT_EVENTS_GROUP_NOTIFICATION = "notification-service-account";
    public static final String USER_EVENTS_GROUP_NOTIFICATION = "notification-service-user";
    public static final String USER_EVENTS_GROUP_USER_SERVICE = "user-service-registration";
    public static final String TRANSACTION_EVENTS_GROUP_FRAUD = "fraud-service-transaction";
    public static final String TRANSACTION_EVENTS_GROUP_AUDIT = "audit-service-transaction";
    public static final String ACCOUNT_EVENTS_GROUP_AUDIT = "audit-service-account";
    public static final String USER_EVENTS_GROUP_AUDIT = "audit-service-user";
    public static final String LOAN_EVENTS_GROUP_AUDIT = "audit-service-loan";
    public static final String FRAUD_ALERTS_GROUP_NOTIFICATION = "notification-service-fraud";
    public static final String LOAN_EVENTS_GROUP_NOTIFICATION = "notification-service-loan";
    public static final String FRAUD_ALERTS_GROUP_AUDIT = "audit-service-fraud";
}
