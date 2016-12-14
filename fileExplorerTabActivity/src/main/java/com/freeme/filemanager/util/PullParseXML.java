package com.freeme.filemanager.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.freeme.filemanager.R;

import com.freeme.filemanager.bean.General_config;

//*/ add by droi liuhaoran for add customized configuration file on 20160428
public class PullParseXML {
    
public static List<General_config> getConfig(InputStream inStream) throws Throwable{
     
        List<General_config> list=null;
        General_config general_config = null;
        
        try {
            XmlPullParserFactory pullParserFactory=XmlPullParserFactory.newInstance();
            XmlPullParser xmlPullParser=pullParserFactory.newPullParser();
            xmlPullParser.setInput(inStream, "UTF-8");
            
            int eventType=xmlPullParser.getEventType();
            
            try {
                while(eventType!=XmlPullParser.END_DOCUMENT){
                    String nodeName=xmlPullParser.getName();
                    switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        list=new ArrayList<General_config>();
                        break;
                    case XmlPullParser.START_TAG:
                        
                        if("general_config".equals(nodeName)){
                            
                            general_config=new General_config();
                            general_config.setId(Integer.parseInt(xmlPullParser.getAttributeValue(0)));
                        }else if("isHideFTP".equals(nodeName)){
                            
                            general_config.setIsHideFTP(xmlPullParser.nextText());
                        }else if("memoryCardInfo".equals(nodeName)){
                            
                            general_config.setMemoryCardInfo(Integer.parseInt(xmlPullParser.nextText()));
                        }else if("isDaMi".equals(nodeName)){
                            
                            general_config.setIsDaMi(xmlPullParser.nextText());
                        }else if("isFeiMa".equals(nodeName)){
                            
                            general_config.setIsFeiMa(xmlPullParser.nextText());
                        }else if("isNeedRingTone".equals(nodeName)){

                            general_config.setIsNeedRingTone(xmlPullParser.nextText());
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if("general_config".equals(nodeName)){
                            list.add(general_config);
                            general_config=null;
                        }
                        break;
                    default:
                        break;
                    }
                    eventType=xmlPullParser.next();
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }

        return list;
    }
}
//*/