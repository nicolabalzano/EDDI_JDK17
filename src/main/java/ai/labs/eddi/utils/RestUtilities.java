package ai.labs.eddi.utils;

import ai.labs.eddi.datastore.IResourceStore;
import org.jboss.resteasy.specimpl.ResponseBuilderImpl;
import org.jboss.resteasy.spi.NoLogWebApplicationException;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static ai.labs.eddi.utils.RuntimeUtilities.isNullOrEmpty;

/**
 * @author ginccc
 */
public class RestUtilities {
    private static final String versionQueryParam = "?version=";

    public static NoLogWebApplicationException createConflictException(String containerUri, IResourceStore.IResourceId currentId) {
        URI resourceUri = RestUtilities.createURI(containerUri, currentId.getId(), versionQueryParam, currentId.getVersion());

        ResponseBuilderImpl builder = new ResponseBuilderImpl();
        builder.status(Response.Status.CONFLICT);
        builder.entity(resourceUri.toString());
        builder.type(MediaType.TEXT_PLAIN);

        return new NoLogWebApplicationException(builder.build());
    }

    private static String sanitizeUriPart(Object uriPart) {
        if (uriPart == null) return "";
        String s = uriPart.toString();
        // Remove control chars, trim spaces
        s = s.replaceAll("[\\p{Cntrl}]", "").trim();
        // Replace backslashes with slashes
        s = s.replace('\\', '/');
        // Remove path traversal and duplicate slashes
        s = s.replaceAll("\\.\\.", "");
        s = s.replaceAll("/{2,}", "/");
        // Encode characters not allowed in URI path
        s = s.replaceAll("[^a-zA-Z0-9\\-._~:/?#\\[\\]@!$&'()*+,;=]", "");
        return s;
    }

    public static URI createURI(Object... uriParts) {
        int capacity = 0;
        for (Object uriPart : uriParts) {
            if (uriPart != null) capacity += uriPart.toString().length();
        }
        StringBuilder sb = new StringBuilder(Math.max(capacity, 64));
        for (Object uriPart : uriParts) {
            sb.append(sanitizeUriPart(uriPart));
        }
        return URI.create(sb.toString());
    }

    public static IResourceStore.IResourceId extractResourceId(URI uri) {
        if (isNullOrEmpty(uri)) {
            return null;
        }

        String uriString = uri.toString();

        String relativeUriString;
        if (uriString.contains("://")) {
            uriString = uriString.substring(uriString.indexOf("://") + 3);
            relativeUriString = uriString.substring(uriString.indexOf("/"));
        } else {
            relativeUriString = uriString;
        }

        if (relativeUriString.startsWith("/")) {
            relativeUriString = relativeUriString.substring(1);
        }

        if (relativeUriString.endsWith("/")) {
            relativeUriString = relativeUriString.substring(0, relativeUriString.length() - 1);
        }


        String[] split = relativeUriString.split("/");
        String lastPartOfUri = split.length > 2 ? split[split.length - 1].split("\\?")[0] : null;
        final String id = isValidId(lastPartOfUri) ? lastPartOfUri : null;

        int queryParamVersion = 0;
        if (relativeUriString.contains(versionQueryParam)) {
            String queryParamsString = relativeUriString.split("\\?")[1];
            Map<String, String> queryMap = getQueryMap(queryParamsString);
            String versionString = queryMap.get("version");
            try {
                queryParamVersion = Integer.parseInt(versionString);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Query param \"version\" must be a non-negative integer.");
            }
        }

        final Integer version = queryParamVersion;
        return new IResourceStore.IResourceId() {
            @Override
            public String getId() {
                return id;
            }

            @Override
            public Integer getVersion() {
                return version;
            }
        };
    }

    private static boolean isValidId(String s) {
        if (s == null || s.length() < 18) {
            return false;
        }

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (c >= '0' && c <= '9') {
                continue;
            }

            if (c >= 'a' && c <= 'f') {
                continue;
            }

            if (c >= 'A' && c <= 'F') {
                continue;
            }

            return false;
        }

        return true;
    }

    private static Map<String, String> getQueryMap(String query) {
        String[] params = query.split("&");
        Map<String, String> map = new HashMap<>();
        for (String param : params) {
            String[] keyValuePair = param.split("=");

            String name = null;
            if (keyValuePair.length > 0) {
                name = keyValuePair[0];
            }

            String value = null;
            if (keyValuePair.length > 1) {
                value = keyValuePair[1];
            }

            if (name != null && value != null) {
                map.put(name, value);
            }
        }
        return map;
    }
}
