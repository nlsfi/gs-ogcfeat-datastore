

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

License
- This Repository contains  original code - see LICENSE
- This repository contains modified and derived code from both GeoTools and Geoserver
- [GeoTools license](./license/geotools/LICENSE.md)
- [GeoServer license](./geoserver/LICENSE.md)

Features
- Supports publishing OGCFeat collections from a cascaded OGCFeat service
- Initializes datastore with data from backend OGCFeat service metadata links (see requirements for backend service below)
- Constructs featuretypes with metadata from backend OGCFeat schema responses 
- Simple Bbox filtering is based on backend OGCFeat service bbox filtering
- Complex filtering is implemented as backend bbox filtering with FilteringFeatureReader post processing
- Projection support is based on backend OGCFeat service CRS support
- Supports next link paging and limiting of page size and page count

Requirements for backend OGCFeat service 
- Backend OGCFeat service must support Landing page, conformance,collections,schema for this extension to work properly
- OGC API - Features - Part 1: Core
- OGC API - Features - Part 2: Coordinate Reference Systems by Reference
- OGC API - Features - Part 3: Filtering and Common Query Language (CQL2)
- OGC API - Features - Part 5: Schemas - draft *partial Schema support for collections is required* 

Implementation status
- Supports BBOX only Simple Filter
- Supports BBOX Filter as part of a Complex Filter.
- A BBOX filter must be supplied either as simple bbox filter or as part of a more complex filter

Issues and Limitations
- (x) Storing a Basic Auth authorized backend fails in outer Geoserver code?
-- landing page, /conformance /collections schema etc work just fine after
- (/) canProcess() modify canProcess to check for OGCFeat datasource type - fixed with constant parameter DBTYPE

To-do with some recent changes
- (x) Confirm compatibility with Geoserver AXIS ORDER and reprojections
- (x) refactor to org.geotools.api interfaces and https://github.com/geotools/geotools/wiki/Remove-OpenGIS 
- (x) JDK17+ and other compatibility issues https://github.com/geoserver/geoserver/wiki/Jakarta-EE

Installation 
- Copy target/gs-ogcfeat-datastore-2.23.2.jar and swagger-models-2.2.16.jar to geoserver/WEB-INF/lib 

Usage
- add an OGCFeat datastore and publish layers as usual
- Datastore URL is the OGCFeat service landing page 

