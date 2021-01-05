package com.kalsym.flowcore.daos.models.vertexsubmodels;

import com.kalsym.flowcore.models.enums.MatchOperator;
import com.kalsym.flowcore.utils.Logger;
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
//        Logger.info(logprefix, logLocation, "value: " + localValue + " data: " + data, "");
        Logger.info(logprefix, logLocation, "match: " + match, "");
        //       Logger.info(logprefix, logLocation, "MatchOperator.IS == match: " + (MatchOperator.IS == match), "");

        if (MatchOperator.IS == match) {
            Logger.info(logprefix, logLocation, "IS: " + data.equalsIgnoreCase(localValue), "");

            return data.equalsIgnoreCase(localValue);
        } else if (MatchOperator.STARTS == match) {
//            LogUtil.info(logprefix, logLocation, "STARTS: " + data.startsWith(localValue), "");

            return data.startsWith(localValue);
        } else if (MatchOperator.ENDS == match) {
//            LogUtil.info(logprefix, logLocation, "ENDS: " + data.endsWith(localValue), "");

            return data.endsWith(localValue);

        } else if (MatchOperator.GREATER_THAN == match || MatchOperator.LESS_THAN == match) {
            try {
                long longVal = Long.parseLong(localValue);
                long longData = Long.parseLong(data);

                if (MatchOperator.GREATER_THAN == match) {
                    Logger.info(logprefix, logLocation, "GREATER_THAN: " + (longData > longVal), "");

                    return longData > longVal;
                } else if (MatchOperator.LESS_THAN == match) {
                    Logger.info(logprefix, logLocation, "LESS_THAN: " + (longData < longVal), "");

                    return longData < longVal;
                }

            } catch (NumberFormatException e) {
                Logger.error(logprefix, logLocation, "Error doing comparison", "", e);
                return false;
            }
        }

        return false;
    }
}
