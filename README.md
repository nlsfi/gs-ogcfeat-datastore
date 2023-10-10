

gs-ogcfeat-datastore
=================

gs-ogcfeat-datastore is a Geoserver extension that can be used to publish collections from an OGCFeat service

Status
- Initial version - Work-in-Progress
- available As-Is 

Geoserver / Geotools Version 
- gs 2.23.2
- gt 29.2

Features
- Supports publishing OGCFeat collections from an OGCFeat service
- Initializes datastore with data from backend OGCFeat service metadata links (conformance,collections,schema)
- Constructs featuretypes with metadata from backend OGCFeat schema responses
- Bbox filtering is based on backend OGCFeat service bbox filtering 
- Projection support is based on backend OGCFeat service CRS support

Implementation status
- Only BBOX Filter is supported at the moment
- No paging support yet - reads features up to default backend limit 

Issues (bugs)
- (x) Storing a Basic Auth authorized backend fails in outer Geoserver code?
-- landing page, /conformance /collections schema etc work just fine after
- (x) fetches only first page of items up to default limit at the moment

Issues (Features)
- (x) Add some form of limit support to avoid fetch-all-of-database queries
- (x) Add paging support with GeoJSONReader to complement limit  support
- (x) Confirm compatibility with Geoserver AXIS ORDER 
- (x) Add GitHub actions 
- (x) refactor to org.geotools.api interfaces and https://github.com/geotools/geotools/wiki/Remove-OpenGIS 
- (x) Reduce logging noise

Installation 
- Copy target/gs-ogcfeat-datastore-2.23.2.jar and swagger-models-2.2.16.jar to geoserver/WEB-INF/lib 

Usage
- add an OGCFeat datastore and publish layers as usual

