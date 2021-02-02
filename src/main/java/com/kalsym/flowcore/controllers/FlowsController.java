package com.kalsym.flowcore.controllers;

import com.kalsym.flowcore.VersionHolder;
import com.kalsym.flowcore.daos.models.*;
import com.kalsym.flowcore.models.*;
import com.kalsym.flowcore.daos.repositories.FlowsRepostiory;
import com.kalsym.flowcore.utils.Logger;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Sarosh
 */
@RestController()
@RequestMapping("/flows")
public class FlowsController {

    @Autowired
    private FlowsRepostiory flowsRepository;

    @GetMapping(path = {"/"}, name = "flows-get")
    public ResponseEntity<Object> getFlows(HttpServletRequest request,
            @RequestParam(required = false) String botId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        String logprefix = "";
        HttpReponse response = new HttpReponse(request.getRequestURI());

        Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "queryString: " + request.getQueryString());

        response.setSuccessStatus(HttpStatus.OK);

        Flow flow = flowsRepository.findByBotId(botId);
        Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "bot flow: " + flow);

        response.setData(flowsRepository.findByBotId(botId));

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

}
