package com.kalsym.flowcore.daos.models.vertexsubmodels;

import com.kalsym.flowcore.models.enums.DataFomat;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Sarosh
 */
@Getter
@Setter
public class ExternalRequestReponse {

    DataFomat format;

    List<ExternalRequestResponseMapping> mapping;
}
