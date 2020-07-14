package com.minicreate.adas.model;

/*
 * 
 * Video or Audio Data
 */
public class VideoData {
	public byte[] buffer;
	public int size;
	public long timestamp;
	public int sequence;
	public VideoData() {
		size = 0;
	}
}
