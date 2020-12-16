package com.kalsym.flowcore.controllers;

import com.kalsym.flowcore.daos.repositories.ConversationsRepostiory;
import com.kalsym.flowcore.daos.repositories.VerticesRepostiory;
import com.kalsym.flowcore.models.*;
import com.kalsym.flowcore.utils.LogUtil;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/callback")
public class CallbackController {

    @Autowired
    VerticesRepostiory verticesRepostiory;

    @Autowired
    FlowsRepostiory flowsRepostiory;

    /**
     * *
     * Postback receives id targetId in the payload as data. If async is true than next
     * message is send to callback after processing on the other hand next
     * message is sent in response.
     *
     * @param request
     * @param senderId
     * @param refrenceId
     * @param async
     * @param requestBody
     * @return
     */
    @PostMapping(path = {"/postaback/", "/postaback"}, name = "callback-postback-post")
    public ResponseEntity<HttpReponse> postback(HttpServletRequest request,
            @RequestParam(name = "senderId", required = true) String senderId,
            @RequestParam(name = "refrenceId", required = true) String refrenceId,
            @RequestParam(name = "async", defaultValue = "false") boolean async,
            @RequestBody(required = true) RequestPayload requestBody) {
        String logprefix = request.getRequestURI() + " ";
        String logLocation = Thread.currentThread().getStackTrace()[1].getMethodName();
        HttpReponse response = new HttpReponse(request.getRequestURI());

        LogUtil.info(logprefix, logLocation, "queryString: " + request.getQueryString(), "");

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * *
     * Message receives plain string in payload as data. If async is true than next
     * message is send to callback after processing on the other hand next
     * message is sent in response.
     *
     * @param request
     * @param senderId
     * @param refrenceId
     * @param async
     * @param requestBody
     * @return
     */
    @PostMapping(path = {"/message/", "/message"}, name = "callback-message-post")
    public ResponseEntity<HttpReponse> message(HttpServletRequest request,
            @RequestParam(name = "senderId", required = true) String senderId,
            @RequestParam(name = "refrenceId", required = true) String refrenceId,
            @RequestParam(name = "async", defaultValue = "false") boolean async,
            @RequestBody(required = true) RequestPayload requestBody) {
        String logprefix = request.getRequestURI() + " ";
        String logLocation = Thread.currentThread().getStackTrace()[1].getMethodName();
        HttpReponse response = new HttpReponse(request.getRequestURI());

        LogUtil.info(logprefix, logLocation, "queryString: " + request.getQueryString(), "");

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
