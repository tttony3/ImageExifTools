package com.github.tttony3;

public class FieldType {
	public static FieldType Byte =new FieldType(1, 8);
	public static FieldType Ascii =new FieldType(2, 8);
	public static FieldType Short=new FieldType(3, 16);
	public static FieldType Long=new FieldType(4, 32);
	public static FieldType Rational=new FieldType(5, 64);
	public static FieldType Undefined=new FieldType(7, 8);
	public static FieldType Slong=new FieldType(8, 32);
	public static FieldType SRational=new FieldType(10, 64);
	
	public int id ;
	public int size;
	private FieldType(int id,int size){
		this.id=id;
		this.size=size;
	}
}
