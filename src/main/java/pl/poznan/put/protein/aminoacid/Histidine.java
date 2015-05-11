package pl.poznan.put.protein.aminoacid;

import java.util.Arrays;

import pl.poznan.put.atom.AtomName;
import pl.poznan.put.protein.ProteinChiType;
import pl.poznan.put.protein.ProteinSidechain;
import pl.poznan.put.protein.torsion.Chi1;
import pl.poznan.put.protein.torsion.Chi2;
import pl.poznan.put.types.Quadruplet;

public class Histidine extends ProteinSidechain {
    private static final Histidine INSTANCE = new Histidine();

    public static Histidine getInstance() {
        return Histidine.INSTANCE;
    }

    private Histidine() {
        super(Arrays.asList(new AtomName[] { AtomName.CB, AtomName.HB1, AtomName.HB2, AtomName.ND1, AtomName.HD1, AtomName.CG, AtomName.CE1, AtomName.HE1, AtomName.NE2, AtomName.HE2, AtomName.CD2, AtomName.HD2 }), "Histidine", 'H', "HIS", "HSD", "HSE", "HSP");
        torsionAngleTypes.add(Chi1.getInstance(this));
        torsionAngleTypes.add(Chi2.getInstance(this));
    }

    @Override
    protected void fillChiAtomsMap() {
        chiAtoms.put(ProteinChiType.CHI1, new Quadruplet<AtomName>(AtomName.N, AtomName.CA, AtomName.CB, AtomName.CG));
        chiAtoms.put(ProteinChiType.CHI2, new Quadruplet<AtomName>(AtomName.CA, AtomName.CB, AtomName.CG, AtomName.ND1));
    }
}
