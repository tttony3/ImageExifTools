package com.github.tttony3;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

public class Main {

	public static void main(String[] args) {
		ExifParser exifParser = new ExifParser(new File("img.jpg"));
		exifParser.loadImage();
		exifParser.getFieldStringMessage().entrySet().iterator().forEachRemaining(new Consumer<Map.Entry<ExifTag,String>>() {
			@Override
			public void accept(Entry<ExifTag, String> t) {
				System.out.println(t.getKey()+" "+t.getValue());
				
			}
		});;
		exifParser.getGpsStringMessage().entrySet().iterator().forEachRemaining(new Consumer<Map.Entry<ExifTag,String>>() {
			@Override
			public void accept(Entry<ExifTag, String> t) {
				System.out.println(t.getKey()+" "+t.getValue());
				
			}
		});;
		System.out.println(exifParser.modifyGps(1,1,1,1,1,1,"modify_"+"img.jpg"));
	}

}
