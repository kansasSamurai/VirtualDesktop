package org.jwellman.files;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

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
		
//		import org.jwellman.files.FileCompressor;
//		import org.jwellman.files.FileSplitter;
//		import org.jwellman.files.LINE;
//		import org.jwellman.files.*;

		FileSplitter fs = FileSplitter
		.splitByContent("filepath/.../filename.ext")
		.startNewFileWhen(LINE.beginsWith("ISA*00*"))
		.begin();
		// this.sourceFile = new File("C:/dev/workspaces/textfiles/loremipsum.txt");
		// loremipsum , Generate_EDI3020_20200326

//		print fs.getAllOutputFiles();
//		fs.addFilesToZip(fs.getAllOutputFiles());
// or
//      FileCompressor.zip(fs.getAllOutputFiles());
	}

	public FileSplitter startNewFileWhen(LINE predicate) {
		this.startFilePredicate = new BEGINSWITH(predicate.value);
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
	 * @return the currentWriter
	 */
	public BufferedWriter getCurrentWriter() {
		return currentWriter;
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
	public Predicate getStartFilePredicate() {
		return startFilePredicate;
	}
	
}

interface Predicate {	
	boolean evaluate(String s);
}

class LINE implements Predicate {

	protected String value;
	
	protected LINE(String value) {
		this.value = value;
	}
	
	public static LINE beginsWith(String s) {
		return new BEGINSWITH(s);
	}
	
	@Override
	public boolean evaluate(String s) {
		return false;
	}
	
}

class BEGINSWITH extends LINE {
	
	protected BEGINSWITH(String value) {
		super(value);
	}
	
	@Override
	public boolean evaluate(String s) {
		return s.startsWith(value);
	}
	
}

class SplitByContent extends FileSplitter {

	protected SplitByContent(String sourceFile) {
		super(sourceFile);
	}

	public FileSplitter begin() throws IOException {

		LineIterator it = null;
		try {
			// this.sourceFile = new File("C:/dev/workspaces/textfiles/loremipsum.txt");
			// loremipsum , Generate_EDI3020_20200326
			
			it = FileUtils.lineIterator(this.sourceFile, "UTF-8");
			while (it.hasNext()) {
				String line = it.nextLine();
				linecount++;

				if (startFilePredicate.evaluate(line)) {
					if (this.currentWriter != null) this.currentWriter.close();
					currentWriter = new BufferedWriter(new FileWriter(this.startNewFile()));										
				}
				/// do something with line
				currentWriter.write(line);
				currentWriter.write(System.lineSeparator());
			}

		} finally {
			if (it != null)
				LineIterator.closeQuietly(it);
			if (currentWriter != null)
				currentWriter.close();
		}		 
		 
		return this;
	}

}
