package net.magister.liberanimalium.platform.net;
import net.minecraft.network.FriendlyByteBuf;

public final class PacketValues {
    public static float finiteFloat(FriendlyByteBuf buffer) {
        float value = buffer.readFloat();
        if (!Float.isFinite(value)) throw new IllegalArgumentException("Non-finite control value");
        return value;
    }
    public static double finiteDouble(FriendlyByteBuf buffer) {
        double value = buffer.readDouble();
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Non-finite control coordinate");
        return value;
    }
}
