package pl.poznan.put.torsion;

import pl.poznan.put.interfaces.DisplayableExportable;
import pl.poznan.put.rna.torsion.Chi;
import pl.poznan.put.rna.torsion.RNATorsionAngleType;

import java.util.Collection;

/**
 * This is to gather under one interface every "master" torsion angle type. A
 * {@link RNATorsionAngleType#CHI} is a master torsion angle type, and {@link
 * Chi#getPurineInstance()} is a non-master instance.
 *
 * @author tzok
 */
public interface MasterTorsionAngleType extends DisplayableExportable {
    Collection<? extends TorsionAngleType> getAngleTypes();
}
