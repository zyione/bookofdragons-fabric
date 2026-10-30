package net.magister.liberanimalium.roost;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

final class RoostNetwork {
   static final Comparator<BlockPos> CONTROLLER_ORDER = Comparator.<BlockPos>comparingInt(p -> p.getX()).thenComparingInt(p -> p.getZ());

   private RoostNetwork() {
   }

   @Nullable
   private static RoostBlockEntity at(ServerLevel level, BlockPos pos) {
      return level.isLoaded(pos) && level.getBlockEntity(pos) instanceof RoostBlockEntity roost ? roost : null;
   }

   static void placed(ServerLevel level, BlockPos pos, Block block, int maxBlocks) {
      RoostBlockEntity self = at(level, pos);
      if (self != null) {
         List<RoostBlockEntity> roots = new ArrayList<>();

         for (Direction d : Plane.HORIZONTAL) {
            BlockPos next = pos.relative(d);
            if (level.getBlockState(next).is(block)) {
               RoostBlockEntity neighbour = at(level, next);
               if (neighbour != null) {
                  RoostBlockEntity root = neighbour.root();
                  if (!roots.contains(root)) {
                     roots.add(root);
                  }
               }
            }
         }

         if (!roots.isEmpty()) {
            int total = 1;

            for (RoostBlockEntity root : roots) {
               total += root.blockCount();
            }

            if (total <= maxBlocks) {
               Set<BlockPos> all = new LinkedHashSet<>();
               all.add(pos);

               for (RoostBlockEntity root : roots) {
                  all.addAll(root.blocks());
               }

               BlockPos controller = all.stream().min(CONTROLLER_ORDER).orElse(pos);
               RoostBlockEntity heir = roots.stream().max(Comparator.comparingInt(RoostBlockEntity::blockCount)).orElseThrow();
               CompoundTag state = heir.saveState();
               RoostBlockEntity next = at(level, controller);
               if (next != null) {
                  for (BlockPos p : all) {
                     RoostBlockEntity e = at(level, p);
                     if (e != null && !p.equals(controller)) {
                        e.becomeMemberOf(controller);
                     }
                  }

                  next.becomeController(all);
                  if (next != heir) {
                     next.loadState(state);
                  }

                  for (RoostBlockEntity root : roots) {
                     if (root != heir) {
                        next.absorbOccupants(root);
                     }
                  }

                  next.trimToSlots();
                  repoint(level, next);
                  next.blockChanged(level, pos, true);
               }
            }
         }
      }
   }

   private static void repoint(ServerLevel level, RoostBlockEntity root) {
      for (UUID id : root.occupants()) {
         if (level.getEntity(id) instanceof FaunaBase creature) {
            creature.setRoostPosition(root.getBlockPos());
         }
      }
   }

   static void removing(ServerLevel level, BlockPos pos) {
      RoostBlockEntity self = at(level, pos);
      if (self != null) {
         RoostBlockEntity root = self.root();
         Set<BlockPos> remaining = new LinkedHashSet<>(root.blocks());
         remaining.remove(pos);
         if (!remaining.isEmpty()) {
            CompoundTag state = root.saveState();
            List<Set<BlockPos>> pieces = pieces(remaining);
            Set<BlockPos> heirPiece = null;

            for (Set<BlockPos> piece : pieces) {
               if (!root.getBlockPos().equals(pos) && piece.contains(root.getBlockPos())) {
                  heirPiece = piece;
               }
            }

            if (heirPiece == null) {
               for (Set<BlockPos> piece : pieces) {
                  if (heirPiece == null
                     || piece.size() > heirPiece.size()
                     || piece.size() == heirPiece.size()
                        && CONTROLLER_ORDER.compare(piece.stream().min(CONTROLLER_ORDER).orElseThrow(), heirPiece.stream().min(CONTROLLER_ORDER).orElseThrow())
                           < 0) {
                     heirPiece = piece;
                  }
               }
            }

            for (Set<BlockPos> piece : pieces) {
               BlockPos controller = piece.stream().min(CONTROLLER_ORDER).orElseThrow();
               RoostBlockEntity next = at(level, controller);
               if (next != null) {
                  for (BlockPos p : piece) {
                     RoostBlockEntity e = at(level, p);
                     if (e != null && !p.equals(controller)) {
                        e.becomeMemberOf(controller);
                     }
                  }

                  next.becomeController(piece);
                  if (piece == heirPiece) {
                     if (next != root) {
                        next.loadState(state);
                     }

                     next.trimToSlots();
                     repoint(level, next);
                     next.blockChanged(level, pos, false);
                  } else {
                     next.loadState(fresh(state));
                  }
               }
            }
         }
      }
   }

   private static CompoundTag fresh(CompoundTag state) {
      CompoundTag out = new CompoundTag();
      if (state.hasUUID("PlacedBy")) {
         out.putUUID("PlacedBy", state.getUUID("PlacedBy"));
      }

      return out;
   }

   static List<Set<BlockPos>> pieces(Set<BlockPos> blocks) {
      List<Set<BlockPos>> out = new ArrayList<>();
      Set<BlockPos> seen = new HashSet<>();

      for (BlockPos start : blocks) {
         if (seen.add(start)) {
            Set<BlockPos> piece = new LinkedHashSet<>();
            Queue<BlockPos> queue = new ArrayDeque<>();
            queue.add(start);
            piece.add(start);

            while (!queue.isEmpty()) {
               BlockPos at = queue.poll();

               for (Direction d : Plane.HORIZONTAL) {
                  BlockPos next = at.relative(d);
                  if (blocks.contains(next) && seen.add(next)) {
                     piece.add(next);
                     queue.add(next);
                  }
               }
            }

            out.add(piece);
         }
      }

      return out;
   }
}
