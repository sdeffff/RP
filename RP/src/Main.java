import java.util.*;
import java.io.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        ArrayList<String> objectsToFilter = new ArrayList<>();
        List<String> mostFilteredElementsForCenter = List.of("High Tower", "Small Tower", "Building", "Grave", "Firing Range", "Railway");
        List<String> mostFilteredElementsForVillage = List.of("Forest: reduced visibility", "Building", "Pond", "Major power line");

        File[] maps = new File("RP\\src\\maps").listFiles();

        System.out.println("Select map to filter from the following list(1 - " + maps.length + ")");

        for (int i = 0; i < maps.length; i++) {
            System.out.println((i + 1) + ". " + maps[i].getName());
        }

        int mapIndex = sc.nextInt();
        sc.nextLine();

        System.out.println("Map selected: " + maps[mapIndex - 1].getName());

        if (mapIndex == 1) System.out.println(mostFilteredElementsForCenter);
        else System.out.println(mostFilteredElementsForVillage);

        System.out.println("\nDo you want to handle overlapping objects? (y/n)");
        boolean handleOverlays = sc.nextLine().trim().equalsIgnoreCase("y");

        boolean removeOverlays = false;
        if (handleOverlays) {
            System.out.println("How do you want to handle overlapping objects?");
            System.out.println("1. Automatically remove lower priority objects");
            int overlayChoice = sc.nextInt();
            sc.nextLine();

            removeOverlays = (overlayChoice == 1);
        }

        System.out.println("Elements that were filtered the most in the " + maps[mapIndex - 1].getName());
        System.out.println("\nEnter object names to filter from the map (one per line, type 'exit' when done):");
        while (true) {
            String objName = sc.nextLine();

            if (objName.trim().equalsIgnoreCase("exit")) break;

            objectsToFilter.add(objName.trim());
        }

        try {
            FilterObjects filterMap = new FilterObjects(maps[mapIndex - 1].getName(), objectsToFilter, handleOverlays, removeOverlays);

            filterMap.filterObjects();

            System.out.println("Filtering complete!");
            System.out.println("Objects deleted from the map: " + filterMap.objsDeleted);

            if (handleOverlays) {
                System.out.println("Overlapping objects handled: " + filterMap.overlaysHandled);

                if (removeOverlays) {
                    System.out.println("\nResult saved to: RP\\src\\maps\\custom_map_no_overlays.omap");
                } else {
                    System.out.println("\nResult saved to: RP\\src\\maps\\custom_map.omap");
                }
            } else {
                System.out.println("\nResult saved to: RP\\src\\maps\\custom_map.omap");
            }
        } catch (Exception e) {
            System.out.println("Happened some error " + e.getMessage());
            e.printStackTrace();
        }
    }
}