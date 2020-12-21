package com.kalsym.flowcore.daos.models.vertexsubmodels;

import com.kalsym.flowcore.models.enums.MatchOperator;
import com.kalsym.flowcore.utils.LogUtil;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Sarosh
 */
@Getter
@Setter
public class ConditionGroup {

    private String field;
    private MatchOperator match;
    private Boolean caseSensitive;
    private String value;

    public boolean match(String data) {
        String logprefix = "";
        String logLocation = Thread.currentThread().getStackTrace()[1].getMethodName();

        String localValue = this.value;

        if (!caseSensitive) {
            localValue = localValue.toUpperCase();
            data = data.toUpperCase();
        }

//        LogUtil.info(logprefix, logLocation, "caseSensitive: " + caseSensitive, "");

//        LogUtil.info(logprefix, logLocation, "value: " + localValue + " data: " + data, "");

        if (MatchOperator.IS == match) {
//            LogUtil.info(logprefix, logLocation, "IS: " + data.equalsIgnoreCase(localValue), "");

            return data.equalsIgnoreCase(localValue);
        } else if (MatchOperator.STARTS == match) {
//            LogUtil.info(logprefix, logLocation, "STARTS: " + data.startsWith(localValue), "");

            return data.startsWith(localValue);
        } else if (MatchOperator.ENDS == match) {
//            LogUtil.info(logprefix, logLocation, "ENDS: " + data.endsWith(localValue), "");

            return data.endsWith(localValue);

        }

        return false;
    }
}
