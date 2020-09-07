package pl.poznan.put;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.biojava.nbio.structure.Structure;
import org.biojava.nbio.structure.io.PDBFileParser;
import org.junit.Test;
import pl.poznan.put.atom.AtomName;
import pl.poznan.put.pdb.PdbAtomLine;
import pl.poznan.put.pdb.analysis.CifModel;
import pl.poznan.put.pdb.analysis.CifParser;
import pl.poznan.put.pdb.analysis.MoleculeType;
import pl.poznan.put.pdb.analysis.PdbChain;
import pl.poznan.put.pdb.analysis.PdbModel;
import pl.poznan.put.pdb.analysis.PdbParser;
import pl.poznan.put.pdb.analysis.PdbResidue;
import pl.poznan.put.structure.secondary.CanonicalStructureExtractor;
import pl.poznan.put.structure.secondary.formats.BpSeq;
import pl.poznan.put.utility.ResourcesHelper;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PdbModelTest {
  @Test
  public final void testParsing() throws Exception {
    final String pdb1EHZ = ResourcesHelper.loadResource("1EHZ.pdb");
    final PdbParser parser = new PdbParser();
    final List<PdbModel> models = parser.parse(pdb1EHZ);
    assertThat(models.size(), is(1));
  }

  @Test
  public final void testResidueAnalysis() throws Exception {
    final String pdb1EHZ = ResourcesHelper.loadResource("1EHZ.pdb");
    final PdbParser parser = new PdbParser();
    final List<PdbModel> models = parser.parse(pdb1EHZ);
    final PdbModel model = models.get(0);
    final List<PdbResidue> residues = model.residues();
    assertThat(residues.size(), is(76));
  }

  @Test
  public final void testChainAnalysis() throws Exception {
    final String pdb1EHZ = ResourcesHelper.loadResource("1EHZ.pdb");
    final PdbParser parser = new PdbParser();
    final List<PdbModel> models = parser.parse(pdb1EHZ);
    final PdbModel model = models.get(0);
    final List<PdbChain> chains = model.getChains();
    assertThat(chains.size(), is(1));
  }

  @Test
  public final void testModifiedResidue() throws Exception {
    final String pdb1EHZ = ResourcesHelper.loadResource("1EHZ.pdb");
    final PdbParser parser = new PdbParser();
    final List<PdbModel> models = parser.parse(pdb1EHZ);
    final PdbModel model = models.get(0);
    final PdbResidue residue = model.findResidue("A", 10, " ");
    assertThat(residue.residueName(), is("2MG"));
    assertThat(residue.modifiedResidueName(), is("G"));
    assertThat(residue.getDetectedResidueName(), is("G"));
  }

  @Test
  public final void testUnmodifiedResidue() throws Exception {
    final String pdb1EHZ = ResourcesHelper.loadResource("1EHZ.pdb");
    final PdbParser parser = new PdbParser();
    final List<PdbModel> models = parser.parse(pdb1EHZ);
    final PdbModel model = models.get(0);
    final PdbResidue residue = model.findResidue("A", 74, " ");
    assertThat(residue.residueName(), is("C"));
    assertThat(residue.modifiedResidueName(), is("C"));
    assertThat(residue.getDetectedResidueName(), is("C"));
  }

  @Test
  public final void testO3PModificationDetection() throws Exception {
    final String pdb1EHZ = ResourcesHelper.loadResource("1EHZ.pdb");
    final PdbParser parser = new PdbParser();
    final List<PdbModel> models = parser.parse(pdb1EHZ);
    final PdbModel model = models.get(0);
    final List<PdbResidue> residues = model.residues();

    // for first residue expect a mismatch in atom count because it has an
    // unusual O3P terminal atom
    final PdbResidue residue = residues.get(0);
    assertThat(residue.isModified(), is(true));
    assertThat(residue.hasAllAtoms(), is(false));
  }

  @Test
  public final void testUridineModificationDetection() throws Exception {
    final String pdb1EHZ = ResourcesHelper.loadResource("1EHZ.pdb");
    final PdbParser parser = new PdbParser();
    final List<PdbModel> models = parser.parse(pdb1EHZ);
    final PdbModel model = models.get(0);

    // the H2U (dihydrouridine) is modified by two additional hydrogens
    // which is undetectable in a non-hydrogen PDB file
    final PdbResidue residueA16 = model.findResidue("A", 16, " ");
    assertThat(residueA16.isModified(), is(true));
    assertThat(residueA16.hasAllAtoms(), is(true));

    // the PSU (pseudouridine) contains the same atoms, but it is an isomer
    // and therefore a modified residue
    final PdbResidue residueA39 = model.findResidue("A", 39, " ");
    assertThat(residueA39.isModified(), is(true));
    assertThat(residueA39.hasAllAtoms(), is(true));

    // the 5MU (methyluridine) is the RNA counterpart of thymine
    final PdbResidue residueA54 = model.findResidue("A", 54, " ");
    assertThat(residueA54.residueName(), is("5MU"));
    assertThat(residueA54.modifiedResidueName(), is("U"));
    assertThat(residueA54.getDetectedResidueName(), is("U"));
    assertThat(residueA54.isModified(), is(true));
    assertThat(residueA54.hasAllAtoms(), is(false));
  }

  @Test
  public final void testPSUModificationDetection() throws Exception {
    final String pdb1EHZ = ResourcesHelper.loadResource("1EHZ.pdb");
    final PdbParser parser = new PdbParser();
    final List<PdbModel> models = parser.parse(pdb1EHZ);
    final PdbModel model = models.get(0);

    // the PSU (dihydrouridine) is modified by two additional hydrogens
    // which is undetectable in a non-hydrogen PDB file; there can be a
    // mismatch between MODRES entry and hasAllAtoms() call
    final PdbResidue residue = model.findResidue("A", 39, " ");
    assertThat(residue.hasAllAtoms(), is(residue.isModified()));
  }

  @Test
  public final void testModificationDetection() throws Exception {
    final String pdb1EHZ = ResourcesHelper.loadResource("1EHZ.pdb");
    final PdbParser parser = new PdbParser();
    final List<PdbModel> models = parser.parse(pdb1EHZ);
    final PdbModel model = models.get(0);

    for (final PdbResidue residue : model.residues()) {
      if (!residue.wasSuccessfullyDetected()) {
        continue;
      }

      if (residue.hasAtom(AtomName.O3P)) {
        assertThat(residue.isModified(), is(true));
        assertThat(residue.hasAllAtoms(), is(false));
      } else if ("H2U".equals(residue.residueName()) || "PSU".equals(residue.residueName())) {
        assertThat(residue.isModified(), is(true));
        assertThat(residue.hasAllAtoms(), is(true));
      } else if ("5MU".equals(residue.residueName())) {
        assertThat(residue.residueName(), is("5MU"));
        assertThat(residue.modifiedResidueName(), is("U"));
        assertThat(residue.getDetectedResidueName(), is("U"));
        assertThat(residue.isModified(), is(true));
        assertThat(residue.hasAllAtoms(), is(false));
      } else {
        assertThat(
            String.format(
                "Detected %s for %s/%s",
                residue.getDetectedResidueName(),
                residue.residueName(),
                residue.modifiedResidueName()),
            residue.getDetectedResidueName(),
            is(residue.modifiedResidueName()));
        assertThat(!residue.hasAllAtoms(), is(residue.isModified()));
      }
    }
  }

  @Test
  public final void testResidueInTerminatedChain() throws Exception {
    final String pdb2Z74 = ResourcesHelper.loadResource("2Z74.pdb");
    final PdbParser parser = new PdbParser();
    final List<PdbModel> models = parser.parse(pdb2Z74);
    final PdbModel model = models.get(0);
    final List<PdbChain> chains = model.getChains();
    assertThat(
        String.format("Found chains (expected [A, B]): %s", Arrays.toString(chains.toArray())),
        chains.size(),
        is(2));
  }

  @Test
  public final void testMissingResidues() throws Exception {
    final String pdb2Z74 = ResourcesHelper.loadResource("2Z74.pdb");
    final PdbParser parser = new PdbParser();
    final List<PdbModel> models = parser.parse(pdb2Z74);
    final PdbModel model = models.get(0);
    final PdbResidue residueA21 = model.findResidue("A", 21, " ");
    assertThat(residueA21.isMissing(), is(true));
    final PdbResidue residueB21 = model.findResidue("B", 21, " ");
    assertThat(residueB21.isMissing(), is(true));
  }

  @Test
  public final void testOutputParsable() throws Exception {
    final String pdb1EHZ = ResourcesHelper.loadResource("1EHZ.pdb");
    final PdbParser parser = new PdbParser();
    final List<PdbModel> models = parser.parse(pdb1EHZ);
    final PdbModel model = models.get(0);
    final List<PdbModel> parsed = parser.parse(model.toPdbString());

    assertThat(models.size(), is(1));
    assertThat(parsed.size(), is(1));

    assertThat(models.get(0).getChains().size(), is(1));
    assertThat(parsed.get(0).getChains().size(), is(1));

    assertThat(models.get(0).getChains().get(0).residues().size(), is(76));
    assertThat(parsed.get(0).getChains().get(0).residues().size(), is(76));
  }

  @Test
  public final void testOutputParsableByBioJava() throws Exception {
    final String pdb1EHZ = ResourcesHelper.loadResource("1EHZ.pdb");
    final PdbParser parser = new PdbParser();
    final List<PdbModel> models = parser.parse(pdb1EHZ);
    final PdbModel model = models.get(0);

    final PDBFileParser fileParser = new PDBFileParser();
    final Structure structure =
        fileParser.parsePDBFile(
            IOUtils.toInputStream(model.toPdbString(), Charset.defaultCharset()));

    assertThat(structure.getChains().size(), is(1));
    assertThat(model.getChains().size(), is(1));

    assertThat(structure.getChain("A").getAtomGroups().size(), is(76));
    assertThat(model.getChains().get(0).residues().size(), is(76));
  }

  @Test
  public final void testSequence() throws Exception {
    final String pdb1EHZ = ResourcesHelper.loadResource("1EHZ.pdb");
    final PdbParser parser = new PdbParser();
    final List<PdbModel> models = parser.parse(pdb1EHZ);
    final PdbModel model = models.get(0);
    assertThat(
        model.getSequence().toUpperCase(Locale.US),
        is("GCGGAUUUAGCUCAGUUGGGAGAGCGCCAGACUGAAGAUCUGGAGGUCCUGUGUUCGAUCCACAGAAUUCGCACCA"));

    final String pdb2Z74 = ResourcesHelper.loadResource("2Z74.pdb");
    final List<PdbModel> models2Z74 = parser.parse(pdb2Z74);
    final PdbModel model2Z74 = models2Z74.get(0);
    final List<PdbChain> chains2Z74 = model2Z74.getChains();
    assertThat(
        chains2Z74.get(0).getSequence().toUpperCase(Locale.US), is("AGCGCCUGGACUUAAAGCCAUUGCACU"));
    assertThat(
        chains2Z74.get(1).getSequence().toUpperCase(Locale.US),
        is(
            "CCGGCUUUAAGUUGACGAGGGCAGGGUUUAUCGAGACAUCGGCGGGUGCCCUGCGGUCUUCCUGCGACCGUUAGAGGACUGGUAAAACCACAGGCGACUGUGGCAUAGAGCAGUCCGGGCAGGAA"));
    assertThat(
        model2Z74.getSequence().toUpperCase(Locale.US),
        is(
            "AGCGCCUGGACUUAAAGCCAUUGCACUCCGGCUUUAAGUUGACGAGGGCAGGGUUUAUCGAGACAUCGGCGGGUGCCCUGCGGUCUUCCUGCGACCGUUAGAGGACUGGUAAAACCACAGGCGACUGUGGCAUAGAGCAGUCCGGGCAGGAA"));

    final String pdb4A04 = ResourcesHelper.loadResource("4A04.pdb");
    final List<PdbModel> models4A04 = parser.parse(pdb4A04);
    final PdbModel model4A04 = models4A04.get(0);
    final List<PdbChain> chains4A04 = model4A04.getChains();
    assertThat(
        chains4A04.get(0).getSequence().toUpperCase(Locale.US),
        is(
            "MHHHHHHENLYFQGGVSVQLEMKALWDEFNQLGTEMIVTKAGRRMFPTFQVKLFGMDPMADYMLLMDFVPVDDKRYRYAFHSSSWLVAGKADPATPGRVHYHPDSPAKGAQWMKQIVSFDKLKLTNNLLDDNGHIILNSMHRYQPRFHVVYVDPRKDSEKYAEENFKTFVFEETRFTAVTAYQNHRITQLKIASNPFAKGFRD"));
    assertThat(
        chains4A04.get(1).getSequence().toUpperCase(Locale.US),
        is(
            "MHHHHHHENLYFQGGVSVQLEMKALWDEFNQLGTEMIVTKAGRRMFPTFQVKLFGMDPMADYMLLMDFVPVDDKRYRYAFHSSSWLVAGKADPATPGRVHYHPDSPAKGAQWMKQIVSFDKLKLTNNLLDDNGHIILNSMHRYQPRFHVVYVDPRKDSEKYAEENFKTFVFEETRFTAVTAYQNHRITQLKIASNPFAKGFRD"));
    assertThat(
        chains4A04.get(2).getSequence().toUpperCase(Locale.US), is("AATTTCACACCTAGGTGTGAAATT"));
  }

  @Test
  public final void test79CharLongAtomLines() throws Exception {
    final String pdbPKB300 = ResourcesHelper.loadResource("PKB300.pdb");
    final PdbParser parser = new PdbParser(false);
    final List<PdbModel> models = parser.parse(pdbPKB300);
    assertThat(models.size(), is(1));
    final PdbModel model = models.get(0);
    final List<PdbChain> chains = model.getChains();
    final List<PdbResidue> residues = model.residues();
    final List<PdbAtomLine> atoms = model.getAtoms();

    assertThat(chains.size(), is(1));
    assertThat(residues.size(), is(37));
    assertThat(atoms.size(), is(1186));
  }

  @Test
  public final void testAmberModel() throws Exception {
    final String pdbAmber = ResourcesHelper.loadResource("amber.pdb");
    final PdbParser parser = new PdbParser(false);
    final List<PdbModel> models = parser.parse(pdbAmber);
    assertThat(models.size(), is(1));
    final PdbModel model = models.get(0);
    final List<PdbChain> chains = model.getChains();
    final List<PdbResidue> residues = model.residues();
    final List<PdbAtomLine> atoms = model.getAtoms();

    assertThat(chains.size(), is(3));
    assertThat(residues.size(), is(49));
    assertThat(atoms.size(), is(1557));
  }

  @Test
  public final void testNMRModelsWithHydrogenAtoms() throws Exception {
    final String pdb2MIY = ResourcesHelper.loadResource("2MIY.pdb");
    final PdbParser parser = new PdbParser(false);
    final List<PdbModel> models = parser.parse(pdb2MIY);
    assertThat(models.size(), is(18));

    final PdbModel model = models.get(0);
    final List<PdbChain> chains = model.getChains();
    final List<PdbResidue> residues = model.residues();
    final List<PdbAtomLine> atoms = model.getAtoms();
    assertThat(chains.size(), is(1));
    assertThat(residues.size(), is(59));
    assertThat(atoms.size(), is(1909));

    final PdbChain chain = chains.get(0);
    final String sequence = chain.getSequence();
    assertThat(Character.isLowerCase(sequence.charAt(0)), is(true));
    assertThat(StringUtils.isAllUpperCase(sequence.substring(1)), is(true));
  }

  @Test
  public final void testCanonicalSecondaryStructure() throws Exception {
    final String pdb1EHZ = ResourcesHelper.loadResource("1EHZ.pdb");
    final String pdb2Z74 = ResourcesHelper.loadResource("2Z74.pdb");
    final String pdb2MIY = ResourcesHelper.loadResource("2MIY.pdb");
    final String bpseq1EHZ = ResourcesHelper.loadResource("1EHZ-2D-bpseq.txt");
    final String bpseq2Z74 = ResourcesHelper.loadResource("2Z74-2D-bpseq.txt");
    final String bpseq2MIY = ResourcesHelper.loadResource("2MIY-2D-bpseq.txt");
    PdbModelTest.assertBpSeqEquals(pdb1EHZ, bpseq1EHZ);
    PdbModelTest.assertBpSeqEquals(pdb2Z74, bpseq2Z74);
    PdbModelTest.assertBpSeqEquals(pdb2MIY, bpseq2MIY);
  }

  private static void assertBpSeqEquals(final String pdbString, final String bpSeqString) {
    final PdbParser parser = new PdbParser(false);
    final List<PdbModel> models = parser.parse(pdbString);
    final PdbModel model = models.get(0);

    final BpSeq bpSeqFromModel = CanonicalStructureExtractor.bpSeq(model);
    assertThat(bpSeqFromModel.toString(), is(bpSeqString));

    final BpSeq bpSeqFromString = BpSeq.fromString(bpSeqString);
    assertThat(bpSeqFromModel, is(bpSeqFromString));
  }

  @Test
  public final void testFrabaseExport() throws Exception {
    final String pdbFrabaseExport = ResourcesHelper.loadResource("FrabaseExport.pdb");
    final PdbParser parser = new PdbParser(false);
    final List<PdbModel> models = parser.parse(pdbFrabaseExport);
    assertThat(models.size(), is(2));

    final PdbModel model0 = models.get(0);
    final List<PdbChain> chains0 = model0.getChains();
    final List<PdbResidue> residues0 = model0.residues();
    final List<PdbAtomLine> atoms0 = model0.getAtoms();
    assertThat(chains0.size(), is(1));
    assertThat(residues0.size(), is(7));
    assertThat(atoms0.size(), is(150));

    final PdbModel model1 = models.get(1);
    final List<PdbChain> chains1 = model1.getChains();
    final List<PdbResidue> residues1 = model1.residues();
    final List<PdbAtomLine> atoms1 = model1.getAtoms();
    assertThat(chains1.size(), is(1));
    assertThat(residues1.size(), is(7));
    assertThat(atoms1.size(), is(150));
  }

  @Test
  public final void testModelWithResiduesUNK() throws Exception {
    final String pdb3KFU = ResourcesHelper.loadResource("3KFU.pdb");
    final PdbParser parser = new PdbParser(false);
    final List<PdbModel> models = parser.parse(pdb3KFU);
    assertThat(models.size(), is(1));

    final PdbModel model = models.get(0);
    assertThat(model.getChains().size(), is(14));

    final PdbModel rna = model.filteredNewInstance(MoleculeType.RNA);
    assertThat(rna.getChains().size(), is(4));
  }

  @Test
  public final void test148L() throws Exception {
    final String pdb148L = ResourcesHelper.loadResource("148L.pdb");
    final PdbParser parser = new PdbParser();
    final List<PdbModel> models = parser.parse(pdb148L);
    assertThat(models.size(), is(1));
    final PdbModel model = models.get(0);

    final PdbResidue residueE164 = model.findResidue("E", 164, " ");
    assertThat(residueE164.isMissing(), is(true));

    final PdbResidue residueS169 = model.findResidue("S", 169, " ");
    assertThat(residueS169.residueName(), is("API"));
    assertThat(residueS169.modifiedResidueName(), is("LYS"));
  }

  @Test
  public final void testCif() throws Exception {
    final String pdb1EHZ = ResourcesHelper.loadResource("1EHZ.pdb");
    final PdbParser pdbParser = new PdbParser();
    final List<PdbModel> pdbModels = pdbParser.parse(pdb1EHZ);
    assertThat(pdbModels.size(), is(1));
    final PdbModel pdbModel = pdbModels.get(0);

    final String cif1EHZ = pdbModel.toCif();

    final CifParser cifParser = new CifParser();
    final List<CifModel> cifModels = cifParser.parse(cif1EHZ);
    assertThat(cifModels.size(), is(1));
    final PdbModel cifModel = cifModels.get(0);

    final List<PdbResidue> pdbResidues = pdbModel.residues();
    final List<PdbResidue> cifResidues = cifModel.residues();
    assertThat(pdbResidues.size(), is(cifResidues.size()));

    for (int i = 0; i < pdbResidues.size(); i++) {
      final PdbResidue pdbResidue = pdbResidues.get(i);
      final PdbResidue cifResidue = cifResidues.get(i);
      if (!pdbResidue.isModified()) {
        assertThat(cifResidue, is(pdbResidue));
      }
    }
  }
}
