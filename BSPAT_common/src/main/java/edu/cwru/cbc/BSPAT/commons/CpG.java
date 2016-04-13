package edu.cwru.cbc.BSPAT.commons;

public interface CpG extends Comparable<CpG> {
	int getPosition();

	int getMethylCount();

	int getNonMethylCount();

	int getCountOfAll();

	double getMethylLevel();
}
