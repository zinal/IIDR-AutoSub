/*
 * AutoSub sample code
 */
package com.ibm.idrcdc.autosub;

/**
 *
 * @author zinal
 */
public enum FailureMessageType {

    M9505("9505"),
    M9602("9602");

    public final String id;

    private FailureMessageType(String id) {
        this.id = id;
    }

}
