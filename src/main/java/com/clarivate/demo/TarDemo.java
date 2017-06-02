package com.clarivate.demo;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

/*
 * example: 

java -cp target/scratch_pad-0.0.1-SNAPSHOT-jar-with-dependencies.jar com.clarivate.demo.TarDemo --print-to-console true --extract_id false --parse-json false --need-content false
 * 
 */
public class TarDemo {
	
	private static Pattern RECORDID_PATTERN = Pattern.compile("([^/]+)\\.json$");
	private static Pattern RECORDID_PATTERN1 = Pattern.compile(".*/(\\d+).json$");
	private static Pattern EOL = Pattern.compile("[\\n\\r]");
	
	private static boolean print2Console = true;
	private static boolean parseJson = true;
	private static boolean extractId = true;
	private static boolean needContent = true;
	private static boolean removeEol = false;
	private static boolean uncompressed = false;

	public static void main(String... args) throws FileNotFoundException {
		String filePath = "data/drug-2016-11-14_1519-all-pp.json.tar.gz";
		for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--path":
                	filePath = args[++i];
                    break;
                case "--print-to-console":
                	print2Console = Boolean.parseBoolean(args[++i]);
                	break;
                case "--parse-json":
                	parseJson = Boolean.parseBoolean(args[++i]);
                	break;
                case "--extract_id":
                	extractId = Boolean.parseBoolean(args[++i]);
                	break;
                case "--need-content":
                	needContent = Boolean.parseBoolean(args[++i]);
                	break;
                case "--remove-eol":
                	removeEol = true;
                	break;
                case "--uncompressed":
                	uncompressed = true;
                	break;
                	
             }
		}
		
		FileInputStream fileInputStream = new FileInputStream(filePath);

		TarDemo demo = new TarDemo();
		demo.testDrive(fileInputStream);
	}

	private String extractId(String name) {
		if (extractId){
			Matcher matcher = RECORDID_PATTERN.matcher(name);
			if (matcher.find()) {
				return matcher.group(1);
			} else {
				return null;
			}
		} else{
			return name;
		}
	}
	
	private String extractId1(String name) {
		if (extractId){
			Matcher matcher = RECORDID_PATTERN1.matcher(name);
			if (matcher.find()) {
				return matcher.group(1);
			} else {
				return null;
			}
		} else{
			return name;
		}
	}

	protected void testDrive(final InputStream in) {
		try {
			long timestamp_0 = System.currentTimeMillis();
			TarArchiveInputStream tarArchive;
			if (uncompressed){
				tarArchive = new TarArchiveInputStream(in);
			} else {
				final BufferedInputStream bin = new BufferedInputStream(in);
				final GzipCompressorInputStream gzIn = new GzipCompressorInputStream(bin);
				tarArchive = new TarArchiveInputStream(gzIn);
			}
			long timestamp_1 = System.currentTimeMillis();
			System.out.println("time spent on decompression (ms): " + ":" + (timestamp_1 - timestamp_0));
			
			TarArchiveEntry entry = tarArchive.getNextTarEntry();
			long counter = 1;
			long timestamp1 = System.currentTimeMillis();
			
			List<String> list = new ArrayList<>();
			
			while (entry != null) {
				
				if (!entry.isDirectory()) {
					
					String recordId = extractId1(entry.getName());
					list.add(entry.getName());
					
					String currentValue = "";
					if (needContent){
						byte[] ba = new byte[(int) entry.getSize()];
						tarArchive.read(ba, 0, ba.length);

						currentValue =new String(ba);
						if (parseJson){
							currentValue = validateAndRemoveWhitespace(currentValue);
						} else if (removeEol){
							currentValue = replaceAllEOL(currentValue);
						}
					}
					
					if (print2Console){
						System.out.println(counter + ":" + recordId + ":" + currentValue);
					}
					counter++;
				} 
				entry = tarArchive.getNextTarEntry();
			}
			long timestamp2 = System.currentTimeMillis();
			System.out.println("time spent on iterating items (ms): " + ":" + (timestamp2 - timestamp1));
			System.out.println("# of entries:" + counter);
			long timestamp3 = System.currentTimeMillis();
			for (String name : list){
				String s = extractId1(name);
			}
			
			long timestamp4 = System.currentTimeMillis();
			System.out.println(list.size());
			System.out.println("3-4 time spent on this (ms): "+ timestamp4 + ":" + timestamp3 + ":" + (timestamp4 - timestamp3));
			
			
			tarArchive.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private String validateAndRemoveWhitespace(String jsonInput)  {
        try {
            StringWriter writer  = new StringWriter();
            JsonFactory factory = new JsonFactory();
            JsonParser parser = factory.createParser(jsonInput);
            try (JsonGenerator gen = factory.createGenerator(writer)) {
                while (parser.nextToken() != null) {
                    gen.getCurrentValue();
                    gen.copyCurrentEvent(parser);
                }
            }
            return writer.toString();
        } catch (IOException e) {
            throw new RuntimeException("Invalid JSON="+jsonInput,e);
        }
    }
	
	private String replaceAllEOL(String jsonInput){
		return EOL.matcher(jsonInput).replaceAll("");
		//return jsonInput.replaceAll("[\\n\\r]","");
	}
	
	
}
