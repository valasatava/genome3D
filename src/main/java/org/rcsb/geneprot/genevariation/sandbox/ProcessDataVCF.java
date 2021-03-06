package org.rcsb.geneprot.genevariation.sandbox;

import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.rcsb.geneprot.genes.constants.StrandOrientation;
import org.rcsb.geneprot.genes.datastructures.Transcript;
import org.rcsb.geneprot.genes.expression.RNApolymerase;
import org.rcsb.geneprot.genes.expression.Ribosome;
import org.rcsb.geneprot.genes.parsers.GenePredictionsParser;
import org.rcsb.geneprot.genevariation.datastructures.Mutation;
import org.rcsb.geneprot.genevariation.datastructures.VariantInterface;
import org.rcsb.geneprot.genevariation.filters.IVariantDataFilter;
import org.rcsb.geneprot.genevariation.filters.VariantDataFilterChromosome;
import org.rcsb.geneprot.genevariation.filters.VariantDataFilterSNP;
import org.rcsb.geneprot.genevariation.io.VariantsDataProvider;
import org.rcsb.geneprot.common.utils.SparkUtils;
import org.rcsb.geneprot.genevariation.utils.VariationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessDataVCF {

	private final static String userHome = System.getProperty("user.home");
	private final static String variationDataPath = userHome + "/data/genevariation/common_and_clinical_20170130.vcf";

	private static final Logger logger = LoggerFactory.getLogger(ProcessDataVCF.class);

	public static List<Mutation> getMutations(String chrName, VariantsDataProvider vdp) throws Exception {

		// --> GET VARIANTS
		long start2 = System.nanoTime();
		Iterator<VariantInterface> variations = vdp.getAllVariants();
		System.out.println("Time to filter the variation data: " + (System.nanoTime() - start2) / 1E9 + " sec.");

		// --> GET COL_CHROMOSOME POSITIONS
		long start3 = System.nanoTime();
		List<Transcript> transcripts = GenePredictionsParser.getChromosomeMappings().stream()
				.filter(t -> t.getChromosomeName().equals(chrName)).collect(Collectors.toList());
		System.out.println("Time to get chromosome data for "+chrName+": " + (System.nanoTime() - start3) / 1E9 + " sec.");

		// --> MAP GENOMIC COORDINATE TO mRNA POSITION

		long start4 = System.nanoTime();

		List<Mutation> allMutations = new ArrayList<>();
		RNApolymerase polymerase = new RNApolymerase();
		while (variations.hasNext()) {

			VariantInterface variant = variations.next();

			for (Transcript transcript : transcripts) {

				if ( ( variant.getPosition() >= transcript.getCodingStart() ) && (variant.getPosition() <= transcript.getCodingEnd()) ) {

					int mRNApos = polymerase.getmRNAPositionForGeneticCoordinate((int) variant.getPosition(), transcript);
					if (mRNApos == -1)
						continue;
					
					String codingSequence = polymerase.getCodingSequence(transcript);
					String codon = polymerase.getCodon(mRNApos, codingSequence);

					String mutBase = variant.getAltBase();
					String codonM="";
					if (transcript.getOrientation().equals(StrandOrientation.FORWARD)) {
						codonM = VariationUtils.mutateCodonForward(mRNApos, codon, mutBase);
					}
					else {
						codonM = VariationUtils.mutateCodonReverse(mRNApos, codon, mutBase);
					}

					Mutation mutation = new Mutation();
					mutation.setChromosomeName(chrName);
					mutation.setGeneBankId(transcript.getGeneBankId());
					mutation.setPosition(variant.getPosition());
					mutation.setRefAminoAcid(Ribosome.getProteinSequence(codon));
					mutation.setMutAminoAcid(Ribosome.getProteinSequence(codonM));
					allMutations.add(mutation);
				}
			}
		}
		// --> THE END
		System.out.println("Time to map chromosome coordinates to mRNA positions: " + (System.nanoTime() - start4) / 1E9 + " sec.");
		return allMutations;
	}
	
	public static void run() throws Exception {
		
		logger.info("Started...");
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
		System.out.println("Job ID:" + timeStamp);
		long start = System.nanoTime();
		
		List<Mutation> allMutations = new ArrayList<>();
		
		String[] chromosomes = {"chr1", "chr2", "chr3", "chr4", "chr5", "chr6", "chr7", "chr8", "chr9", "chr10", "chr11",  
				"chr12", "chr13", "chr14", "chr15", "chr16", "chr17", "chr18", "chr19",  "chr20", "chr21", "chr22", "chrX", "chrY"};		
		for (String chr : chromosomes) {

			// --> READ VCF FILE
			VariantsDataProvider vdp = new VariantsDataProvider();
			vdp.readVariantsFromVCFWithParser(Paths.get(variationDataPath));
			IVariantDataFilter dataFilterChr = new VariantDataFilterChromosome(chr);
			IVariantDataFilter dataFilterVar = new VariantDataFilterSNP();
			vdp.setVariants(vdp.getVariantsByFilter(dataFilterChr));
			vdp.setVariants(vdp.getVariantsByFilter(dataFilterVar));
			System.out.println("Time to read VCF file: " + (System.nanoTime() - start) / 1E9 + " sec.");
			
			List<Mutation> mutations = getMutations(chr, vdp);
			allMutations.addAll(mutations);
		}

		Dataset<Row> mydf = SparkUtils.getSparkSession().createDataFrame(allMutations, Mutation.class);
		mydf.write().mode(SaveMode.Overwrite).parquet(userHome + "/data/genevariation/mutations");

		System.out.println("DONE!");
		System.out.println("Total time: " + (System.nanoTime() - start) / 1E9 + " sec.");
	}
	
	public static void main(String[] args) throws Exception {
		
		long start = System.nanoTime();
		VariantsDataProvider vdp = new VariantsDataProvider();
		vdp.readVariantsFromVCF();
		List<Mutation> mutations = vdp.getSNPMutations();
		vdp.createVariationDataFrame(mutations, "mutations.parquet");
		System.out.println("Done: " + (System.nanoTime() - start) / 1E9 + " sec.");
	}
}
