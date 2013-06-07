package com.codepath.apps.restclienttemplate;

import org.scribe.builder.api.Api;
import org.scribe.builder.api.FlickrApi;

import android.content.Context;

import com.codepath.oauth.OAuthBaseClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

public class RestClient extends OAuthBaseClient {
    public static final Class<? extends Api> REST_API_CLASS = FlickrApi.class; // Change this
    public static final String REST_URL = "http://api.flickr.com/services"; // Change this, base API URL
    public static final String REST_CONSUMER_KEY = "SOME_KEY";       // Change this
    public static final String REST_CONSUMER_SECRET = "SOME_SECRET"; // Change this
    public static final String REST_CALLBACK_URL = "oauth://cprest"; // Don't change this
    
    public RestClient(Context context) {
        super(context, REST_API_CLASS, REST_URL, REST_CONSUMER_KEY, REST_CONSUMER_SECRET, REST_CALLBACK_URL);
    }
    
    // CHANGE THIS
    // DEFINE METHODS for different API endpoints here
    public void getInterestingnessList(AsyncHttpResponseHandler handler) {
        String apiUrl = getApiUrl("?nojsoncallback=1&method=flickr.interestingness.getList");
        // Can specify query string params directly or through RequestParams.
        RequestParams params = new RequestParams();
        params.put("format", "json");
        client.get(apiUrl, params, handler);
    }
}