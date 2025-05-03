import org.w3c.dom.*;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.util.*;

public class OverlayHandler {
    private final Document document;
    private final Map<String, Integer> symbolPriorities;
    private final Map<Integer, Element> objectsById;
    private final List<OverlapPair> overlaps;

    public static class OverlapPair {
        public final Element object1;
        public final Element object2;
        public final int symbolId1;
        public final int symbolId2;
        public final double overlapArea;

        public OverlapPair(Element object1, Element object2, int symbolId1, int symbolId2, double overlapArea) {
            this.object1 = object1;
            this.object2 = object2;
            this.symbolId1 = symbolId1;
            this.symbolId2 = symbolId2;
            this.overlapArea = overlapArea;
        }

        @Override
        public String toString() {
            return "Overlap between object #" + object1.getAttribute("id") +
                    " (symbol " + symbolId1 + ") and object #" +
                    object2.getAttribute("id") + " (symbol " + symbolId2 +
                    "), overlap area: " + String.format("%.2f", overlapArea);
        }
    }

    public OverlayHandler(Document document) {
        this.document = document;
        this.symbolPriorities = new HashMap<>();
        this.objectsById = new HashMap<>();
        this.overlaps = new ArrayList<>();

        loadSymbolPriorities();

        loadObjects();
    }

    private void loadSymbolPriorities() {
        NodeList symbols = document.getElementsByTagName("symbol");
        for (int i = 0; i < symbols.getLength(); i++) {
            Node node = symbols.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element symbol = (Element) node;
                int id = Integer.parseInt(symbol.getAttribute("id"));
                String name = symbol.getAttribute("name").toLowerCase();

                int priority = calculatePriority(symbol, name);
                symbolPriorities.put(String.valueOf(id), priority);
            }
        }
    }

    private int calculatePriority(Element symbol, String name) {
        int priority = 50;

        if (name.contains("building") || name.contains("tower")) {
            priority = 90;
        }

        else if (name.contains("water") || name.contains("lake") || name.contains("pond")) {
            priority = 80;
        }

        else if (name.contains("path") || name.contains("road") || name.contains("track")) {
            priority = 85;
        }

        else if (name.contains("forest") || name.contains("vegetation")) {
            priority = 30;
        }

        else if (name.contains("open") || name.contains("field")) {
            priority = 20;
        }

        // Check for point, line or area type
        NodeList typeNodes = symbol.getElementsByTagName("point");
        if (typeNodes.getLength() > 0) {
            priority += 5;
        }

        typeNodes = symbol.getElementsByTagName("line");
        if (typeNodes.getLength() > 0) {
            priority += 3;
        }

        return priority;
    }

    private void loadObjects() {
        NodeList objects = document.getElementsByTagName("object");
        for (int i = 0; i < objects.getLength(); i++) {
            Element object = (Element) objects.item(i);
            String id = object.getAttribute("id");
            if (!id.isEmpty()) {
                objectsById.put(Integer.parseInt(id), object);
            }
        }
    }

    public void identifyOverlays() {
        NodeList objects = document.getElementsByTagName("object");
        int objectCount = objects.getLength();

        System.out.println("Scanning " + objectCount + " objects for overlaps...");

        // For each pair of objects, check if they overlap
        for (int i = 0; i < objectCount; i++) {
            Element object1 = (Element) objects.item(i);
            String symbolId1 = object1.getAttribute("symbol");

            if (symbolId1.isEmpty()) continue;

            // Extract geometry of first object
            Path2D path1 = extractGeometry(object1);
            if (path1 == null) continue;

            Area area1 = new Area(path1);

            for (int j = i + 1; j < objectCount; j++) {
                Element object2 = (Element) objects.item(j);
                String symbolId2 = object2.getAttribute("symbol");

                if (symbolId2.isEmpty()) continue;

                // Extract geometry of second object
                Path2D path2 = extractGeometry(object2);
                if (path2 == null) continue;

                // Check for overlap
                Area area2 = new Area(path2);
                Area overlapArea = new Area(area1);
                overlapArea.intersect(area2);

                if (!overlapArea.isEmpty()) {
                    // Calculate overlap area
                    double overlapSize = getAreaSize(overlapArea);
                    if (overlapSize > 0.01) { // Skip tiny overlaps
                        overlaps.add(new OverlapPair(
                                object1,
                                object2,
                                Integer.parseInt(symbolId1),
                                Integer.parseInt(symbolId2),
                                overlapSize
                        ));
                    }
                }
            }
        }

        System.out.println("Found " + overlaps.size() + " overlapping objects");
    }

    private Path2D extractGeometry(Element object) {
        Path2D path = new Path2D.Double();
        boolean hasGeometry = false;

        // Handle area objects (polygons)
        NodeList coords = object.getElementsByTagName("coord");
        if (coords.getLength() > 0) {
            for (int i = 0; i < coords.getLength(); i++) {
                Element coord = (Element) coords.item(i);
                double x = Double.parseDouble(coord.getAttribute("x"));
                double y = Double.parseDouble(coord.getAttribute("y"));

                if (i == 0) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
                hasGeometry = true;
            }
            path.closePath();
        }

        NodeList linePoints = object.getElementsByTagName("point");
        if (linePoints.getLength() > 0 && !hasGeometry) {
            for (int i = 0; i < linePoints.getLength(); i++) {
                Element point = (Element) linePoints.item(i);
                double x = Double.parseDouble(point.getAttribute("x"));
                double y = Double.parseDouble(point.getAttribute("y"));

                if (i == 0) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
                hasGeometry = true;
            }
        }

        return hasGeometry ? path : null;
    }

    private double getAreaSize(Area area) {
        return area.getBounds2D().getWidth() * area.getBounds2D().getHeight();
    }

    public List<OverlapPair> getOverlaps() {
        return overlaps;
    }

    public Element decideVisibility(OverlapPair overlap) {
        int priority1 = symbolPriorities.getOrDefault(String.valueOf(overlap.symbolId1), 0);
        int priority2 = symbolPriorities.getOrDefault(String.valueOf(overlap.symbolId2), 0);

        if (priority1 >= priority2) {
            return overlap.object2;
        } else {
            return overlap.object1;
        }
    }

    public List<Element> getObjectsToHide() {
        Set<Element> objectsToHide = new HashSet<>();

        for (OverlapPair overlap : overlaps) {
            Element lowerPriorityObject = decideVisibility(overlap);
            objectsToHide.add(lowerPriorityObject);
        }

        return new ArrayList<>(objectsToHide);
    }

    public void applyTransparencyToOverlaps(Document resultMap) {
        for (OverlapPair overlap : overlaps) {
            Element lowerPriorityObject = decideVisibility(overlap);
            String id = lowerPriorityObject.getAttribute("id");

            // Find this object in the result document
            NodeList resultObjects = resultMap.getElementsByTagName("object");
            for (int i = 0; i < resultObjects.getLength(); i++) {
                Element obj = (Element) resultObjects.item(i);
                if (obj.getAttribute("id").equals(id)) {
                    // Set opacity attribute
                    obj.setAttribute("opacity", "0.5");
                    break;
                }
            }
        }
    }

    public Map<String, Integer> getSymbolPriorities() {
        return symbolPriorities;
    }
}