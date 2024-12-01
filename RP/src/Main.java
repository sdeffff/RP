import java.util.*;
import java.io.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        ArrayList<String> objectsToFilter = new ArrayList<>();

        File[] maps = new File("RP\\src\\maps").listFiles();

        System.out.println("Select map to filter from the following list(1 - " + maps.length + ")");

        for(File f : maps) {
            System.out.println(f.getName());
        }

        int mapIndex = sc.nextInt();

        while(true) {
            System.out.println("Enter the object name to filter from the map (or 'exit' to quit):");
            String objName = sc.nextLine();

            if(objName.equalsIgnoreCase("exit")) break;

            objectsToFilter.add(objName);
        }

        try {
            FilterObjects filterMap = new FilterObjects(maps[mapIndex - 1].getName(), objectsToFilter);

            filterMap.filterObjects();

            System.out.println("You are done, amount of objects deleted from the map: " + filterMap.objsDeleted);
        } catch (Exception ignored) {}
    }
}