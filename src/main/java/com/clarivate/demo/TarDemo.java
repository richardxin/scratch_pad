package com.clarivate.demo;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

public class TarDemo {
	//private TarArchiveInputStream tarArchiveInputStream;
	private static Pattern recordIdPattern = Pattern.compile("([^/]+)\\.json$");

	public static void main(String[] args) throws FileNotFoundException {
		String filePath = "data/drug-2016-11-14_1519-all-pp.json.tar.gz";
		FileInputStream fileInputStream = new FileInputStream(filePath);

		TarDemo demo = new TarDemo();
		demo.testDrive(fileInputStream);

	}

	private String extractId(String name) {
		Matcher matcher = recordIdPattern.matcher(name);
		if (matcher.find()) {
			return matcher.group(1);
		} else {
			return null;
		}
	}

	protected void testDrive(final InputStream in) {
		try {
			final BufferedInputStream bin = new BufferedInputStream(in);
			final GzipCompressorInputStream gzIn = new GzipCompressorInputStream(bin);
			final TarArchiveInputStream tarArchive = new TarArchiveInputStream(gzIn);

			TarArchiveEntry entry = tarArchive.getNextTarEntry();
			long counter = 1;
			long timestamp1 = System.currentTimeMillis();
			while (entry != null) {
				if (!entry.isDirectory()) {
					String recordId = extractId(entry.getName());
					byte[] ba = new byte[(int) entry.getSize()];
					tarArchive.read(ba, 0, ba.length);
					String currentValue = new String(ba);
					System.out.println(counter + ":" + recordId + ":" + currentValue);
					counter++;
				}
				entry = tarArchive.getNextTarEntry();
			}
			long timestamp2 = System.currentTimeMillis();
			System.out.println("time spent on this (second): "+ (timestamp2 - timestamp1)/1000);
			tarArchive.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
