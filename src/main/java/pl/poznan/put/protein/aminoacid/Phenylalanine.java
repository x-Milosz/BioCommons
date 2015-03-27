package pl.poznan.put.protein.aminoacid;

import java.util.Arrays;

import pl.poznan.put.atom.AtomName;
import pl.poznan.put.protein.ProteinChiType;
import pl.poznan.put.protein.ProteinSidechain;
import pl.poznan.put.types.Quadruplet;

public class Phenylalanine extends ProteinSidechain {
    private static final Phenylalanine INSTANCE = new Phenylalanine();

    public static Phenylalanine getInstance() {
        return Phenylalanine.INSTANCE;
    }

    private Phenylalanine() {
        super(Arrays.asList(new AtomName[] { AtomName.CB, AtomName.HB1, AtomName.HB2, AtomName.CG, AtomName.CD1, AtomName.HD1, AtomName.CE1, AtomName.HE1, AtomName.CZ, AtomName.HZ, AtomName.CD2, AtomName.HD2, AtomName.CE2, AtomName.HE2 }), "Phenylalanine", 'F', "PHE");
    }

    @Override
    protected void fillChiAtomsMap() {
        chiAtoms.put(ProteinChiType.CHI1, new Quadruplet<AtomName>(AtomName.N, AtomName.CA, AtomName.CB, AtomName.CG));
        chiAtoms.put(ProteinChiType.CHI2, new Quadruplet<AtomName>(AtomName.CA, AtomName.CB, AtomName.CG, AtomName.CD1));
    }
}