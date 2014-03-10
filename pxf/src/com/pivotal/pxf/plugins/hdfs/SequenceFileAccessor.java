package com.pivotal.pxf.plugins.hdfs;

import com.pivotal.pxf.api.OneRow;
import com.pivotal.pxf.api.WriteAccessor;
import com.pivotal.pxf.api.utilities.InputData;
import com.pivotal.pxf.plugins.hdfs.utilities.HdfsUtilities;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CreateFlag;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.SequenceFile.CompressionType;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.mapred.*;

import java.io.IOException;
import java.util.EnumSet;

/**
 * A PXF Accessor for reading and writing Sequence File records
 */
public class SequenceFileAccessor extends HdfsSplittableDataAccessor implements WriteAccessor {

    private Configuration conf;
    private FileContext fc;
    private Path file;
    private CompressionCodec codec;
    private CompressionType compressionType;
    private SequenceFile.Writer writer;
    private LongWritable defaultKey; // used when recordkey is not defined

    private Log Log;

    /**
     * Constructs a SequenceFileAccessor
     * 
     * @param input all input parameters coming from the client request
     * @throws Exception
     */
    public SequenceFileAccessor(InputData input) throws Exception {

        super(input,
                new SequenceFileInputFormat<Writable, Writable>());

        Log = LogFactory.getLog(SequenceFileAccessor.class);
    }

    /*
     * Override virtual method to create specialized record reader
     */
    protected Object getReader(JobConf jobConf, InputSplit split) throws IOException {
        return new SequenceFileRecordReader(jobConf, (FileSplit) split);
    }

    @Override
    public boolean openForWrite() throws Exception {
        FileSystem fs;
        Path parent;
        String fileName = inputData.getProperty("X-GP-DATA-PATH");
        conf = new Configuration();

        getCompressionCodec(inputData);
        fileName = updateFileExtension(fileName, codec);

        // construct the output stream
        file = new Path(fileName);
        fs = file.getFileSystem(conf);
        fc = FileContext.getFileContext();
        defaultKey = new LongWritable(inputData.segmentId());

        if (fs.exists(file)) {
            throw new IOException("file " + file + " already exists, can't write data");
        }
        parent = file.getParent();
        if (!fs.exists(parent)) {
            fs.mkdirs(parent);
            Log.debug("Created new dir " + parent);
        }

        writer = null;
        return true;
    }

    /**
     * Compression: based on compression codec and compression type (default value RECORD).
     * If there is no codec, compression type is ignored, and NONE is used.
     *
     * @param inputData - container where compression codec and type are held.
     */
    private void getCompressionCodec(InputData inputData) {

        String compressCodec = inputData.compressCodec();
        String compressType = inputData.compressType();

        compressionType = SequenceFile.CompressionType.NONE;
        codec = null;
        if (compressCodec != null) {
            codec = HdfsUtilities.getCodec(conf, compressCodec);

            try {
                compressionType = CompressionType.valueOf(compressType);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Illegal value for compression type " +
                        "'" + compressType + "'");
            }
            if (compressionType == null) {
                throw new IllegalArgumentException("Compression type must be defined");
            }

            Log.debug("Compression ON: " +
                    "compression codec: " + compressCodec +
                    ", compression type: " + compressionType);
        }
    }

    /*
     * Returns fileName with the codec's file extension appended
     */
    private String updateFileExtension(String fileName, CompressionCodec codec) {

        if (codec != null) {
            fileName += codec.getDefaultExtension();
        }
        Log.debug("File name for write: " + fileName);
        return fileName;
    }

    @Override
    public boolean writeNextObject(OneRow onerow) throws IOException {
        Writable value = (Writable) onerow.getData();
        Writable key = (Writable) onerow.getKey();

        // init writer on first approach here, based on onerow.getData type
        // TODO: verify data is serializable.
        if (writer == null) {
            Class valueClass = value.getClass();
            Class keyClass = (key == null) ? LongWritable.class : key.getClass();
            // create writer - do not allow overwriting existing file
            writer = SequenceFile.createWriter(fc, conf, file, keyClass, valueClass,
                    compressionType, codec, new SequenceFile.Metadata(), EnumSet.of(CreateFlag.CREATE));
        }

        try {
            writer.append((key == null) ? defaultKey : key, value);
        } catch (IOException e) {
            Log.error("Failed to write data to file: " + e.getMessage());
            return false;
        }

        return true;
    }

    @Override
    public void closeForWrite() throws Exception {
        if (writer != null) {
            writer.sync();
            /*
             * From release 0.21.0 sync() is deprecated in favor of hflush(),
			 * which only guarantees that new readers will see all data written to that point, 
			 * and hsync(), which makes a stronger guarantee that the operating system has flushed 
			 * the data to disk (like POSIX fsync), although data may still be in the disk cache.
			 */
            writer.hsync();
            writer.close();
        }
    }
}
