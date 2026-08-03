package utils;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

public class RestProperties {

    private String baseURI;
    private String basePath;
    private String accessToken;

    public RestProperties() {
        loadFromClasspath("application.yaml");
    }

    @SuppressWarnings("unchecked")
    private void loadFromClasspath(String resource) {
        Yaml yaml = new Yaml();
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            if (is == null) {
                throw new IllegalStateException(resource + " nao encontrado no classpath");
            }
            Map<String, Object> root = yaml.load(is);
            Object restObj = root.get("rest");
            if (restObj instanceof Map) {
                Map<String, Object> rest = (Map<String, Object>) restObj;
                this.baseURI = getString(rest, "baseURI");
                this.basePath = getString(rest, "basePath");

                Object headerObj = rest.get("header");
                if (headerObj instanceof Map) {
                    Map<String, Object> header = (Map<String, Object>) headerObj;
                    this.accessToken = getString(header, "Access_Token");
                    if (this.accessToken == null) {
                        this.accessToken = getString(header, "access_token");
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Falha ao carregar configuracoes REST do YAML: " + e.getMessage(), e);
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v == null ? null : v.toString();
    }

    public String getUrlSandbox() {
        String uri = baseURI == null ? "" : baseURI;
        String path = basePath == null ? "" : basePath;

        if (uri.endsWith("/")) {
            uri = uri.substring(0, uri.length() - 1);
        }
        if (!path.startsWith("/") && !path.isEmpty()) {
            path = "/" + path;
        }

        return uri + path + "/";
    }

    public String getAccessToken() {
        return accessToken;
    }
}

