package pl.poznan.put.structure.pseudoknots.elimination;

import pl.poznan.put.structure.formats.BpSeq;
import pl.poznan.put.structure.formats.ImmutableBpSeq;
import pl.poznan.put.structure.pseudoknots.ConflictMap;
import pl.poznan.put.structure.pseudoknots.Region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Java implementation of region removal algorithm as presented in: Smit, S. et al., 2008. From
 * knotted to nested RNA structures: A variety of computational methods for pseudoknot removal. RNA,
 * 14, pp.410–416.
 */
public abstract class AbstractRegionRemover implements RegionRemover {
  // Unremove all Regions that were removed but are no longer in conflict
  private static void restoreNonConflicting(final Iterable<Region> regions) {
    for (final Region ri : regions) {
      if (!ri.isRemoved()) {
        continue;
      }

      boolean nonConflicting = true;
      for (final Region rj : regions) {
        if (!rj.isRemoved() && ConflictMap.isConflicting(ri, rj)) {
          nonConflicting = false;
          break;
        }
      }

      if (nonConflicting) {
        ri.setRemoved(false);
      }
    }
  }

  @Override
  public final List<BpSeq> findPseudoknots(final BpSeq bpSeq) {
    final List<Region> regions = Region.createRegions(bpSeq);
    final ConflictMap conflictMap = new ConflictMap(regions);

    while (conflictMap.hasAnyConflicts()) {
      final Region region = selectRegionToRemove(conflictMap);
      region.setRemoved(true);
      conflictMap.remove(region);
    }

    AbstractRegionRemover.restoreNonConflicting(regions);

    final Collection<BpSeq.Entry> validPairs = new ArrayList<>();
    for (final Region region : regions) {
      if (!region.isRemoved()) {
        validPairs.addAll(region.getEntries());
      }
    }

    BpSeq result = ImmutableBpSeq.copyOf(bpSeq);
    for (final BpSeq.Entry validPair : validPairs) {
      result = result.withoutPair(validPair);
    }
    return Collections.singletonList(result);
  }
}