package org.jwellman.demo.files;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileOrganizerPOC {
    
    public static void main(String[] args) {
        try {
            // Step 1: Get list of sample objects
            List<LocationData> dataList = createSampleData();
            
            // Get system temp directory
            String tempDir = System.getProperty("java.io.tmpdir");
            Path rootPath = Paths.get(tempDir, "root");
            
            System.out.println("Starting File Organizer POC...");
            System.out.println("Using temp directory: " + tempDir);
            System.out.println("Root path: " + rootPath + "\n");
            
            // Track all locationId paths for zipping later
            Map<String, Path> locationPaths = new HashMap<>();
            
            // Step 2: Process each object
            for (LocationData data : dataList) {
                System.out.println("Processing: " + data);
                
                // Step 2a: Create directory structure in temp folder
                String relativePath = String.format("%d/abc/%s/%s/%s",
                        data.getYear(),
                        data.getAgencyId(),
                        data.getCompanyId(),
                        data.getLocationId());
                
                Path dirPath = rootPath.resolve(relativePath);
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
            
            // Step 3: Create zip files for each locationId folder in the root directory
            System.out.println("\n=== Creating ZIP files ===\n");
            for (Map.Entry<String, Path> entry : locationPaths.entrySet()) {
                Path locationDir = entry.getValue();
                String zipFileName = entry.getKey() + ".zip";
                Path zipFilePath = rootPath.resolve(zipFileName);
                
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
