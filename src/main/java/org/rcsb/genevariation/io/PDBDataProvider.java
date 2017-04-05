package org.rcsb.genevariation.io;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.rcsb.genevariation.utils.SaprkUtils;

public class PDBDataProvider extends DataProvider {
	
	private final static String dfGenevariationPath = getProjecthome() + "parquet/hg38/";
	private final static String dfUniprotpdbPath = getProjecthome() + "parquet/uniprotpdb/20161104/";
	

	public static Dataset<Row> readHumanChromosomeMapping(String chr) {
        Dataset<Row> chrMapping = SaprkUtils.getSparkSession().read().parquet(dfGenevariationPath+chr);
        return chrMapping;
	}
	
	public static Dataset<Row> readPdbUniprotMapping() {
		Dataset<Row> mapping = SaprkUtils.getSparkSession().read().parquet(dfUniprotpdbPath);
		return mapping;
	}
	
	public static void main(String[] args) {
		
		Dataset<Row> map = readHumanChromosomeMapping("chr1");

		map.createOrReplaceTempView("map");

		Dataset<Row> df1 = SaprkUtils.getSparkSession().sql("select distinct geneSymbol, uniProtId from map where geneSymbol='RHD'");
		df1.show();

		Dataset<Row> df2 = SaprkUtils.getSparkSession().sql("select distinct geneSymbol, uniProtId from map where uniProtId='H3BT10'");
		df2.show();
	}
}
