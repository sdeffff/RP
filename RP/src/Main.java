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

        for(File f : maps) {
            System.out.println(f.getName());
        }

        int mapIndex = sc.nextInt();

        System.out.println("Elements that were filtered the most in the " + maps[mapIndex - 1].getName());

        if (mapIndex == 1) System.out.println(mostFilteredElementsForCenter);
        else System.out.println(mostFilteredElementsForVillage);


        while(true) {
            System.out.println("Enter the object name to filter from the map (or 'exit' to quit):");
            String objName = sc.nextLine();

            if(objName.trim().equalsIgnoreCase("exit")) break;

            objectsToFilter.add(objName.trim());
        }

        try {
            FilterObjects filterMap = new FilterObjects(maps[mapIndex - 1].getName(), objectsToFilter);

            filterMap.filterObjects();

            System.out.println("You are done, amount of objects deleted from the map: " + filterMap.objsDeleted);
        } catch (Exception ignored) {}
    }
}