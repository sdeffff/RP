import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

import org.w3c.dom.*;
import java.util.*;
import java.io.*;
import java.util.stream.*;

public class FilterObjects {
    private static void filterObjsOnTheMap(List<String> objName) {
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

            // Filtering objects


            if (matchingObjects.isEmpty()) {
                System.out.println("No matching objects found.");
            } else {
                System.out.println("Matching objects:");
                matchingObjects.forEach(System.out::println);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String elementToString(Element element) {
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

        filterObjsOnTheMap(objectsToFilter);
    }
}