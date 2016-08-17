package pl.poznan.put.structure.secondary.pseudoknots;

import pl.poznan.put.structure.secondary.formats.BpSeq;
import pl.poznan.put.structure.secondary.formats.DotBracket;
import pl.poznan.put.structure.secondary.formats.InvalidStructureException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class BpSeqToDotBracketConverter {
    private static final char[] BRACKETS_OPENING =
            "([{<ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final char[] BRACKETS_CLOSING =
            ")]}>abcdefghijklmnopqrstuvwxyz".toCharArray();

    private final PseudoknotFinder pkRemover;
    private final int maxSolutions;

    /**
     * Construct an instance of converter from BPSEQ to dot-bracket.
     *
     * @param pkRemover    An instance of algorithm to find which pairs are
     *                     pseudoknots.
     * @param maxSolutions Maximum number of solutions to be considered in a
     *                     single step of the algorithm.
     */
    public BpSeqToDotBracketConverter(final PseudoknotFinder pkRemover,
                                      final int maxSolutions) {
        super();
        this.pkRemover = pkRemover;
        this.maxSolutions = maxSolutions;
    }

    public final DotBracket convert(final BpSeq bpSeq)
            throws InvalidStructureException {
        List<State> states = new ArrayList<>();
        states.add(new State(null, bpSeq, 0));

        while (BpSeqToDotBracketConverter.isProcessingNeeded(states)) {
            states = processStates(states);
        }

        Collections.sort(states);
        String structure = BpSeqToDotBracketConverter.traceback(states.get(0));
        return new DotBracket(bpSeq.getSequence(), structure);
    }

    protected static boolean isProcessingNeeded(final Iterable<State> states) {
        for (State state : states) {
            if (!state.isFinal()) {
                return true;
            }
        }
        return false;
    }

    protected final List<State> processStates(final Collection<State> states)
            throws InvalidStructureException {
        List<State> nextStates = new ArrayList<>(states.size());
        for (State state : states) {
            for (BpSeq bpSeq : pkRemover.findPseudoknots(state.bpSeq)) {
                State nextState = new State(state, bpSeq, state.level + 1);
                nextStates.add(nextState);

                if (nextStates.size() > maxSolutions) {
                    return nextStates;
                }
            }
        }
        return nextStates;
    }

    private static String traceback(final State state) {
        char[] structure = new char[state.size()];
        Arrays.fill(structure, '.');
        State current = state.parent;

        while (current != null) {
            for (BpSeq.Entry pairs : current.bpSeq.getPaired()) {
                int i = pairs.getIndex();
                int j = pairs.getPair();

                if (structure[i - 1] == '.') {
                    structure[i - 1] =
                            BpSeqToDotBracketConverter
                                    .BRACKETS_OPENING[current.level];
                    structure[j - 1] =
                            BpSeqToDotBracketConverter
                                    .BRACKETS_CLOSING[current.level];
                }
            }

            current = current.parent;
        }

        return new String(structure);
    }

    private static final class State implements Comparable<State> {
        private final State parent;
        private final BpSeq bpSeq;
        private final int level;
        private final int score;

        private State(final State parent, final BpSeq bpSeq, final int level) {
            super();
            this.parent = parent;
            this.bpSeq = bpSeq;
            this.level = level;
            score = bpSeq.getPaired().size();
        }

        public boolean isFinal() {
            return score == 0;
        }

        public int size() {
            return bpSeq.size();
        }

        @Override
        public int compareTo(final State t) {
            if (level < t.level) {
                return -1;
            }
            if (level > t.level) {
                return 1;
            }
            if (score < t.score) {
                return -1;
            }
            if (score > t.score) {
                return 1;
            }
            if (level != 0) {
                return parent.compareTo(t.parent);
            }
            return 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(bpSeq, level, parent, score);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if ((obj == null) || (getClass() != obj.getClass())) {
                return false;
            }
            State state = (State) obj;
            boolean result = level == state.level;
            result &= score == state.score;
            result &= Objects.equals(bpSeq, state.bpSeq);
            result &= Objects.equals(parent, state.parent);
            return result;
        }

        @Override
        public String toString() {
            return String.format("State{level=%d, score=%d}", level, score);
        }
    }
}
