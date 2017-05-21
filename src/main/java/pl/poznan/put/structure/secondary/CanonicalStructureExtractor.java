package pl.poznan.put.structure.secondary;

import pl.poznan.put.notation.BPh;
import pl.poznan.put.notation.BR;
import pl.poznan.put.notation.LeontisWesthof;
import pl.poznan.put.notation.Saenger;
import pl.poznan.put.pdb.PdbResidueIdentifier;
import pl.poznan.put.pdb.analysis.PdbResidue;
import pl.poznan.put.pdb.analysis.ResidueCollection;
import pl.poznan.put.rna.RNAInteractionType;
import pl.poznan.put.structure.secondary.formats.BpSeq;
import pl.poznan.put.structure.secondary.formats.InvalidStructureException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public final class CanonicalStructureExtractor {
    private CanonicalStructureExtractor() {
        super();
        // empty constructor
    }

    /*
     * This is just a simple implementation. For a robust solution, see RNApdbee
     * http://rnapdbee.cs.put.poznan.pl
     */
    public static BpSeq getCanonicalSecondaryStructure(
            final ResidueCollection residueCollection)
            throws InvalidStructureException {
        final List<PdbResidue> residues = residueCollection.getResidues();
        final Collection<ClassifiedBasePair> basePairs = new ArrayList<>();
        final Collection<PdbResidueIdentifier> paired = new HashSet<>();

        for (int i = 0; i < residues.size(); i++) {
            final PdbResidue left = residues.get(i);
            final PdbResidueIdentifier leftId = left.getResidueIdentifier();

            for (int j = i + 2; j < residues.size(); j++) {
                final PdbResidue right = residues.get(j);
                final PdbResidueIdentifier rightId = right.getResidueIdentifier();

                if (BasePair.isCanonicalPair(left, right)) {
                    final BasePair basePair = new BasePair(leftId, rightId);
                    final ClassifiedBasePair classifiedBasePair =
                            new ClassifiedBasePair(basePair,
                                                   RNAInteractionType.BASE_BASE,
                                                   Saenger.XIX,
                                                   LeontisWesthof.CWW,
                                                   BPh.UNKNOWN, BR.UNKNOWN,
                                                   HelixOrigin.UNKNOWN);

                    if (!paired.contains(leftId) && !paired.contains(rightId)) {
                        basePairs.add(classifiedBasePair);
                        paired.add(leftId);
                        paired.add(rightId);
                    }
                }
            }
        }

        return BpSeq.fromResidueCollection(residueCollection, basePairs);
    }
}
