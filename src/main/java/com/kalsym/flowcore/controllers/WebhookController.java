package com.kalsym.flowcore.controllers;

import com.kalsym.flowcore.daos.models.*;
import com.kalsym.flowcore.daos.repositories.VerticesRepostiory;
import com.kalsym.flowcore.utils.LogUtil;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import com.kalsym.flowcore.services.ReceivedEvent;
import com.kalsym.flowcore.services.Entry;
import com.kalsym.flowcore.services.MessageDecoder;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.kalsym.flowcore.daos.repositories.FlowsRepostiory;

/**
 *
 * @author Sarosh
 */
@RestController()
@RequestMapping("/webhook")
public class WebhookController {

    @Autowired
    MessageDecoder messageDecoder;

    @Autowired
    VerticesRepostiory verticesRepostiory;

    @Autowired
    FlowsRepostiory flowsRepostiory;

    @GetMapping(path = {"/"}, name = "webhook-verify-get")
    public ResponseEntity<Object> verify(HttpServletRequest request,
            @RequestParam(name = "hub.mode", required = false) String hubMode,
            @RequestParam(name = "hub.challenge", required = false) String hubChalleng,
            @RequestParam(name = "hub.verify_token", required = false) String hubVerifyToken) {
        String logprefix = request.getRequestURI() + " ";
        String logLocation = Thread.currentThread().getStackTrace()[1].getMethodName();
        LogUtil.info(logprefix, logLocation, request.getQueryString(), "");

        LogUtil.info(logprefix, logLocation, "hubMode: " + hubMode, "");
        LogUtil.info(logprefix, logLocation, "hubChalleng: " + hubChalleng, "");
        LogUtil.info(logprefix, logLocation, "hubVerifyToken: " + hubVerifyToken, "");

        if ("kbot123".equals(hubVerifyToken)) {
            return ResponseEntity.status(HttpStatus.OK).body(hubChalleng);
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
    }

    @PostMapping(path = {"/"}, name = "webhook-receive-request-post")
    public ResponseEntity<Object> receivedRequest(HttpServletRequest request,
            @RequestBody String requestBody) throws IOException {
        String logprefix = request.getRequestURI() + " ";
        String logLocation = Thread.currentThread().getStackTrace()[1].getMethodName();
        try {

            LogUtil.info(logprefix, logLocation, "queryString: " + request.getQueryString(), "");
            //LogUtil.info(logprefix, logLocation, "body: "+request.getReader().lines().collect(Collectors.joining(System.lineSeparator())), "");
            LogUtil.info(logprefix, logLocation, "body: " + requestBody, "");

            ReceivedEvent receivedEvent = messageDecoder.getReceivedEvent(requestBody);

            LogUtil.info(logprefix, logLocation, "receivedEvent: " + receivedEvent, "");

            Entry entry = receivedEvent.getEntry().get(0);
            LogUtil.info(logprefix, logLocation, "entry: " + entry.getId(), "");

            Flow flow = new Flow();

            Vertex vertex = new Vertex();

            vertex.setId("sfsdf");

            verticesRepostiory.save(vertex);

            flowsRepostiory.save(flow);

        } catch (JSONException e) {
            LogUtil.error(logprefix, logLocation, "Error processing request", "", e);

        }

        return ResponseEntity.status(HttpStatus.OK).body("hello");
    }
}
