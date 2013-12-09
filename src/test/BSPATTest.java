package test;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import BSPAT.Utilities;

public class BSPATTest {

	@Test
	public void test() throws IOException {
		Utilities.convertPSLtoCoorPair("/home/ke/software/blat");
	}

}
