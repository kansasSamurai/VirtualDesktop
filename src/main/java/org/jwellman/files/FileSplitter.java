package org.jwellman.files;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;

public class FileSplitter {

	protected int linecount = 0;
	protected int filecount = 0;
	protected File sourceFile;
	protected File currentOutputFile;
	protected File[] allOutputFiles;
	protected BufferedWriter currentWriter;
	protected String destinationPath;
	protected String destinationPrefix = "output_";
	protected String destinationExtension = ".txt";
	protected Predicate startFilePredicate;
	
	public static FileSplitter splitByContent() {		
		return new SplitByContent();
	}
	
	public static FileSplitter splitBySize() {
		throw new RuntimeException("Method not implemented");
	}
	
	public FileSplitter begin() throws IOException {
		// meant to be overriddent; no default behavior.
		return this;
	}
	
	protected File startNewFile() throws IOException {
		this.filecount++;
		
		String absolutePath = sourceFile.getCanonicalPath();
		String filepath = absolutePath.substring(0,absolutePath.lastIndexOf(File.separator));
		
		currentOutputFile = new File(
				filepath, 
				this.destinationPrefix 
				+ this.filecount 
				+ this.destinationExtension);
		return currentOutputFile;
	}

	public void stuff() {
		
//		import org.jwellman.files.FileSplitter;
//		import org.jwellman.files.LINE;

		try {
			FileSplitter
			.splitByContent()
			.startNewFileWhen(LINE.beginsWith("START "))
			.begin();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	public FileSplitter startNewFileWhen(LINE predicate) {
		this.startFilePredicate = new BEGINSWITH(predicate.value);
		return this;
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

	public FileSplitter begin() throws IOException {

		LineIterator it = null;
		try {
			this.sourceFile = new File("C:/dev/workspaces/textfiles/loremipsum.txt");
			
			it = FileUtils.lineIterator(this.sourceFile, "UTF-8");
			while (it.hasNext()) {
				String line = it.nextLine();
				linecount++;

				if (startFilePredicate.evaluate(line)) {
					if (this.currentWriter != null)
						this.currentWriter.close();
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
