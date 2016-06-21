package com.zauber.domain;

/**
 * Created by Zauber Ltd on 21/06/2016.
 */
public class OktaServer {

    private String name;
    private String url;
    private String apiKey;

    public OktaServer(String name, String url, String apiKey) {
        this.name = name;
        this.url = url;
        this.apiKey = apiKey;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getApiKey() {
        return apiKey;
    }

}
