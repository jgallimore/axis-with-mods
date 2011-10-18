/*
 * Copyright 2001-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.axis.attachments;


import org.apache.axis.components.logger.LogFactory;
import org.apache.axis.utils.Messages;
import org.apache.commons.logging.Log;

import java.io.IOException;


/**
 * This class takes the input stream and turns it multiple streams.
 DIME version 0 format
 <pre>
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+  ---
 | VERSION |B|E|C| TYPE_T| OPT_T |         OPTIONS_LENGTH        |   A
 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 |          ID_LENGTH          |             TYPE_LENGTH         |   Always present 12 bytes
 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+   even on chunked data.
 |                          DATA_LENGTH                          |   V
 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+  ---
 |                                                               /
 /                       OPTIONS + PADDING                       /
 /                     (absent for version 0)                    |
 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 |                                                               /
 /                        ID + PADDING                           /
 /                                                               |
 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 |                                                               /
 /                        TYPE + PADDING                         /
 /                                                               |
 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 |                                                               /
 /                        DATA + PADDING                         /
 /                                                               |
 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
</pre>
 * This implementation of input stream does not support marking operations.
 *
 * @author Rick Rineholt
 */
public class DimeDelimitedInputStream extends java.io.FilterInputStream {
    protected static Log log =
        LogFactory.getLog(DimeDelimitedInputStream.class.getName());

    java.io.InputStream is = null; //The source input stream.
    volatile boolean closed = true; //The stream has been closed.
    boolean theEnd = false; //There are no more streams left.
    boolean moreChunks = false; //More chunks are a coming!
    boolean MB = false;  //First part of the stream. MUST be SOAP.
    boolean ME = false;  //Last part of stream.
    DimeTypeNameFormat tnf = null;
    String type = null;
    String id = null;
    long    recordLength = 0L; //length of the record.
    long    bytesRead = 0; //How many bytes of the record have been read.
    int dataPadLength = 0; //How many pad bytes there are.
    private static byte[] trash = new byte[4];
    protected int streamNo = 0;
    protected IOException streamInError = null;

    protected static int streamCount = 0; //number of streams produced.

    protected static synchronized int newStreamNo() {
        log.debug(Messages.getMessage("streamNo", "" + (streamCount + 1)));
        return ++streamCount;
    }

    static boolean isDebugEnabled = false;

    /**
     * Gets the next stream. From the previous using  new buffer reading size.
     *
     * @return the dime delmited stream, null if there are no more streams
     * @throws IOException if there was an error loading the data for the next
     *              stream
     */
    synchronized DimeDelimitedInputStream getNextStream()
            throws IOException
    {
        if (null != streamInError) throw streamInError;
        if (theEnd) return null;
        if (bytesRead < recordLength || moreChunks) //Stream must be read in succession
            throw new RuntimeException(Messages.getMessage(
             "attach.dimeReadFullyError"));
        dataPadLength -= readPad(dataPadLength);

        //Create an new dime stream  that comes after this one.
        return  new DimeDelimitedInputStream(this.is);
    }

    /**
     * Create a new dime stream.
     *
     * @param is  the <code>InputStream</code> to wrap
     * @throws IOException if anything goes wrong
     */
    DimeDelimitedInputStream(java.io.InputStream is) throws IOException {
        super(null); //we handle everything so this is not necessary, don't won't to hang on to a reference.
        isDebugEnabled = log.isDebugEnabled();
        streamNo = newStreamNo();
        closed = false;
        this.is = is;
        readHeader(false);
    }

    private final int readPad(final int  size) throws IOException {
        if (0 == size) return 0;
        int read = readFromStream(trash, 0, size);

        if (size != read) {
            streamInError = new IOException(Messages.getMessage(
            "attach.dimeNotPaddedCorrectly"));
            throw streamInError;
        }
        return read;
    }

    private final int readFromStream(final byte[] b) throws IOException {
        return readFromStream(b, 0, b.length);
    }

    private final int readFromStream(final byte[] b,
        final int start, final int length)
        throws IOException {
        if (length == 0) return 0;

        int br = 0;
        int brTotal = 0;

        do {
            try {
                br = is.read(b, brTotal + start, length - brTotal);
            } catch (IOException e) {
                streamInError = e;
                throw e;
            }
            if (br > 0) brTotal += br;
        }
        while (br > -1 && brTotal < length);

        return br > -1 ? brTotal : br;
    }

    /**
     * Get the id for this stream part.
     * @return the id;
     */
    public String getContentId() {
        return id;
    }

    public DimeTypeNameFormat getDimeTypeNameFormat() {
        return tnf;
    }

    /**
     * Get the type, as read from the header.
     *
     * @return the type of this dime
     */

    public String getType() {
        return type;
    }

    /**
     * Read from the DIME stream.
     *
     * @param b is the array to read into.
     * @param off is the offset
     * @return the number of bytes read. -1 if endof stream
     * @throws IOException if data could not be read from the stream
     */
    public synchronized int read(byte[] b, final int off,
        final int len) throws IOException {

        if (closed) {
            dataPadLength -= readPad(dataPadLength);
            throw new IOException(Messages.getMessage("streamClosed"));
        }
        return _read(b, off, len);
    }

    protected int _read(byte[] b, final int off, final int len)
        throws IOException {
        if (len < 0) throw new IllegalArgumentException
                (Messages.getMessage("attach.readLengthError",
                 "" + len));

        if (off < 0) throw new IllegalArgumentException
                (Messages.getMessage("attach.readOffsetError",
                 "" + off));
        if (b == null) throw new IllegalArgumentException
                (Messages.getMessage("attach.readArrayNullError"));
        if (b.length < off + len) throw new IllegalArgumentException
                (Messages.getMessage("attach.readArraySizeError",
                        "" + b.length, "" + len, "" + off));

        if (null != streamInError) throw streamInError;

        if (0 == len) return 0; //quick.

        if(recordLength == 0 && bytesRead == 0 &&  !moreChunks){
          ++bytesRead; //odd case no data to read -- give back 0 next time -1;
          if(ME){
              finalClose();
          }
          return 0;
        }
        if (bytesRead >= recordLength && !moreChunks) {
            dataPadLength -= readPad(dataPadLength);
            if(ME){
              finalClose();
            }
            return -1;
        }

        int totalbytesread = 0;
        int bytes2read = 0;

        do {
            if (bytesRead >= recordLength && moreChunks)
              readHeader(true);

            bytes2read = (int) Math.min(recordLength - bytesRead,
                        (long) len - totalbytesread);
            bytes2read = (int) Math.min(recordLength - bytesRead,
                        (long) len - totalbytesread);
            try {
                bytes2read = is.read(b, off + totalbytesread,
                 bytes2read);
            } catch (IOException e) {
                streamInError = e;
                throw e;
            }

            if (0 < bytes2read) {
                totalbytesread += bytes2read;
                bytesRead += bytes2read;
            }

        }
        while (bytes2read > -1 && totalbytesread < len &&
            (bytesRead < recordLength || moreChunks));

        if (0 > bytes2read) {
            if (moreChunks) {
                streamInError = new IOException(Messages.getMessage(
                                "attach.DimeStreamError0"));
                throw streamInError;
            }
            if (bytesRead < recordLength) {
                streamInError = new IOException(Messages.getMessage
                            ("attach.DimeStreamError1",
                              "" + (recordLength - bytesRead)));
                throw streamInError;
            }
            if (!ME) {
                streamInError = new IOException(Messages.getMessage(
                                "attach.DimeStreamError0"));
                throw streamInError;
            }
            //in theory the last chunk of data should also have been padded, but lets be tolerant of that.
            dataPadLength = 0;

        } else if (bytesRead >= recordLength) {
            //get rid of pading.
            try {
                dataPadLength -= readPad(dataPadLength);
            } catch (IOException e) {
                //in theory the last chunk of data should also have been padded, but lets be tolerant of that.
                if (!ME) throw e;
                else {
                    dataPadLength = 0;
                    streamInError = null;
                }
            }
        }

        if (bytesRead >= recordLength && ME) {
              finalClose();
        }

        return totalbytesread >= 0 ? totalbytesread : -1;
    }

    void readHeader(boolean isChunk) throws IOException {

        bytesRead = 0; //How many bytes of the record have been read.
        if (isChunk) {
            if (!moreChunks) throw new RuntimeException(
                        Messages.getMessage("attach.DimeStreamError2"));
            dataPadLength -= readPad(dataPadLength); //Just incase it was left over.
        }

        byte[] header = new byte[12];

        if (header.length != readFromStream(header)) {
            streamInError = new IOException(Messages.getMessage(
                            "attach.DimeStreamError3",
                              "" + header.length));
            throw streamInError;
        }

        //VERSION
        byte version = (byte) ((header[0] >>> 3) & 0x1f);

        if (version > DimeMultiPart.CURRENT_VERSION) {
            streamInError = new IOException(Messages.getMessage("attach.DimeStreamError4",
                            "" + version,
                             "" + DimeMultiPart.CURRENT_VERSION));
            throw streamInError;
        }

        //B, E, C
        MB = 0 != (0x4 & header[0]);
        ME = 0 != (0x2 & header[0]);
        moreChunks = 0 != (0x1 & header[0]);

        //TYPE_T
        if (!isChunk)
            tnf = DimeTypeNameFormat.parseByte((byte) ((header[1] >>> 4) & (byte) 0xf));

        //OPTIONS_LENGTH
        int optionsLength =
            ((((int) header[2]) << 8) & 0xff00) | ((int) header[3]);

        //ID_LENGTH
        int idLength =
            ((((int) header[4]) << 8) & 0xff00) | ((int) header[5]);

        //TYPE_LENGTH
        int typeLength = ((((int) header[6]) << 8) & 0xff00)
          | ((int) header[7]);

        //DATA_LENGTH
        recordLength = ((((long) header[8]) << 24) & 0xff000000L) |
                ((((long) header[9]) << 16) & 0xff0000L) |
                ((((long) header[10]) << 8) & 0xff00L) |
                ((long) header[11] & 0xffL);

        //OPTIONS + PADDING

        if (0 != optionsLength) {
            byte[] optBytes = new byte[optionsLength];

            if (optionsLength != readFromStream(optBytes)) {
                streamInError = new IOException(Messages.getMessage(
                                "attach.DimeStreamError5",
                                 "" + optionsLength));
                throw streamInError;
            }
            optBytes = null; //Yup throw it away, don't know anything about options.

            int pad = DimeBodyPart.dimePadding(optionsLength);

            if (pad != readFromStream(header, 0, pad)) {
                streamInError = new IOException(
                 Messages.getMessage("attach.DimeStreamError7"));
                throw streamInError;
            }
        }

        // ID + PADDING
        if (0 < idLength) {
            byte[] idBytes = new byte[ idLength];

            if (idLength != readFromStream(idBytes)) {
                streamInError = new IOException(
                Messages.getMessage("attach.DimeStreamError8"));
                throw streamInError;
            }
            if (idLength != 0 && !isChunk) {
                id = new String(idBytes);
            }
            int pad = DimeBodyPart.dimePadding(idLength);

            if (pad != readFromStream(header, 0, pad)) {
                streamInError = new IOException(Messages.getMessage(
                "attach.DimeStreamError9"));
                throw streamInError;
            }
        }

        //TYPE + PADDING
        if (0 < typeLength) {
            byte[] typeBytes = new byte[typeLength];

            if (typeLength != readFromStream(typeBytes)) {
                streamInError = new IOException(Messages.getMessage(
                "attach.DimeStreamError10"));
                throw streamInError;
            }
            if (typeLength != 0 && !isChunk) {
                type = new String(typeBytes);
            }
            int pad = DimeBodyPart.dimePadding(typeLength);

            if (pad != readFromStream(header, 0, pad)) {
                streamInError = new IOException(Messages.getMessage(
                "attach.DimeStreamError11"));

                throw streamInError;
            }
        }
        log.debug("MB:" + MB + ", ME:" + ME + ", CF:" + moreChunks +
            "Option length:" + optionsLength +
             ", ID length:" + idLength +
            ", typeLength:" + typeLength + ", TYPE_T:" + tnf);
        log.debug("id:\"" + id + "\"");
        log.debug("type:\"" + type + "\"");
        log.debug("recordlength:\"" + recordLength + "\"");

        dataPadLength = DimeBodyPart.dimePadding(recordLength);
    }

    /**
     * Read from the delimited stream.
     * @param b is the array to read into. Read as much as possible
     *   into the size of this array.
     * @return the number of bytes read. -1 if endof stream
     * @throws IOException if data could not be read from the stream
     */
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    // fixme: this seems a bit inefficient
    /**
     * Read from the boundary delimited stream.
     *
     * @return the byte read, or -1 if endof stream
     * @throws IOException if there was an error reading the data
     */
    public int read() throws IOException {
        byte[] b = new byte[1];
        int read = read(b, 0, 1);

        if (read < 0)
            return -1; // fixme: should we also check for read != 1?
        return (b[0] & 0xff); // convert byte value to a positive int
    }

    /**
     * Closes the stream.
     * <p>
     * This will take care of flushing any remaining data to the strea.
     * <p>
     * Multiple calls to this method will result in the stream being closed once
     * and then all subsequent calls being ignored.
     *
     * @throws IOException if the stream could not be closed
     */
    public void close() throws IOException {
        synchronized(this){
        if (closed) return;
        closed = true; //mark it closed.
        }
        log.debug(Messages.getMessage("bStreamClosed", "" + streamNo));
        if (bytesRead < recordLength || moreChunks) {
            //We need get this off the stream.
            //Easy way to flush through the stream;
            byte[] readrest = new byte[1024 * 16];
            int bread = 0;

            do {
                bread = _read(readrest, 0, readrest.length);//should also close the orginal stream.
            }
            while (bread > -1);
        }
        dataPadLength -= readPad(dataPadLength);
    }

    // fixme: if mark is not supported, do we throw an exception here?
    /**
     * Mark the stream.
     * This is not supported.
     */
    public void mark(int readlimit) {//do nothing
    }

    public void reset() throws IOException {
        streamInError = new IOException(Messages.getMessage(
        "attach.bounday.mns"));
        throw streamInError;
    }

    public boolean markSupported() {
        return false;
    }

    public synchronized int available() throws IOException {
        if (null != streamInError) throw streamInError;
        int chunkAvail = (int) Math.min((long)
        Integer.MAX_VALUE, recordLength - bytesRead);

        int streamAvail = 0;

        try {
            streamAvail = is.available();
        } catch (IOException e) {
            streamInError = e;
            throw e;
        }

        if (chunkAvail == 0 && moreChunks && (12 + dataPadLength)
          <= streamAvail) {
            dataPadLength -= readPad(dataPadLength);
            readHeader(true);
            return available();
        }
        return  Math.min(streamAvail, chunkAvail);
    }

    protected void finalClose() throws IOException {
       try{
         theEnd = true;
         if(null != is) is.close();
       }finally{
         is= null;
       }
    }
}
