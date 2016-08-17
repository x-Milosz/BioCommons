package pl.poznan.put.rna;

import pl.poznan.put.atom.AtomName;

import java.util.Collections;
import java.util.List;

public abstract class Sugar extends NucleicAcidResidueComponent {
    private static final Sugar INVALID =
            new Sugar(Collections.<AtomName>emptyList()) {
                // empty block
            };

    public static Sugar invalidInstance() {
        return Sugar.INVALID;
    }

    protected Sugar(List<AtomName> atoms) {
        super(RNAResidueComponentType.SUGAR, atoms);
    }
}
