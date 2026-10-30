package net.magister.liberanimalium.chassis.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;

public class FaunaComponentRegistry {
   private final FaunaBase host;
   private final List<IFaunaComponent> components = new ArrayList<>();
   private final Map<Class<? extends IFaunaComponent>, IFaunaComponent> componentMap = new HashMap<>();

   public FaunaComponentRegistry(FaunaBase host) {
      this.host = host;
   }

   public <T extends IFaunaComponent> void register(T component) {
      this.components.add(component);
      this.componentMap.put((Class<? extends IFaunaComponent>)component.getClass(), component);
      this.indexHierarchy(component.getClass(), component);
      component.onAttach(this.host);
   }

   private void indexHierarchy(Class<?> clazz, IFaunaComponent component) {
      if (clazz != null && clazz != Object.class) {
         for (Class<?> iface : clazz.getInterfaces()) {
            if (IFaunaComponent.class.isAssignableFrom(iface) && iface != IFaunaComponent.class) {
               this.componentMap.put((Class<? extends IFaunaComponent>)iface, component);
               this.indexHierarchy(iface, component);
            }
         }

         this.indexHierarchy(clazz.getSuperclass(), component);
      }
   }

   public <T extends IFaunaComponent> T get(Class<T> componentClass) {
      return (T)this.componentMap.get(componentClass);
   }

   public <T extends IFaunaComponent> List<T> getAll(Class<T> componentClass) {
      List<T> result = new ArrayList<>();

      for (IFaunaComponent c : this.components) {
         if (componentClass.isInstance(c)) {
            result.add((T)c);
         }
      }

      return result;
   }

   public void tickServer() {
      for (IFaunaComponent component : this.components) {
         component.serverTick();
      }
   }

   public void tickClient() {
      for (IFaunaComponent component : this.components) {
         component.clientTick();
      }
   }

   public void saveAll(CompoundTag tag) {
      for (IFaunaComponent component : this.components) {
         component.save(tag);
      }
   }

   public void loadAll(CompoundTag tag) {
      for (IFaunaComponent component : this.components) {
         component.load(tag);
      }
   }

   public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
      for (IFaunaComponent component : this.components) {
         component.onSyncedDataUpdated(key);
      }
   }
}
