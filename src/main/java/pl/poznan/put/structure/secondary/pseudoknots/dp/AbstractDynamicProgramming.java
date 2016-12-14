package pl.poznan.put.structure.secondary.pseudoknots.dp;

import org.apache.commons.collections4.CollectionUtils;
import pl.poznan.put.structure.secondary.formats.BpSeq;
import pl.poznan.put.structure.secondary.formats.InvalidStructureException;
import pl.poznan.put.structure.secondary.pseudoknots.ConflictMap;
import pl.poznan.put.structure.secondary.pseudoknots.Region;
import pl.poznan.put.structure.secondary.pseudoknots.elimination.RegionRemover;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Java implementation of OPT ALL algorithm as presented in: Smit, S.
 * et al., 2008. From knotted to nested RNA structures: A variety of
 * computational methods for pseudoknot removal. RNA, 14, pp.410–416.
 */
public abstract class AbstractDynamicProgramming implements DynamicProgramming {
    private final RegionRemover regionRemover;
    private final int maxCliqueSize;

    /**
     * Construct an instance of dynamic solver for all solutions. The
     * regionRemover will be used only until any clique has size bigger then
     * maxCliqueSize! If not, the conflicts are resolved optimally.
     *
     * @param regionRemover A region remover to be used.
     * @param maxCliqueSize Maximum number of conflicts allowed to be in the
     *                      clique. The algorithm slows down very much when the
     *                      cliques are getting bigger, so it is advisable to
     *                      use heuristic to remove single regions prior to a
     *                      dynamic programming attempt.
     */
    protected AbstractDynamicProgramming(
            final RegionRemover regionRemover, final int maxCliqueSize) {
        super();
        this.regionRemover = regionRemover;
        this.maxCliqueSize = maxCliqueSize;
    }

    protected static SubSolution[] solveSingleCase(
            final SubSolution[][][] matrix, final Clique clique, final int i,
            final int j) {
        int size = clique.endpointCount();
        Collection<SubSolution> candidates = new HashSet<>(size);

        // add solutions from the left
        if (matrix[i][j - 1].length > 0) {
            candidates.addAll(Arrays.asList(matrix[i][j - 1]));
        }

        // add solutions from the bottom
        if (matrix[i + 1][j].length > 0) {
            candidates.addAll(Arrays.asList(matrix[i + 1][j]));
        }

        // if there is a region from i to j, then add it and all from
        // the bottom left
        Region region =
                clique.findRegion(clique.getEndpoint(i), clique.getEndpoint(j));
        if (region != null) {
            SubSolution current = new SubSolution(region);

            if (matrix[i + 1][j - 1].length > 0) {
                for (final SubSolution subSolution : matrix[i + 1][j - 1]) {
                    candidates.add(SubSolution.merge(subSolution, current));
                }
            } else {
                candidates.add(current);
            }
        }

        // merge solution from next-row, previous-column
        candidates
                .addAll(AbstractDynamicProgramming.merge(matrix, clique, i, j));

        // select all candidates with highest score
        List<SubSolution> bestCandidates =
                AbstractDynamicProgramming.selectBestCandidates(candidates);
        return bestCandidates.toArray(new SubSolution[bestCandidates.size()]);
    }

    protected static Collection<SubSolution> merge(
            final SubSolution[][][] matrix, final Clique clique, final int i,
            final int j) {
        SubSolution[] left = matrix[i][j - 1];
        SubSolution[] below = matrix[i + 1][j];

        if ((left.length == 0) || (below.length == 0)) {
            return Collections.emptyList();
        }

        Collection<SubSolution> result = new ArrayList<>();

        for (final SubSolution leftSub : left) {
            int highestEndpoint = leftSub.getHighestEndpoint();

            for (final SubSolution belowSub : below) {
                int lowestEndpoint = belowSub.getLowestEndpoint();

                if (highestEndpoint < lowestEndpoint) {
                    result.add(SubSolution.merge(leftSub, belowSub));
                    continue;
                }

                int begin = clique.indexOfEndpoint(lowestEndpoint) - 1;
                int end = clique.indexOfEndpoint(highestEndpoint) + 1;

                for (int k = begin; k < end; k++) {
                    for (final SubSolution s1 : matrix[i][k]) {
                        for (final SubSolution s2 : matrix[k + 1][j]) {
                            result.add(SubSolution.merge(s1, s2));
                        }
                    }
                }
            }
        }

        return result;
    }

    protected static List<SubSolution> selectBestCandidates(
            final Collection<SubSolution> candidates) {
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        SortedMap<Integer, List<SubSolution>> map = new TreeMap<>();

        for (final SubSolution candidate : candidates) {
            int score = candidate.getScore();

            if (!map.containsKey(score)) {
                map.put(score, new ArrayList<SubSolution>());
            }

            map.get(score).add(candidate);
        }

        return map.get(map.lastKey());
    }

    @Override
    public final List<BpSeq> findPseudoknots(final BpSeq bpSeq)
            throws InvalidStructureException {
        List<Region> regions = Region.createRegions(bpSeq);
        ConflictMap conflictMap = new ConflictMap(regions);
        conflictMap = AbstractDynamicProgramming.simplify(regions, conflictMap);

        List<BpSeq.Entry> nonConflicting = new ArrayList<>();
        for (final Region region : regions) {
            if (!conflictMap.hasConflicts(region)) {
                nonConflicting.addAll(region.getEntries());
            }
        }

        int max;
        do {
            max = Integer.MIN_VALUE;
            for (final Clique clique : conflictMap.getConflictCliques(false)) {
                if (clique.size() > max) {
                    max = clique.size();
                }
            }
            if (max > maxCliqueSize) {
                conflictMap.remove(regionRemover
                                           .selectRegionToRemove(conflictMap));
            }
        } while (max > maxCliqueSize);

        List<List<BpSeq.Entry>> results = new ArrayList<>();
        results.add(nonConflicting);
        List<Clique> cliques = conflictMap.getConflictCliques(false);

        for (final Clique clique : cliques) {
            List<List<BpSeq.Entry>> nextResults = new ArrayList<>();
            SubSolution[] solutions = findOptimalSolutions(clique);

            for (final SubSolution solution : solutions) {
                for (final List<BpSeq.Entry> previousResult : results) {
                    List<BpSeq.Entry> nextResult =
                            new ArrayList<>(previousResult);
                    for (final Region region : solution.getRegions()) {
                        nextResult.addAll(region.getEntries());
                    }
                    nextResults.add(nextResult);
                }
            }

            results = nextResults;
        }

        List<BpSeq> bpSeqs = new ArrayList<>();

        for (final List<BpSeq.Entry> result : results) {
            BpSeq nextBpSeq = new BpSeq(bpSeq.getEntries());
            for (final BpSeq.Entry entry : result) {
                nextBpSeq.removePair(entry);
            }
            bpSeqs.add(nextBpSeq);
        }

        return bpSeqs;
    }

    private static ConflictMap simplify(
            final List<Region> regions, final ConflictMap conflictMap) {
        ConflictMap result = conflictMap;
        Collection<Region> toRemove = new ArrayList<>();
        Collection<Region> toAdd = new ArrayList<>();

        do {
            List<Region> conflicted =
                    new ArrayList<>(result.getRegionsWithConflicts());
            toRemove.clear();
            toAdd.clear();

            for (int i = 0; i < conflicted.size(); i++) {
                Region ri = conflicted.get(i);
                int riBegin = ri.getBegin();
                int riEnd = ri.getEnd();

                for (int j = i + 1; j < conflicted.size(); j++) {
                    Region rj = conflicted.get(j);
                    int rjBegin = rj.getBegin();
                    int rjEnd = rj.getEnd();

                    boolean b1 = (riBegin <= rjBegin) && (riEnd >= rjEnd);
                    boolean b2 = (rjBegin <= riBegin) && (rjEnd >= riEnd);
                    if ((b1 || b2) && CollectionUtils
                            .isEqualCollection(result.conflictsWith(ri),
                                               result.conflictsWith(rj))) {
                        toRemove.add(ri);
                        toRemove.add(rj);
                        toAdd.add(Region.merge(ri, rj));
                        break;
                    }
                }

                if (!toRemove.isEmpty()) {
                    break;
                }
            }

            if (!toRemove.isEmpty()) {
                regions.removeAll(toRemove);
                regions.addAll(toAdd);
                result = new ConflictMap(regions);
            }
        } while (!toRemove.isEmpty());

        return result;
    }
}