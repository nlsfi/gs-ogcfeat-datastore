package org.geoserver.extension.ogcfeat.datastore.model;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TemporalExtent {

    private List<Instant[]> interval;
    private String trs;

    public List<Instant[]> getInterval() {
        return interval;
    }

    public void setInterval(List<Instant[]> interval) {
        this.interval = interval;
    }

    public String getTrs() {
        return trs;
    }

    public void setTrs(String trs) {
        this.trs = trs;
    }

}
