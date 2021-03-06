/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package es.gob.fire.logs.handlers;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.Flushable;
import java.io.Writer;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import es.gob.fire.logs.ExtHandler;

/**
 * A handler which writes to any {@code Writer}.
 */
public class WriterHandler extends ExtHandler {

    protected final Object outputLock = new Object();
    private Writer writer;

    /** {@inheritDoc} */
    @Override
	protected void doPublish(final LogRecord record) {
        final String formatted;
        final Formatter formatter = getFormatter();
        try {
            formatted = formatter.format(record);
        }
        catch (final Exception ex) {
            reportError("Formatting error", ex, ErrorManager.FORMAT_FAILURE);
            return;
        }
        if (formatted.length() == 0) {
            // nothing to write; don't bother
            return;
        }
        try {
            synchronized (this.outputLock) {
                if (this.writer == null) {
                    return;
                }
                preWrite(record);
                final Writer writer = this.writer;
                if (writer == null) {
                    return;
                }
                writer.write(formatted);
                // only flush if something was written
                super.doPublish(record);
            }
        } catch (final Exception ex) {
            reportError("Error writing log message", ex, ErrorManager.WRITE_FAILURE);
            return;
        }
    }

    /**
     * Execute any pre-write policy, such as file rotation.  The write lock is held during this method, so make
     * it quick.  The default implementation does nothing.
     *
     * @param record the record about to be logged
     */
    protected void preWrite(final LogRecord record) {
        // do nothing by default
    }

    /**
     * Set the writer.  The writer will then belong to this handler; when the handler is closed or a new writer is set,
     * this writer will be closed.
     *
     * @param writer the new writer, or {@code null} to disable logging
     */
    public void setWriter(final Writer writer) {
        checkAccess(this);
        Writer oldWriter = null;
        boolean ok = false;
        try {
            synchronized (this.outputLock) {
                oldWriter = this.writer;
                if (oldWriter != null) {
                    writeTail(oldWriter);
                    safeFlush(oldWriter);
                }
                if (writer != null) {
                    writeHead(this.writer = new BufferedWriter(writer));
                } else {
                    this.writer = null;
                }
                ok = true;
            }
        } finally {
            safeClose(oldWriter);
            if (! ok) {
				safeClose(writer);
			}
        }
    }

    private void writeHead(final Writer writer) {
        try {
            final Formatter formatter = getFormatter();
            if (formatter != null) {
				writer.write(formatter.getHead(this));
			}
        }
        catch (final Exception e) {
            reportError("Error writing section header", e, ErrorManager.WRITE_FAILURE);
        }
    }

    private void writeTail(final Writer writer) {
        try {
            final Formatter formatter = getFormatter();
            if (formatter != null) {
				writer.write(formatter.getTail(this));
			}
        }
        catch (final Exception ex) {
            reportError("Error writing section tail", ex, ErrorManager.WRITE_FAILURE);
        }
    }

    /**
     * Flush this logger.
     */
    @Override
	public void flush() {
        // todo - maybe this synch is not really needed... if there's a perf detriment, drop it
        synchronized (this.outputLock) {
            safeFlush(this.writer);
        }
        super.flush();
    }

    /**
     * Close this logger.
     *
     * @throws SecurityException if the caller does not have sufficient permission
     */
    @Override
	public void close() throws SecurityException {
        checkAccess(this);
        setWriter(null);
        super.close();
    }

    /**
     * Safely close the resource, reporting an error if the close fails.
     *
     * @param c the resource
     */
    protected void safeClose(final Closeable c) {
        try {
            if (c != null) {
				c.close();
			}
        }
        catch (final Exception e) {
            reportError("Error closing resource", e, ErrorManager.CLOSE_FAILURE);
        }
        catch (final Throwable ignored) {}
    }

    void safeFlush(final Flushable f) {
        try {
            if (f != null) {
				f.flush();
			}
        }
        catch (final Exception e) {
            reportError("Error on flush", e, ErrorManager.FLUSH_FAILURE);
        }
        catch (final Throwable ignored) {}
    }
}
