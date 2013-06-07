package com.codepath.apps.restclienttemplate;

import android.os.Bundle;
import android.view.Menu;
import android.view.View;

import com.codepath.oauth.OAuthLoginActivity;

public class LoginActivity extends OAuthLoginActivity<RestClient> {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.login, menu);
		return true;
	}
	
	// OAuth authenticated successfully, launch primary authenticated activity
    @Override
    public void onLoginSuccess() {
    	// Intent i = new Intent(this, PhotosActivity.class);
    	// startActivity(i);
    }
    
    // OAuth authentication flow failed, handle the error
    @Override
    public void onLoginFailure(Exception e) {
        e.printStackTrace();
    }
    
    // Click handler method for the button used to start OAuth flow
    // Uses the client to initiate OAuth authorization
    public void loginToRest(View view) {
        getClient().connect();
    }

}
