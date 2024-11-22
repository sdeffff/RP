import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import java.util.*;
import java.io.*;
import java.util.stream.*;

//Create class where will be creating a map that user will choose and
//map will be accessible for any method

public class FilterObjects {
    private static void filterObjects(List<String> objName) {
        ArrayList<Integer> result = new ArrayList<>();

        try {
            // Loading the .omap file from the maps folder
            File inputFile = new File("RP\\src\\maps\\center_map.omap");

            // XML parser
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(inputFile);
            doc.getDocumentElement().normalize();

            // Getting all symbols from xml file
            NodeList objects = doc.getElementsByTagName("symbol");

            List<String> matchingElements = new ArrayList<>();

            // Filtering objects
            for(String name: objName) {
                for(int i = 0; i < objects.getLength(); i++) {
                    Node node = objects.item(i);

                    if(node.getNodeType() == Node.ELEMENT_NODE) {
                        Element el = (Element) node;
                        String res = el.getAttribute("name");

                        if(name.equalsIgnoreCase(res) && !res.isEmpty()) {
                            result.add(Integer.parseInt(el.getAttribute("id")));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        filter(result);
    }

    private static void filter(ArrayList<Integer> ids) {
        try {
            File inputFile = new File("RP\\src\\maps\\center_map.omap");

            // Enable namespace-aware parsing
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(inputFile);
            doc.getDocumentElement().normalize();

            // Retrieve the namespace URI from the root element
            String namespaceURI = doc.getDocumentElement().getNamespaceURI();

            // Fetch all <object> elements
            NodeList objectsToFilter = doc.getElementsByTagNameNS(namespaceURI, "object");

            // Debugging: print the number of <object> elements
            System.out.println("Number of <object> elements: " + objectsToFilter.getLength());

            ArrayList<String> matchingObjects = new ArrayList<>();

            for(int id : ids) {
                for (int i = 0; i < objectsToFilter.getLength(); i++) {
                    Node node = objectsToFilter.item(i);


                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element el = (Element) node;

                        String symbol = el.getAttribute("symbol");
                        if (!symbol.isEmpty() && id == Integer.parseInt(el.getAttribute("symbol"))) {
                            String string = toString(el);
                            matchingObjects.add(string);
                        }
                    }
                }

                for (String el : matchingObjects) {
                    System.out.println(el);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String toString(Element element) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(element);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            transformer.transform(source, result);
            return writer.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        List<String> objectsToFilter = new ArrayList<>();

        while(true) {
            System.out.println("Enter the object name to filter from the name (or 'exit' to quit):");
            String objName = sc.nextLine();

            if(objName.equalsIgnoreCase("exit")) break;

            objectsToFilter.add(objName);
        }

        filterObjects(objectsToFilter);
    }
}