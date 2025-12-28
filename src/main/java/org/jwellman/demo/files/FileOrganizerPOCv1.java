package org.jwellman.demo.files;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public class FileOrganizerPOCv1 {
    
    public static void main(String[] args) {
        try {
            // Step 1: Get list of sample objects
            List<LocationData> dataList = createSampleData();
            
            System.out.println("Starting File Organizer POC...\n");
            
            // Track all locationId paths for zipping later
            Map<String, Path> locationPaths = new HashMap<>();
            
            // Step 2: Process each object
            for (LocationData data : dataList) {
                System.out.println("Processing: " + data);
                
                // Step 2a: Create directory structure
                String directoryPath = String.format("root/%d/abc/%s/%s/%s",
                        data.getYear(),
                        data.getAgencyId(),
                        data.getCompanyId(),
                        data.getLocationId());
                
                Path dirPath = Paths.get(directoryPath);
                Files.createDirectories(dirPath);
                System.out.println("  Created directory: " + dirPath);
                
                // Track this locationId path
                String locationKey = data.getYear() + "_" + data.getAgencyId() + "_" + 
                                   data.getCompanyId() + "_" + data.getLocationId();
                locationPaths.put(locationKey, dirPath);
                
                // Step 2b: Create sample file
                String fileName = String.format("%s_%d.txt",
                        data.getLocationName(),
                        data.getYear());
                
                Path filePath = dirPath.resolve(fileName);
                String fileContent = String.format(
                        "Sample file for location: %s\n" +
                        "Year: %d\n" +
                        "Agency ID: %s\n" +
                        "Company ID: %s\n" +
                        "Location ID: %s\n" +
                        "Generated at: %s\n",
                        data.getLocationName(),
                        data.getYear(),
                        data.getAgencyId(),
                        data.getCompanyId(),
                        data.getLocationId(),
                        new Date()
                );
                
                Files.write(filePath, fileContent.getBytes());
                System.out.println("  Created file: " + filePath + "\n");
            }
            
            // Step 3: Create zip files for each locationId folder
            System.out.println("\n=== Creating ZIP files ===\n");
            for (Map.Entry<String, Path> entry : locationPaths.entrySet()) {
                Path locationDir = entry.getValue();
                String zipFileName = locationDir.getFileName().toString() + ".zip";
                Path zipFilePath = locationDir.resolveSibling(zipFileName);
                
                zipDirectory(locationDir, zipFilePath);
                System.out.println("Created ZIP: " + zipFilePath);
            }
            
            System.out.println("\n=== POC Completed Successfully ===");
            
        } catch (IOException e) {
            System.err.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Creates sample data objects for the POC
     */
    private static List<LocationData> createSampleData() {
        List<LocationData> dataList = new ArrayList<>();
        
        dataList.add(new LocationData(2024, "AG001", "CO001", "LOC001", "Downtown_Office"));
        dataList.add(new LocationData(2024, "AG001", "CO002", "LOC002", "Westside_Branch"));
        dataList.add(new LocationData(2023, "AG002", "CO001", "LOC003", "North_Campus"));
        dataList.add(new LocationData(2023, "AG002", "CO003", "LOC004", "East_Facility"));
        
        return dataList;
    }
    
    /**
     * Zips the contents of a directory
     */
    private static void zipDirectory(Path sourceDir, Path zipFilePath) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFilePath.toFile()))) {
            Files.walk(sourceDir)
                .filter(path -> !Files.isDirectory(path))
                .forEach(path -> {
                    ZipEntry zipEntry = new ZipEntry(sourceDir.relativize(path).toString());
                    try {
                        zos.putNextEntry(zipEntry);
                        Files.copy(path, zos);
                        zos.closeEntry();
                    } catch (IOException e) {
                        System.err.println("Error zipping file: " + path);
                        e.printStackTrace();
                    }
                });
        }
    }
}
