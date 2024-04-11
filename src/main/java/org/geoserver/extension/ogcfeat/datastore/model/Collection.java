package org.geoserver.extension.ogcfeat.datastore.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Collection {
    private String id;
    private String title;
    private String description;
    private List<Link> links;
    private Extent extent;
    private String itemType = "feature";
    private String[] crs;
    private String storageCrs;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public Extent getExtent() {
        return extent;
    }

    public void setExtent(Extent extent) {
        this.extent = extent;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String[] getCrs() {
        return crs;
    }

    public void setCrs(String[] crs) {
        this.crs = crs;
    }

    public String getStorageCrs() {
        return storageCrs;
    }

    public void setStorageCrs(String storageCrs) {
        this.storageCrs = storageCrs;
    }

}
