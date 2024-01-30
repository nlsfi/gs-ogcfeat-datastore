package org.geoserver.extension.ogcfeat.datastore;

import java.awt.RenderingHints.Key;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.geotools.api.data.DataAccessFactory;
import org.geotools.api.data.DataStoreFactorySpi;
import org.geotools.api.data.Parameter;
import org.geotools.util.logging.Logging;

public class OGCFeatDataStoreFactory implements DataStoreFactorySpi, DataAccessFactory {

    protected static final Logger LOGGER = Logging.getLogger(OGCFeatDataStoreFactory.class.getName());

    public static final String NAME = "OGCFeat";
    public static final String DESC = "OGCFeat DataStore";

    private static final List<Param> parameterInfos = new ArrayList<>(10);

    public static final String DBTYPE_STRING = "OGCFeat";

    public static final Param DBTYPE = new Param("dbtype", String.class, "Fixed value '" + DBTYPE_STRING + "'", true,
            DBTYPE_STRING, Collections.singletonMap(Parameter.LEVEL, "program"));
    public static final Param NS_PARAM = new Param("namespace", String.class, "Namespace for OGCFeat type", true);
    public static final Param URL_PARAM = new Param("url", String.class, "OGCFeat landing page", true);
    public static final Param USER_PARAM = new Param("username", String.class, "username", false, null);
    public static final Param PASSWORD_PARAM = new Param("password", String.class, "password", false, null,
            Collections.singletonMap(Parameter.IS_PASSWORD, Boolean.TRUE));
    public static final Param TIMEOUT_PARAM = new Param("timeout", Integer.class,
            "OGCFeat backend timeout in milliseconds", false);
    public static final Param POOLMAX_PARAM = new Param("poolmax", Integer.class, "OGCFeat backend pool size", false);

    public static final Param FEAT_LIMIT_PARAM = new Param("featlimit", Integer.class,
            "OGCFeat features batch size limit", true);
    public static final Param FEAT_PAGING_MAX_PARAM = new Param("batchmax", Integer.class,
            "OGCFeat features max total pages", true);

    static {
        parameterInfos.add(DBTYPE);
        parameterInfos.add(NS_PARAM);
        parameterInfos.add(URL_PARAM);
        parameterInfos.add(USER_PARAM);
        parameterInfos.add(PASSWORD_PARAM);
        parameterInfos.add(TIMEOUT_PARAM);
        parameterInfos.add(POOLMAX_PARAM);
        parameterInfos.add(FEAT_PAGING_MAX_PARAM);
        parameterInfos.add(FEAT_LIMIT_PARAM);
    }

    @Override
    public OGCFeatDataStore createNewDataStore(Map<String, ?> params) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canProcess(Map<String, ?> params) {

        LOGGER.info("CANPROCESS");
        params.forEach((k, v) -> {
            LOGGER.info("CANPROCESS parmams " + k + " -> " + v);
        });

        try {
            Object dbType = DBTYPE.lookUp(params);
            if (dbType == null) {
                return false;
            }
            if (!DBTYPE_STRING.equals(dbType)) {
                LOGGER.info("DBTYPE" + dbType + " NOT EQUAL TO REQUIRED " + dbType);
                return false;
            }
            ;

            String urlStr = (String) params.get(OGCFeatDataStoreFactory.URL_PARAM.key);
            if (urlStr == null) {
                LOGGER.info("URL missing");
                return false;
            }

            URL url = new URL(urlStr);

            if (!url.getProtocol().startsWith("http")) {
                LOGGER.info("URL is not http(s)");
                return false;
            }

        } catch (MalformedURLException e) {
            LOGGER.info("URL failed " + e.toString());
            return false;
        } catch (IOException e) {
            LOGGER.info("LOOKUP failed " + e.toString());
            return false;
        }
        LOGGER.info("URL OK");

        return true;
    }

    @Override
    public OGCFeatDataStore createDataStore(Map<String, ?> params) throws IOException {

        LOGGER.info("Creating " + NAME);
        String urlValue = (String) URL_PARAM.lookUp(params);
        String nsValue = (String) NS_PARAM.lookUp(params);
        String user = (String) USER_PARAM.lookUp(params);
        String pass = (String) PASSWORD_PARAM.lookUp(params);
        Integer timeoutMillis = 3000;
        if (TIMEOUT_PARAM.lookUp(params) != null) {
            timeoutMillis = (int) TIMEOUT_PARAM.lookUp(params);
        }
        Integer poolMax = 10;
        if (POOLMAX_PARAM.lookUp(params) != null) {
            poolMax = (int) POOLMAX_PARAM.lookUp(params);
        }

        Integer limitMax = 1000;
        if (FEAT_LIMIT_PARAM.lookUp(params) != null) {
            limitMax = (int) FEAT_LIMIT_PARAM.lookUp(params);
        }
        Integer pagingMax = 1;
        if (FEAT_PAGING_MAX_PARAM.lookUp(params) != null) {
            pagingMax = (int) FEAT_PAGING_MAX_PARAM.lookUp(params);
        }

        OGCFeatDataStore ds = null;

        try {
            ds = new OGCFeatDataStore(nsValue, urlValue, user, pass, poolMax, timeoutMillis, limitMax, pagingMax);
            LOGGER.info("Created " + ds);
        } catch (MalformedURLException e) {
            LOGGER.warning(e.toString());
            throw e;
        }

        return ds;
    }

    @Override
    public String getDisplayName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESC;
    }

    @Override
    public Param[] getParametersInfo() {
        return (Param[]) parameterInfos.toArray(new Param[parameterInfos.size()]);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public Map<Key, ?> getImplementationHints() {
        return null;
    }

    public void setBaseDirectory(File baseDirectory) {

    }

}
