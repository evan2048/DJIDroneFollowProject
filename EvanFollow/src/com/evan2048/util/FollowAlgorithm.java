package com.evan2048.util;

public class FollowAlgorithm {
	
	//general
	private double followDistanceThreshold = 4.0;
	//drone
	private double droneLocationLatitude = 0.0;
	private double droneLocationLongitude = 0.0;
	private double droneAltitude = 0.0;
	//target
	private double targetLocationLatitude = 0.0;
	private double targetLocationLongitude = 0.0;
	private int targetSatelliteCount = 0;
	private double targetHDOP = 0.0;
	//drone target
	private float drone2TargetDistance = 0;
	private float drone2TargetBearing = 0;
	//output
	private float droneYaw = 0;
	private float dronePitch = 0;
	private double droneGimbalPitch = 0;
	private float droneSpeed = 0;
	
	public FollowAlgorithm(double droneLat, double droneLng, double droneAlt, double targetLat, double targetLng, int targetSatCount, double targetHdop)
	{
		this.droneLocationLatitude=droneLat;
		this.droneLocationLongitude=droneLng;
		this.droneAltitude=droneAlt;
		this.targetLocationLatitude=targetLat;
		this.targetLocationLongitude=targetLng;
		this.targetSatelliteCount=targetSatCount;
		this.targetHDOP=targetHdop;
    	//calculate target distance and bearing
    	float[] results=new float[3];
    	LocationTools.distanceBetween(droneLocationLatitude, droneLocationLongitude, targetLocationLatitude, targetLocationLongitude, results);
    	this.drone2TargetDistance=results[0];
    	this.drone2TargetBearing=results[2];
    	//check drone 2 target distance
    	if(drone2TargetDistance<=followDistanceThreshold)
    	{
    		//less than threshold,just update droneYaw and droneGimbalPitch
    		droneYaw=drone2TargetBearing;
    		droneGimbalPitch=(Math.atan2(droneAltitude, drone2TargetDistance)/Math.PI*180);
    		dronePitch=0;
    	}
    	else
    	{
    		//update droneYaw and droneGimbalPitch
    		droneYaw=drone2TargetBearing;
    		droneGimbalPitch=(Math.atan2(droneAltitude, drone2TargetDistance)/Math.PI*180);
    		dronePitch=1;
    	}
	}
	
	//get droneYaw
	public float getDroneYaw()
	{
		return this.droneYaw;
	}
	
	//get dronePitch
	public float getDronePitch()
	{
		return this.dronePitch;
	}
	
	//get droneGimbalPitch
	public double getDroneGimbalPitch()
	{
		return this.droneGimbalPitch;
	}
	
	//get droneSpeed
	public float getDroneSpeed()
	{
		return this.droneSpeed;
	}
	
}
