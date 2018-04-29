# RestClientTemplate [![Build Status](https://travis-ci.org/codepath/android-rest-client-template.svg?branch=master)](https://travis-ci.org/codepath/android-rest-client-template)

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
 * [Glide](https://github.com/bumptech/glide) - Used for async image loading and caching them in memory and on disk.
 * [Room](https://developer.android.com/training/data-storage/room/index.html) - Simple ORM for persisting a local SQLite database on the Android device

## Usage

### 1. Configure the REST client

Open `src/com.codepath.apps.restclienttemplate/RestClient.java`. Configure the `REST_API_INSTANCE`, `REST_URL`, `REST_CONSUMER_KEY`, `REST_CONSUMER_SECRET` based on the values needed to connect to your particular API. The `REST_URL` should be the base URL used for connecting to the API (i.e `https://api.twitter.com`). The `REST_API_INSTANCE` should be the class defining the service you wish to connect to. Check out the [full list of services](https://github.com/scribejava/scribejava/tree/master/scribejava-apis/src/main/java/com/github/scribejava/apis) you can select (i.e `FlickrApi.instance()`).

For example if I wanted to connect to Twitter:

```java
// RestClient.java
public class RestClient extends OAuthBaseClient {
    public static final BaseApi REST_API_INSTANCE = TwitterApi.instance();
    public static final String REST_URL = "https://api.twitter.com/1.1";
    public static final String REST_CONSUMER_KEY = "57fdgdfh345195e071f9a761d763ca0";
    public static final String REST_CONSUMER_SECRET = "d657sdsg34435435";
    // ...constructor and endpoints
}
```

Next, change the `intent_scheme` and `intent_host` in `strings.xml` to a unique name that is special for this application.
This is used for the OAuth authentication flow for launching the app through web pages through an [Android intent](https://developer.chrome.com/multidevice/android/intents).

```xml
<string name="intent_scheme">oauth</string>
<string name="intent_host">codepathtweets</string>
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

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import org.json.JSONException;
import org.json.JSONObject;

@Entity
public class Tweet {
  // Define database columns and associated fields
  @PrimaryKey
  @ColumnInfo
  Long id;
  @ColumnInfo
  String userHandle;
  @ColumnInfo
  String timestamp;
  @ColumnInfo
  String body;

  // Use @Embedded to keep the column entries as part of the same table while still
  // keeping the logical separation between the two objects.
  @Embedded
  User user;
}
```

Note there is a separate `User` object but it will not actually be declared as a separate table.  By using the `@Embedded` annotation, the fields in this class will be stored as part of the Tweet table.  Room specifically does not load references between two different entities for performance reasons (see https://developer.android.com/training/data-storage/room/referencing-data), so declaring it this way causes the data to be denormalized as one table.

```java
// models/User.java

public class User {

    @ColumnInfo
    String name;

    // normally this field would be annotated @PrimaryKey because this is an embedded object
    // it is not needed
    @ColumnInfo  
    Long twitter_id;
}
```
Notice here we specify the SQLite table for a resource, the columns for that table, and a constructor for turning the JSON object fetched from the API into this object. For more information on creating a model, check out the [Room guide](https://developer.android.com/training/data-storage/room/).

In addition, we also add functions into the model to support parsing JSON attributes in order to instantiate the model based on API data.  For the User object, the parsing logic would be:

```java
// Parse model from JSON
public static User parseJSON(JSONObject tweetJson) {

    User user = new User();
    this.twitter_id = tweetJson.getLong("id");
    this.name = tweetJson.getString("name");
    return user;
}
```

For the Tweet object, the logic would would be:

```java
// models/Tweet.java
@Entity
public class Tweet {
  // ...existing code from above...

  // Add a constructor that creates an object from the JSON response
  public Tweet(JSONObject object){
    try {
      this.user = User.parseJSON(object.getJSONObject("user"));
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
        tweets.add(tweet);
    }

    return tweets;
  }
}
```


Now you have a model that supports proper creation based on JSON. Create models for all the resources necessary for your mobile client.

### 4. Define your queries

Next, you will need to define the queries by creating a Data Access Object (DAO) class.   Here is an example of declaring queries to return a Tweet by the post ID, retrieve the most recent tweets, and insert tweets.   

```java
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface TwitterDao {
    // Record finders
    @Query("SELECT * FROM Tweet WHERE post_id = :tweetId")
    Tweet byTweetId(Long tweetId);

    @Query("SELECT * FROM Tweet ORDER BY created_at")
    List<Tweet> getRecentTweets();

    // Replace strategy is needed to ensure an update on the table row.  Otherwise the insertion will
    // fail.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTweet(Tweet... tweets);
}
```

The examples here show how to perform basic queries on the Tweet table.  If you need to declare one-to-many or many-to-many relations, see the guides on using the [@Relation](https://developer.android.com/reference/android/arch/persistence/room/Relation) and [@ForeignKey](https://developer.android.com/reference/android/arch/persistence/room/ForeignKey) annotations.

### 5. Create database

We need to define a database that extends `RoomDatabase` and describe which entities as part of this database. We also need to include what data access objects are to be included.  If the entities are modified or additional ones are included, the version number will need to be changed.  Note that only the `Tweet` class is declared:

```java
// bump version number if your schema changes
@Database(entities={Tweet.class}, version=1)
public abstract class MyDatabase extends RoomDatabase {
  // Declare your data access objects as abstract
  public abstract TwitterDao twitterDao();

  // Database name to be used
  public static final String NAME = "MyDataBase";

```    

When compiling the code, the schemas will be stored in a `schemas/` directory assuming this statement
has been included your `app/build.gradle` file.  These schemas should be checked into your code based.

```gradle
android {
    defaultConfig {

        javaCompileOptions {
            annotationProcessorOptions {
                arguments = ["room.schemaLocation": "$projectDir/schemas".toString()]
            }
        }
    }

}
```

### 6. Initialize database

Inside your application class, you will need to initialize the database and specify a name for it.

```java
public class RestClientApp extends Application {

  MyDatabase myDatabase;

  @Override
  public void onCreate() {
    // when upgrading versions, kill the original tables by using fallbackToDestructiveMigration()
    myDatabase = Room.databaseBuilder(this, MyDatabase.class, MyDatabase.NAME).fallbackToDestructiveMigration().build();
  }

  public MyDatabase getMyDatabase() {
    return myDatabase;
  }

}
```

### 7. Setup Your Authenticated Activities

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
```

To save, you will need to perform the database operation on a separate thread by creating an `AsyncTask` and adding the item:

```java
AsyncTask<Tweet, Void, Void> task = new AsyncTask<Tweet, Void, Void>() {
    @Override
    protected Void doInBackground(Tweet... tweets) {
      TwitterDao twitterDao = ((RestApplication) getApplicationContext()).getMyDatabase().twitterDao();
      twitterDao.insertModel(tweets);
      return null;
    };
  };
  task.execute(tweets);
```

That's all you need to get started. From here, hook up your activities and their behavior, adjust your models and add more REST endpoints.

### Extras

#### Loading Images with Glide

If you want to load a remote image url into a particular ImageView, you can use Glide to do that with:

```java
Glide.with(this).load(imageUrl)
     .into(imageView);
```

This will load an image into the specified ImageView and resize the image to fit.

#### Logging Out

You can log out by clearing the access token at any time through the client object:

```java
RestClient client = RestApplication.getRestClient();
client.clearAccessToken();
```

### Viewing SQL table

You can use `chrome://inspect` to view the SQL tables once the app is running on your emulator.  See [this guide](https://guides.codepath.com/android/Debugging-with-Stetho) for more details.

### Troubleshooting

* If you receive the following error `org.scribe.exceptions.OAuthException: Cannot send unauthenticated requests for TwitterApi client. Please attach an access token!` then check the following:
 * Is your intent-filter with `<data>` attached to the `LoginActivity`? If not, make sure that the `LoginActivity` receives the request after OAuth authorization.
 * Is the `onLoginSuccess` method being executed in the `LoginActivity`. On launch of your app, be sure to start the app on the LoginActivity so authentication routines execute on launch and take you to the authenticated activity.
 * If you are plan to test with Android API 24 or above, you will need to use Chrome to launch the OAuth flow.  
 * Note that the emulators (both the Google-provided x86 and Genymotion versions) for API 24+ versions can introduce intermittent issues when initiating the OAuth flow for the first time.  For best results, use an device for this project.
