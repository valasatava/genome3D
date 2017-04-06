package exonscorrelation.mappers;

import exonscorrelation.ExonProteinFeatures;
import exonscorrelation.UniprotMapping;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Row;
import org.rcsb.genevariation.tools.DisorderPredictor;

import java.util.List;

public class MapToProteinDisorder implements MapFunction<Row, ExonProteinFeatures> {

	/**
	 *
	 */
	private static final long serialVersionUID = -1382222868798031985L;

	@Override
	public ExonProteinFeatures call(Row row) throws Exception {

		String uniprotIds = row.getString(10);
		int isoformNum = row.getInt(9);

		String uniprotId;
		if (uniprotIds.contains(",")) {
			System.out.println(uniprotIds);
			uniprotId = uniprotIds.split(",")[0];
		}
		else {
			uniprotId = uniprotIds;
		}

		String isoform = UniprotMapping.getIsoform(uniprotId, isoformNum);
		List<Integer> isosten = exonscorrelation.utils.CommonUtils.getIsoStartEndForRow(row);
		int isoformStart = isosten.get(0);
		int isoformEnd = isosten.get(1);
		if (isoformStart == -1 || isoformEnd == -1)
			return null;

		float[] disorder = DisorderPredictor.run(isoform);

		int len = isoformEnd-isoformStart-1;
		float[] disorderExon = new float[len];
		System.arraycopy(disorder, isoformStart, disorderExon, 0, len);

		ExonProteinFeatures feature = new ExonProteinFeatures();
		feature.setChromosome(row.getString(2));
		feature.setEnsemblId(row.getString(4));
		feature.setStart(row.getInt(8));
		feature.setEnd(row.getInt(3));
		feature.setDisorder(disorderExon);

		return feature;
	}
}