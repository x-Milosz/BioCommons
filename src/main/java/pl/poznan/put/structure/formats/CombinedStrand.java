package pl.poznan.put.structure.formats;

import org.immutables.value.Value;
import pl.poznan.put.structure.DotBracketSymbol;
import pl.poznan.put.structure.ModifiableDotBracketSymbol;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** A dot-bracket encoded structure made from combining one or more strands. */
@Value.Immutable
public abstract class CombinedStrand implements DotBracket {
  /** @return The list of input strands. */
  @Value.Parameter(order = 1)
  protected abstract List<Strand> inputStrands();

  @Override
  public final String toString() {
    final String builder = strands().stream().map(Strand::name).collect(Collectors.joining());
    return ">strand_" + builder + '\n' + sequence(false) + '\n' + structure(false);
  }

  @Override
  public final String sequence() {
    return sequence(false);
  }

  @Override
  public final String structure() {
    return structure(false);
  }

  @Value.Lazy
  @Value.Auxiliary
  public List<DotBracketSymbol> symbols() {
    return strands().stream()
        .map(Strand::symbols)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  @Value.Lazy
  @Value.Auxiliary
  public List<Strand> strands() {
    final Map<DotBracketSymbol, Integer> symbolToIndex = new HashMap<>();
    int i = 0;
    for (final Strand strand : inputStrands()) {
      for (final DotBracketSymbol symbol : strand.symbols()) {
        symbolToIndex.put(symbol, i);
        i += 1;
      }
    }

    final List<Strand> strands = new ArrayList<>();
    final List<ModifiableDotBracketSymbol> symbols = new ArrayList<>();

    for (final Strand strand : inputStrands()) {
      final List<DotBracketSymbol> strandSymbols = new ArrayList<>();

      for (final DotBracketSymbol symbol : strand.symbols()) {
        final char sequence = symbol.sequence();
        final char structure = symbol.structure();
        final int index = symbolToIndex.get(symbol);
        final ModifiableDotBracketSymbol renumbered =
            ModifiableDotBracketSymbol.create(sequence, structure, index);
        renumbered.setPrevious(symbol.previous());
        renumbered.setNext(symbol.next());
        renumbered.setPair(symbol.pair());
        renumbered.setIsNonCanonical(symbol.isNonCanonical());

        strandSymbols.add(renumbered);
        symbols.add(renumbered);
      }

      strands.add(ImmutableStrandDirect.of(strand.name(), strandSymbols));
    }

    for (final Strand strand : strands) {
      for (final DotBracketSymbol symbol : strand.symbols()) {
        if (symbol.isPairing()) {
          final ModifiableDotBracketSymbol u = symbols.get(symbolToIndex.get(symbol));
          final ModifiableDotBracketSymbol v =
              symbols.get(
                  symbolToIndex.get(
                      symbol
                          .pair()
                          .orElseThrow(
                              () ->
                                  new IllegalArgumentException(
                                      "Paired dot-bracket symbol without `pair` field set: "
                                          + symbol))));
          u.setPair(v);
          v.setPair(u);
        }
      }
    }

    return strands;
  }

  @Override
  public final List<DotBracket> combineStrands() {
    return Collections.singletonList(this);
  }

  @Override
  public final int originalIndex(final DotBracketSymbol symbol) {
    return symbol.index() + 1;
  }
}
