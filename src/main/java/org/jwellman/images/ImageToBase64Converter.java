package org.jwellman.images;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;

public class ImageToBase64Converter {

    public static String convertPngToBase64(String filePath) throws IOException {
        File file = new File(filePath);
        byte[] fileBytes = new byte[(int) file.length()];
        
        // Use try-with-resources to ensure the FileInputStream is closed automatically
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            fileInputStream.read(fileBytes);
        }

        // Encode the byte array to a Base64 string
        String base64Image = Base64.getEncoder().encodeToString(fileBytes);
        return base64Image;
    }

    public static void main(String[] args) {
        // Replace "path/to/your/image.png" with the actual path to your PNG file
        String imagePath = "path/to/your/image.png"; 

        try {
            String base64String = convertPngToBase64(imagePath);
            System.out.println("Base64 Encoded String:\n" + base64String);
        } catch (IOException e) {
            System.err.println("Error converting image to Base64: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
