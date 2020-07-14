package com.ffmpeg;

public class H264Decode {

	static{
		System.loadLibrary("ffmpeg");
	}
	/**
	 * 初始化第几个通道
	 */
	public native static int Initialize(int n);


	/**
	 * 指定获取通道的宽度
	 */
	public native static  int GetWidth (int n);


	/**
	 * 指定获取通道的高度
	 */
	public native static int GetHeight(int n);


	/**
	 * 指定通道解码
	 */
	public native static int DecodeOneFrame(int n,byte[] inBuf,int begin,int inLen);


	/**
	 * 销毁指定通道的解码库
	 */
	public native static int Destory(int n);


	/**
	 * 获取指定通道的像素数组
	 */
	public native static int GetPixel(int n,int [] pixel);


	/**
	 * 获取YUV数组
	 */
	public native static int GetYUVPixels(int n,byte[] yCompontent,byte[] uCompontent,byte[] vCompontent);


}
