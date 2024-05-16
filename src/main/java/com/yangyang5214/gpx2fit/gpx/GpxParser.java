package com.yangyang5214.gpx2fit.gpx;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.garmin.fit.DateTime;
import com.garmin.fit.Sport;
import com.yangyang5214.gpx2fit.model.Point;
import com.yangyang5214.gpx2fit.model.Session;
import org.w3c.dom.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class GpxParser {

    private final String xmlFile;

    public GpxParser(String xmlFile) {
        this.xmlFile = xmlFile;
    }

    public Session parser() {
        List<Point> points;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db;
        Document document;
        float totalTimerTime = 0;
        float distance = 0;

        try {
            db = dbf.newDocumentBuilder();
            document = db.parse(xmlFile);
            NodeList trkpts = document.getElementsByTagName("trkpt");

            int len = trkpts.getLength();
            points = new ArrayList<>(len);


            for (int i = 0; i < len; i++) {
                Node trkpt = trkpts.item(i);
                Element trkptElm = (Element) trkpt;

                NodeList times = trkptElm.getElementsByTagName("time");

                Point point = new Point();
                point.setLon(Double.parseDouble(trkptElm.getAttribute("lon")));
                point.setLat(Double.parseDouble(trkptElm.getAttribute("lat")));
                point.setTime(convertToDateTime(times.item(0).getTextContent()));
                NodeList eles = trkptElm.getElementsByTagName("ele");
                point.setEle(Float.parseFloat(eles.item(0).getTextContent()));

                if (i != 0) {
                    Point prePoint = points.get(i - 1);
                    float subDistance = point.calculateDistance(prePoint);
                    distance = distance + subDistance;
                    if (subDistance != 0) {
                        totalTimerTime = totalTimerTime + point.subTs(prePoint);
                    }
                }
                point.setDistance(distance);

                //todo with extensions
                points.add(point);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        DateTime startTime = points.get(0).getTime();
        DateTime endTime = points.get(points.size() - 1).getTime();

        Session session = new Session();
        session.setPoints(points);
        session.setStartTime(startTime);
        session.setEndTime(endTime);
        session.setSport(getSport(document));
        session.setTotalTimerTime(totalTimerTime);
        session.setTotalElapsedTime(endTime.getTimestamp() - startTime.getTimestamp());
        session.setTotalDistance(distance);

        return session;
    }

    private DateTime convertToDateTime(String time) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            Date date = sdf.parse(time);
            return new DateTime(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Sport getSport(Document document) {
        NodeList nodeList = document.getElementsByTagName("trk");
        Element elm = (Element) nodeList.item(0);
        NodeList types = elm.getElementsByTagName("type");
        Element typeElm = (Element) types.item(0);
        if (typeElm == null) {
            return Sport.GENERIC;
        }
        String type = typeElm.getTextContent();
        Sport sport = Sport.GENERIC;
        try {
            sport = Sport.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            //可能是中文这里就是枚举了
            if (type.indexOf("骑行") > 0) {
                return Sport.CYCLING;
            }
            if (type.indexOf("跑步") > 0) {
                return Sport.RUNNING;
            }
            System.err.format("UnKnow type %s\n", type);
        }
        return sport;
    }
}
