package net.magister.liberanimalium.mixin;

import java.nio.ByteBuffer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.magister.liberanimalium.platform.client.BulkVertexUpload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BufferBuilder.class)
public abstract class BulkVertexUploadMixin implements BulkVertexUpload {
    @Shadow private ByteBuffer buffer;
    @Shadow private int nextElementByte;
    @Shadow private int vertices;
    @Shadow private VertexFormat format;
    @Shadow private boolean building;
    @Shadow protected abstract void ensureCapacity(int bytes);
    public void liberanimalium$upload(ByteBuffer data) {
        if (!building) throw new IllegalStateException("Buffer is not building");
        ByteBuffer source = data.duplicate();
        int bytes = source.remaining();
        int stride = format.getVertexSize();
        if (bytes % stride != 0) throw new IllegalArgumentException("Incomplete vertex data");
        ensureCapacity(bytes);
        ByteBuffer destination = buffer.duplicate();
        destination.position(nextElementByte);
        destination.put(source);
        nextElementByte += bytes;
        vertices += bytes / stride;
    }
}
