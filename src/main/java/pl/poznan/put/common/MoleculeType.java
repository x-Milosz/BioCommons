package pl.poznan.put.common;

import org.biojava.bio.structure.Chain;
import org.biojava.bio.structure.Group;

import pl.poznan.put.atoms.AtomName;
import pl.poznan.put.helper.StructureHelper;
import pl.poznan.put.nucleic.RNABackboneAtoms;
import pl.poznan.put.nucleic.RNABondRule;
import pl.poznan.put.nucleic.RNATorsionAngle;
import pl.poznan.put.protein.ProteinBackboneAtoms;
import pl.poznan.put.protein.ProteinBondRule;
import pl.poznan.put.protein.ProteinTorsionAngle;
import pl.poznan.put.torsion.AtomsBasedTorsionAngle;

public enum MoleculeType {
    RNA(RNABackboneAtoms.getAtoms(), AtomName.P, new RNABondRule(), RNATorsionAngle.values()),
    PROTEIN(ProteinBackboneAtoms.getAtoms(), AtomName.CA, new ProteinBondRule(), ProteinTorsionAngle.values()),
    UNKNOWN(new AtomName[0], null, null, new AtomsBasedTorsionAngle[0]);

    private final AtomName[] backboneAtoms;
    private final AtomName mainAtom;
    private final ResidueBondRule bondRule;
    private final AtomsBasedTorsionAngle[] torsionAngles;

    private MoleculeType(AtomName[] backboneAtoms, AtomName mainAtom,
            ResidueBondRule bondRule, AtomsBasedTorsionAngle[] torsionAngles) {
        this.backboneAtoms = backboneAtoms;
        this.mainAtom = mainAtom;
        this.bondRule = bondRule;
        this.torsionAngles = torsionAngles;
    }

    public AtomName[] getBackboneAtoms() {
        return backboneAtoms;
    }

    public AtomName getMainAtom() {
        return mainAtom;
    }

    public boolean areConnected(Group g1, Group g2) {
        return bondRule.areConnected(g1, g2);
    }

    public AtomsBasedTorsionAngle[] getBackboneTorsionAngles() {
        return torsionAngles;
    }

    public static MoleculeType detect(Chain chain) {
        // decide upon first residue only!
        Group residue = chain.getAtomGroup(0);
        return detect(residue);
    }

    public static MoleculeType detect(Group residue) {
        int bestScore = 3; // at least three atoms must match!
        MoleculeType bestType = MoleculeType.UNKNOWN;

        for (MoleculeType type : MoleculeType.values()) {
            int score = 0;

            for (AtomName atomType : type.getBackboneAtoms()) {
                if (StructureHelper.findAtom(residue, atomType) != null) {
                    score++;
                }
            }

            if (score > bestScore) {
                bestScore = score;
                bestType = type;
            }
        }

        return bestType;
    }
}