package test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.reflect.Method;

import org.junit.Test;

import BSPAT.DrawPattern;
import BSPAT.Utilities;

public class BSPATTest {

	@Test
	public void testConvertCoordinates() throws IOException {
		Class<?> c;
		try {
			DrawPattern dp = new DrawPattern("PNG", "hg18", "WebContent/tools");
			c = dp.getClass();
			Method method = c.getDeclaredMethod("convertCoordinates", new Class[] { String.class, Long.TYPE, String.class });
			method.setAccessible(true);
			Object o = method.invoke(dp, "chr4", 20000, "hg19");
			assertEquals(30000,(long)o);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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

}
