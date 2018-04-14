package testframework.result;

import testframework.TestPhase;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A single result.
 */
public interface IResult {
    /**
     * @return The recorded phase of the test result
     */
    TestPhase getTestPhase();
    /**
     * @return The test's group name that is recorded.
     */
    String getTestGroupName();

    /**
     * @return get the result as a CSV result preferably ended with <tt>System.getProperty("line.separator")</tt>
     */
    String CSVLine(boolean writeHeader);

    /**
     * @param timeStamp timestamp to use
     * @return a readable format of the timestamp provided as argument.
     */
    default String timeStampReadable(long timeStamp){
        return new SimpleDateFormat("HH:mm:ss.SSS").format(new Date(timeStamp));
    }
}
