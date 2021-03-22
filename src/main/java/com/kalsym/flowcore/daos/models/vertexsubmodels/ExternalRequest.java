package com.kalsym.flowcore.daos.models.vertexsubmodels;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.kalsym.flowcore.VersionHolder;
import com.kalsym.flowcore.models.*;
import com.kalsym.flowcore.daos.models.conversationsubmodels.Data;
import com.kalsym.flowcore.models.enums.DataFomat;
import com.kalsym.flowcore.utils.Logger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author Sarosh
 */
@Getter
@Setter
public class ExternalRequest {

    private String url;
    private HttpMethod httpMethod;
    private HashMap<String, String> headers;
    private ExternalRequestBody body;

    private ExternalRequestReponse response;

    private Step errorStep;

    /**
     * Sends external request to provided URL and saves dataVariables after
     * successful response.
     *
     * @param data
     * @param restTemplate
     * @param refId
     * @return Next Step
     */
    public Dispatch processExternalRequest(Data data, RestTemplate restTemplate, String refId) {
        String logprefix = refId;

        Dispatch dispatch = new Dispatch();
        Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "url: " + this.url);
        Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "httpMethod: " + this.httpMethod);

        try {
            HashMap<String, String> erHeaders = this.headers;

            HttpHeaders httpHeaders = new HttpHeaders();

            if (null != erHeaders) {
                erHeaders.entrySet().forEach(mapElement -> {
                    httpHeaders.add(mapElement.getKey(), mapElement.getValue());
                });
            }

            ExternalRequestBody erBody = this.body;
            HttpEntity<String> requestEntity = null;
            if (DataFomat.JSON == erBody.getFormat()) {
                String payload = erBody.getPayload();
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "payload: " + payload);

                if (null != data.getVariables() && null != payload) {

                    HashMap<String, String> datavariables = data.getVariables();
                    for (Map.Entry<String, String> mapElement : datavariables.entrySet()) {
                        String key = mapElement.getKey();
                        String value = mapElement.getValue();
                        Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "replacing " + "$%" + key + "$%  with " + value);

                        payload = payload.replace("$%" + key + "$%", value);
                    }
                }

                Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "payload: " + payload);

                requestEntity = new HttpEntity<>(payload, httpHeaders);
            }

            ResponseEntity<String> responseEntity = restTemplate.exchange(this.url, this.httpMethod, requestEntity, String.class);

            Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "responseStatus: " + responseEntity.getStatusCode());

            if (HttpStatus.ACCEPTED == responseEntity.getStatusCode()
                    || HttpStatus.CREATED == responseEntity.getStatusCode()
                    || HttpStatus.OK == responseEntity.getStatusCode()) {

                String responseBody = responseEntity.getBody();
                DocumentContext jsonContext = JsonPath.parse(responseBody);

                Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "responseBody: " + responseBody);

                List<ExternalRequestResponseMapping> responseMappings = this.response.getMapping();

                for (ExternalRequestResponseMapping errm : responseMappings) {

                    try {
                        String value = jsonContext.read(errm.getPath()) + "";
                        dispatch.setVariableValue(errm.getDataVariable(), value);
                        Logger.application.info("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "added variable " + errm.getDataVariable() + ": " + value);

                    } catch (Exception e) {
                        if (errm.isOptional()) {

                            Logger.application.warn("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "could not read optional " + errm.getDataVariable());

                        } else {
                            Logger.application.warn("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "Cannot find non-optional variable " + errm.getDataVariable(), e);

                            dispatch.setStepId(this.errorStep.getTargetId());
                            return dispatch;
                        }
                    }

                }

            } else {
                dispatch.setStepId(this.errorStep.getTargetId());
                return dispatch;
            }
        } catch (RestClientException e) {
            Logger.application.warn("[v{}][{}] {}", VersionHolder.VERSION, logprefix, "Error processing external request", e);
            dispatch.setStepId(this.errorStep.getTargetId());
            return dispatch;
        }

        return dispatch;
    }

}
