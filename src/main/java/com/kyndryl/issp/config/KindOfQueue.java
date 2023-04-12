package com.kyndryl.issp.config;

public enum KindOfQueue {

    PARAM_UCI("I"),
    PARAM_UCD("D"),
    PARAM_UCP("P");

    public final String kind;

    KindOfQueue(String kind) {
        this.kind = kind;
    }

}
