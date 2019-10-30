package pl.poznan.put.sequence.alignment;

import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.biojava.nbio.core.exceptions.CompoundNotFoundException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import pl.poznan.put.pdb.analysis.PdbCompactFragment;
import pl.poznan.put.pdb.analysis.PdbModel;
import pl.poznan.put.pdb.analysis.PdbParser;
import pl.poznan.put.utility.ResourcesHelper;

public class SequenceAlignerTest {
  // @formatter:off
  private static final String GLOBAL_ALIGNMENT =
      "1EHZ        gC---GGAUUUAgCUCAGuuGGGAGAGCgCCAGAcUgAAgAu--cUGGAGgUCcUGUGuu\n"
          + "2MIY        gCUUGGUGCUUAGCUUCUUU-------CACCAAGC---AUAUUACACGCGG---------\n"
          + "            **   *   *** **            * ***      * *      * *          \n"
          + "\n"
          + "1EHZ        CGaU--CCACAGAAUUCGCACCA\n"
          + "2MIY        --AUAACCGCCAAA---GGAGAA\n"
          + "               *  ** *  **   * *  *\n"
          + "\n";
  // @formatter:on

    private PdbCompactFragment fragment1EHZ;
  private PdbCompactFragment fragment2MIY;

  @Before
  public final void setUp() throws IOException {
    final PdbParser parser = new PdbParser();
      final List<PdbModel> models1EHZ = parser.parse(ResourcesHelper.loadResource("1EHZ.pdb"));
      final List<PdbModel> models2MIY = parser.parse(ResourcesHelper.loadResource("2MIY.pdb"));

    Assert.assertThat(!models1EHZ.isEmpty(), is(true));
    Assert.assertThat(!models2MIY.isEmpty(), is(true));

    fragment1EHZ = new PdbCompactFragment("1EHZ", models1EHZ.get(0).getResidues());
    fragment2MIY = new PdbCompactFragment("2MIY", models2MIY.get(0).getResidues());
  }

  @Test
  public final void align() throws CompoundNotFoundException {
    final SequenceAligner aligner =
        new SequenceAligner(Arrays.asList(fragment1EHZ, fragment2MIY), true);
    final SequenceAlignment actual = aligner.align();
    final SequenceAlignment expected =
        new SequenceAlignment(true, SequenceAlignerTest.GLOBAL_ALIGNMENT);
    Assert.assertThat(actual, is(expected));
  }
}
