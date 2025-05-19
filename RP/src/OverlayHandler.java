import org.w3c.dom.*;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.text.DecimalFormat;
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
        public final double overlapSize;

        public OverlapPair(Element object1, Element object2, int symbolId1, int symbolId2, double overlapSize) {
            this.object1 = object1;
            this.object2 = object2;
            this.symbolId1 = symbolId1;
            this.symbolId2 = symbolId2;
            this.overlapSize = overlapSize;
        }

        @Override
        public String toString() {
            return "Overlap between object #" + object1.getAttribute("id") +
                    " (symbol " + symbolId1 + ") and object #" +
                    object2.getAttribute("id") + " (symbol " + symbolId2 +
                    "), overlap area: ";
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
                String idAttr = symbol.getAttribute("id");

                if (idAttr == null || idAttr.trim().isEmpty()) {
                    continue;
                }

                int id;

                try {
                    id = Integer.parseInt(idAttr);
                } catch (NumberFormatException e) {continue;}

                String name = symbol.getAttribute("name").toLowerCase();

                int priority = calculatePriority(symbol, name);
                symbolPriorities.put(String.valueOf(id), priority);
            }
        }
    }

    private int calculatePriority(Element symbol, String name) {
        int priority = 50;
        name = name.toLowerCase();

        if (name.contains("building") || name.contains("tower")) {
            priority = 90;
        } else if (name.contains("water") || name.contains("lake") || name.contains("pond")) {
            priority = 80;
        } else if (name.contains("path") || name.contains("road") || name.contains("track") || name.contains("area")) {
            priority = 85;
        } else if (name.contains("forest") || name.contains("vegetation") || name.equals("vineyard")) {
            priority = 30;
        } else if (name.contains("open") || name.contains("field")) {
            priority = 20;
        } else if (name.contains("contour") || name.contains("slope")) {
            priority = 70;
        } else if (name.contains("index")) {
            priority = 75;
        }

        if (symbol.getElementsByTagName("point_symbol").getLength() > 0 ||
                symbol.getElementsByTagName("point").getLength() > 0) {
            priority += 5;
        }

        if (symbol.getElementsByTagName("line_symbol").getLength() > 0 ||
                symbol.getElementsByTagName("line").getLength() > 0) {
            priority += 3;
        }

        priority = Math.max(0, Math.min(priority, 100));

        return priority;
    }


    private void loadObjects() {
        NodeList objects = document.getElementsByTagName("object");
        for (int i = 0; i < objects.getLength(); i++) {
            Element object = (Element) objects.item(i);
            String id = object.getAttribute("id");
            if (id != null && !id.trim().isEmpty()) {
                try {
                    objectsById.put(Integer.parseInt(id), object);
                } catch (NumberFormatException e) {
                    // Skip objects with non-integer IDs
                    continue;
                }
            }
        }
    }

    public void identifyOverlays() {
        NodeList objects = document.getElementsByTagName("object");
        int objectCount = objects.getLength();

        for (int i = 0; i < objectCount; i++) {
            Element object1 = (Element) objects.item(i);
            String symbolId1 = object1.getAttribute("symbol");

            Path2D path1 = extractGeometry(object1);
            if (path1 == null) continue;

            Area area1 = new Area(path1);

            for (int j = i + 1; j < objectCount; j++) {
                Element object2 = (Element) objects.item(j);
                String symbolId2 = object2.getAttribute("symbol");

                Path2D path2 = extractGeometry(object2);
                if (path2 == null) continue;

                Area area2 = new Area(path2);
                Area overlapArea = new Area(area1);
                overlapArea.intersect(area2);

                if (!overlapArea.isEmpty()) {
                    double overlapSize = (getAreaSize(overlapArea));

                    if(overlapSize > 100 && !object1.getAttribute("symbol").equals(object2.getAttribute("symbol"))) {
                        try {
                            overlaps.add(new OverlapPair(
                                    object1,
                                    object2,
                                    Integer.parseInt(symbolId1),
                                    Integer.parseInt(symbolId2),
                                    overlapSize
                            ));
                        } catch (NumberFormatException e) {
                            // Skip this overlap if there's an issue parsing the symbol IDs
                            continue;
                        }
                    }
                }
            }
        }

        System.out.println("Found " + overlaps.size() + " overlapping objects");
    }

    private Path2D extractGeometry(Element object) {
        NodeList coordsList = object.getElementsByTagName("coords");
        if (coordsList.getLength() == 0) return null;

        Element coordsElement = (Element) coordsList.item(0);
        String coordsText = coordsElement.getTextContent().trim();
        String[] pointStrings = coordsText.split(";");

        Path2D path = new Path2D.Double();
        boolean started = false;

        for (String pointStr : pointStrings) {
            String[] parts = pointStr.trim().split("\\s+");

            try {
                double x = Double.parseDouble(parts[0]);
                double y = Double.parseDouble(parts[1]);

                if (!started) {
                    path.moveTo(x, y);
                    started = true;
                } else {
                    path.lineTo(x, y);
                }
            } catch (NumberFormatException e) {
                System.err.println("Skipping invalid coordinate: " + pointStr);
            }
        }

        if (started) path.closePath();
        return started ? path : null;
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

    private boolean checkSmallObject(Element object) {
        if((Integer.parseInt(object.getAttribute("symbol")) > 43 &&
                Integer.parseInt(object.getAttribute("symbol")) < 65) ||
                (Integer.parseInt(object.getAttribute("symbol")) > 0 &&
                        Integer.parseInt(object.getAttribute("symbol")) < 19)) {
            return true;
        }

        return false;
    }

    public List<Element> getObjectsToHide() {
        Set<Element> objectsToHide = new HashSet<>();

        for (OverlapPair overlap : overlaps) {
            if(overlap.object1.equals(overlap.object2) || overlap.symbolId2 == overlap.symbolId1) {
                continue;
            }

            Element lowerPriorityObject = decideVisibility(overlap);

            if(checkSmallObject(lowerPriorityObject)) {
                continue;
            } else {
                objectsToHide.add(lowerPriorityObject);
            }
        }

        return new ArrayList<>(objectsToHide);
    }

    public Map<String, Integer> getSymbolPriorities() {
        return symbolPriorities;
    }
}