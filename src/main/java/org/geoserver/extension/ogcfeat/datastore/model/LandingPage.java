package org.geoserver.extension.ogcfeat.datastore.model;

import java.util.ArrayList;
import java.util.List;

public class LandingPage {

	private String title;
	private String description;
	private List<Link> links = new ArrayList<>();

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<Link> getLinks() {
		return links;
	}

	public void setLinks(List<Link> links) {
		this.links = links;
	}

}
