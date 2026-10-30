package net.magister.liberanimalium.body.anim.binding;

import net.magister.liberanimalium.anim.api.SignalView;
import net.magister.liberanimalium.body.state.FaunaStateContext;
import net.magister.liberanimalium.body.state.TransportMode;
import net.magister.liberanimalium.chassis.FaunaBase;

public final class StateSnapshot implements SignalView {
   private final TransportMode transport;
   private final String[] enums;
   private final boolean[] bools;
   private final float[] scalars;

   private StateSnapshot(TransportMode transport, String[] enums, boolean[] bools, float[] scalars) {
      this.transport = transport;
      this.enums = enums;
      this.bools = bools;
      this.scalars = scalars;
   }

   public static StateSnapshot from(FaunaBase body) {
      FaunaStateContext ctx = body.getStateContext();
      return new StateSnapshot(ctx.getTransportMode(), AnimSignals.captureEnums(body), AnimSignals.captureBools(body), AnimSignals.captureScalars(body));
   }

   static StateSnapshot of(String[] enums, boolean[] bools, float[] scalars) {
      return new StateSnapshot(TransportMode.GROUNDED, enums, bools, scalars);
   }

   public TransportMode transport() {
      return this.transport;
   }

   @Override
   public String enumValue(String key) {
      return this.getEnumValue(key);
   }

   @Override
   public Boolean boolValue(String key) {
      return this.getBoolValue(key);
   }

   @Override
   public Float scalarValue(String key) {
      return this.getScalarValue(key);
   }

   public String getEnumValue(String key) {
      int i = AnimSignals.enumIndex(key);
      return i < 0 ? null : this.enums[i];
   }

   public Boolean getBoolValue(String key) {
      int i = AnimSignals.boolIndex(key);
      return i < 0 ? null : this.bools[i];
   }

   public Float getScalarValue(String key) {
      int i = AnimSignals.scalarIndex(key);
      return i < 0 ? null : this.scalars[i];
   }
}
