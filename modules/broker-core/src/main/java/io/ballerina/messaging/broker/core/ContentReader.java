package io.ballerina.messaging.broker.core;

import java.util.List;

/**
 * Read content list as a composite chunk.
 */
class ContentReader {
    private List<ContentChunk> chunkList;
    private int currentChunkIndex = 0;
    private int srcReaderIndex = 0;

    ContentReader(List<ContentChunk> chunkList) {
        this.chunkList = chunkList;
    }

    byte[] getNextBytes(int size) {

        byte[] newContent = new byte[size];
        int bytesCopied = 0;
        while (bytesCopied < size) {
            ContentChunk currentChunk = chunkList.get(currentChunkIndex);
            byte[] srcContent = currentChunk.getBytes();
            int length = Math.min((size - bytesCopied), srcContent.length - srcReaderIndex);
            System.arraycopy(srcContent, srcReaderIndex, newContent, bytesCopied, length);
            bytesCopied += length;
            srcReaderIndex += length;
            if (srcReaderIndex >= srcContent.length) {
                jumpToNextChunk();
            }
        }
        return newContent;
    }

    private void jumpToNextChunk() {
        currentChunkIndex++;
        srcReaderIndex = 0;
    }
}
