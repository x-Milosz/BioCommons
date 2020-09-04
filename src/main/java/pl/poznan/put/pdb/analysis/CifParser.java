package pl.poznan.put.pdb.analysis;

import org.apache.commons.io.IOUtils;
import org.biojava.nbio.structure.io.mmcif.MMcifParser;
import org.biojava.nbio.structure.io.mmcif.SimpleMMcifParser;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;

/** A parser of mmCIF format. */
public class CifParser {
  private final MMcifParser parser = new SimpleMMcifParser();
  private final CifConsumer consumer = new CifConsumer();

  /** Construct an instance of mmCIF parser. */
  public CifParser() {
    super();
    parser.addMMcifConsumer(consumer);
  }

  /**
   * Parse content in mmCIF format.
   *
   * @param structureContent A string with data in mmCIF format.
   * @return A parsed object representing a molecular structure.
   * @throws IOException When parsing of the data fails.
   */
  public final List<CifModel> parse(final String structureContent) throws IOException {
    synchronized (parser) {
      try (final Reader reader = new StringReader(structureContent)) {
        parser.parse(IOUtils.toBufferedReader(reader));
      }
      return consumer.getModels();
    }
  }
}
