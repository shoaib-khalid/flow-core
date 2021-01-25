package com.kalsym.flowcore.daos.models.vertexsubmodels;

import com.kalsym.flowcore.VersionHolder;
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

        String localValue = this.value;

        if (!caseSensitive) {
            localValue = localValue.toUpperCase();
            data = data.toUpperCase();
        }

        Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "match: " + match);
        if (MatchOperator.IS == match) {
            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "IS: " + data.equalsIgnoreCase(localValue));
            return data.equalsIgnoreCase(localValue);
        } else if (MatchOperator.STARTS == match) {
            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "STARTS: " + data.startsWith(localValue));
            return data.startsWith(localValue);
        } else if (MatchOperator.ENDS == match) {
            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "ENDS: " + data.endsWith(localValue));
            return data.endsWith(localValue);

        } else if (MatchOperator.GREATER_THAN == match || MatchOperator.LESS_THAN == match) {
            try {
                long longVal = Long.parseLong(localValue);
                long longData = Long.parseLong(data);

                if (MatchOperator.GREATER_THAN == match) {
                    Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "GREATER_THAN: " + (longData > longVal));
                    return longData > longVal;
                } else if (MatchOperator.LESS_THAN == match) {
                    Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "LESS_THAN: " + (longData < longVal));
                    return longData < longVal;
                }

            } catch (NumberFormatException e) {
                Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "Error doing comparison", e);
                return false;
            }
        }

        return false;
    }
}
