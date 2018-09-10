package ee.ut.bench.config;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public final class AtomixConfig extends MemberConfig {

    public static void reset() {
        resetSystemDataDir();
        resetPrimitiveDataDir();
    }

    public static void resetSystemDataDir() {
        try {
            String dir = (new File("user.dir")).toURI().relativize(new File(getSystemDataDir()).toURI()).getPath();
            FileUtils.deleteDirectory(new File(dir));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void resetPrimitiveDataDir() {
        try {
            String dir = (new File("user.dir")).toURI().relativize(new File(getPrimitiveDataDir()).toURI()).getPath();
            FileUtils.deleteDirectory(new File(dir));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getSystemDataDir() {
        return TestPropertiesLoader.getString("atomix.systemdata.dir");
    }

    public static String getPrimitiveDataDir() {
        return TestPropertiesLoader.getString("atomix.primitivedata.dir");
    }

    public static File getSystemDataDirFor(int memberIndex) {
        return new File(String.format("%s/mem%d", getSystemDataDir(), memberIndex));
    }

    public static File getPrimitiveDataDirFor(int memberIndex) {
        return new File(String.format("%s/mem%d", getPrimitiveDataDir(), memberIndex));
    }

}
