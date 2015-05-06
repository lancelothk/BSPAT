package edu.cwru.cbc.BSPAT.DataType;

/**
 * Created by kehu on 8/19/14.
 */
public interface CpG {
	int getPosition();

	int getMethylCount();

	int getNonMethylCount();

	int getCountOfAll();

	double getMethylLevel();
}
