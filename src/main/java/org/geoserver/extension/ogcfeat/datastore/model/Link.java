package org.geoserver.extension.ogcfeat.datastore.model;

public class Link {

    private String href;
    private String rel;
    private String type;
    private String title;
    private String hreflang;

    public String getHref() {
        return href;
    }

    public String getRel() {
        return rel;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getHreflang() {
        return hreflang;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public void setRel(String rel) {
        this.rel = rel;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setHreflang(String hreflang) {
        this.hreflang = hreflang;
    }

}
