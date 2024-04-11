package org.geoserver.extension.ogcfeat.datastore.model;

import java.util.ArrayList;
import java.util.List;

public class Collections {

    private List<Link> links = new ArrayList<>();
    private List<Collection> collections = new ArrayList<>();

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }

    public List<Collection> getCollections() {
        return collections;
    }

    public void setCollections(List<Collection> collections) {
        this.collections = collections;
    }

}
