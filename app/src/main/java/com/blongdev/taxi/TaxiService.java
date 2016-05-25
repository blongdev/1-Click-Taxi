package com.blongdev.taxi;

import android.graphics.Bitmap;

//Holds info for a taxi service
class TaxiService 
{
	private boolean detailed;
	private String reference;
	private String name;
	private String distance;
	private String phone;
	private Bitmap photo;
	private String open;
	private String rating;
	
	TaxiService(String ref)
	{
		reference = ref;
		detailed = false;
	}
	
	TaxiService()
	{
		detailed = false;
	}
	
	public void setDetailed(boolean d)
	{
		detailed = d;
	}
	
	public void setReference(String r)
	{
		reference = r;
	}
	
	public void setName(String n)
	{
		name = n;
	}
	
	public void setDistance(String d)
	{
		distance = d;
	}
	
	public void setPhone(String p)
	{
		phone = p;
	}
	
	public void setPhoto(Bitmap p)
	{
		photo = p;
	}
	
	public void setOpen(String o)
	{
		open = o;
	}
	
	public void setRating(String r)
	{
		rating = r;
	}
	
	public boolean getDetailed()
	{
		return detailed;
	}
	
	public String getReference()
	{
		return reference;
	}
	
	public String getName()
	{
		return name;
	}
	
	public String getDistance()
	{
		return distance;
	}
	
	public String getPhone()
	{
		return phone;
	}
	
	public Bitmap getPhoto()
	{
		return photo;
	}
	
	public String getOpen()
	{
		return open;
	}
	
	public String getRating()
	{
		return rating;
	}
}



