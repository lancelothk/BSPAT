package DataType;

import java.io.Serializable;

import gov.nih.nlm.ncbi.www.soap.eutils.EFetchSnpServiceStub.Assembly_type0;
import gov.nih.nlm.ncbi.www.soap.eutils.EFetchSnpServiceStub.Frequency_type0;
import gov.nih.nlm.ncbi.www.soap.eutils.EFetchSnpServiceStub.MolType_type1;
import gov.nih.nlm.ncbi.www.soap.eutils.EFetchSnpServiceStub.Phenotype_type0;
import gov.nih.nlm.ncbi.www.soap.eutils.EFetchSnpServiceStub.SnpType_type0;

public class SNP implements Serializable{
	// general info
	private int rsID;
	private SnpType_type0 snpType;
	private MolType_type1 molType;
	private Phenotype_type0[] phenoTypes;
	private Assembly_type0[] assemblies;
	private Frequency_type0[] frequencies;

	public SNP(int rsID, SnpType_type0 snpType, MolType_type1 molType, Phenotype_type0[] phenoTypes,
			Assembly_type0[] assemblies, Frequency_type0[] frequencies) {
		super();
		this.rsID = rsID;
		this.snpType = snpType;
		this.molType = molType;
		this.phenoTypes = phenoTypes;
		this.assemblies = assemblies;
		this.frequencies = frequencies;
	}
	
	public int getrsID() {
		return rsID;
	}

	public void print(){
		System.out.println("rsID: " + rsID);
		System.out.println("SNP type: " + snpType);
		System.out.println("getMolType: " + molType);
		if (phenoTypes != null) {
			System.out.println("getPhenotype: " + phenoTypes[0]);
		}

//		System.out.println("getGenomeBuild: " + assemblies[0].getGenomeBuild());
//		System.out.println("getDbSnpBuild: " + assemblies[0].getDbSnpBuild());
//		System.out.println("getChromosome: " + assemblies[0].getComponent()[0].getChromosome());
		System.out.println("getLocType: " + assemblies[0].getComponent()[0].getMapLoc()[0].getLocType());
		System.out.println("getOrient: " + assemblies[0].getComponent()[0].getMapLoc()[0].getOrient());
//		System.out.println("getAsnFrom: " + assemblies[0].getComponent()[0].getMapLoc()[0].getAsnFrom());
//		System.out.println("getAsnTo: " + assemblies[0].getComponent()[0].getMapLoc()[0].getAsnTo());
		System.out.println("getPhysMapInt: "
				+ assemblies[0].getComponent()[0].getMapLoc()[0].getPhysMapInt());

		if (frequencies != null) {
			System.out.println("getAllele: " + frequencies[0].getAllele());
			System.out.println("getFreq: " + frequencies[0].getFreq());
			System.out.println("getSampleSize: " + frequencies[0].getSampleSize());
		}
		System.out.println("------------------------------------------");
	}
}
