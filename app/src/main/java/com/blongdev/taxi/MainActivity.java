package com.blongdev.taxi;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.json.JSONException;
import org.json.JSONObject;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Point;
import android.support.v4.view.GestureDetectorCompat;
import android.util.TypedValue;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity
{	
	//set constants
	private static final String KEY = "AIzaSyDLylNkbp8re6uFkJC5cfzrz21Jo9L9XEA";
	private static final double FIVEMILES = 8046.7; 
	private static final int MENU_ABOUT = Menu.FIRST+1;	

	
	//allocate refList variables
	private List<TaxiService> refList; 
	private int size = 0;
	
	//allocate view objects
	private TextView nameText;
	private TextView ratingText;
	private TextView ratingLabel;
	private TextView phoneText;
	private TextView phoneLabel;
	private TextView openText;
	private TextView openLabel;
	private TextView distanceText;
	private TextView distanceLabel;
	private ImageView image;
	private ImageView left;
	private ImageView right;
	private View myView;
	
    //allocate search result stuff	
	private Location location = null;
	private GestureDetectorCompat mDetector; 
	private LocationManager locationManager;
	private String provider;
	private JSONObject placeResultsJSON;
	private int placeNum = 0;
	private String name = null; 
	private String phone = null;
	private String rating = null;
	private String photo = null;
	private String open = null;
	private String lat = null;
	private String lng = null;
	private String distance = null;
	private String detailResults = null;
	
	private Handler handler = new Handler() 
	{
		  @Override
		  public void handleMessage(Message msg) 
		  {			
			  //get items from bundle
			  Bundle bundle = msg.getData(); 
			  String nameStr = bundle.getString("name");
			  String ratingStr = bundle.getString("rating");
			  String phoneStr = bundle.getString("phone");
			  String openStr = bundle.getString("open");
			  String distanceStr = bundle.getString("distance");
			  
			  //set view items
			  nameText.setText(nameStr);
			  ratingText.setText(ratingStr);
			  phoneText.setText(phoneStr);
			  openText.setText(openStr);
			  distanceText.setText(distanceStr);
			  
			  //set image
			  Bitmap img = (Bitmap) bundle.getParcelable("imageBitmap");
			  if(img != null)
				  image.setImageBitmap(img);
			  else
				  image.setImageResource(R.drawable.no_photo);
			  
			  //set left arrow to invisible on first result
			  if(placeNum == 0)
				  left.setVisibility(View.INVISIBLE);
			  else 
				  left.setVisibility(View.VISIBLE);
			  
			  //set right arrow to invisible on last result
			  if(placeNum == size - 1)
				  right.setVisibility(View.INVISIBLE);
			  else
				  right.setVisibility(View.VISIBLE);
	  
		  }
	};
		 
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		
		
		//load splash for a couple seconds while getting places results
		startSplash();
		
		if(isNetworkConnected())
		{
			//initialize arrayList
			refList = new ArrayList<TaxiService>();
			
			//get view references
			myView = (View) findViewById(R.id.my_view);
			nameText = (TextView) findViewById(R.id.name);
			phoneLabel = (TextView) findViewById(R.id.phoneLabel);
			phoneText = (TextView) findViewById(R.id.phone);
			ratingLabel = (TextView) findViewById(R.id.ratingLabel);
			ratingText = (TextView) findViewById(R.id.rating);
			openLabel = (TextView) findViewById(R.id.openLabel);
			openText = (TextView) findViewById(R.id.open);		
			distanceLabel = (TextView) findViewById(R.id.distanceLabel);
			distanceText = (TextView) findViewById(R.id.distance);
			image = (ImageView) findViewById(R.id.imageView);
			left = (ImageView) findViewById(R.id.left);
			right = (ImageView) findViewById(R.id.right);
			
			
			
			//get places results and store references in refList
			getPlaces();
			
			//set gesture listener for swipes and taps
			mDetector = new GestureDetectorCompat(this, new MyGestureListener());		
			myView.setOnTouchListener(new OnTouchListener() {
		            @Override
		            public boolean onTouch(final View view, final MotionEvent event) {
		                mDetector.onTouchEvent(event);
		                return true;
		            }
		        });
			
			left.setOnClickListener(new OnClickListener() {
			    public void onClick(View v) {
			    	//create runnable
   					Runnable runnable = (new Runnable() 
   					{
   						public void run() 
   						{     	
   							//load the place to the left
   							if(placeNum-1 >= 0)
   								getPlaceInfo(--placeNum);
   						}
   					});
   					
   					//start runnable on new thread
   					Thread mythread = new Thread(runnable);
   					mythread.start();
			    }
			});
			    
			right.setOnClickListener(new OnClickListener() {
			    public void onClick(View v) {
			    	
			    	//create runnable
   					Runnable runnable = (new Runnable() 
   					{
   						public void run() 
   						{     	
   							//load the place to the right
   							if(placeNum+1 < size)
   								getPlaceInfo(++placeNum);
   						}
   					});
   					
   					//start runnable on new thread
   					Thread mythread = new Thread(runnable);
   					mythread.start();

			    }
			});
	
			//get screen size
			Display display = getWindowManager().getDefaultDisplay();
			Point screenSize = new Point();
			display.getSize(screenSize);
			
			//find shorter side (landscape or portrait)
			int shortSide;
			if(screenSize.x < screenSize.y)
				shortSide = screenSize.x;
			else
				shortSide = screenSize.y;
			
			//set text sizes
			nameText.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float) .1 * shortSide);
			phoneLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float) .05 * shortSide);
			phoneText.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float) .05 * shortSide);
			ratingLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float) .05 * shortSide);
			ratingText.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float) .05 * shortSide);
			openLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float) .05 * shortSide);
			openText.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float) .05 * shortSide);
			distanceLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float) .05 * shortSide);
			distanceText.setTextSize(TypedValue.COMPLEX_UNIT_PX, (float) .05 * shortSide);	
			
		}
		else
		{
			showConnectionErrorDialog();
		}
		
	}
	
	private boolean isNetworkConnected() 
	{
		  ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		  NetworkInfo ni = cm.getActiveNetworkInfo();
		  if (ni == null) 
		  {
			  return false;
		  }
		  else
			  return true;
	}
	
	public Bitmap getImage(String ref)
	{		 
		//check if the search returned a photo
		if(photo != null)
		{
			URL url;				
			BufferedInputStream is = null;

			//get image using reference
			try {
				url = new URL("https://maps.googleapis.com/maps/api/place/photo" +
			  		"?maxwidth=1000" + 
			  		"&photoreference=" + ref +
					"&sensor=true" +
					"&key=" + KEY);

				is = new BufferedInputStream(url.openConnection().getInputStream(), 1024);
				Options options = new BitmapFactory.Options();
				Bitmap bit = BitmapFactory.decodeStream(is, null, options);
				
				return bit;
				
			}catch (Exception e) {
				e.printStackTrace();
				return null;
			}
			finally
			{
				//close inputStream
				try{if(is != null)is.close();}catch(Exception squish){}
			}
		}
		return null;
	}
	
	public Location getLocation()
	{
		//set location manager
	    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
	       
    	//choose best provider
    	if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
    		provider = LocationManager.NETWORK_PROVIDER;
    	else if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
    		provider = LocationManager.GPS_PROVIDER;
    	else
    		return null;
	   
		//return the current location
		return locationManager.getLastKnownLocation(provider);
	}
		
	private void showConnectionErrorDialog()
	{
		new AlertDialog.Builder(this)		//create a dialog box with about information
		.setMessage("Could not get connect to the internet.")	
		.setCancelable(false)
		.setPositiveButton("Ok", new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int id){
				dialog.dismiss();
			    //startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));	
			}
		}
		).show();
	}	
	
	private void showLocationErrorDialog()
	{
		new AlertDialog.Builder(this)		//create a dialog box with about information
		.setMessage("Could not get location information. You may need to enable location access in your settings.")	
		.setCancelable(false)
		.setPositiveButton("Ok", new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int id){
				dialog.dismiss();
			    //startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));	
			}
		}
		).show();
	}	
	
	public void getPlaces()
	{
		//create runnable
		Runnable runnable = (new Runnable() 
		{
			public void run() 
			{     	
				//empty old results
				placeNum = 0;
				if(refList != null)
					refList.clear();
				
				//get current location
				if((location = getLocation()) == null)
				{
					return;
				}
				
				String places = "https://maps.googleapis.com/maps/api/place/search/json" +
						"?location=" + location.getLatitude() + "," + location.getLongitude() +
						"&keyword=taxi" +
						"&rankby=distance" +
						"&sensor=true" +
						"&key=" + KEY;
				
				String placesResults = getJSON(places);
				
				if(placesResults == null)
				{
					return;
				}
				
				
				//get list of places 
				try {
					
					placeResultsJSON = new JSONObject(placesResults);
					
					size = placeResultsJSON.getJSONArray("results").length();
					
					//store all references in refList
					for(int i = 0; i < size; i++)
					{	
						TaxiService taxi = new TaxiService(placeResultsJSON.getJSONArray("results").getJSONObject(i).getString("reference"));
						refList.add(taxi);
					}
					
				} catch (JSONException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
				}
			
				//get and load info on first place in results
				getPlaceInfo(0);						    

			}
		
		});
		
		//start runnable in new thread
		Thread mythread = new Thread(runnable);
		mythread.start();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		menu.add(Menu.NONE, MENU_ABOUT, Menu.NONE, "About");		//add options menu item	
		return true;
	}
	
	@Override
	protected void onResume() {
	    super.onResume();
	    
	  //get current location
	    Location loc;
	    if((loc = getLocation()) == null)
		{
			showLocationErrorDialog();
			return;
		}
	    else
	    {
	    
	    //refresh the places list if the user has moved by 5 or more miles
	    if(location == null || (loc.distanceTo(location) > FIVEMILES) )
	    	getPlaces();
	    }
	}

	@Override
	protected void onPause() {
	    super.onPause();
	}

	public String getJSON(String post)
	{
		//set http parameters
		DefaultHttpClient   httpclient = new DefaultHttpClient(new BasicHttpParams());
		HttpPost httppost = new HttpPost(post);
		httppost.setHeader("Content-type", "application/json");

		InputStream inputStream = null;
		String result = null;
		
		//get json
		try 
		{
			//get http response
		    HttpResponse response = httpclient.execute(httppost);           
		    HttpEntity entity = response.getEntity();

		    //create inputStream for json content
		    inputStream = entity.getContent();
		    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"), 8);
		    StringBuilder sb = new StringBuilder();

		    //copy inputStream into sb line by line
		    String line = null;
		    while ((line = reader.readLine()) != null)
		    {
		        sb.append(line + "\n");
		    }
		    result = sb.toString();
		
		}    
		catch (Exception e) 
		{ 

		}
		finally 
		{
			//close inputStream
			try{if(inputStream != null)inputStream.close();}catch(Exception squish){}
		}
		
		 return result;

	}

	private boolean getPlaceInfo(int index)
	{
		//make sure there are objects in refList
		if(size > 0)
		{
			//create message and bundle
			Message msg = handler.obtainMessage();
			Bundle bundle = new Bundle();
		
			//get current taxiService
			TaxiService taxi = refList.get(index);
		
			//check if it has already had info queried. if so, fill info from refList
			if(taxi.getDetailed())
			{
				bundle.putString("name", taxi.getName());
				bundle.putString("phone", taxi.getPhone());
				bundle.putString("rating", taxi.getRating());
				bundle.putParcelable("imageBitmap", taxi.getPhoto());
				bundle.putString("open", taxi.getOpen());
				bundle.putString("distance", taxi.getDistance());
						
				//send bundle to main thread
				msg.setData(bundle);
				handler.sendMessage(msg);
				
				return true;
			}
		
			try
			{
				//get detailed place result
				String details = "https://maps.googleapis.com/maps/api/place/details/json" +
						"?reference=" + refList.get(index).getReference() +
						"&sensor=true" +
						"&key=" + KEY;
				
				if((detailResults = getJSON(details)) == null)
				{
					return false;
				}
				
				JSONObject detailResultJSON = new JSONObject(detailResults);
				JSONObject detailAttrJSON = detailResultJSON.getJSONObject("result");
				
				//get place name
				try
				{
					
					name = detailAttrJSON.getString("name");
				}catch (JSONException e) {
					// TODO Auto-generated catch block
					name = "Unavailable";
					e.printStackTrace();
				}
				
				//get place phone number
				try
				{
					phone = detailAttrJSON.getString("formatted_phone_number");
				}catch (JSONException e) {
					// TODO Auto-generated catch block
					phone = "Unavailable";
					e.printStackTrace();
				}
				
				//get place rating
				try
				{
					rating = detailAttrJSON.getString("rating") + "/5.0";
				}catch (JSONException e) {
					// TODO Auto-generated catch block
					rating = "Unavailable";
					e.printStackTrace();
				}
				
				//get place availability
				try
				{
					JSONObject openJSON = detailAttrJSON.getJSONObject("opening_hours");
					boolean openBool = openJSON.getBoolean("open_now");
					if(openBool)
						open = "Yes";
					else
						open = "No";
				}catch (JSONException e) {
					// TODO Auto-generated catch block
					open = "Unavailable";
					e.printStackTrace();
				}
				
				//get place photo
				try
				{
					JSONObject photoJSON = detailAttrJSON.getJSONArray("photos").getJSONObject(0);
					photo = photoJSON.getString("photo_reference");
				}catch (JSONException e) {
					// TODO Auto-generated catch block
					photo = null;
					e.printStackTrace();
				}
	
				//get place distance
				try
				{
					//get latitude and longitude
					JSONObject geoJSON = detailAttrJSON.getJSONObject("geometry");
					JSONObject locJSON = geoJSON.getJSONObject("location");
					lat = locJSON.getString("lat");
					lng = locJSON.getString("lng");
					
					//create location object
					Location placeLoc = new Location("Place");
					placeLoc.setLatitude(Double.parseDouble(lat));
					placeLoc.setLongitude(Double.parseDouble(lng));
					
					//get current location
					Location currentLoc = getLocation();
					
					//calculate distance
					float distMeters = currentLoc.distanceTo(placeLoc);
					float distKilometers = distMeters/1000;
					double distMiles = distKilometers * 0.621371;
					
					distance = String.format("%.2f M / %.2f km", distMiles, distKilometers);
							
				}catch (JSONException e) {
					// TODO Auto-generated catch block
					lat = "Unavailable";
					lng = "Unavailable";
					e.printStackTrace();
				}
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
			if(photo != null)
			{
				Bitmap bit = getImage(photo);
				bundle.putParcelable("imageBitmap", bit);
				taxi.setPhoto(bit);
			}
				
			
			//add info to refList object
			taxi.setName(name);
			taxi.setPhone(phone);
			taxi.setRating(rating);
			taxi.setOpen(open);
			taxi.setDistance(distance);
			taxi.setDetailed(true);
			
			//add info to bundle
			bundle.putString("name", name);
			bundle.putString("phone", phone);
			bundle.putString("rating", rating);
			bundle.putString("photo", photo);
			bundle.putString("open", open);
			bundle.putString("lat", lat);
			bundle.putString("lng", lng);
			bundle.putString("distance", distance);
			
			//send info to main thread
			msg.setData(bundle);
			handler.sendMessage(msg);
		
		}
		
		return true;
	}
	
	@Override 
    public boolean onTouchEvent(MotionEvent event){ 
        this.mDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }
    
    class MyGestureListener extends GestureDetector.SimpleOnGestureListener 
    {
        //set swipe threshold
        private static final int SWIPE_THRESHOLD = 100;
    	
        @Override
        public boolean onDown(MotionEvent event) { 
        	return true;
        }
        
        @Override
        public boolean onSingleTapConfirmed(MotionEvent event) {
        	
        	//check that refList isnt empty
        	if(size > 0)
        	{
        		//get place phone number and start call
        		Intent intent = new Intent(Intent.ACTION_CALL);
        		intent.setData(Uri.parse("tel:" + refList.get(placeNum).getPhone()));
        		startActivity(intent);

        		return true;
        	}
        	
        	return false;
        }

        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2, 
                float velocityX, float velocityY) {

    		//get the swipe distance
        	float diffX = event2.getX() - event1.getX();

        	//make sure swipe was far enough
           	if(Math.abs(diffX) > SWIPE_THRESHOLD) 
           	{
           		//if the user swiped right
           		if(diffX > 0) 				
           		{
           			//create runnable
   					Runnable runnable = (new Runnable() 
   					{
   						public void run() 
   						{     	
   							//load the place to the left
   							if(placeNum-1 >= 0)
   								getPlaceInfo(--placeNum);
   						}
   					});
   					
   					//start runnable on new thread
   					Thread mythread = new Thread(runnable);
   					mythread.start();
               } 
               else if(diffX < 0)						//if the user swiped left
               {
            	   //create runnable
            	   	Runnable runnable = (new Runnable() 
   					{
            	   		public void run() 
            	   		{     	
            	   				//load place to the right
								if(placeNum+1 < size)
									getPlaceInfo(++placeNum);
            	   		}
   					});
   				
            	   	//start runnable on new thread
            	   	Thread mythread = new Thread(runnable);
            	   	mythread.start();
               }
           	}
           return true;
        }
    }

    
    private void startSplash() 
    {
        Intent intent = new Intent(this, SplashActivity.class);
        startActivity(intent);
    }
    
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())	//determine which option was selected
		{
			case MENU_ABOUT:			//if ABOUT
				showAboutDialog();		//show the about information
				return true;			//exit function
		}
		return (super.onOptionsItemSelected(item));	//call parent class method
	}
	
	private void showAboutDialog()
	{
		new AlertDialog.Builder(this)		//create a dialog box with about information
		.setMessage("Tap to Call\nSlide to Change\nCreated by Brian Long")	
		.setCancelable(false)
		.setPositiveButton("Ok", new DialogInterface.OnClickListener(){
			public void onClick(DialogInterface dialog, int id){ dialog.dismiss();}
		}
		).show();
	}	
}
	
	
	


