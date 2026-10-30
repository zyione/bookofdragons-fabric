package net.magister.liberanimalium.fauna.know;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.HeardSound;
import net.magister.liberanimalium.fauna.api.SensedItem;
import net.magister.liberanimalium.fauna.api.SpeciesProfile;
import net.magister.liberanimalium.fauna.identity.Temper;
import net.minecraft.world.phys.Vec3;

public interface FacultyContext {
   AnimalHandle handle();

   Temper temper();

   long now();

   SpeciesProfile species();

   List<HeardSound> mysteries();

   <T extends Record> List<T> stimuli(Class<T> var1);

   List<SensedItem> items();

   Collection<Representation> known();

   Vec3 estimate(Representation var1);

   double reach();

   float space();

   <T extends Faculty> Optional<T> faculty(Class<T> var1);
}
