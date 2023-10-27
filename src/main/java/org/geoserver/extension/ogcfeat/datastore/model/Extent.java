package org.geoserver.extension.ogcfeat.datastore.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Extent {
	private SpatialExtent spatial;
	private TemporalExtent temporal;

	public SpatialExtent getSpatial() {
		return spatial;
	}

	public void setSpatial(SpatialExtent spatial) {
		this.spatial = spatial;
	}

	public TemporalExtent getTemporal() {
		return temporal;
	}

	public void setTemporal(TemporalExtent temporal) {
		this.temporal = temporal;
	}

}
