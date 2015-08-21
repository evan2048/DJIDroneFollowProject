package com.evan2048.util;

public class GNGGAParser {
	
	private double latitude = 0.0;
	private double longitude = 0.0;
	private int satelliteCount = 0;
	private double HDOP = 0.0;
	//private String demoSentence="$GNGGA,065747.00,2232.39441,N,11356.82451,E,1,12,0.88,22.9,M,-2.7,M,,*67";
	//private String emptySentence="$GNGGA,,,,,,0,00,99.99,,,,,,*56";
	
	//parser sentence
	public GNGGAParser(String sentence)
	{
		//split sentence
		String[] splitSentence=sentence.split(",");
		//get raw string
		String latRaw=splitSentence[2];
		String lngRaw=splitSentence[4];
		String satelliteCountRaw=splitSentence[7];
		String HDOPRaw=splitSentence[8];
		//check null
		if(latRaw.equals("") || lngRaw.equals("") || satelliteCountRaw.equals("") || HDOPRaw.equals(""))
		{
			//gps not fix yet
			return;
		}
		//change position from like 2232.39441 to 22.3239441
		double latChange=Double.parseDouble(latRaw)*0.01;
		double lngChange=Double.parseDouble(lngRaw)*0.01;
		//get position decimal
		double latDecimal=(latChange%1)/0.6;  //0.6 means minute to degree
		double lngDecimal=(lngChange%1)/0.6;
		//get final position
		double lat=(int)latChange+latDecimal;
		double lng=(int)lngChange+lngDecimal;
		int sat=Integer.parseInt(satelliteCountRaw);
		double hdop=Double.parseDouble(HDOPRaw);
		//save
		this.latitude=lat;
		this.longitude=lng;
		this.satelliteCount=sat;
		this.HDOP=hdop;
	}
	
	//get latitude
	public double getLatitude()
	{
		return this.latitude;
	}
	
	//get longitude
	public double getLongitude()
	{
		return this.longitude;
	}
	
	//get satelliteCount
	public int getSatelliteCount()
	{
		return this.satelliteCount;
	}
	
	//get HDOP
	public double getHDOP()
	{
		return this.HDOP;
	}
}
