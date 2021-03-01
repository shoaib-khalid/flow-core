package com.kalsym.flowcore.daos.models.vertexsubmodels;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author Sarosh
 */
@Getter
@Setter
public class WizardContent {

    private String activePage;
    private String store;
    private List<WizardResource> resources;
    private List<WizardPage> pages;
}
