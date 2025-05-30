
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import java.util.*;
import java.io.*;

public class FilterObjects {
    private final ArrayList<String> objects;
    private final Document document;
    private Document resultMap;
    public int objsDeleted;
    public int overlaysHandled;
    private boolean handleOverlays;
    private boolean removeOverlays;
    private OverlayHandler overlayManager;

    public FilterObjects(String chosenMapName, ArrayList<String> objects, boolean handleOverlays, boolean removeOverlays) throws ParserConfigurationException, IOException, SAXException {
        this.objects = objects;
        this.handleOverlays = handleOverlays;
        this.removeOverlays = removeOverlays;
        this.overlaysHandled = 0;

        File chosenMap = new File("RP\\src\\maps\\" + chosenMapName);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = factory.newDocumentBuilder();
        this.document = docBuilder.parse(chosenMap);
        this.document.getDocumentElement().normalize();
        this.resultMap = docBuilder.newDocument();

        if (handleOverlays) {
            this.overlayManager = new OverlayHandler(this.document);
        }
    }

    public FilterObjects(String chosenMapName, ArrayList<String> objects) throws ParserConfigurationException, IOException, SAXException {
        this(chosenMapName, objects, false, false);
    }

    public void filterObjects() {
        Map<String, Integer> symbolMap = new HashMap<>();

        NodeList symbols = this.document.getElementsByTagName("symbol");
        for (int i = 0; i < symbols.getLength(); i++) {
            Node currentTag = symbols.item(i);

            if (currentTag.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) currentTag;
                String name = el.getAttribute("name");
                String id = el.getAttribute("id");

                if (!name.isEmpty() && !id.isEmpty()) {
                    try {
                        symbolMap.put(name.toLowerCase(), Integer.parseInt(id));
                    } catch (NumberFormatException e) {
                        // Skip symbols with non-integer IDs
                        continue;
                    }
                }
            }
        }

        List<Integer> idsToFilter = new ArrayList<>();
        for (String name : this.objects) {
            if (symbolMap.containsKey(name.toLowerCase())) {
                idsToFilter.add(symbolMap.get(name.toLowerCase()));
            }
        }

        if (handleOverlays) {
            overlayManager.identifyOverlays();
            System.out.println("Found " + overlayManager.getOverlaps().size() + " overlapping objects");
        }

        filter(idsToFilter, symbols);

        if (handleOverlays) {
            if (removeOverlays) {
                List<Element> objectsToHide = overlayManager.getObjectsToHide();

                overlaysHandled = removeOverlappingObjects(objectsToHide);
            }
        }
    }

    private void filter(List<Integer> idsToFilter, NodeList symbols) {
        NodeList objectNodes = this.document.getElementsByTagName("objects");

        if (objectNodes.getLength() == 0) {
            System.out.println("Warning: No 'objects' element found in the document");
            return;
        }

        Node objectsNode = objectNodes.item(0);
        NodeList objects = objectsNode.getChildNodes();

        // Root element for the result
        Element root = this.resultMap.createElement("map");
        root.setAttribute("xmlns", "http://openorienteering.org/apps/mapper/xml/v2");
        root.setAttribute("version", "9");
        this.resultMap.appendChild(root);

        // Add default tags in the beginning
        appendGeoreferencingAndColors(root);
        appendBarrierAndSymbols(root, symbols);

        // Create parts wrapper
        Element parts = this.resultMap.createElement("parts");
        parts.setAttribute("count", "1");
        parts.setAttribute("current", "0");

        Element part = this.resultMap.createElement("part");
        part.setAttribute("name", "default part");

        Element objectsWrapper = this.resultMap.createElement("objects");
        int objectCount = 0;

        // Copy <object> elements
        for (int i = 0; i < objects.getLength(); i++) {
            Node currentTag = objects.item(i);

            if (currentTag.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) currentTag;
                String symbolAttr = el.getAttribute("symbol");

                // Skip objects with filtered symbols or empty symbol attributes
                if (symbolAttr.isEmpty()) {
                    continue;
                }

                try {
                    int symbolId = Integer.parseInt(symbolAttr);
                    if (idsToFilter.contains(symbolId)) {
                        this.objsDeleted++;
                        continue;
                    }
                } catch (NumberFormatException e) {
                    // Skip objects with non-integer symbol IDs
                    continue;
                }

                // Ensure the `symbol` attribute is retained
                Element copiedObject = (Element) this.resultMap.importNode(el, true);
                objectsWrapper.appendChild(copiedObject);
                objectCount++;
            }
        }

        objectsWrapper.setAttribute("count", String.valueOf(objectCount));
        part.appendChild(objectsWrapper);
        parts.appendChild(part);
        root.appendChild(parts);

        appendTemplatesAndView(root);

        if(!handleOverlays) saveDoc(this.resultMap, "custom_map.omap");
    }

    private int removeOverlappingObjects(List<Element> objectsToHide) {
        int removed = 0;

        Set<String> idsToHide = new HashSet<>();
        for (Element element : objectsToHide) {
            String id = element.getAttribute("symbol");
            if (id != null && !id.isEmpty()) {
                idsToHide.add(id);
            }
        }

        NodeList allObjectsNodes = resultMap.getElementsByTagName("object");
        List<Node> toRemove = new ArrayList<>();

        for (int i = 0; i < allObjectsNodes.getLength(); i++) {
            Element object = (Element) allObjectsNodes.item(i);
            String id = object.getAttribute("symbol");

            if (id != null && !id.isEmpty() && idsToHide.contains(id)) {
                toRemove.add(object);
                removed++;
            }
        }

        for (Node node : toRemove) {
            node.getParentNode().removeChild(node);
        }

        NodeList objectsWrapperList = resultMap.getElementsByTagName("objects");
        if (objectsWrapperList.getLength() > 0) {
            Element objectsWrapper = (Element) objectsWrapperList.item(0);
            int currentCount = Integer.parseInt(objectsWrapper.getAttribute("count"));
            objectsWrapper.setAttribute("count", String.valueOf(currentCount - removed));
        }

        // Save the updated document
        saveDoc(resultMap, "custom_map_no_overlays.omap");

        return removed;
    }

    private void appendGeoreferencingAndColors(Element root) {
        // Georeferencing
        NodeList georeferencingNodes = this.document.getElementsByTagName("georeferencing");
        if (georeferencingNodes.getLength() > 0) {
            Node georeferencingNode = georeferencingNodes.item(0);
            Node importedGeoreferencing = this.resultMap.importNode(georeferencingNode, true);
            root.appendChild(importedGeoreferencing);
        } else {
            System.out.println("Warning: No georeferencing element found in the document");
        }

        // Colors
        Element colors = this.resultMap.createElement("colors");
        colors.setAttribute("count", "25");

        // Adding pre-defined color elements
        String[][] colorData = {
                {"0", "Purple", "0.2", "1", "0", "0", "PURPLE", "0.8", "0", "1"},
                {"1", "Black", "0", "0", "0", "1", "BLACK", "0", "0", "0"},
                {"2", "Lower Purple", "0.2", "1", "0", "0", "PURPLE", "0.8", "0", "1"},
                {"3", "Black 70%", "0", "0", "0", "0.7", "BLACK", "0.3", "0.3", "0.3"},
                {"4", "Brown 50%", "0", "0.28", "0.5", "0.09", "BROWN", "0.91", "0.68", "0.5"},
                {"5", "Black below light browns", "0", "0", "0", "1", "BLACK", "0", "0", "0"},
                {"6", "Brown", "0", "0.56", "1", "0.18", "BROWN", "0.82", "0.361", "0"},
                {"7", "OpenOrienteering Orange", "0", "0.474", "0.895", "0.09", "ORANGE", "0.91", "0.497", "0.105"},
                {"8", "Opaque Blue", "0.87", "0.18", "0", "0", "BLUE", "0.13", "0.82", "1"},
                {"9", "Blue", "0.87", "0.18", "0", "0", "BLUE", "0.13", "0.82", "1"},
                {"10", "Blue 50%", "0.435", "0.09", "0", "0", "BLUE", "0.565", "0.91", "1"},
                {"11", "Black 30%", "0", "0", "0", "0.3", "BLACK", "0.7", "0.7", "0.7"},
                {"12", "Green 50%, Yellow", "0.38", "0.27", "0.886", "0", "GREEN", "0.62", "0.73", "0.114"},
                {"13", "Green over White over Green", "0.76", "0", "0.91", "0", "GREEN", "0.24", "1", "0.09"},
                {"14", "Opaque White over Green", "0", "0", "0", "0", "WHITE", "1", "1", "1"},
                {"15", "Yellow over Green", "0", "0.27", "0.79", "0", "YELLOW", "1", "0.73", "0.21"},
                {"16", "Opaque Green", "0.76", "0", "0.91", "0", "GREEN", "0.24", "1", "0.09"},
                {"17", "Green", "0.76", "0", "0.91", "0", "GREEN", "0.24", "1", "0.09"},
                {"18", "Green 60%", "0.456", "0", "0.546", "0", "GREEN", "0.544", "1", "0.454"},
                {"19", "Green 30%", "0.228", "0", "0.273", "0", "GREEN", "0.772", "1", "0.727"},
                {"20", "Green below light greens", "0.76", "0", "0.91", "0", "GREEN", "0.24", "1", "0.09"},
                {"21", "Yellow", "0", "0.27", "0.79", "0", "YELLOW", "1", "0.73", "0.21"},
                {"22", "Yellow 50%", "0", "0.135", "0.395", "0", "YELLOW", "1", "0.865", "0.605"},
                {"23", "White over Yellow 70%", "0", "0", "0", "0", "WHITE", "1", "1", "1"},
                {"24", "Yellow 70%", "0", "0.189", "0.553", "0", "YELLOW", "1", "0.811", "0.447"},
        };

        for (String[] color : colorData) {
            Element colorElement = this.resultMap.createElement("color");
            colorElement.setAttribute("priority", color[0]);
            colorElement.setAttribute("name", color[1]);
            colorElement.setAttribute("c", color[2]);
            colorElement.setAttribute("m", color[3]);
            colorElement.setAttribute("y", color[4]);
            colorElement.setAttribute("k", color[5]);
            colorElement.setAttribute("opacity", "1");

            Element spotColors = this.resultMap.createElement("spotcolors");
            Element namedColor = this.resultMap.createElement("namedcolor");
            namedColor.setTextContent(color[6]);
            spotColors.appendChild(namedColor);

            Element cmyk = this.resultMap.createElement("cmyk");
            cmyk.setAttribute("method", "custom");

            Element rgb = this.resultMap.createElement("rgb");
            rgb.setAttribute("method", "cmyk");
            rgb.setAttribute("r", color[7]);
            rgb.setAttribute("g", color[8]);
            rgb.setAttribute("b", color[9]);

            colorElement.appendChild(spotColors);
            colorElement.appendChild(cmyk);
            colorElement.appendChild(rgb);
            colors.appendChild(colorElement);
        }

        root.appendChild(colors);
    }

    private void appendBarrierAndSymbols(Element root, NodeList symbols) {
        Element barrier = this.resultMap.createElement("barrier");
        barrier.setAttribute("version", "6");
        barrier.setAttribute("required", "0.6.0");

        Element symbolsWrapper = this.resultMap.createElement("symbols");
        symbolsWrapper.setAttribute("count", String.valueOf(symbols.getLength()));
        symbolsWrapper.setAttribute("id", "ISMTBOM");

        //Copy all <symbol> elements
        for (int i = 0; i < symbols.getLength(); i++) {
            Node symbol = symbols.item(i);
            symbolsWrapper.appendChild(this.resultMap.importNode(symbol, true));
        }

        barrier.appendChild(symbolsWrapper);
        root.appendChild(barrier);
    }

    private void appendTemplatesAndView(Element root) {
        //Templates
        Element templates = this.resultMap.createElement("templates");
        templates.setAttribute("count", "1");
        templates.setAttribute("first_front_template", "1");

        Element template = this.resultMap.createElement("template");
        template.setAttribute("type", "OgrTemplate");
        template.setAttribute("open", "true");
        template.setAttribute("name", "map (1).osm");
        template.setAttribute("path", "C:/Users/38066/Downloads/map (1).osm");
        template.setAttribute("relpath", "../../../../../../Downloads/map (1).osm");
        template.setAttribute("georef", "true");
        templates.appendChild(template);

        NodeList defaultsTag = this.document.getElementsByTagName("defaults");
        if (defaultsTag.getLength() > 0) {
            Node defaultsNode = defaultsTag.item(0);
            templates.appendChild(this.resultMap.importNode(defaultsNode, true));
        }
        root.appendChild(templates);

        //View part
        Element view = this.resultMap.createElement("view");

        Element grid = this.resultMap.createElement("grid");
        grid.setAttribute("color", "#646464");
        grid.setAttribute("display", "0");
        grid.setAttribute("alignment", "0");
        grid.setAttribute("additional_rotation", "0");
        grid.setAttribute("unit", "1");
        grid.setAttribute("h_spacing", "500");
        grid.setAttribute("v_spacing", "500");
        grid.setAttribute("h_offset", "0");
        grid.setAttribute("v_offset", "0");
        grid.setAttribute("snapping_enabled", "true");
        view.appendChild(grid);

        Element mapView = this.resultMap.createElement("map_view");
        mapView.setAttribute("zoom", "1.41421");
        mapView.setAttribute("position_x", "867");
        mapView.setAttribute("position_y", "-10751");

        Element map = this.resultMap.createElement("map");
        map.setAttribute("opacity", "1");
        map.setAttribute("visible", "true");
        mapView.appendChild(map);

        Element templatesRef = this.resultMap.createElement("templates");
        templatesRef.setAttribute("count", "1");

        Element refTemplate = this.resultMap.createElement("ref");
        refTemplate.setAttribute("template", "0");
        refTemplate.setAttribute("opacity", "1");
        templatesRef.appendChild(refTemplate);

        mapView.appendChild(templatesRef);
        view.appendChild(mapView);
        root.appendChild(view);
    }

    public Map<String, Integer> getSymbolPriorities() {
        if (overlayManager != null) {
            return overlayManager.getSymbolPriorities();
        }
        return new HashMap<>();
    }

    private void saveDoc(Document document, String fileName) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(new File("RP\\src\\maps\\", fileName));
            transformer.transform(source, result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}