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

import org.apache.axis.InternalException;
import org.apache.axis.MessageContext;
import org.apache.axis.components.logger.LogFactory;
import org.apache.axis.utils.Messages;
import org.apache.commons.logging.Log;

import java.io.File;
import java.io.BufferedInputStream;

/**
 * This class allows small attachments to be cached in memory, while large ones are
 * cached out.  It implements a Java Activiation Data source interface.
 *
 * @author Rick Rineholt
 */
public class ManagedMemoryDataSource implements javax.activation.DataSource {

    /** Field log           */
    protected static Log log =
         LogFactory.getLog(ManagedMemoryDataSource.class.getName());

    /**
     * The content type. This defaults to
     * <code>application/octet-stream</code>.
     */
    protected String contentType = "application/octet-stream";

    /** The incoming source stream. */
    java.io.InputStream ss = null;

    /** Field MIN_MEMORY_DISK_CACHED           */
    public static final int MIN_MEMORY_DISK_CACHED = -1;

    /** Field MAX_MEMORY_DISK_CACHED           */
    public static final int MAX_MEMORY_DISK_CACHED = 16 * 1024;

    /** Field maxCached           */
    protected int maxCached = MAX_MEMORY_DISK_CACHED;       // max in memory cached. Default.

    // If set the file the disk is cached to.

    /** Field diskCacheFile           */
    protected java.io.File diskCacheFile = null;

    // A list of open input Streams.

    /** Field readers           */
    protected java.util.WeakHashMap readers = new java.util.WeakHashMap();

    /**
     * Flag to show if the resources behind this have been deleted.
     */
    protected boolean deleted =
            false;

    // Memory is allocated in these size chunks.

    /** Field READ_CHUNK_SZ           */
    public static final int READ_CHUNK_SZ = 32 * 1024;

    /** Field debugEnabled           */
    protected boolean debugEnabled = false;    // Log debugging if true.

    // Should not be called;

    /**
     * Constructor ManagedMemoryDataSource.
     */
    protected ManagedMemoryDataSource() {
    }

    /**
     * Create a new boundary stream.
     *
     * @param ss is the source input stream that is used to create this data source.
     * @param maxCached  This is the max memory that is to be used to cache the data.
     * @param contentType the mime type for this data stream.
     *   by buffering you can some effiency in searching.
     *
     * @throws java.io.IOException
     */
    public ManagedMemoryDataSource(
            java.io.InputStream ss, int maxCached, String contentType)
            throws java.io.IOException {
        this(ss, maxCached, contentType, false);
    }

    /**
     * Create a new boundary stream.
     *
     * @param ss is the source input stream that is used to create this data source.
     * @param maxCached  This is the max memory that is to be used to cache the data.
     * @param contentType the mime type for this data stream.
     *   by buffering you can some effiency in searching.
     * @param readall if true will read in the whole source.
     *
     * @throws java.io.IOException
     */
    public ManagedMemoryDataSource(
            java.io.InputStream ss, int maxCached, String contentType, boolean readall)
            throws java.io.IOException {

        if(ss instanceof BufferedInputStream) {
            this.ss = ss;
        } else {
            this.ss = new BufferedInputStream(ss);
        }
        this.maxCached = maxCached;

        if ((null != contentType) && (contentType.length() != 0)) {
            this.contentType = contentType;
        }

        if (maxCached < MIN_MEMORY_DISK_CACHED) {
            throw new IllegalArgumentException(
                    Messages.getMessage("badMaxCached", "" + maxCached));
        }

        if (log.isDebugEnabled()) {
            debugEnabled = true;    // Logging should be initialized by time;
        }

        // for now read all in to disk.
        if (readall) {
            byte[] readbuffer = new byte[READ_CHUNK_SZ];
            int read = 0;

            do {
                read = ss.read(readbuffer);

                if (read > 0) {
                    write(readbuffer, read);
                }
            } while (read > -1);

            close();
        }
    }

    /* javax.activation.Interface DataSource implementation */

    /**
     * This method returns the MIME type of the data in the form of a string.
     * @return The mime type.
     */
    public java.lang.String getContentType() {
        return contentType;
    }

    /**
     * This method returns an InputStream representing the the data and throws the appropriate exception if it can not do so.
     * @return the java.io.InputStream for the data source.
     *
     * @throws java.io.IOException
     */
    public synchronized java.io.InputStream getInputStream()
            throws java.io.IOException {

        /*
         * if (memorybuflist == null) {
         *   return  new java.io.FileInputStream(diskCacheFile);
         * }
         * else
         */
        return new Instream();    // Return the memory held stream.
    }

    /**
     * This will flush any memory source to disk and
     * provide the name of the file if desired.
     *
     * @return the name of the file of the stream
     */
    public java.lang.String getName() {

        String ret = null;

        try {
            flushToDisk();

            if (diskCacheFile != null) {
                ret = diskCacheFile.getAbsolutePath();
            }
        } catch (Exception e) {
            diskCacheFile = null;
        }

        return ret;
    }

    /**
     * This method returns an OutputStream where the data can be written and
     * throws the appropriate exception if it can not do so.
     * NOT SUPPORTED, not need for axis, data sources are create by constructors.
     *
     *
     * @return always <code>null</code>
     *
     * @throws java.io.IOException
     */
    public java.io.OutputStream getOutputStream() throws java.io.IOException {
        return null;
    }

    /** The linked list to hold the in memory buffers. */
    protected java.util.LinkedList memorybuflist =
            new java.util.LinkedList();

    /** Hold the last memory buffer. */
    protected byte[] currentMemoryBuf = null;

    /** The number of bytes written to the above buffer. */
    protected int currentMemoryBufSz =
            0;

    /** The total size in bytes in this data source. */
    protected long totalsz = 0;

    /** This is the cached disk stream. */
    protected java.io.BufferedOutputStream cachediskstream =
            null;

    /** If true the source input stream is now closed. */
    protected boolean closed = false;

    /**
     * Write bytes to the stream.
     *
     * @param data all bytes of this array are written to the stream
     * @throws java.io.IOException if there was a problem writing the data
     */
    protected void write(byte[] data) throws java.io.IOException {
        write(data, data.length);
    }

    /**
     * This method is a low level write.
     * Note it is designed to in the future to allow streaming to both memory
     *  AND to disk simultaneously.
     *
     * @param data
     * @param length
     *
     * @throws java.io.IOException
     */
    protected synchronized void write(byte[] data, int length)
            throws java.io.IOException {

        if (closed) {
            throw new java.io.IOException(Messages.getMessage("streamClosed"));
        }

        int writesz = length;
        int byteswritten = 0;

        if ((null != memorybuflist)
                && (totalsz + writesz > maxCached)) {    // Cache to disk.
            if (null == cachediskstream) {               // Need to create a disk cache
                flushToDisk();
            }
        }

        if (memorybuflist != null) {    // Can write to memory.
            do {
                if (null == currentMemoryBuf) {
                    currentMemoryBuf = new byte[READ_CHUNK_SZ];
                    currentMemoryBufSz = 0;

                    memorybuflist.add(currentMemoryBuf);
                }

                // bytes to write is the min. between the remaining bytes and what is left in this buffer.
                int bytes2write = Math.min((writesz - byteswritten),
                        (currentMemoryBuf.length
                        - currentMemoryBufSz));

                // copy the data.
                System.arraycopy(data, byteswritten, currentMemoryBuf,
                        currentMemoryBufSz, bytes2write);

                byteswritten += bytes2write;
                currentMemoryBufSz += bytes2write;

                if (byteswritten
                        < writesz) {    // only get more if we really need it.
                    currentMemoryBuf = new byte[READ_CHUNK_SZ];
                    currentMemoryBufSz = 0;

                    memorybuflist.add(currentMemoryBuf);    // add it to the chain.
                }
            } while (byteswritten < writesz);
        }

        if (null != cachediskstream) {    // Write to the out going stream.
            cachediskstream.write(data, 0, length);
        }

        totalsz += writesz;

        return;
    }

    /**
     * This method is a low level write.
     * Close the stream.
     *
     * @throws java.io.IOException
     */
    protected synchronized void close() throws java.io.IOException {

        if (!closed) {
            closed = true;                    // Markit as closed.

            if (null != cachediskstream) {    // close the disk cache.
                cachediskstream.close();

                cachediskstream = null;
            }

            if (null != memorybuflist) {      // There is a memory buffer.
                if (currentMemoryBufSz > 0) {
                    byte[] tmp =
                            new byte[currentMemoryBufSz];    // Get the last buffer and make it the sizeof the actual data.

                    System.arraycopy(currentMemoryBuf, 0, tmp, 0,
                            currentMemoryBufSz);
                    memorybuflist.set(
                            memorybuflist.size() - 1,
                            tmp);                 // Now replace the last buffer with this size.
                }

                currentMemoryBuf = null;      // No need for this anymore.
            }
        }
    }

    protected void finalize() throws Throwable {

        if (null != cachediskstream) {    // close the disk cache.
            cachediskstream.close();

            cachediskstream = null;
        }
    }

    /**
     * Routine to flush data to disk if is in memory.
     *
     * @throws java.io.IOException
     * @throws java.io.FileNotFoundException
     */
    protected void flushToDisk()
            throws java.io.IOException, java.io.FileNotFoundException {

        java.util.LinkedList ml = memorybuflist;

        log.debug(Messages.getMessage("maxCached", "" + maxCached,
                "" + totalsz));

        if (ml != null) {
            if (null == cachediskstream) {    // Need to create a disk cache
                try {
                    MessageContext mc = MessageContext.getCurrentContext();
                    String attdir = (mc == null)
                            ? null
                            : mc.getStrProp(
                                    MessageContext.ATTACHMENTS_DIR);

                    diskCacheFile = java.io.File.createTempFile("Axis", ".att",
                            (attdir == null)
                            ? null
                            : new File(
                                    attdir));

                    if(log.isDebugEnabled()) {
                        log.debug(
                            Messages.getMessage(
                                    "diskCache", diskCacheFile.getAbsolutePath()));
                     }

                    cachediskstream = new java.io.BufferedOutputStream(
                            new java.io.FileOutputStream(diskCacheFile));

                    int listsz = ml.size();

                    // Write out the entire memory held store to disk.
                    for (java.util.Iterator it = ml.iterator();
                         it.hasNext();) {
                        byte[] rbuf = (byte[]) it.next();
                        int bwrite = (listsz-- == 0)
                                ? currentMemoryBufSz
                                : rbuf.length;

                        cachediskstream.write(rbuf, 0, bwrite);

                        if (closed) {
                            cachediskstream.close();

                            cachediskstream = null;
                        }
                    }

                    memorybuflist = null;
                } catch (java.lang.SecurityException se) {
                    diskCacheFile = null;
                    cachediskstream = null;
                    maxCached = java.lang.Integer.MAX_VALUE;

                    log.info(Messages.getMessage("nodisk00"), se);
                }
            }
        }
    }

    public synchronized boolean delete() {

        boolean ret = false;

        deleted = true;

        memorybuflist = null;

        if (diskCacheFile != null) {
            if (cachediskstream != null) {
                try {
                    cachediskstream.close();
                } catch (Exception e) {
                }

                cachediskstream = null;
            }

            Object[] array = readers.keySet().toArray();
            for (int i = 0; i < array.length; i++) {
                Instream stream = (Instream) array[i];
                if (null != stream) {
                    try {
                        stream.close();
                    } catch (Exception e) {
                    }
                }
            }
            readers.clear();

            try {
                diskCacheFile.delete();

                ret = true;
            } catch (Exception e) {

                // Give it our best shot.
                diskCacheFile.deleteOnExit();
            }
        }


        return ret;
    }

    // inner classes cannot have static declarations...

    /** Field is_log           */
    protected static Log is_log =
        LogFactory.getLog(Instream.class.getName());

    /**
     * Inner class to handle getting an input stream to this data source
     *  Handles creating an input stream to the source.
     */
    private class Instream extends java.io.InputStream {

        /** bytes read. */
        protected long bread = 0;

        /** The real stream. */
        java.io.FileInputStream fin = null;

        /** The position in the list were we are reading from. */
        int currentIndex =
                0;

        /** the buffer we are currently reading from. */
        byte[] currentBuf = null;

        /** The current position in there. */
        int currentBufPos = 0;

        /** The read stream has been closed. */
        boolean readClosed = false;

        /**
         * Constructor Instream.
         *
         * @throws java.io.IOException if the Instream could not be created or
         *              if the data source has been deleted
         */
        protected Instream() throws java.io.IOException {

            if (deleted) {
                throw new java.io.IOException(
                        Messages.getMessage("resourceDeleted"));
            }

            readers.put(this, null);
        }

        /**
         * Query for the number of bytes available for reading.
         *
         * @return the number of bytes left
         *
         * @throws java.io.IOException if this stream is not in a state that
         *              supports reading
         */
        public int available() throws java.io.IOException {

            if (deleted) {
                throw new java.io.IOException(
                        Messages.getMessage("resourceDeleted"));
            }

            if (readClosed) {
                throw new java.io.IOException(
                        Messages.getMessage("streamClosed"));
            }

            int ret = new Long(Math.min(Integer.MAX_VALUE, totalsz - bread)).intValue();

            if (debugEnabled) {
                is_log.debug("available() = " + ret + ".");
            }

            return ret;
        }

        /**
         * Read a byte from the stream.
         *
         * @return byte read or -1 if no more data.
         *
         * @throws java.io.IOException
         */
        public int read() throws java.io.IOException {

            synchronized (ManagedMemoryDataSource.this) {
                byte[] retb = new byte[1];
                int br = read(retb, 0, 1);

                if (br == -1) {
                    return -1;
                }
                return 0xFF & retb[0];
            }
        }

        /**
         * Not supported.
         *
         * @return
         */
        public boolean markSupported() {

            if (debugEnabled) {
                is_log.debug("markSupported() = " + false + ".");
            }

            return false;
        }

        /**
         * Not supported.
         *
         * @param readlimit
         */
        public void mark(int readlimit) {

            if (debugEnabled) {
                is_log.debug("mark()");
            }
        }

        /**
         * Not supported.
         *
         * @throws java.io.IOException
         */
        public void reset() throws java.io.IOException {

            if (debugEnabled) {
                is_log.debug("reset()");
            }

            throw new java.io.IOException(Messages.getMessage("noResetMark"));
        }

        public long skip(long skipped) throws java.io.IOException {

            if (debugEnabled) {
                is_log.debug("skip(" + skipped + ").");
            }

            if (deleted) {
                throw new java.io.IOException(
                        Messages.getMessage("resourceDeleted"));
            }

            if (readClosed) {
                throw new java.io.IOException(
                        Messages.getMessage("streamClosed"));
            }

            if (skipped < 1) {
                return 0;    // nothing to skip.
            }

            synchronized (ManagedMemoryDataSource.this) {
                skipped = Math.min(skipped,
                        totalsz
                        - bread);    // only skip what we've read.

                if (skipped == 0) {
                    return 0;
                }

                java.util.List ml = memorybuflist;    // hold the memory list.
                int bwritten = 0;

                if (ml != null) {
                    if (null == currentBuf) {    // get the buffer we need to read from.
                        currentBuf = (byte[]) ml.get(currentIndex);
                        currentBufPos = 0;    // start reading from the begining.
                    }

                    do {
                        long bcopy = Math.min(currentBuf.length
                                - currentBufPos,
                                skipped - bwritten);

                        bwritten += bcopy;
                        currentBufPos += bcopy;

                        if (bwritten < skipped) {
                            currentBuf = (byte[]) ml.get(++currentIndex);
                            currentBufPos = 0;
                        }
                    } while (bwritten < skipped);
                }

                if (null != fin) {
                    fin.skip(skipped);
                }

                bread += skipped;
            }

            if (debugEnabled) {
                is_log.debug("skipped " + skipped + ".");
            }

            return skipped;
        }

        public int read(byte[] b, int off, int len) throws java.io.IOException {

            if (debugEnabled) {
                is_log.debug(this.hashCode() + " read(" + off + ", " + len
                        + ")");
            }

            if (deleted) {
                throw new java.io.IOException(
                        Messages.getMessage("resourceDeleted"));
            }

            if (readClosed) {
                throw new java.io.IOException(
                        Messages.getMessage("streamClosed"));
            }

            if (b == null) {
                throw new InternalException(Messages.getMessage("nullInput"));
            }

            if (off < 0) {
                throw new IndexOutOfBoundsException(
                        Messages.getMessage("negOffset", "" + off));
            }

            if (len < 0) {
                throw new IndexOutOfBoundsException(
                        Messages.getMessage("length", "" + len));
            }

            if (len + off > b.length) {
                throw new IndexOutOfBoundsException(
                        Messages.getMessage("writeBeyond"));
            }

            if (len == 0) {
                return 0;
            }

            int bwritten = 0;

            synchronized (ManagedMemoryDataSource.this) {
                if (bread == totalsz) {
                    return -1;
                }

                java.util.List ml = memorybuflist;
                
                long longlen = len;
                longlen = Math.min(
                        longlen,
                        totalsz
                        - bread);    // Only return the number of bytes in the data store that is left.
                len = new Long(longlen).intValue();
                
                if (debugEnabled) {
                    is_log.debug("len = " + len);
                }

                if (ml != null) {
                    if (null == currentBuf) {    // Get the buffer we need to read from.
                        currentBuf = (byte[]) ml.get(currentIndex);
                        currentBufPos = 0;    // New buffer start from the begining.
                    }

                    do {

                        // The bytes to copy, the minimum of the bytes left in this buffer or bytes remaining.
                        int bcopy = Math.min(currentBuf.length - currentBufPos,
                                len - bwritten);

                        // Copy the data.
                        System.arraycopy(currentBuf, currentBufPos, b,
                                off + bwritten, bcopy);

                        bwritten += bcopy;
                        currentBufPos += bcopy;

                        if (bwritten < len) {    // Get the next buffer.
                            currentBuf = (byte[]) ml.get(++currentIndex);
                            currentBufPos = 0;
                        }
                    } while (bwritten < len);
                }

                if ((bwritten == 0) && (null != diskCacheFile)) {
                    if (debugEnabled) {
                        is_log.debug(Messages.getMessage("reading", "" + len));
                    }

                    if (null == fin) {           // we are now reading from disk.
                        if (debugEnabled) {
                            is_log.debug(
                                    Messages.getMessage(
                                            "openBread",
                                            diskCacheFile.getCanonicalPath()));
                        }

                        if (debugEnabled) {
                            is_log.debug(Messages.getMessage("openBread",
                                    "" + bread));
                        }

                        fin = new java.io.FileInputStream(diskCacheFile);

                        if (bread > 0) {
                            fin.skip(bread);     // Skip what we've read so far.
                        }
                    }

                    if (cachediskstream != null) {
                        if (debugEnabled) {
                            is_log.debug(Messages.getMessage("flushing"));
                        }

                        cachediskstream.flush();
                    }

                    if (debugEnabled) {
                        is_log.debug(Messages.getMessage("flushing"));
                        is_log.debug("len=" + len);
                        is_log.debug("off=" + off);
                        is_log.debug("b.length=" + b.length);
                    }

                    bwritten = fin.read(b, off, len);
                }

                if (bwritten > 0) {
                    bread += bwritten;
                }
            }

            if (debugEnabled) {
                is_log.debug(this.hashCode()
                        + Messages.getMessage("read", "" + bwritten));
            }

            return bwritten;
        }

        /**
         * close the stream.
         *
         * @throws java.io.IOException
         */
        public synchronized void close() throws java.io.IOException {

            if (debugEnabled) {
                is_log.debug("close()");
            }

            if (!readClosed) {
                readers.remove(this);

                readClosed = true;

                if (fin != null) {
                    fin.close();
                }

                fin = null;
            }
        }

        protected void finalize() throws Throwable {
            close();
        }
    }                                          // endof innerclass Instream

    // Used to test.

    /**
     * Method main
     *
     * @param arg
     */
    public static void main(String arg[]) {    // test

        try {
            String readFile = arg[0];
            String writeFile = arg[1];
            java.io.FileInputStream ss =
                    new java.io.FileInputStream(readFile);
            ManagedMemoryDataSource ms =
                    new ManagedMemoryDataSource(ss, 1024 * 1024, "foo/data", true);
            javax.activation.DataHandler dh =
                    new javax.activation.DataHandler(ms);
            java.io.InputStream is = dh.getInputStream();
            java.io.FileOutputStream fo =
                    new java.io.FileOutputStream(writeFile);
            byte[] buf = new byte[512];
            int read = 0;

            do {
                read = is.read(buf);

                if (read > 0) {
                    fo.write(buf, 0, read);
                }
            } while (read > -1);

            fo.close();
            is.close();
        } catch (java.lang.Exception e) {
            log.error(Messages.getMessage("exception00"), e);
        }
    }

    /**
     * get the filename of the content if it is cached to disk.
     * @return file object pointing to file, or null for memory-stored content
     */
    public File getDiskCacheFile() {
        return diskCacheFile;
    }
}
