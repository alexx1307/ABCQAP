package com.luka.algorithm;

/**
 * Created by lukas on 18.07.2017.
 */
public enum ESelectionMethod {
    NONE(0),
    ELITE_SELECTION(1),
    ORIGINAL(2),
    EMPLOYEE_BEE_VERSION(3);

    private final int code;

    ESelectionMethod(int code) {
        this.code = code;
    }

    public short getCode(){
        return (short)code;
    }

}
