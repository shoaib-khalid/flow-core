package com.kalsym.flowcore.models.pushmessages;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Sarosh
 */
@Getter
@Setter
public class PushMessage {

    private List<String> recipientIds;
    private String title;
    private String subTitle;
    private String url;
    private String urlType;

    private List<MenuItem> menuItems;
    private String message;

    private String refId;
}
