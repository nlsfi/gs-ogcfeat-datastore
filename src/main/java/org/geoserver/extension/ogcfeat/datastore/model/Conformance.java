package org.geoserver.extension.ogcfeat.datastore.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Conformance {

    public List<String> conformsTo = new ArrayList<>();

}
