# RestClientTemplate

## Overview

RestClientTemplate is a skeleton Android project that makes writing Android apps sourced from OAuth JSON REST APIs as easy as possible. This skeleton project
combines the best libraries and structure to enable quick development of rich API clients. The following things are supported out of the box:

 * Authenticating with any OAuth 1.0a or OAuth 2 API
 * Sending requests for and parsing JSON API data using a defined client
 * Persisting data to a local SQLite store through an ORM layer
 * Displaying and caching remote image data into views

The following libraries are used to make this possible:

 * [scribe-java](https://github.com/fernandezpablo85/scribe-java) - Simple OAuth library for handling the authentication flow.
 * [Android Async HTTP](https://github.com/loopj/android-async-http) - Simple asynchronous HTTP requests with JSON parsing
 * [codepath-oauth](https://github.com/thecodepath/android-oauth-handler) - Custom-built library for managing OAuth authentication and signing of requests
 * [Picasso](https://github.com/square/picasso) - Used for async image loading and caching them in memory and on disk.
 * [ActiveAndroid](https://github.com/pardom/ActiveAndroid) - Simple ORM for persisting a local SQLite database on the Android device

## Usage

### 1. Configure the REST client

Open `src/com.codepath.apps.restclienttemplate/RestClient.java`. Configure the `REST_API_CLASS`, `REST_URL`, `REST_CONSUMER_KEY`, `REST_CONSUMER_SECRET` based on the values needed to connect to your particular API. The `REST_URL` should be the base URL used for connecting to the API (i.e `https://api.twitter.com`). The `REST_API_CLASS` should be the class defining the [service](https://github.com/fernandezpablo85/scribe-java/tree/master/src/main/java/org/scribe/builder/api) you wish to connect to. Check out the [full list of services](https://github.com/fernandezpablo85/scribe-java/tree/master/src/main/java/org/scribe/builder/api) you can select (i.e `FlickrApi.class`).

For example if I wanted to connect to Twitter:

```java
// RestClient.java
public class RestClient extends OAuthBaseClient {
    public static final Class<? extends Api> REST_API_CLASS = TwitterApi.class;
    public static final String REST_URL = "http://api.twitter.com/1.1";
    public static final String REST_CONSUMER_KEY = "57fdgdfh345195e071f9a761d763ca0";
    public static final String REST_CONSUMER_SECRET = "d657sdsg34435435";
    // ...constructor and endpoints
}
```

Next, change the REST_CALLBACK_URL to a unique name that is special for this application.
This is used for the OAuth authentication flow:

```java
// RestClient.java
public static final String REST_CALLBACK_URL = "oauth://codepathtweets";
```

Also, be sure to **change this value** in the `AndroidManifest.xml` to match the same host:

```java
// AndroidManifest.xml
// manifest => application => activity
<intent-filter>
    <action android:name="android.intent.action.VIEW" />

    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />

    <data
        android:host="codepathtweets"
        android:scheme="oauth" />
</intent-filter>
```

Next, you want to define the endpoints which you want to retrieve data from or send data to within your client:

```java
// RestClient.java
public void getHomeTimeline(int page, AsyncHttpResponseHandler handler) {
  String apiUrl = getApiUrl("statuses/home_timeline.json");
  RequestParams params = new RequestParams();
  params.put("page", String.valueOf(page));
  getClient().get(apiUrl, params, handler);
}
```

Note we are using `getApiUrl` to get the full URL from the relative fragment and `RequestParams` to control the request parameters.
You can easily send post requests (or put or delete) using a similar approach:

```java
// RestClient.java
public void postTweet(String body, AsyncHttpResponseHandler handler) {
    String apiUrl = getApiUrl("statuses/update.json");
    RequestParams params = new RequestParams();
    params.put("status", body);
    getClient().post(apiUrl, params, handler);
}
```

These endpoint methods will automatically execute asynchronous requests signed with the authenticated access token. To use JSON endpoints, simply invoke the method
with a `JsonHttpResponseHandler` handler:

```java
// SomeActivity.java
RestClient client = RestApplication.getRestClient();
client.getHomeTimeline(1, new JsonHttpResponseHandler() {
    @Override
    public void onSuccess(int statusCode, Header[] headers, JSONArray json) {
    // Response is automatically parsed into a JSONArray
    // json.getJSONObject(0).getLong("id");
  }
});
```

Based on the JSON response (array or object), you need to declare the expected type inside the OnSuccess signature i.e
`public void onSuccess(JSONObject json)`. If the endpoint does not return JSON, then you can use the `AsyncHttpResponseHandler`:

```java
RestClient client = RestApplication.getRestClient();
client.getSomething(new AsyncHttpResponseHandler() {
    @Override
    public void onSuccess(int statusCode, Header[] headers, String response) {
        System.out.println(response);
    }
});
```
Check out [Android Async HTTP Docs](http://loopj.com/android-async-http/) for more request creation details.

### 2. Define the Models

In the `src/com.codepath.apps.restclienttemplate.models`, create the models that represent the key data to be parsed and persisted within your application.
For example, if you were connecting to Twitter, you would want a Tweet model as follows:

```java
// models/Tweet.java
package com.codepath.apps.restclienttemplate.models;

import org.json.JSONException;
import org.json.JSONObject;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

@Table(name = "Tweets")
public class Tweet extends Model {
  // Define database columns and associated fields
  @Column(name = "userId")
  String userId;
  @Column(name = "userHandle")
  String userHandle;
  @Column(name = "timestamp")
  String timestamp;
  @Column(name = "body")
  String body;

  // Make sure to always define this constructor with no arguments
  public Tweet() {
    super();
  }
}
```

Notice here we specify the SQLite table for a resource, the columns for that table, and a constructor for
turning the JSON object fetched from the API into this object. For more information on creating a model,
check out the [ActiveAndroid Wiki](https://github.com/pardom/ActiveAndroid/wiki/Creating-your-database-model).

In addition, we can also add functions into the model to support parsing JSON attributes in order to instantiate the model based on API data. This might look like:

```java
// models/Tweet.java
@Table(name = "Tweets")
public class Tweet extends Model {
  // ...existing code from above...

  // Add a constructor that creates an object from the JSON response
  public Tweet(JSONObject object){
    super();

    try {
      this.userId = object.getString("user_id");
      this.userHandle = object.getString("user_username");
      this.timestamp = object.getString("timestamp");
      this.body = object.getString("body");
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  public static ArrayList<Tweet> fromJson(JSONArray jsonArray) {
    ArrayList<Tweet> tweets = new ArrayList<Tweet>(jsonArray.length());

    for (int i=0; i < jsonArray.length(); i++) {
        JSONObject tweetJson = null;
        try {
            tweetJson = jsonArray.getJSONObject(i);
        } catch (Exception e) {
            e.printStackTrace();
            continue;
        }

        Tweet tweet = new Tweet(tweetJson);
        tweet.save();
        tweets.add(tweet);
    }

    return tweets;
  }
}
```

Now you have a model that supports proper creation based on JSON. Create models for all the resources
necessary for your mobile client.

### 3. Setup Your Authenticated Activities

Open `src/com.codepath.apps.restclienttemplate/LoginActivity.java` and configure the `onLoginSuccess` method
which fires once your app has access to the authenticated API. Launch an activity and begin using your REST client:

```java
// LoginActivity.java
@Override
public void onLoginSuccess() {
  Intent i = new Intent(this, TimelineActivity.class);
  startActivity(i);
}
```

In your new authenticated activity, you can access your client anywhere with:

```java
RestClient client = RestApplication.getRestClient();
client.getHomeTimeline(1, new JsonHttpResponseHandler() {
  public void onSuccess(int statusCode, Header[] headers, JSONArray jsonArray) {
    Log.d("DEBUG", "timeline: " + jsonArray.toString());
    // Load json array into model classes
  }
});
```

You can then load the data into your models from a `JSONArray` using:

```java
ArrayList<Tweet> tweets = Tweet.fromJSON(jsonArray);
```

or load the data from a single `JSONObject` with:

```java
Tweet t = new Tweet(json);
// t.body = "foo"
t.save();
```

That's all you need to get started. From here, hook up your activities and their behavior, adjust your models and add more REST endpoints.

### Extras

#### Loading Images with Picasso

If you want to load a remote image url into a particular ImageView, you can use Picasso to do that with:

```java
Picasso.with(this).load(imageUrl).
  noFade().fit().into(imageView);
```

This will load an image into the specified ImageView and resize the image to fit.

#### Logging Out

You can log out by clearing the access token at any time through the client object:

```java
RestClient client = RestApplication.getRestClient();
client.clearAccessToken();
```
