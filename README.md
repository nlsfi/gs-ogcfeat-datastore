

gs-ogcfeat-datastore
=================

gs-ogcfeat-datastore is a Geoserver DataStore extension that can be used to publish cascaded collections from a backend OGCFeat service 
as WMS, WMTS or other Geoserver services.

Module Status
- Initial version - Work-in-Progress
- available As-Is 

Geoserver / Geotools Version in branches
- main: gs 2.27.1, gt 33.1
- 2.27.1: gs 2.27.1, gt 33.1
- 2.27.0: gs 2.27.0, gt 33.0

License
- This Repository contains  original code - see [LICENSE](./LICENSE.md)
- This repository contains modified and derived code from both GeoTools and Geoserver
- [GeoTools license](./license/geotools/LICENSE.md)
- [GeoServer license](./license/geoserver/LICENSE.md)

Features
- Supports publishing OGCFeat collections from a cascaded OGCFeat service
- Initializes datastore with data from backend OGCFeat service metadata links (see requirements for backend service below)
- Constructs featuretypes with metadata from backend OGCFeat schema responses
- BBOX filter MUST be supplied in Query
- Simple BBOX filtering is based on backend OGCFeat service BBOX filtering
- Complex filtering is implemented as backend OGCFeat service BBOX filtering with post processing via FilteringFeatureReader in gs-ogcfeat-datastore
- Projection support is based on backend OGCFeat service CRS support
- Supports next link paging and limiting of page size and page count

Requirements for backend OGCFeat service 
- Backend OGCFeat service must support Landing page, conformance,collections,schema for this extension to work properly
- OGC API - Features - Part 1: Core
- OGC API - Features - Part 2: Coordinate Reference Systems by Reference
- OGC API - Features - Part 3: Filtering and Common Query Language (CQL2)
- OGC API - Features - Part 5: Schemas - draft *partial Schema support for collections is required* 

Issues and Todos
- (x) Fix: Storing a Basic Auth authorized backend fails in outer Geoserver code?
-- landing page, /conformance /collections schema etc work just fine after
- (x) Todo: Confirm compatibility with Geoserver AXIS ORDER and reprojections
- (/) Done: refactor to org.geotools.api interfaces and https://github.com/geotools/geotools/wiki/Remove-OpenGIS
- (x) Partial: JDK17+ and other compatibility issues https://github.com/geoserver/geoserver/wiki/Jakarta-EE
  -- github actions builds for JDK17, JDK21 

Installation 
- Copy target/gs-ogcfeat-datastore-2.24.2.jar and swagger-models-2.2.16.jar to geoserver/WEB-INF/lib 

Usage
- add an OGCFeat datastore and publish layers as usual
- Datastore URL is the OGCFeat service landing page 

