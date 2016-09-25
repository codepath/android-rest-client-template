package com.codepath.apps.restclienttemplate.models;

import com.codepath.apps.restclienttemplate.MyDatabase;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.sql.language.Select;
import com.raizlabs.android.dbflow.structure.BaseModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/*
 * This is a temporary, sample model that demonstrates the basic structure
 * of a SQLite persisted Model object. Check out the DBFlow wiki for more details:
 * https://github.com/pardom/ActiveAndroid/wiki/Creating-your-database-model
 * 
 */
@Table(database = MyDatabase.class)
public class SampleModel extends BaseModel {

	@PrimaryKey
	@Column
	Long id;

	// Define table fields
	@Column(name = "name")
	private String name;

	public SampleModel() {
		super();
	}

	// Parse model from JSON
	public SampleModel(JSONObject object){
		super();

		try {
			this.name = object.getString("title");
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	// Getters
	public String getName() {
		return name;
	}

	// Setters
	public void setName(String name) {
		this.name = name;
	}

	// Record Finders
	public static SampleModel byId(long id) {
		return new Select().from(SampleModel.class).where(SampleModel_Table.id.eq(id)).querySingle();
	}

	public static List<SampleModel> recentItems() {
		return new Select().from(SampleModel.class).orderBy(SampleModel_Table.id, false).limit(300).queryList();
	}
}
