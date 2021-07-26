package com.dji.sdk.sample.demo.flightcontroller;

import java.lang.Math;

public class GPSRotateAngle {

    public static double BaseVer = 12.0;
    public static double[] CalAngle(double[] coord1,double[] coord2 ){
        double lng2 = coord2[1];
        double lng1 = coord1[1];
        double lat1  =coord1[0];
        double lat2 = coord2[0];
        double radlat1 = Math.toRadians(lat1);
        double radlon1 = Math.toRadians(lng1);
        double radlat2 = Math.toRadians(lat2);
        double radlon2 = Math.toRadians(lng2);
        double dLon = radlon2 - radlon1 ;
        double y = Math.sin(dLon) * Math.cos(radlat2);
        double x = Math.cos(radlat1) * Math.sin(radlat2) - Math.sin(radlat1) * Math.cos(radlat2) * Math.cos(dLon) ;
        double brng = Math.toDegrees(Math.atan2(y, x)) ;
        brng = (brng + 360) % 360;

        double ra = 6378.140;
        double rb = 6356.755;
        double flatten = (ra-rb)/ra;
        double pA = Math.atan(rb / ra * Math.tan(radlat1));
        double pB = Math.atan(rb / ra * Math.tan(radlat2));
        double xx = Math.acos(Math.sin(pA) * Math.sin(pB) + Math.cos(pA) * Math.cos(pB) * Math.cos(radlon1 - radlon2));
        double c1 = (Math.sin(xx) - xx) * Math.pow((Math.sin(pA) + Math.sin(pB)) , 2) / Math.pow(Math.cos(xx / 2) , 2);
        double c2 = (Math.sin(xx) + xx) * Math.pow((Math.sin(pA) - Math.sin(pB)) , 2) / Math.pow(Math.sin(xx / 2) , 2);
        double dr = flatten / 8 * (c1 - c2);
        double distance = ra * (xx + dr)*1000.0;
        double[] res = {distance,brng};
        return res;

    }

    public static double[] CalControlData(double[] res ){
        //double BaseVer = 12.0;
        double XaisVer = BaseVer*Math.cos(Math.toRadians(res[1]));
        double YaisVer = BaseVer*Math.sin(Math.toRadians(res[1]));
        double FlightTime = res[0]/BaseVer;
        double[] ControlData = {XaisVer,YaisVer};
        return ControlData;
    }
}