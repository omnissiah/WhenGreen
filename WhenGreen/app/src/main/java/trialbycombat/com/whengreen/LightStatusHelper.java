package trialbycombat.com.whengreen;

import android.content.Context;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.format.Time;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Created by IS96266 on 03.06.2015.
 */
public class LightStatusHelper {
    private ArrayList<Light> mLights;
    private static LightStatusHelper sLightStatusHelper;
    private Context mAppContext;

    private LightStatusHelper(Context appContext)
    {
        mAppContext=appContext;
        mLights = new ArrayList<>();
        mLights = ReadStatusDataFromLocal(mAppContext);


    }

    public void DeleteLightData(Context appContext,UUID lightID) {
        String filePathString = appContext.getFilesDir() + "/" + lightID + ".xml";
        File f = new File(filePathString);
        if (f.exists()) {
            f.delete();
        }
    }

    public void CheckForDataXml(Context appContext,UUID lightID)
    {
        try {
            String filePathString = appContext.getFilesDir() + "/" + lightID + ".xml";
            File f = new File(filePathString);
            if (!f.exists()) {
                f.createNewFile();
                Light newLight=new Light();
                newLight.setLightID(lightID);
                WriteStatusDataToLocal(appContext,newLight);
            }
        }catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void UpdateLightDetails(Context appContext,Light light)
    {
        CheckForDataXml(appContext, light.getLightID());
        WriteStatusDataToLocal(appContext, light);
    }

    public void UpdateGreenStatus(Context appContext,UUID lightID, String greenTime)
    {
        CheckForDataXml(appContext, lightID);
        String filePathString=appContext.getFilesDir()+"/"+lightID+".xml";
        File file=new File(filePathString);

        try {

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(file);

            doc.getDocumentElement().normalize();

            //get light element
            Node greenTimes = doc.getElementsByTagName("GreenTimes").item(0);

            greenTimes.setTextContent(greenTimes.getTextContent() + ";" + greenTime);


            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(filePathString));
            transformer.transform(source, result);


        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        }catch (TransformerException te) {
            System.out.println(te.getMessage());
        }

    }

    public void WriteStatusDataToLocal(Context appContext,Light light)
    {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            Element mainRootElement = doc.createElementNS("naber", "Lights");
            doc.appendChild(mainRootElement);

            // append light to dom
            mainRootElement.appendChild(getLightBasicDataAsXml(doc, light));

            try {
                Transformer tr = TransformerFactory.newInstance().newTransformer();
                tr.setOutputProperty(OutputKeys.INDENT, "yes");
                tr.setOutputProperty(OutputKeys.METHOD, "xml");
                tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                tr.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "roles.dtd");
                tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

                String filePathString=appContext.getFilesDir()+"/"+light.getLightID()+".xml";
                // send DOM to file
                tr.transform(new DOMSource(doc), new StreamResult(new FileOutputStream(filePathString)));

            } catch (TransformerException te) {
                System.out.println(te.getMessage());
            } catch (IOException ioe) {
                System.out.println(ioe.getMessage());
            }

        }catch (ParserConfigurationException e)
        {
            e.printStackTrace();
        }
    }

    private static Node getLightBasicDataAsXml(Document doc, Light light) {
        Element lightElement = doc.createElement("Light");
        lightElement.setAttribute("LightID", light.getLightID().toString());
        lightElement.setAttribute("LightName", light.getLightName());
        lightElement.setAttribute("LightLocation", light.getLightLocation());

        StringBuilder greenTimesString=new StringBuilder();
        for(String greenTime : light.getLightActivityTimes())
        {
            greenTimesString.append(greenTime+";");
        }

        lightElement.appendChild(getLightElements(doc, lightElement, "GreenTimes", greenTimesString.toString()));
        return lightElement;
    }

    // utility method to create text node
    private static Node getLightElements(Document doc, Element element, String name, String value) {
        Element node = doc.createElement(name);
        node.appendChild(doc.createTextNode(value));
        return node;
    }

    private ArrayList<Light> ReadStatusDataFromLocal(Context appContext) {
        ArrayList<Light> lightList = new ArrayList<>();

            for (File file : appContext.getFilesDir().listFiles()) {
                try {
                    if (file.getName().endsWith("xml")) {

                        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                        Document doc = dBuilder.parse(file);

                        doc.getDocumentElement().normalize();

                        NodeList lightNodes = doc.getElementsByTagName("Light");
                        if (lightNodes != null) {
                            Light newLight = new Light();
                            Element lightElement = (Element) lightNodes.item(0);
                            newLight.setLightID(UUID.fromString(lightElement.getAttribute("LightID")));
                            newLight.setLightName(lightElement.getAttribute("LightName"));
                            newLight.setLightLocation(lightElement.getAttribute("LightLocation"));

                            ArrayList<String> greenTimeList = new ArrayList<>();
                            for (String greenTimeString : lightElement.getElementsByTagName("GreenTimes").item(0).getTextContent().split(";")) {
                                greenTimeList.add(greenTimeString);
                            }
                            newLight.setLightActivityTimes(greenTimeList);
                            lightList.add(newLight);
                        }
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    file.delete();
                    e.printStackTrace();
                } catch (Exception e) {
                    file.delete();
                    e.printStackTrace();
                }
            }
        return lightList;
    }

    public static LightStatusHelper get(Context c) {
        if (sLightStatusHelper == null) {
            sLightStatusHelper = new LightStatusHelper(c.getApplicationContext());
        }
        return sLightStatusHelper;
    }

    public ArrayList<Light> getLights() {
        return mLights;
    }


    public ArrayList<Light> refreshLights() {
        if(mLights==null)
            mLights=new ArrayList<>();

        mLights= ReadStatusDataFromLocal(mAppContext);
        return mLights;
    }


    public Light getLight(Context appContext, UUID lightID) {
        String filePathString = appContext.getFilesDir() + "/" + lightID + ".xml";
        File f = new File(filePathString);
        try {
            if (filePathString.endsWith("xml")) {

                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(f);

                doc.getDocumentElement().normalize();

                NodeList lightNodes = doc.getElementsByTagName("Light");
                if (lightNodes != null) {
                    Light newLight = new Light();
                    Element lightElement = (Element) lightNodes.item(0);
                    newLight.setLightID(UUID.fromString(lightElement.getAttribute("LightID")));
                    newLight.setLightName(lightElement.getAttribute("LightName"));
                    newLight.setLightLocation(lightElement.getAttribute("LightLocation"));

                    ArrayList<String> greenTimeList = new ArrayList<>();
                    for (String greenTimeString : lightElement.getElementsByTagName("GreenTimes").item(0).getTextContent().split(";")) {
                        greenTimeList.add(greenTimeString);
                    }
                    newLight.setLightActivityTimes(greenTimeList);
                   return newLight;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
