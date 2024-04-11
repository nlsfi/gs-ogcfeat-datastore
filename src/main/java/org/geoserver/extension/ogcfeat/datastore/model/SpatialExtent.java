package org.geoserver.extension.ogcfeat.datastore.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SpatialExtent {

    private List<double[]> bbox;
    private String crs;

    public List<double[]> getBbox() {
        return bbox;
    }

    public void setBbox(List<double[]> bbox) {
        this.bbox = bbox;
    }

    public String getCrs() {
        return crs;
    }

    public void setCrs(String crs) {
        this.crs = crs;
    }

}
