package net.magister.liberanimalium.qte.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.qte.QteRenderState;
import net.magister.liberanimalium.qte.QteType;

public final class ClientQteState {
   private static final Map<Integer, QteRenderState> BY_ENTITY = new ConcurrentHashMap<>();

   private ClientQteState() {
   }

   public static void set(int entityId, QteRenderState state) {
      BY_ENTITY.put(entityId, state);
   }

   public static void clear(int entityId) {
      BY_ENTITY.remove(entityId);
   }

   @Nullable
   public static QteRenderState get(int entityId) {
      return BY_ENTITY.get(entityId);
   }

   public static boolean tameCircleActiveFor(int entityId) {
      QteRenderState state = BY_ENTITY.get(entityId);
      return state != null && state.type() == QteType.TAME_CIRCLE;
   }
}
