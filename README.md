

gs-ogcfeat-datastore
=================

gs-ogcfeat-datastore is a Geoserver DataStore extension that can be used to publish cascaded collections from a backend OGCFeat service 
as WMS, WMTS or other Geoserver services.

Module Status
- Initial version - Work-in-Progress
- available As-Is 

Geoserver / Geotools Version 
- gs 2.23.2
- gt 29.2

License Issues
- (x) Apply Geoserver and Geotools compatible license: gs GPL2 , gt LGPL ?
- Repository contains  original code as well as some copy-paste-modified code from both GeoTools and Geoserver

Features
- Supports publishing OGCFeat collections from an OGCFeat service
- Initializes datastore with data from backend OGCFeat service metadata links (conformance,collections,schema)
- Constructs featuretypes with metadata from backend OGCFeat schema responses
- Bbox filtering is based on backend OGCFeat service bbox filtering 
- Projection support is based on backend OGCFeat service CRS support
- Supports next link paging and limiting of page size

Implementation status
- Only BBOX Filter is supported at the moment

Issues and Limitations
- (x) Storing a Basic Auth authorized backend fails in outer Geoserver code?
-- landing page, /conformance /collections schema etc work just fine after

To-do with some recent changes
- (/) fetches only first page of items up to default limit at the moment
- (/) Add some form of limit support to avoid fetch-all-of-database queries
- (/) Add paging support with a custom GeoJSONReader to complement limit  support
- (x) Confirm compatibility with Geoserver AXIS ORDER and reprojections
- (/) Add GitHub actions - JDK11 atm
- (x) refactor to org.geotools.api interfaces and https://github.com/geotools/geotools/wiki/Remove-OpenGIS 
- (x) JDK17+ and other compatibility issues https://github.com/geoserver/geoserver/wiki/Jakarta-EE
- (/) Reduce logging noise

Installation 
- Copy target/gs-ogcfeat-datastore-2.23.2.jar and swagger-models-2.2.16.jar to geoserver/WEB-INF/lib 

Usage
- add an OGCFeat datastore and publish layers as usual
- Datastore URL is the OGCFeat service landing page 

