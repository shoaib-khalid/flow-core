package com.kalsym.flowcore.daos.models.vertexsubmodels;

import com.kalsym.flowcore.models.pushmessages.MenuItem;
import com.kalsym.flowcore.utils.Logger;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Sarosh
 */
@Getter
@Setter
public class Option {

    private String text;
    private Step step;
    private String value;

    public MenuItem getMenuItem(HashMap<String, String> dataVariables) {
        MenuItem menuItem = new MenuItem();

        String menuItemTitle = this.text;
        String menuValue = this.value;

        if (null != dataVariables) {

            if (menuItemTitle.startsWith("$%") && menuItemTitle.endsWith("$%")) {
                String variableName = menuItemTitle.replace("$%", "");
                menuItemTitle = dataVariables.get(variableName);

            }

            if (menuValue.startsWith("$%") && menuValue.endsWith("$%")) {
                String variableName = menuValue.replace("$%", "");
                menuValue = dataVariables.get(variableName);

            }
        }

        menuItem.setTitle(menuItemTitle);
        menuItem.setType("postback");
        menuItem.setPayload(menuValue);
        return menuItem;
    }

    public boolean match(String value, HashMap<String, String> dataVariables) {

        if (this.value.startsWith("$%") && this.value.endsWith("$%")) {
            this.value = this.value.replace("$%", "");
            this.value = dataVariables.get(this.value);
        }

        return this.value.equalsIgnoreCase(value);

    }
}
