package org.rcsb.geneprot.transcriptomics.sandbox;

import org.rcsb.uniprot.auto.Entry;
import org.rcsb.uniprot.auto.FeatureType;
import org.rcsb.uniprot.auto.Uniprot;
import org.rcsb.uniprot.config.RCSBUniProtMirror;

import javax.xml.bind.JAXBException;
import java.io.FileNotFoundException;

/**
 * Created by yana on 4/26/17.
 */
public class PrintUniprotFeatures {

    public static void main(String args[]) throws FileNotFoundException, JAXBException {

        String uniProtId = "P13569";
        Uniprot up = RCSBUniProtMirror.getUniProtFromFile(uniProtId);

        for(Entry e : up.getEntry()){
            for (FeatureType ft : e.getFeature()){
                System.out.println(ft.getType());
                if (ft.getType().equals("topological domain") || ft.getType().equals("transmembrane region")) {
                    // ranged features
                    if (ft.getLocation() != null && ft.getLocation().getBegin() != null && ft.getLocation().getEnd() != null)
                        System.out.println("RANGED: " + ft.getDescription() + " --- " +
                                ft.getType() + " --- " +
                                ft.getLocation().getBegin().getPosition() + " : " + ft.getLocation().getEnd().getPosition());

                    // single resiude position features
                    if (ft.getLocation() != null && ft.getLocation().getPosition() != null && ft.getLocation().getPosition().getPosition() != null)
                        System.out.println(
                                "FT: " + ft.getDescription() + " --- " +
                                        ft.getType() + " --- " +
                                        ft.getLocation().getPosition().getPosition()
                        );
                }
            }
        }
    }
}