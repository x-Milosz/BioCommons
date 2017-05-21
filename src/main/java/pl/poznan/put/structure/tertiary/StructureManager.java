package pl.poznan.put.structure.tertiary;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import pl.poznan.put.pdb.PdbParsingException;
import pl.poznan.put.pdb.analysis.CifParser;
import pl.poznan.put.pdb.analysis.PdbModel;
import pl.poznan.put.pdb.analysis.PdbParser;
import pl.poznan.put.pdb.analysis.StructureParser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * A common manager of loaded PDB files shared between all classes.
 *
 * @author tzok
 */
public final class StructureManager {
    private static final String ENCODING_UTF_8 = "UTF-8";
    private static final List<StructureInfo> STRUCTURES = new ArrayList<>();
    private static final PdbParser PDB_PARSER = new PdbParser(false);
    private static final StructureParser CIF_PARSER = new CifParser();

    private StructureManager() {
        super();
    }

    public static List<PdbModel> getAllStructures() {
        final List<PdbModel> result = new ArrayList<>();
        for (final StructureInfo si : StructureManager.STRUCTURES) {
            result.add(si.getStructure());
        }
        return result;
    }

    public static List<String> getAllNames() {
        final List<String> result = new ArrayList<>();
        for (final StructureInfo si : StructureManager.STRUCTURES) {
            result.add(si.getName());
        }
        return result;
    }

    public static File getFile(final PdbModel structure) {
        for (final StructureInfo si : StructureManager.STRUCTURES) {
            if (Objects.equals(si.getStructure(), structure)) {
                return si.getPath();
            }
        }
        throw new IllegalArgumentException("Failed to find PdbModel");
    }

    public static PdbModel getStructure(final String name) {
        for (final StructureInfo si : StructureManager.STRUCTURES) {
            if (Objects.equals(si.getName(), name)) {
                return si.getStructure();
            }
        }
        throw new IllegalArgumentException("Failed to find PdbModel");
    }

    public static List<String> getNames(final Iterable<PdbModel> structures) {
        final List<String> result = new ArrayList<>();
        for (final PdbModel s : structures) {
            result.add(StructureManager.getName(s));
        }
        return result;
    }

    public static String getName(final PdbModel structure) {
        for (final StructureInfo si : StructureManager.STRUCTURES) {
            if (Objects.equals(si.getStructure(), structure)) {
                return si.getName();
            }
        }
        throw new IllegalArgumentException("Failed to find PdbModel");
    }

    /**
     * Load a structure and remember it being already cached.
     *
     * @param file Path to the PDB file.
     * @return Structure object..
     */
    public static List<? extends PdbModel> loadStructure(final File file)
            throws IOException, PdbParsingException {
        final List<PdbModel> models = StructureManager.getModels(file);
        if (!models.isEmpty()) {
            return models;
        }

        final StructureParser parser;
        final String fileContent = StructureManager.readFileUnzipIfNeeded(file);
        final String name = file.getName();

        if (name.endsWith(".cif") || name.endsWith(".cif.gz")) {
            if (!StructureManager.isCif(fileContent)) {
                throw new IOException("File is not a mmCIF structure: " + file);
            }
            parser = StructureManager.CIF_PARSER;
        } else {
            if (!StructureManager.isPdb(fileContent)) {
                throw new IOException("File is not a PDB structure: " + file);
            }
            parser = StructureManager.PDB_PARSER;
        }

        final List<? extends PdbModel> structures = parser.parse(fileContent);
        StructureManager.storeStructureInfo(file, structures);
        return structures;
    }

    public static List<PdbModel> getModels(final File file) {
        final List<PdbModel> result = new ArrayList<>();
        for (final StructureInfo si : StructureManager.STRUCTURES) {
            if (Objects.equals(si.getPath(), file)) {
                result.add(si.getStructure());
            }
        }
        return result;
    }

    private static String readFileUnzipIfNeeded(final File file)
            throws IOException {
        ByteArrayOutputStream copyStream = null;
        FileInputStream inputStream = null;

        try {
            inputStream = new FileInputStream(file);
            copyStream = new ByteArrayOutputStream();
            IOUtils.copy(inputStream, copyStream);
            final byte[] byteArray = copyStream.toByteArray();

            if (StructureManager.isGzipStream(byteArray)) {
                return StructureManager.unzipContent(byteArray);
            }

            return copyStream.toString(StructureManager.ENCODING_UTF_8);
        } finally {
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(copyStream);
        }
    }

    private static boolean isCif(final String fileContent) {
        return fileContent.startsWith("data_");
    }

    private static boolean isPdb(final CharSequence fileContent) {
        final Pattern pdbPattern = Pattern.compile("^ATOM", Pattern.MULTILINE);
        final Matcher matcher = pdbPattern.matcher(fileContent);
        return matcher.find();
    }

    private static void storeStructureInfo(final File file,
                                           final List<? extends PdbModel>
                                                   structures) {
        String format = "%s";

        if (structures.size() > 1) {
            final int count = structures.size();
            int order = 10;
            int leading = 1;
            while (order < count) {
                leading++;
                order *= 10;
            }
            format = "%s.%0" + leading + 'd';
        }

        for (int i = 0; i < structures.size(); i++) {
            final PdbModel model = structures.get(i);
            String name = model.getIdCode();

            if (StringUtils.isBlank(name)) {
                name = file.getName();
                if (name.endsWith(".pdb") || name.endsWith(".cif")) {
                    name = name.substring(0, name.length() - 4);
                } else {
                    if (name.endsWith(".pdb.gz") || name.endsWith(".cif.gz")) {
                        name = name.substring(0, name.length() - 7);
                    }
                }
            }

            StructureManager.STRUCTURES.add(new StructureInfo(model, file,
                                                              String.format(
                                                                      format,
                                                                      name,
                                                                      i + 1)));
        }
    }

    private static boolean isGzipStream(final byte[] bytes) {
        if (bytes.length < 2) {
            return false;
        }

        final int head = (bytes[0] & 0xff) | ((bytes[1] << 8) & 0xff00);
        return head == GZIPInputStream.GZIP_MAGIC;
    }

    private static String unzipContent(final byte[] byteArray)
            throws IOException {
        ByteArrayInputStream inputStream = null;
        GZIPInputStream gzipInputStream = null;

        try {
            inputStream = new ByteArrayInputStream(byteArray);
            gzipInputStream = new GZIPInputStream(inputStream);
            return IOUtils.toString(gzipInputStream, Charset.defaultCharset());
        } finally {
            IOUtils.closeQuietly(gzipInputStream);
            IOUtils.closeQuietly(inputStream);
        }
    }

    public static List<PdbModel> loadStructure(final String pdbId)
            throws IOException, PdbParsingException, MalformedURLException {
        InputStream stream = null;

        try {
            final URL url =
                    new URL("http://www.rcsb.org/pdb/download/downloadFile" +
                            ".do?fileFormat=pdb&compression=NO&structureId=" +
                            pdbId);
            stream = url.openStream();
            final String pdbContent =
                    IOUtils.toString(stream, StructureManager.ENCODING_UTF_8);

            final File pdbFile = File.createTempFile("mcq", ".pdb");
            FileUtils.writeStringToFile(pdbFile, pdbContent,
                                        StructureManager.ENCODING_UTF_8);

            final List<PdbModel> models =
                    StructureManager.PDB_PARSER.parse(pdbContent);
            StructureManager.storeStructureInfo(pdbFile, models);
            return models;
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }

    public static void remove(final File path) {
        final Collection<Integer> toRemove = new ArrayList<>();

        for (int i = 0; i < StructureManager.STRUCTURES.size(); i++) {
            final StructureInfo si = StructureManager.STRUCTURES.get(i);
            if (Objects.equals(si.getPath(), path)) {
                toRemove.add(i);
            }
        }

        for (final int i : toRemove) {
            StructureManager.STRUCTURES.remove(i);
        }
    }
}
