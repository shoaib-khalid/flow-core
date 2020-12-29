package com.kalsym.flowcore.daos.models.vertexsubmodels;

import java.util.HashMap;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpMethod;

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

}
