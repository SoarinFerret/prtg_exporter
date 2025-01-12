package org.infobip.prometheus.prtgexporter.prtg;

// special thanks to 'rees' from https://stackoverflow.com/a/52244937
// since this is from Stack Overflow in its entirety, this specific library
// shall be licensed under the CC-BY-SA, separate from the Apache2 license
// used for the rest of the project

import java.io.IOException;
import java.io.InputStream;

public class TokenReplacingStream extends InputStream {

    private final InputStream source;
    private final byte[] oldBytes;
    private final byte[] newBytes;
    private int tokenMatchIndex = 0;
    private int bytesIndex = 0;
    private boolean unwinding;
    private int mismatch;
    private int numberOfTokensReplaced = 0;

    public TokenReplacingStream(InputStream source, byte[] oldBytes, byte[] newBytes) {
        assert oldBytes.length > 0;
        this.source = source;
        this.oldBytes = oldBytes;
        this.newBytes = newBytes;
    }

    @Override
    public int read() throws IOException {

        if (unwinding) {
            if (bytesIndex < tokenMatchIndex) {
                return oldBytes[bytesIndex++];
            } else {
                bytesIndex = 0;
                tokenMatchIndex = 0;
                unwinding = false;
                return mismatch;
            }
        } else if (tokenMatchIndex == oldBytes.length) {
            if (bytesIndex == newBytes.length) {
                bytesIndex = 0;
                tokenMatchIndex = 0;
                numberOfTokensReplaced++;
            } else {
                return newBytes[bytesIndex++];
            }
        }

        int b = source.read();
        if (b == oldBytes[tokenMatchIndex]) {
            tokenMatchIndex++;
        } else if (tokenMatchIndex > 0) {
            mismatch = b;
            unwinding = true;
        } else {
            return b;
        }

        return read();

    }

    @Override
    public void close() throws IOException {
        source.close();
    }

    public int getNumberOfTokensReplaced() {
        return numberOfTokensReplaced;
    }

}