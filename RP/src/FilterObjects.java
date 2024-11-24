//Libs to work with xml files:
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

//to interact with nodes(tags) in xml files:
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import java.util.*;
import java.io.*;

public class FilterObjects {
    private final ArrayList<String> objects;
    private final Document document;
    private final Document resultMap;
    private int objsDeleted;

    public FilterObjects(String chosenMapName, ArrayList<String> objects) throws ParserConfigurationException, IOException, SAXException {
        this.objects = objects;

        File chosenMap = new File("RP\\src\\maps\\" + chosenMapName);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = factory.newDocumentBuilder();
        this.document = docBuilder.parse(chosenMap);
        this.document.getDocumentElement().normalize();
        this.resultMap = docBuilder.newDocument();
    }

    //Getting ids of symbols to delete
    private void filterObjects() {
        ArrayList<Integer> result = new ArrayList<>();

        // Getting all symbols from xml file
        NodeList objects = this.document.getElementsByTagName("symbol");

        // Filtering objects
        for(String name: this.objects) {
            for(int i = 0; i < objects.getLength(); i++) {
                Node currentTag = objects.item(i);

                if(currentTag.getNodeType() == Node.ELEMENT_NODE) {
                    Element el = (Element) currentTag;
                    String nameOfCurrEl = el.getAttribute("name");

                    if(name.equalsIgnoreCase(nameOfCurrEl) && !nameOfCurrEl.isEmpty()) {
                        result.add(Integer.parseInt(el.getAttribute("id")));
                    }
                }
            }
        }

        filter(result);
    }

    //Depending on symbol id filter objects which have attribute symbol equal to id in symbol
    private void filter(ArrayList<Integer> ids) {
        // Get all <object> elements
        NodeList objectsToFilter = this.document.getElementsByTagName("object");

        Element root = this.document.getDocumentElement();
        Element newRoot = (Element) this.resultMap.importNode(root, false);
        this.resultMap.appendChild(newRoot);

        for(int id : ids) {
            for (int i = 0; i < objectsToFilter.getLength(); i++) {
                Node currentTag = objectsToFilter.item(i);

                if (currentTag.getNodeType() == Node.ELEMENT_NODE) {
                    Element el = (Element) currentTag;
                    String symbol = el.getAttribute("symbol");

                    //Skip, if the current element is the element we want to filter
                    if (!symbol.isEmpty() && id == Integer.parseInt(el.getAttribute("symbol"))) {
                        this.objsDeleted++;
                        continue;
                    }

                    //All other elements we are adding to the final map
                    newRoot.appendChild(this.resultMap.importNode(el, true));
                }
            }
        }

        saveDoc(this.resultMap, "custom_map.omap");
    }

    //Method to save new map in the directory maps:
    private void saveDoc(Document document, String fileName) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            //Fixing code writing (without them all the xml code will be in one line)
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(new File("RP\\src\\maps\\", fileName));
            transformer.transform(source, result);
        } catch (Exception e) {
            e.printStackTrace();
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

        try {
            FilterObjects filterMap = new FilterObjects(mapName, objectsToFilter);

            filterMap.filterObjects();

            System.out.println("You are done, amount of objects deleted from the map: " + filterMap.objsDeleted);
        } catch (Exception ignored) {}
    }
}