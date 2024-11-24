//Libs to work with xml files
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

//to
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import java.util.*;
import java.io.*;
import java.util.stream.*;

//Create class where will be creating a map that user will choose and
//map will be accessible for any method

public class FilterObjects {
    private final ArrayList<String> objects;
    private final Document document;

    public FilterObjects(String chosenMapName, ArrayList<String> objects) throws ParserConfigurationException, IOException, SAXException {
        this.objects = objects;

        File chosenMap = new File("RP\\src\\maps\\" + chosenMapName);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = factory.newDocumentBuilder();
        this.document = docBuilder.parse(chosenMap);
        this.document.getDocumentElement().normalize();
    }

    private void filterObjects() {
        System.out.println(this.document.getBaseURI());

        ArrayList<Integer> result = new ArrayList<>();

        // Getting all symbols from xml file
        NodeList objects = this.document.getElementsByTagName("symbol");

        // Filtering objects
        for(String name: this.objects) {
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

        filter(result);
    }

    private void filter(ArrayList<Integer> ids) {
        String namespaceURI = this.document.getNamespaceURI();

        // Get all <object> elements
        NodeList objectsToFilter = this.document.getElementsByTagName("object");

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
    }

    private String toString(Element element) {
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
        ArrayList<String> objectsToFilter = new ArrayList<>();

        File[] files = new File("RP\\src\\maps").listFiles();

        System.out.println("Select map to filter from the following list: ");

        for(File f : files) {
            System.out.println(f.getName());
        }

        String mapName = sc.nextLine();

        while(true) {
            System.out.println("Enter the object name to filter from the name (or 'exit' to quit):");
            String objName = sc.nextLine();

            if(objName.equalsIgnoreCase("exit")) break;

            objectsToFilter.add(objName);
        }

        System.out.println(mapName + " " + objectsToFilter);

        try {
            FilterObjects filterMap = new FilterObjects(mapName, objectsToFilter);

            filterMap.filterObjects();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}