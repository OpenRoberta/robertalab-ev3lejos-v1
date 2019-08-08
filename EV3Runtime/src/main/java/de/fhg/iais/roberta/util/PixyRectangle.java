package de.fhg.iais.roberta.util;
import lejos.robotics.geometry.RectangleInt32;

public class PixyRectangle extends RectangleInt32 {

	public int signature;
	public int angle;
	private final static int x_max = 255;
	private final static int y_max = 200;
	public PixyRectangle(int signature, int angle, int x, int y, int width, int height)
	{
		//super(x, y, width, height); //original coordinates
		super(x, y, width, height);
		this.angle = angle;
		this.signature = signature;
	}
	
	public PixyRectangle(int signature, int x, int y, int width, int height)
	{
		super(-x+(x_max/2), -y+(y_max/2), width, height);
		this.angle = 0;
		this.signature = signature;
	}
}
