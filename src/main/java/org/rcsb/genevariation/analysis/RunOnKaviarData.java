package org.rcsb.genevariation.analysis;

import java.util.List;

import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoder;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.biojava.nbio.genome.parsers.genename.GeneChromosomePosition;
import org.rcsb.genevariation.datastructures.VcfContainer;
import org.rcsb.genevariation.io.DataProvider;
import org.rcsb.genevariation.mappers.FilterCodingRegion;
import org.rcsb.genevariation.mappers.FilterSNPs;
import org.rcsb.genevariation.mappers.MapToVcfContainer;
import org.rcsb.genevariation.parser.GenePredictionsParser;
import org.rcsb.genevariation.utils.SaprkUtils;

public class RunOnKaviarData {
	
	public static void run() throws Exception {
		
		long start = System.nanoTime();
		
		//String filename = "common_and_clinical_20170130.vcf";
		String filename = "Kaviar-160204-Public/vcfs/Kaviar-160204-Public-hg38-trim.vcf";
		String filepath = DataProvider.getProjecthome() + filename;
		
		List<GeneChromosomePosition> transcripts = GenePredictionsParser.getGeneChromosomePositions();

		JavaSparkContext sc = SaprkUtils.getSparkContext();
		Broadcast<List<GeneChromosomePosition>> transcriptsBroadcast = sc.broadcast(transcripts);

		Encoder<VcfContainer> vcfContainerEncoder = Encoders.bean(VcfContainer.class);
		
		SaprkUtils.getSparkSession().read()
				.format("com.databricks.spark.csv")
				.option("header", "false")
				.option("delimiter", "\t")
				.option("comment", "#")
				.load(filepath)
				.flatMap(new MapToVcfContainer(), vcfContainerEncoder)
				.filter(new FilterCodingRegion(transcriptsBroadcast))
				.filter(new FilterSNPs())
				.repartition(500)
				.write().mode(SaveMode.Overwrite).parquet(DataProvider.getProjecthome() + "coding-snps-Kaviar.parquet");
	
		System.out.println("Done: " + (System.nanoTime() - start) / 1E9 + " sec.");
	}
	
	public static void read() {
		String filepath = DataProvider.getProjecthome() + "coding-snps-Kaviar.parquet";
		Dataset<Row> df = SaprkUtils.getSparkSession().read().parquet(filepath);
        df.createOrReplaceTempView("Kaviar");
        df.show();
	}
	
	public static void main(String[] args) throws Exception {
		run();
		//read();
	}
}
