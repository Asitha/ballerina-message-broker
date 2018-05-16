package io.ballerina.messaging.broker.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Converter used to convert the chunk sizes  less than a configured maximum.
 */
public class ChunkConverter {

    private int maxChunkSizeLimit;
    private final ContentChunkFactory contentChunkFactory;

    public ChunkConverter(int maxChunkSizeLimit, ContentChunkFactory contentChunkFactory) {
        this.maxChunkSizeLimit = maxChunkSizeLimit;
        this.contentChunkFactory = contentChunkFactory;
    }

    public List<ContentChunk> convert(List<ContentChunk> chunkList, long totalLength) {
        if (chunkList.isEmpty() || isChunksUnderLimit(chunkList)) {
            return chunkList;
        }

        ArrayList<ContentChunk> convertedChunks = new ArrayList<>();

        long pendingBytes = totalLength;
        long offset = 0;
        ContentReader contentReader = new ContentReader(chunkList);

        while (pendingBytes > 0) {
            long newChunkLength = Math.min(pendingBytes, maxChunkSizeLimit);
            ContentChunk newChunk = contentChunkFactory.getNewChunk(offset,
                                                                    contentReader.getNextBytes((int) newChunkLength));
            convertedChunks.add(newChunk);

            pendingBytes = pendingBytes - newChunkLength;
            offset = offset + newChunkLength;
        }

        return convertedChunks;
    }

    private boolean isChunksUnderLimit(List<ContentChunk> chunkList) {
        boolean allChunksUnderLimit = true;
        for (ContentChunk chunk : chunkList) {
            if (chunk.getBytes().length > maxChunkSizeLimit) {
                allChunksUnderLimit = false;
                break;
            }
        }
        return allChunksUnderLimit;
    }

}
