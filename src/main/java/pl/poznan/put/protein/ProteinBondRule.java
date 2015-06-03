package pl.poznan.put.protein;

import org.biojava.bio.structure.Atom;
import org.biojava.bio.structure.Calc;
import org.biojava.bio.structure.Group;
import org.biojava.bio.structure.StructureException;

import pl.poznan.put.atom.AtomName;
import pl.poznan.put.atom.AtomType;
import pl.poznan.put.atom.Bond;
import pl.poznan.put.common.ResidueBondRule;
import pl.poznan.put.structure.tertiary.StructureHelper;

public class ProteinBondRule implements ResidueBondRule {
    @Override
    public boolean areConnected(Group r1, Group r2) {
        Atom c = StructureHelper.findAtom(r1, AtomName.C);
        Atom n = StructureHelper.findAtom(r2, AtomName.N);

        try {
            if (c != null && n != null) {
                double distance = Calc.getDistance(c, n);
                if (distance <= Bond.length(AtomType.C, AtomType.N).getMax() * 1.5) {
                    return true;
                }
            }
        } catch (StructureException e) {
            // do nothing
        }

        return false;
    }
}