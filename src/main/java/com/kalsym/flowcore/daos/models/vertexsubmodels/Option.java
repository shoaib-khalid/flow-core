package com.kalsym.flowcore.daos.models.vertexsubmodels;

import com.kalsym.flowcore.models.pushmessages.MenuItem;
import java.util.HashMap;
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

    public MenuItem getMenuItem() {
        MenuItem menuItem = new MenuItem();
        menuItem.setTitle(this.text);
        menuItem.setType("postback");
        menuItem.setPayload(this.value);
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
