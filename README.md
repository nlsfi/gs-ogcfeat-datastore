

gs-ogcfeat-datastore
=================

gs-ogcfeat-datastore is a Geoserver extension that can be used to 
publish collections from from a OGCFeat service

Status
- Initial version - Work-in-some-Progress
- available as-is 

Geoserver / Geotools Version 
- gs 2.23.2
- gt 29.2

Restrictions
- Only BBOX Filtering at the moment

Features
- Initializes datastore with backend OGCFeat service metadata (conformance,collections,schema)
- Creates featuretypes with backend OGCFeat schema responses
- Bbox filtering is based on backend OGCFeat service bbox filtering 
- Projection support is based on backend OGCFeat service CRS support

Issues (bugs)
- (x) Initial Basic Auth backends fails on fetching landing page with Apache HttpClient
-- /conformance /collections etc work just fine
- (x) fetches only first page of items up to default limit at the moment

Issues (Features)
- (x) Add some form of limit support to avoid fetch-all-of-database queries
- (x) Add paging support with GeoJSONReader to complement limit  support
- (x) Confirm compatibility with Geoserver AXIS ORDER 
- (x) refactor to org.geotools.api interfaces and https://github.com/geotools/geotools/wiki/Remove-OpenGIS 

Installation 
- Copy target/gs-ogcfeat-datastore-2.23.2.jar and swagger-models-2.2.16.jar to geoserver/WEB-INF/lib 

Usage
- add an OGCFeat datastore and publish layers as usual

