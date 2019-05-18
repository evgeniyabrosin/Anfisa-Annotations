package org.forome.annotation.annotator.input;

import org.forome.annotation.exception.ExceptionBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.zip.GZIPInputStream;

public class FileReaderIterator implements Iterator<String>, AutoCloseable {

    private final InputStream inputStream;
    private final BufferedReader bufferedReader;

    private String nextValue;

    public FileReaderIterator(Path pathVepJson) {
        this(getInputStream(pathVepJson), pathVepJson.getFileName().toString().endsWith(".gz"));
    }

    public FileReaderIterator(InputStream inputStream) {
        this(inputStream, false);
    }

    public FileReaderIterator(InputStream inputStream, boolean gzip) {
        this.inputStream = inputStream;
        if (gzip) {
            try {
                this.bufferedReader = new BufferedReader(new InputStreamReader(new GZIPInputStream(inputStream)));
            } catch (IOException e) {
                throw ExceptionBuilder.buildIOErrorException(e);
            }
        } else {
            this.bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        }

        nextValue = readNextValue();
    }

    private String readNextValue() {
        try {
            return bufferedReader.readLine();
        } catch (IOException e) {
            throw ExceptionBuilder.buildIOErrorException(e);
        }
    }

    @Override
    public boolean hasNext() {
        return (nextValue != null);
    }

    @Override
    public String next() {
        if (nextValue == null) {
            throw new NoSuchElementException();
        }

        String value = nextValue;
        nextValue = readNextValue();

        return value;
    }

    @Override
    public void close() throws IOException {
        bufferedReader.close();
        inputStream.close();
    }

    private static InputStream getInputStream(Path file) {
        try {
            return Files.newInputStream(file);
        } catch (IOException e) {
            throw ExceptionBuilder.buildIOErrorException(e);
        }
    }
}
