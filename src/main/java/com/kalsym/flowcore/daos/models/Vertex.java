package com.kalsym.flowcore.daos.models;

import com.kalsym.flowcore.daos.models.conversationsubmodels.Data;
import com.kalsym.flowcore.daos.models.vertexsubmodels.*;
import com.kalsym.flowcore.models.enums.VertexType;
import com.kalsym.flowcore.models.pushmessages.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 *
 * @author Sarosh
 */
@Getter
@Setter
@Document
public class Vertex {

    @Id
    private String id;

    private Info info;

    /**
     * Below will be null and only one will be assigned a value
     */
    private Validation validation;
    private List<Condition> conditions;
    private List<Action> actions;
    private List<Option> options;
    private Handover handover;

    private Step step;

    private String dataVariable;

    @CreatedDate
    private Date createdDate;
    @LastModifiedDate
    private Date lastModifiedDate;

    private String flowId;
    
    
    private Integer isLastVertex;

    /**
     * Returns the step after matching the conditions inside the vertex. If non
     * of the conditions match returns the step from vertex.
     *
     * @param data Data from conversation
     * @return Step
     */
    public Step matchConditions(Data data) {

        Step step = null;
        for (Condition condition : conditions) {
            step = condition.match(data);
            if (step != null) {
                break;
            }
        }

        if (step == null) {
            step = this.step;
        }

        return step;
    }

    /**
     * Traverses through options and takes matching value.If non of the options
     * match returns the step from vertex.
     *
     * @param value
     * @param dataVariables
     * @return Step
     */
    public Option matchOptions(String value, HashMap<String, String> dataVariables) {

        for (Option option : this.options) {
            if (option.match(value, dataVariables)) {
                return option;
            }
        }

        return null;
    }

    /**
     * Generates push message for the vertex.
     *
     * @param data
     * @param recipients
     * @param refId
     * @return PushMessage
     */
//    public PushMessage getPushMessage(Data data, List<String> recipients, String refId) {
//        String logprefix = refId;
//        String logLocation = Thread.currentThread().getStackTrace()[1].getMethodName();
//        Logger.info(logprefix, logLocation, "vertexType: " + this.getInfo().getType(), "");
//
//        PushMessage pushMessage = new PushMessage();
//
//        pushMessage.setTitle(this.info.getTitle());
//        pushMessage.setSubTitle(this.info.getText());
//
//        if (VertexType.MENU_MESSAGE == this.getInfo().getType()) {
//            List<MenuItem> menuItems = new ArrayList<>();
//            for (Option option : this.options) {
//                MenuItem menuItem = null;
//                if (data == null) {
//                    menuItem = option.getMenuItem(null);
//                } else {
//                    menuItem = option.getMenuItem(data.getVariables());
//                }
//
//                Logger.info(logprefix, logLocation, "menuTile: " + menuItem.getTitle(), "");
//                Logger.info(logprefix, logLocation, "menuPayload: " + menuItem.getPayload(), "");
//
//                if (menuItem.getPayload() != null && menuItem.getTitle() != null) {
//                    menuItems.add(menuItem);
//                }
//            }
//
//            if (null == this.options || options.isEmpty()) {
//                MenuItem menuItem = new MenuItem();
//                menuItem.setTitle("Default");
//                menuItem.setType("postback");
//                menuItem.setPayload(this.step.getTargetId());
//                menuItems.add(menuItem);
//            }
//
//            if (null == data) {
//                pushMessage.setSubTitle(insertDataVaiables(null));
//            } else {
//                pushMessage.setSubTitle(insertDataVaiables(data.getVariables()));
//            }
//            pushMessage.setMessage(null);
//            pushMessage.setMenuItems(menuItems);
//        }
//
//        if (VertexType.TEXT_MESSAGE == this.info.getType()
//                || VertexType.IMMEDIATE_TEXT_MESSAGE == this.info.getType()
//                || VertexType.HANDOVER == this.info.getType()) {
//            if (null == data) {
//                pushMessage.setMessage(insertDataVaiables(null));
//            } else {
//                pushMessage.setMessage(insertDataVaiables(data.getVariables()));
//            }
//
//        }
//
//        pushMessage.setUrl("https://www.techopedia.com/images/uploads/6e13a6b3-28b6-454a-bef3-92d3d5529007.jpeg");
//        pushMessage.setUrlType("IMAGE");
//
//        pushMessage.setRecipientIds(recipients);
//        pushMessage.setRefId(refId);
//
//        return pushMessage;
//    }
    /**
     * Generates push message for the vertex.
     *
     * @param data
     * @param recipients
     * @param refId
     * @return PushMessage
     */
    public PushMessage getPushMessage(Data data, String refId) {


        PushMessage pushMessage = new PushMessage();

        pushMessage.setTitle(this.info.getTitle());
        pushMessage.setSubTitle(this.info.getText());

        if (VertexType.MENU_MESSAGE == this.getInfo().getType()) {
            List<MenuItem> menuItems = new ArrayList<>();
            for (Option option : this.options) {
                MenuItem menuItem = null;
                if (data == null) {
                    menuItem = option.getMenuItem(null);
                } else {
                    menuItem = option.getMenuItem(data.getVariables());
                }

                if (menuItem.getPayload() != null && menuItem.getTitle() != null) {
                    menuItems.add(menuItem);
                }
            }

            if (null == this.options || options.isEmpty()) {
                MenuItem menuItem = new MenuItem();
                menuItem.setTitle("Default");
                menuItem.setType("postback");
                menuItem.setPayload(this.step.getTargetId());
                menuItems.add(menuItem);
            }

            if (null == data) {
                pushMessage.setSubTitle(insertDataVaiables(null));
            } else {
                pushMessage.setSubTitle(insertDataVaiables(data.getVariables()));
            }
            pushMessage.setMessage(null);
            pushMessage.setMenuItems(menuItems);
        }

        if (VertexType.TEXT_MESSAGE == this.info.getType()
                || VertexType.IMMEDIATE_TEXT_MESSAGE == this.info.getType()) {
            if (null == data) {
                pushMessage.setMessage(insertDataVaiables(null));
            } else {
                pushMessage.setMessage(insertDataVaiables(data.getVariables()));
            }

        }

        if (VertexType.HANDOVER == this.info.getType()) {
            if (null == data) {
                pushMessage.setMessage(insertDataVaiables(null));
            } else {
                pushMessage.setMessage(insertDataVaiables(data.getVariables()));
            }
        }

        pushMessage.setUrl("https://www.techopedia.com/images/uploads/6e13a6b3-28b6-454a-bef3-92d3d5529007.jpeg");
        pushMessage.setUrlType("IMAGE");

        //pushMessage.setRecipientIds(recipients);
        pushMessage.setRefId(refId);

        return pushMessage;
    }

    public String insertDataVaiables(HashMap<String, String> dataVariables) {

        String text = this.info.getText();
        if (null != dataVariables) {
            for (Map.Entry<String, String> mapElement : dataVariables.entrySet()) {
                String key = mapElement.getKey();
                String value = mapElement.getValue();
                text = text.replace("$%" + key + "$%", value);
            }
        }
        return text;
    }

}
