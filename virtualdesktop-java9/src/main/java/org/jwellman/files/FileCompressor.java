package org.jwellman.files;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

/**
 * File Compression utilities.
 * 
 * @author Rick Wellman
 *
 */
public class FileCompressor {

	/**
	 * Creates a .zip file from the file.
	 * 
	 * By default, the zip file will be in the same directory
	 * and have the same base filename as the original.
	 * i.e. C:\example\zipme.txt will become C:\example\zipme.zip
	 * 
	 * @param file
	 * @throws IOException
	 * @throws ArchiveException
	 */
    public static void zip(File file) throws IOException, ArchiveException {

        final BufferedInputStream input = new BufferedInputStream(new FileInputStream(file));
        
    	String path = file.getPath();
    	String absolute = file.getAbsolutePath();
    	String canonical = file.getCanonicalPath();
    	String basename = FilenameUtils.getBaseName(canonical);
    	String fullpath = FilenameUtils.getFullPath(canonical);
    	
        String entryName = FilenameUtils.getName(canonical);
        ZipArchiveEntry entry = new ZipArchiveEntry(file, entryName); //(getEntryName(source, file));

        final OutputStream archiveStream = new FileOutputStream( fullpath + basename + ".zip"); //(destination);            		
        final ArchiveOutputStream archive = new ArchiveStreamFactory().createArchiveOutputStream(ArchiveStreamFactory.ZIP, archiveStream);
        archive.putArchiveEntry(entry);
        IOUtils.copy(input, archive);
        archive.closeArchiveEntry();            
        archive.finish();           
        
        archiveStream.close();
        input.close();            

    }

    /**
	 * Creates a .zip file from the file.
	 * 
	 * By default, the zip file will be in the same directory
	 * and have the same base filename as the original.
	 * i.e. C:\example\zipme.txt will become C:\example\zipme.zip
	 * 
     * @param location
     * @throws IOException
     * @throws ArchiveException
     */
    public static void zip(String location) throws IOException, ArchiveException {
    	FileCompressor.zip(new File(location));
    }

	/**
	 * Creates a .zip file for each file in the list.
	 * (This is just a convenience wrapper around zip(File) )
	 * 
	 * By default, the zip file will be in the same directory
	 * and have the same base filename as the original.
	 * i.e. C:\example\zipme.txt will become C:\example\zipme.zip
	 * 
	 * @param fileList Collection<File> := // full pathname of files to archive
	 * @throws IOException
	 * @throws ArchiveException
	 */
    public static void zip(Collection<File> fileList) throws IOException, ArchiveException {
	    for (File file : fileList) {
	    	FileCompressor.zip(file);
	    }
	}

}
