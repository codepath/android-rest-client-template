package com.codepath.apps.restclienttemplate.models;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;

@Table(name = "photos")
public class SampleModel extends Model { 
	@Column(name = "name")
	private String name;
	
	public SampleModel() {
		super();
	}
	
	public SampleModel(JSONObject object){
		super();

		try {
			this.name = object.getString("title");
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public String getName() {
		return name;
	}
	
	public static SampleModel byId(long id) {
	   return new Select().from(SampleModel.class).where("id = ?", id).executeSingle();
	}
	
	public static ArrayList<SampleModel> recentItems() {
      return new Select().from(SampleModel.class).orderBy("id DESC").limit("300").execute();
	}
}
