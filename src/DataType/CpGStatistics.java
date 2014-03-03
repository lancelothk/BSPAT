package DataType;

public class CpGStatistics {

	private int countOfAllSites;
	private double countOfmethylatedSites;
	private double methylationRate;
	private int position;


	public CpGStatistics(int position) {
		// TODO Auto-generated constructor stub
		countOfAllSites = 0;
		countOfmethylatedSites = 0;
		methylationRate = 0;
		this.position = position;
	}

	public void setMethylationRate(double methylationRate) {
		this.methylationRate = methylationRate;
	}

	public void calcMethylRate() {
		methylationRate = countOfmethylatedSites / countOfAllSites;
	}

	public double getMethylationRate() {
		return methylationRate;
	}

	public int getPosition() {
		return position;
	}

	public void allSitePlus() {
		countOfAllSites++;
	}

	public void methylSitePlus() {
		countOfmethylatedSites++;
	}

	public int getCountOfAllSites() {
		return countOfAllSites;
	}

	public double getCountOfmethylatedSites() {
		return countOfmethylatedSites;
	}
}