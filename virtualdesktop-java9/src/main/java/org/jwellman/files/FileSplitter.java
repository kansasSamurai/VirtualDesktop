package org.jwellman.files;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.jwellman.files.predicate.LINE;
import org.jwellman.files.predicate.Predicate;

/**
 * 
 * @author rwellman
 *
 */
public class FileSplitter {

	protected int linecount = 0;
	protected int filecount = 0;
	protected File sourceFile;
	protected File currentOutputFile;
	protected List<File> allOutputFiles = new ArrayList<>();
	protected BufferedWriter currentWriter;
	protected String destinationPath;
	protected String destinationPrefix = "output_";
	protected String destinationExtension = ".txt";
	protected Predicate startFilePredicate;

	/**
	 * Constructor
	 * Do NOT make public
	 * ... this is called by subclasses only!
	 * ... and subclasses are instantiated via the factory methods:
	 * splitByContent()
	 * splitByLineCount()
	 * 
	 * @param sourceFile
	 */
	protected FileSplitter(String sourceFile) {
		this.sourceFile = new File(sourceFile);
	}

	public static FileSplitter splitByContent(String sourceFile) {		
		return new SplitByContent(sourceFile);
	}
	
	public static FileSplitter splitByLineCount(String sourceFile) {
		throw new RuntimeException("Method not implemented");
	}
	
	public FileSplitter begin() throws IOException {
		// meant to be overriddent; no default behavior.
		return this;
	}
	
	protected File startNewFile() throws IOException {
		this.filecount++;
		
		final String absolutePath = sourceFile.getCanonicalPath();
		final String filepath = absolutePath.substring(0,absolutePath.lastIndexOf(File.separator));
		final String destination = this.destinationPrefix + this.filecount + this.destinationExtension;
				
		this.allOutputFiles.add(currentOutputFile = new File(filepath, destination));
		return currentOutputFile;
	}
	
	public void stuff() throws IOException {
		
//      import org.jwellman.files.FileCompressor;
//      import org.jwellman.files.FileSplitter;
//      import org.jwellman.files.LINE;
//      import org.jwellman.files.*;
//      import org.jwellman.files.predicate.*;

      FileSplitter fs = FileSplitter
      .splitByContent("filepath/.../filename.ext")
      .startNewFileWhen (LINE.beginsWith("ISA*00*")) // (LINELENGTH.equals(7))
      .begin();
      // this.sourceFile = new File("C:/dev/workspaces/textfiles/loremipsum.txt");
      // loremipsum , Generate_EDI3020_20200326

//      print fs.getAllOutputFiles();
//      fs.addFilesToZip(fs.getAllOutputFiles());
//or
//    FileCompressor.zip(fs.getAllOutputFiles());
	}

	public FileSplitter startNewFileWhen(Predicate<?> predicate) {
		this.startFilePredicate = predicate;
		return this;
	}

	/**
	 * @return the linecount
	 */
	public int getLinecount() {
		return linecount;
	}

	/**
	 * @return the filecount
	 */
	public int getFilecount() {
		return filecount;
	}

	/**
	 * @return the sourceFile
	 */
	public File getSourceFile() {
		return sourceFile;
	}

	/**
	 * @return the currentOutputFile
	 */
	public File getCurrentOutputFile() {
		return currentOutputFile;
	}

	/**
	 * @return the allOutputFiles
	 */
	public List<File> getAllOutputFiles() {
		return allOutputFiles;
	}

	/**
	 * @return the destinationPath
	 */
	public String getDestinationPath() {
		return destinationPath;
	}

	/**
	 * @return the destinationPrefix
	 */
	public String getDestinationPrefix() {
		return destinationPrefix;
	}

	/**
	 * @return the destinationExtension
	 */
	public String getDestinationExtension() {
		return destinationExtension;
	}

	/**
	 * @return the startFilePredicate
	 */
	public Predicate<?> getStartFilePredicate() {
		return startFilePredicate;
	}
	
}

/**
 * 
 * @author rwellman
 *
 */
class SplitByContent extends FileSplitter {

	protected SplitByContent(String sourceFile) {
		super(sourceFile);
	}

	public FileSplitter begin() throws IOException {

    	try (LineIterator it = FileUtils.lineIterator(this.sourceFile, "UTF-8")) {
						
			while (it.hasNext()) {
				
				// Read the next line and increment the line count
				final String line = it.nextLine(); linecount++;

				// First thing is first... should this line start a new output file?
				if (startFilePredicate.evaluate(line)) {
					if (this.currentWriter != null) this.currentWriter.close();
					// TODO... this assignment of currentWriter is probably best embedded inside startNewFile()
					currentWriter = new BufferedWriter(new FileWriter(this.startNewFile()));										
				}
				
				
				

				// ... if a file is open, write the line to the current output file
				if (currentWriter != null) {
					currentWriter.write(line);
					currentWriter.newLine();					
				}
			}

		} finally {
			if (currentWriter != null) currentWriter.close();
		}		 
		 
		return this;
	}

}
