package edu.cwru.cbc.BSPAT.MethylFigure;

/**
 * Created by kehu on 8/19/14.
 */
public interface CpG extends Comparable<CpG> {
	int getPosition();

	int getMethylCount();

	int getNonMethylCount();

	int getCountOfAll();

	double getMethylLevel();
}
