package org.nunn.gephiserver.server.system;

import java.io.IOException;
import java.io.Writer;

public class StringBuilderWriter extends Writer {
	
	private final StringBuilder	buf;
	
	public StringBuilderWriter() {
		this(1024);
	}
	
	public StringBuilderWriter(int capacity) {
		this(new StringBuilder(capacity));
	}
	
	public StringBuilderWriter(StringBuilder buf) {
		this.buf = buf;
	}

	public StringBuilder getBuf() {
		return buf;
	}
	
	@Override
	public void write(int c) throws IOException {
		buf.append(Character.toChars(c));
	}
	
	@Override
	public void write(char cbuf[]) throws IOException {
		buf.append(cbuf);
	}
	
	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		buf.append(cbuf, off, len);
	}
	
	@Override
	public void write(String str, int off, int len) throws IOException {
		buf.append(str.toCharArray(), off, len);
	}
	
	@Override
	public Writer append(CharSequence csq) throws IOException {
        buf.append(csq);
        return this;
    }
	
	@Override
	public Writer append(CharSequence csq, int start, int end) throws IOException {
		buf.append(csq, start, end);
		return this;
	}
	
	@Override
	public void flush() throws IOException {
		
	}
	
	@Override
	public void close() throws IOException {
		buf.trimToSize();
	}
	
	@Override
	public String toString() {
		return buf.toString();
	}
	
}
