package net.magister.liberanimalium.genetics.core;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import org.jetbrains.annotations.Nullable;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class SequenceStore {
   private static final int MAGIC = 1279345409;
   private static final int SALT = 16;
   private static final int NONCE = 12;
   private static final int TAG_BITS = 128;
   private static final String CIPHER = "AES/GCM/NoPadding";
   private final byte[] bytes;
   private final int dataStart;
   private final SecretKeySpec key;
   private final Map<String, SequenceStore.Entry> entries;

   private SequenceStore(byte[] bytes, int dataStart, SecretKeySpec key, Map<String, SequenceStore.Entry> entries) {
      this.bytes = bytes;
      this.dataStart = dataStart;
      this.key = key;
      this.entries = entries;
   }

   public static SequenceStore read(byte[] bytes, byte[] seed, String namespace) throws GeneralSecurityException, IOException {
      ByteBuffer in = ByteBuffer.wrap(bytes);
      if (in.remaining() >= 36 && in.getInt() == 1279345409) {
         byte[] salt = new byte[16];
         in.get(salt);
         byte[] nonce = new byte[12];
         in.get(nonce);
         int indexLength = in.getInt();
         int indexStart = in.position();
         if (indexLength >= 0 && indexLength <= in.remaining()) {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(seed, "HmacSHA256"));
            mac.update(salt);
            SecretKeySpec key = new SecretKeySpec(mac.doFinal(namespace.getBytes(StandardCharsets.UTF_8)), "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(2, key, new GCMParameterSpec(128, nonce));
            cipher.updateAAD(bytes, 0, 20);
            byte[] index = cipher.doFinal(bytes, indexStart, indexLength);
            Map<String, SequenceStore.Entry> entries = new HashMap<>();

            try (DataInputStream data = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(index)))) {
               int count = data.readInt();

               for (int i = 0; i < count; i++) {
                  String path = data.readUTF();
                  entries.put(path, new SequenceStore.Entry(data.readInt(), data.readInt(), data.readInt(), data.readBoolean()));
               }
            }

            return new SequenceStore(bytes, indexStart + indexLength, key, Map.copyOf(entries));
         } else {
            throw new IOException("the sealed asset index is cut short");
         }
      } else {
         throw new IOException("not a sealed asset container");
      }
   }

   public Set<String> paths() {
      return this.entries.keySet();
   }

   public boolean has(String path) {
      return this.entries.containsKey(path);
   }

   @Nullable
   public byte[] open(String path) {
      SequenceStore.Entry entry = this.entries.get(path);
      if (entry == null) {
         return null;
      }

      try {
         int start = this.dataStart + entry.offset();
         Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
         cipher.init(2, this.key, new GCMParameterSpec(128, this.bytes, start, 12));
         cipher.updateAAD(path.getBytes(StandardCharsets.UTF_8));
         byte[] body = cipher.doFinal(this.bytes, start + 12, entry.length() - 12);
         return entry.deflated() ? inflate(body, entry.plainLength()) : body;
      } catch (GeneralSecurityException | DataFormatException | IOException e) {
         throw new IllegalStateException("the sealed asset " + path + " does not open", e);
      }
   }

   private static byte[] inflate(byte[] body, int plainLength) throws DataFormatException, IOException {
      byte[] plain = new byte[plainLength];
      Inflater inflater = new Inflater();

      try {
         inflater.setInput(body);
         int read = 0;

         while (read < plainLength && !inflater.finished()) {
            int n = inflater.inflate(plain, read, plainLength - read);
            if (n == 0 && inflater.needsInput()) {
               break;
            }

            read += n;
         }

         if (read != plainLength) {
            throw new IOException("expected " + plainLength + " bytes, read " + read);
         }
      } finally {
         inflater.end();
      }

      return plain;
   }

   private record Entry(int offset, int length, int plainLength, boolean deflated) {
   }
}
