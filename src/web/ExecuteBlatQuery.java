package web;

import BSPAT.Utilities;
import DataType.Constant;

import java.io.File;
import java.io.IOException;

public class ExecuteBlatQuery implements Runnable {
	private Constant constant;

	public ExecuteBlatQuery(Constant constant) {
		this.constant = constant;
	}

	@Override
	public void run() {
		File refFolder = new File(constant.originalRefPath);
		String[] files = refFolder.list();
		String blatQueryPath = constant.toolsPath + "BlatQuery/";
		for (String name : files) {
			try {
				System.out.println("start blat query for " + name);
				String blatQuery = String.format("%sBlatQuery.sh %s %s %s %s", blatQueryPath, blatQueryPath, constant.refVersion, constant.originalRefPath, name);
				Utilities.callCMD(blatQuery, new File(constant.coorFilePath));
				System.out.println("blat query is finished for " + name);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			Utilities.convertPSLtoCoorPair(constant.coorFilePath, constant.coorFileName, constant.refVersion);
			constant.coorReady = true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("blat result converted");
	}
}
