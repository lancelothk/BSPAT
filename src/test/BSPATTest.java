package test;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;

import org.junit.Test;

import BSPAT.DrawPattern;
import BSPAT.Utilities;
import DataType.Constant;

public class BSPATTest {

	@Test
	public void testConvertCoordinates() throws IOException {
//		Class<?> c;
//		try {
//			DrawPattern dp = new DrawPattern("PNG", "hg18", "WebContent/tools");
//			c = dp.getClass();
//			Method method = c.getDeclaredMethod("convertCoordinates", new Class[] { String.class, Long.TYPE, String.class });
//			method.setAccessible(true);
//			Object o = method.invoke(dp, "chr4", 20000, "hg19");
//			assertEquals(30000,(long)o);
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

	@Test
	public void testConvertPSLtoCoorPair() {
//		try {
//			Utilities.convertPSLtoCoorPair("testTemp/", "coordinates", "hg18");
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}
	@Test
	public void testObjSerWrite() throws FileNotFoundException, IOException {
//		Constant constant = Constant.getInstance();
//		Constant.WEBAPPFOLDER = "/home/ke/";
//		constant.runID = "12345";
//		Constant.writeConstant();
		
//		String s = "12345";
//		ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("/home/ke/test.data"));
//		out.writeObject(s);
//		out.close();
	}
	
	@Test
	public void testObjRead() throws FileNotFoundException, IOException, ClassNotFoundException{
		Constant.WEBAPPFOLDER = "/home/ke/";
		Constant constant = Constant.getInstance();
		constant = Constant.readConstant("12345");
		System.out.println(constant.runID);
		
//		ObjectInputStream in = new ObjectInputStream(new FileInputStream("/home/ke/test.data"));
//		String s = (String) in.readObject();
//		System.out.println(s);
	}
}
