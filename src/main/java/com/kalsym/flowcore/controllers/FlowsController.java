package com.kalsym.flowcore.controllers;

import com.kalsym.flowcore.VersionHolder;
import com.kalsym.flowcore.daos.models.*;
import com.kalsym.flowcore.models.*;
import com.kalsym.flowcore.daos.repositories.FlowsRepostiory;
import com.kalsym.flowcore.utils.Logger;
import java.util.List;
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

        String[] botIds = {botId};
        List<Flow> flows = flowsRepository.findByBotIds(botId);
        Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "total flows: " + flows.size());

        if (flows.isEmpty()) {
            response.setErrorStatus(HttpStatus.NOT_FOUND, "No flows foud with match botId");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        Flow flow = flows.get(0);
        if (null == flow) {
            response.setErrorStatus(HttpStatus.NOT_FOUND);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        }
        flow.setId(botId);
        response.setData(flow);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

}
